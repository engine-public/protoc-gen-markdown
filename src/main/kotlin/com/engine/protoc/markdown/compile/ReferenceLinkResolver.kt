package com.engine.protoc.markdown.compile

import com.engine.protoc.markdown.ProtocGenMarkdown
import com.engine.protoc.util.enums.EnumDescriptorProtoWrapper
import com.engine.protoc.util.file.FileDescriptorProtoWrapper
import com.engine.protoc.util.message.DescriptorProtoWrapper
import org.commonmark.node.Link
import org.commonmark.parser.beta.LinkInfo
import org.commonmark.parser.beta.LinkProcessor
import org.commonmark.parser.beta.LinkResult
import org.commonmark.parser.beta.Scanner
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(ReferenceLinkResolver::class.java)

/**
 * Resolves CommonMark reference-link syntax inside proto leading comments to Markdown links
 * pointing at the heading anchors this generator already emits.  Both the shortcut form
 * (`[label]`) and the full form (`[display text][label]`) are recognized; in the full form
 * the bracketed label is the lookup key and the display text passes through unchanged.
 * Escaped brackets (`\[foo\]`) are honored — the parser never invokes the resolver for them,
 * so they survive as literal text.  Inline links (`[text](url)`) are left to the core
 * CommonMark processor.
 *
 * Constructed once per output document so the href shape (bare `#anchor` vs
 * `relative/path.md#anchor`) is bound to that document's filename.  Hooked into the parser
 * as a [LinkProcessor] (see [linkProcessor]); the per-comment anchor descriptor flows in
 * through `currentScope` which the caller sets around each `parser.parse(...)` call.
 *
 * The resolver indexes every type, field, enum value, and RPC reachable from the compile
 * scope.  Bare-name lookups consult both a global short-name index and a per-scope local
 * index built from the comment's anchor descriptor — so a comment on `message User` can
 * write `[name]` to find `User.name` without the explicit qualifier, and a comment on a
 * field of type `Foo` can write `[Foo]` to find that specific target type (disambiguating
 * against any other `Foo` defined elsewhere in the scope).  Qualified labels
 * (`[Outer.Inner]`, `[Message.field]`, `[Service.Method]`, …) match directly against the
 * qualified index.
 *
 * Logging is reference-driven, not index-driven.  Key collisions during indexing — top-level
 * `Foo` plus nested `Outer.Foo` both registering the short key `Foo`, or two enums declaring
 * the same value name — only emit a `debug` line; nothing louder fires until a comment
 * actually depends on the colliding key.  Per-occurrence access-time logging follows the
 * configured [mode]:
 *
 *  - [ProtocGenMarkdown.Options.ResolveReferenceLinksMode.NONE] — nothing logged (and the
 *    resolver isn't installed on the parser at all in this mode).
 *  - [ProtocGenMarkdown.Options.ResolveReferenceLinksMode.WARN] — each ambiguous or
 *    unresolved comment reference logs at `warn` with the request site (proto file, comment
 *    scope, label) and the candidates / reason.
 *  - [ProtocGenMarkdown.Options.ResolveReferenceLinksMode.FAIL_ON_INVALID] — each failing
 *    reference logs at `error` with the same request-site details and is recorded as a
 *    [Failure]; the caller (via [drainFailures]) surfaces the bundle at end-of-compile so
 *    protoc fails the run.
 */
internal class ReferenceLinkResolver(
    scopeFiles: List<FileDescriptorProtoWrapper>,
    peerFiles: List<FileDescriptorProtoWrapper>,
    options: ProtocGenMarkdown.Options,
    fileToGroup: Map<FileDescriptorProtoWrapper, Compiler.OutputGroup>,
    peerFileToGroup: Map<FileDescriptorProtoWrapper, Compiler.OutputGroup>,
    private val mode: ProtocGenMarkdown.Options.ResolveReferenceLinksMode,
    private val referenceLinkOverrides: Map<String, String>,
    private val hrefFor: (FileDescriptorProtoWrapper, List<String>) -> String,
) {
    /** Global qualified-name index — covers every dotted form of every type and member. */
    private val qualified = AmbiguityAwareMap("qualified")

    /** Global short-name index for types (messages, enums, services). */
    private val globalShortTypes = AmbiguityAwareMap("global short type")

    /** Global short-name index for enum values — bare-resolvable per the option's spec. */
    private val globalShortEnumValues = AmbiguityAwareMap("global short enum value")

    /** Per-message-FQN map of `fieldName → href` for bare-name lookup from message scope. */
    private val fieldsByMessage = mutableMapOf<String, MutableMap<String, String>>()

    /** Per-message-FQN map of `nestedTypeShortName → href` for bare-name lookup. */
    private val nestedTypesByMessage = mutableMapOf<String, MutableMap<String, String>>()

    /** Per-enum-FQN map of `valueName → href`. */
    private val valuesByEnum = mutableMapOf<String, MutableMap<String, String>>()

    /** Per-service-FQN map of `methodName → href`. */
    private val rpcsByService = mutableMapOf<String, MutableMap<String, String>>()

    /** fieldFqn → its containing message's FQN (so a field-scope comment can find its siblings). */
    private val parentMessageOfField = mutableMapOf<String, String>()

    /** valueFqn → its containing enum's FQN. */
    private val parentEnumOfValue = mutableMapOf<String, String>()

    /** methodFqn → its containing service's FQN. */
    private val parentServiceOfRpc = mutableMapOf<String, String>()

    /** fieldFqn → message FQN of the field's declared type, if that type is a message. */
    private val fieldTargetMessageOf = mutableMapOf<String, String>()

    /** methodFqn → input message FQN. */
    private val rpcInputOf = mutableMapOf<String, String>()

    /** methodFqn → output message FQN. */
    private val rpcOutputOf = mutableMapOf<String, String>()

    /** Any indexed FQN → the proto file it came from, for actionable failure messages. */
    private val fileByFqn = mutableMapOf<String, String>()

    /** typeFqn → its heading-anchor href, used to add a field/RPC's declared target as a
     *  bare-scope candidate by short name. */
    private val typeHrefByFqn = mutableMapOf<String, String>()

    private val collectedFailures = mutableListOf<Failure>()

    /**
     * The descriptor FQN of the comment currently being parsed, set by the caller via
     * [setCurrentScope] / [clearCurrentScope] around each `parser.parse(...)` invocation.
     * The [LinkProcessor] reads this on every link encountered.  Empty string means
     * "file scope" (no anchor descriptor).
     */
    private var currentScope: String = ""

    init {
        val consolidatedScope = options.outputType != ProtocGenMarkdown.Options.OutputType.PER_FILE
        indexFileSet(scopeFiles, consolidatedScope, fileToGroup)
        // Peer files are assumed to be rendered by a sibling protoc run under PER_FILE
        // conventions; their heading paths drop the per-file H2 layer regardless of this
        // run's outputType, matching what `Compiler.appendTypeReference` does for transitive
        // field-type targets.
        indexFileSet(peerFiles, consolidated = false, peerFileToGroup)
    }

    private fun indexFileSet(
        files: List<FileDescriptorProtoWrapper>,
        consolidated: Boolean,
        groupOfFile: Map<FileDescriptorProtoWrapper, Compiler.OutputGroup>,
    ) {
        for (file in files) {
            val pkg = file.`package`?.value.orEmpty()
            val pkgPrefix = if (pkg.isEmpty()) "" else "$pkg."
            val group = groupOfFile[file] ?: continue
            val groupTitle = group.title
            val fileTitle = file.name ?: "(unnamed)"
            val protoFile = file.name ?: "(unnamed)"
            val sectionBase: (String) -> List<String> = { section ->
                if (consolidated) listOf(groupTitle, fileTitle, section) else listOf(groupTitle, section)
            }
            for (m in file.messageTypes) indexMessage(m, pkgPrefix, "", file, protoFile, sectionBase("Messages"))
            for (e in file.enumTypes) indexEnum(e, pkgPrefix, "", file, protoFile, sectionBase("Enums"))
            for (s in file.services) {
                val sname = s.name?.value ?: continue
                val sFqn = "$pkgPrefix$sname"
                val sPath = sectionBase("Services") + sname
                val sHref = hrefFor(file, sPath)
                fileByFqn[sFqn] = protoFile
                globalShortTypes.put(sname, sHref, "service $sFqn")
                qualified.put(sname, sHref, "service $sFqn")
                qualified.put(sFqn, sHref, "service $sFqn")
                val rpcMap = rpcsByService.getOrPut(sFqn) { mutableMapOf() }
                for (method in s.methods) {
                    val mname = method.name?.value ?: continue
                    val mFqn = "$sFqn.$mname"
                    val mPath = sPath + "RPC Details" + mname
                    val href = hrefFor(file, mPath)
                    fileByFqn[mFqn] = protoFile
                    qualified.put("$sname.$mname", href, "rpc $mFqn")
                    qualified.put(mFqn, href, "rpc $mFqn")
                    rpcMap[mname] = href
                    parentServiceOfRpc[mFqn] = sFqn
                    method.inputType?.value?.removePrefix(".")?.let { rpcInputOf[mFqn] = it }
                    method.outputType?.value?.removePrefix(".")?.let { rpcOutputOf[mFqn] = it }
                }
            }
        }
    }

    private fun indexMessage(
        msg: DescriptorProtoWrapper,
        pkgPrefix: String,
        ancestorPrefix: String,
        file: FileDescriptorProtoWrapper,
        protoFile: String,
        sectionPath: List<String>,
    ) {
        if (msg.options?.mapEntry?.value == true) return
        val short = msg.name?.value ?: return
        val dotted = if (ancestorPrefix.isEmpty()) short else "$ancestorPrefix.$short"
        val fqn = "$pkgPrefix$dotted"
        val typePath = sectionPath + dotted
        val typeHref = hrefFor(file, typePath)
        fileByFqn[fqn] = protoFile
        typeHrefByFqn[fqn] = typeHref
        globalShortTypes.put(short, typeHref, "message $fqn")
        qualified.put(dotted, typeHref, "message $fqn")
        qualified.put(fqn, typeHref, "message $fqn")

        if (ancestorPrefix.isNotEmpty()) {
            val parentFqn = pkgPrefix + ancestorPrefix
            nestedTypesByMessage.getOrPut(parentFqn) { mutableMapOf() }[short] = typeHref
        }

        val fieldMap = fieldsByMessage.getOrPut(fqn) { mutableMapOf() }
        val detailsPath = typePath + "Field Details"
        for (field in msg.fields) {
            val fname = field.name?.value ?: continue
            val fFqn = "$fqn.$fname"
            val fHref = hrefFor(file, detailsPath + fname)
            fileByFqn[fFqn] = protoFile
            qualified.put("$short.$fname", fHref, "field $fFqn")
            if (ancestorPrefix.isNotEmpty()) qualified.put("$dotted.$fname", fHref, "field $fFqn")
            qualified.put(fFqn, fHref, "field $fFqn")
            fieldMap[fname] = fHref
            parentMessageOfField[fFqn] = fqn
            val typeName = field.typeName?.value?.removePrefix(".")
            if (!typeName.isNullOrEmpty()) fieldTargetMessageOf[fFqn] = typeName
        }

        for (n in msg.nestedTypes) indexMessage(n, pkgPrefix, dotted, file, protoFile, sectionPath)
        for (e in msg.enumTypes) indexEnum(e, pkgPrefix, dotted, file, protoFile, sectionPath)
    }

    private fun indexEnum(
        enum: EnumDescriptorProtoWrapper,
        pkgPrefix: String,
        ancestorPrefix: String,
        file: FileDescriptorProtoWrapper,
        protoFile: String,
        sectionPath: List<String>,
    ) {
        val short = enum.name?.value ?: return
        val dotted = if (ancestorPrefix.isEmpty()) short else "$ancestorPrefix.$short"
        val fqn = "$pkgPrefix$dotted"
        val effectiveSection = if (ancestorPrefix.isEmpty()) sectionPath else sectionPath.dropLast(1) + "Enums"
        val typePath = effectiveSection + dotted
        val typeHref = hrefFor(file, typePath)
        fileByFqn[fqn] = protoFile
        typeHrefByFqn[fqn] = typeHref
        globalShortTypes.put(short, typeHref, "enum $fqn")
        qualified.put(dotted, typeHref, "enum $fqn")
        qualified.put(fqn, typeHref, "enum $fqn")

        if (ancestorPrefix.isNotEmpty()) {
            val parentFqn = pkgPrefix + ancestorPrefix
            nestedTypesByMessage.getOrPut(parentFqn) { mutableMapOf() }[short] = typeHref
        }

        val valueMap = valuesByEnum.getOrPut(fqn) { mutableMapOf() }
        val detailsPath = typePath + "Value Details"
        for (v in enum.values) {
            val vname = v.name?.value ?: continue
            val vFqn = "$fqn.$vname"
            val vHref = hrefFor(file, detailsPath + vname)
            fileByFqn[vFqn] = protoFile
            qualified.put("$short.$vname", vHref, "enum value $vFqn")
            if (ancestorPrefix.isNotEmpty()) qualified.put("$dotted.$vname", vHref, "enum value $vFqn")
            qualified.put(vFqn, vHref, "enum value $vFqn")
            valueMap[vname] = vHref
            parentEnumOfValue[vFqn] = fqn
            globalShortEnumValues.put(vname, vHref, "enum value $vFqn")
        }
    }

    /**
     * Resolve a bracketed [label] under the comment's [scopeFqn] (the FQN of the descriptor the
     * comment is attached to, or `""` when no scope is known).  Resolution order:
     *
     *  1. [referenceLinkOverrides] — user-supplied URL takes precedence over everything.
     *  2. Qualified lookup when the label contains a dot — `Outer.Inner`, `Message.field`, etc.
     *  3. Bare-scope lookup — sibling members of the comment's anchor descriptor, plus the
     *     anchor's declared type target when it has one (so `[Foo]` on a field of type `Foo`
     *     resolves to that specific target).
     *  4. Global short-name lookups against types, then enum values.
     *
     * Returns `Outcome.Resolved` as soon as a step produces one.  If no step produces
     * Resolved but at least one produces `Outcome.Ambiguous`, the first Ambiguous wins —
     * caller surfaces it as an Ambiguous failure with candidate list, rather than the
     * misleading Unresolved that would result from dropping ambiguity on the floor.
     */
    fun resolve(
        label: String,
        scopeFqn: String,
    ): Outcome {
        if (label.isEmpty()) return Outcome.Unresolved
        referenceLinkOverrides[label]?.let { return Outcome.Resolved(it) }
        if ('.' in label) {
            return qualified.lookup(label)
        }
        var ambiguous: Outcome.Ambiguous? = null
        bareScopeOutcome(label, scopeFqn)?.let {
            if (it is Outcome.Resolved) return it
            if (it is Outcome.Ambiguous && ambiguous == null) ambiguous = it
        }
        globalShortTypes.lookup(label).let {
            if (it is Outcome.Resolved) return it
            if (it is Outcome.Ambiguous && ambiguous == null) ambiguous = it
        }
        globalShortEnumValues.lookup(label).let {
            if (it is Outcome.Resolved) return it
            if (it is Outcome.Ambiguous && ambiguous == null) ambiguous = it
        }
        return ambiguous ?: Outcome.Unresolved
    }

    private fun bareScopeOutcome(
        label: String,
        scopeFqn: String,
    ): Outcome? {
        if (scopeFqn.isEmpty()) return null
        val sources = bareScopeSources(scopeFqn) ?: return null
        val hits = mutableListOf<Pair<String, String>>() // (sourceLabel, href)
        for ((sourceLabel, map) in sources) {
            map[label]?.let { hits += sourceLabel to it }
        }
        return when {
            hits.isEmpty() -> null
            hits.size == 1 -> Outcome.Resolved(hits[0].second)
            hits.distinctBy { it.second }.size == 1 -> Outcome.Resolved(hits[0].second)
            else -> Outcome.Ambiguous(hits[0].second, hits.map { "${it.first} → ${it.second}" })
        }
    }

    private fun bareScopeSources(scopeFqn: String): List<Pair<String, Map<String, String>>>? {
        fieldsByMessage[scopeFqn]?.let { fields ->
            val sources = mutableListOf<Pair<String, Map<String, String>>>()
            sources += "field of $scopeFqn" to fields
            nestedTypesByMessage[scopeFqn]?.let { sources += "nested type of $scopeFqn" to it }
            return sources
        }
        valuesByEnum[scopeFqn]?.let { return listOf("value of $scopeFqn" to it) }
        rpcsByService[scopeFqn]?.let { return listOf("rpc of $scopeFqn" to it) }
        parentMessageOfField[scopeFqn]?.let { parentMsg ->
            val sources = mutableListOf<Pair<String, Map<String, String>>>()
            fieldsByMessage[parentMsg]?.let { sources += "sibling field in $parentMsg" to it }
            fieldTargetMessageOf[scopeFqn]?.let { target ->
                fieldsByMessage[target]?.let { sources += "field of referenced type $target" to it }
                addTypeAsSource(sources, "declared type of $scopeFqn", target)
            }
            return sources
        }
        parentEnumOfValue[scopeFqn]?.let { parentEnum ->
            return listOf("sibling value in $parentEnum" to (valuesByEnum[parentEnum] ?: return@let null))
        }
        parentServiceOfRpc[scopeFqn]?.let { parentService ->
            val sources = mutableListOf<Pair<String, Map<String, String>>>()
            rpcsByService[parentService]?.let { sources += "sibling rpc in $parentService" to it }
            rpcInputOf[scopeFqn]?.let { input ->
                fieldsByMessage[input]?.let { sources += "field of input $input" to it }
                addTypeAsSource(sources, "input type of $scopeFqn", input)
            }
            rpcOutputOf[scopeFqn]?.let { output ->
                fieldsByMessage[output]?.let { sources += "field of output $output" to it }
                addTypeAsSource(sources, "output type of $scopeFqn", output)
            }
            return sources
        }
        return null
    }

    /** Add `typeFqn`'s heading anchor as a bare-scope candidate, keyed by the type's short
     *  (last-segment) name.  No-op when the type isn't indexed — out-of-scope types skip
     *  silently rather than poisoning the candidate set with a missing href. */
    private fun addTypeAsSource(
        sources: MutableList<Pair<String, Map<String, String>>>,
        sourceLabel: String,
        typeFqn: String,
    ) {
        val href = typeHrefByFqn[typeFqn] ?: return
        val short = typeFqn.substringAfterLast('.')
        sources += sourceLabel to mapOf(short to href)
    }

    /**
     * Bind the descriptor FQN of the comment about to be parsed.  Must be paired with
     * [clearCurrentScope] (typically in a `try`/`finally`) so a thrown exception inside the
     * parser doesn't leak a stale scope into the next comment.  Empty string means file scope.
     */
    fun setCurrentScope(scopeFqn: String) {
        currentScope = scopeFqn
    }

    /** Reset the scope to file scope.  See [setCurrentScope]. */
    fun clearCurrentScope() {
        currentScope = ""
    }

    /**
     * [LinkProcessor] installed on the [org.commonmark.parser.Parser] in [Compiler].  Fires for
     * every parsed link/image — inline (`[text](url)`), shortcut (`[text]`), collapsed
     * (`[text][]`), and full (`[text][label]`).  Inline links and images are handed back to
     * the core processor untouched; the three reference forms are resolved against the
     * compile scope (with full-form's `label` as the lookup key and `text` preserved as
     * display content).  Returning [LinkResult.none] for unresolved labels lets the core
     * processor fall through to literal text — preserving the brackets in the output so the
     * failure surface in `FAIL_ON_INVALID` mode is easy to spot.
     */
    val linkProcessor: LinkProcessor =
        LinkProcessor { linkInfo, scanner, _ -> processLink(linkInfo, scanner) }

    private fun processLink(
        linkInfo: LinkInfo,
        scanner: Scanner,
    ): LinkResult? {
        if (linkInfo.destination() != null) return LinkResult.none()
        if (linkInfo.marker() != null) return LinkResult.none()
        val rawLabel = linkInfo.label()
        val text = linkInfo.text()
        // shortcut `[text]` → label null; collapsed `[text][]` → label "" — both use text as the key.
        // full `[text][label]` → resolve label, but the visible link text stays as text.
        val key = if (rawLabel.isNullOrEmpty()) text else rawLabel
        if (key.isEmpty()) return LinkResult.none()
        val scopeFqn = currentScope
        val outcome = resolve(key, scopeFqn)
        if (log.isTraceEnabled) {
            val outcomeDescription =
                when (outcome) {
                    is Outcome.Resolved -> "resolved → ${outcome.href}"
                    is Outcome.Ambiguous -> "ambiguous → ${outcome.href} (candidates: ${outcome.candidates.joinToString("; ")})"
                    Outcome.Unresolved -> "unresolved"
                }
            log.trace(
                "reference-link [{}] identified in {} :: {} — {}",
                key,
                fileByFqn[scopeFqn] ?: "(unknown)",
                scopeFqn.ifEmpty { "(file scope)" },
                outcomeDescription,
            )
        }
        return when (outcome) {
            is Outcome.Resolved -> LinkResult.wrapTextIn(Link(outcome.href, null), scanner.position())

            is Outcome.Ambiguous -> {
                reportFailure(scopeFqn, key, FailureReason.Ambiguous(outcome.candidates))
                LinkResult.wrapTextIn(Link(outcome.href, null), scanner.position())
            }

            Outcome.Unresolved -> {
                reportFailure(scopeFqn, key, FailureReason.Unresolved)
                LinkResult.none()
            }
        }
    }

    /**
     * Funnel for every comment-level reference that doesn't resolve cleanly under [scopeFqn].
     * Logs at a level chosen by [mode] (silent under [ProtocGenMarkdown.Options.ResolveReferenceLinksMode.NONE],
     * `warn` under [ProtocGenMarkdown.Options.ResolveReferenceLinksMode.WARN], `error` under
     * [ProtocGenMarkdown.Options.ResolveReferenceLinksMode.FAIL_ON_INVALID]) and, under
     * `FAIL_ON_INVALID`, also appends a [Failure] so [Compiler] can surface the bundle through
     * `CodeGeneratorResponse.error` at end of compile.
     */
    private fun reportFailure(
        scopeFqn: String,
        label: String,
        reason: FailureReason,
    ) {
        val protoFile = fileByFqn[scopeFqn] ?: "(unknown)"
        val scopeDescription = scopeFqn.ifEmpty { "(file scope)" }
        val reasonDescription =
            when (reason) {
                is FailureReason.Unresolved ->
                    "no matching type, field, enum value, or RPC in compile scope"

                is FailureReason.Ambiguous ->
                    "ambiguous; candidates: ${reason.candidates.joinToString("; ")}"
            }
        when (mode) {
            ProtocGenMarkdown.Options.ResolveReferenceLinksMode.NONE -> Unit

            ProtocGenMarkdown.Options.ResolveReferenceLinksMode.WARN ->
                log.warn(
                    "reference-link [{}] in {} :: {} — {}",
                    label,
                    protoFile,
                    scopeDescription,
                    reasonDescription,
                )

            ProtocGenMarkdown.Options.ResolveReferenceLinksMode.FAIL_ON_INVALID -> {
                log.error(
                    "reference-link [{}] in {} :: {} — {}",
                    label,
                    protoFile,
                    scopeDescription,
                    reasonDescription,
                )
                collectedFailures += Failure(
                    protoFile = protoFile,
                    scopeFqn = scopeFqn,
                    label = label,
                    reason = reason,
                )
            }
        }
    }

    /** Drain (and clear) the failures recorded during [rewrite] calls. */
    fun drainFailures(): List<Failure> {
        val out = collectedFailures.toList()
        collectedFailures.clear()
        return out
    }

    /** Per-map `key → href` index that records every owner registering each key, so collisions
     *  produce both a `trace` log line at index time and an [Outcome.Ambiguous] at resolve time. */
    private inner class AmbiguityAwareMap(private val mapLabel: String) {
        private val hrefByKey = mutableMapOf<String, String>()
        private val candidatesByKey = mutableMapOf<String, MutableList<Candidate>>()

        fun put(
            key: String,
            href: String,
            owner: String,
        ) {
            val candidates = candidatesByKey.getOrPut(key) { mutableListOf() }
            if (candidates.any { it.owner == owner && it.href == href }) return
            candidates += Candidate(owner, href)
            val prior = hrefByKey[key]
            if (prior != null && prior != href) {
                log.trace(
                    "reference-link key '{}' in {} is ambiguous — '{}' collides with a prior entry resolving to '{}'; " +
                        "keeping the latest.  Use a qualified form to disambiguate in comments.",
                    key,
                    mapLabel,
                    owner,
                    prior,
                )
            }
            hrefByKey[key] = href
        }

        fun lookup(key: String): Outcome {
            val href = hrefByKey[key] ?: return Outcome.Unresolved
            val candidates = candidatesByKey[key].orEmpty()
            val distinctHrefs = candidates.map { it.href }.distinct()
            return if (distinctHrefs.size <= 1) {
                Outcome.Resolved(href)
            } else {
                Outcome.Ambiguous(href, candidates.map { "${it.owner} → ${it.href}" })
            }
        }
    }

    private data class Candidate(val owner: String, val href: String)

    sealed class Outcome {
        data class Resolved(val href: String) : Outcome()
        data class Ambiguous(val href: String, val candidates: List<String>) : Outcome()
        object Unresolved : Outcome()
    }

    data class Failure(
        val protoFile: String,
        val scopeFqn: String,
        val label: String,
        val reason: FailureReason,
    )

    sealed class FailureReason {
        object Unresolved : FailureReason()
        data class Ambiguous(val candidates: List<String>) : FailureReason()
    }
}
