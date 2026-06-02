package com.engine.protoc.markdown.compile

import com.engine.protoc.markdown.ProtocGenMarkdown
import com.engine.protoc.util.enums.EnumDescriptorProtoWrapper
import com.engine.protoc.util.file.FileDescriptorProtoWrapper
import com.engine.protoc.util.message.DescriptorProtoWrapper
import org.commonmark.node.AbstractVisitor
import org.commonmark.node.Link
import org.commonmark.node.Node
import org.commonmark.node.Text

/**
 * Resolves CommonMark shortcut-reference syntax (`[label]`) inside proto leading comments to
 * Markdown links pointing at the heading anchors this generator already emits.  Constructed
 * once per output document so the href shape (bare `#anchor` vs `relative/path.md#anchor`)
 * is bound to that document's filename.
 *
 * The resolver indexes every type, field, enum value, and RPC reachable from the compile
 * scope.  Bare-name lookups consult both a global short-name index and a per-scope local
 * index built from the comment's anchor descriptor (`scopeFqn`) — so a comment on
 * `message User` can write `[name]` to find `User.name` without the explicit qualifier.
 * Qualified labels (`[Outer.Inner]`, `[Message.field]`, `[Service.Method]`, …) match
 * directly against the qualified index.
 *
 * Key collisions during indexing — top-level `Foo` plus nested `Outer.Foo` both registering
 * the short key `Foo`, or two enums declaring the same value name — log a warning naming the
 * colliding owners so authors can disambiguate by qualifying.  Under
 * [ProtocGenMarkdown.Options.ResolveReferenceLinksMode.FAIL_ON_INVALID], any comment that
 * actually references an ambiguous or unresolved label is recorded as a [Failure], and the
 * caller (via [failures]) can surface those at end-of-compile.
 */
internal class ReferenceLinkResolver(
    scopeFiles: List<FileDescriptorProtoWrapper>,
    options: ProtocGenMarkdown.Options,
    fileToGroup: Map<FileDescriptorProtoWrapper, Compiler.OutputGroup>,
    private val mode: ProtocGenMarkdown.Options.ResolveReferenceLinksMode,
    private val log: System.Logger,
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

    private val collectedFailures = mutableListOf<Failure>()

    init {
        val consolidated = options.outputType != ProtocGenMarkdown.Options.OutputType.PER_FILE
        for (file in scopeFiles) {
            val pkg = file.`package`?.value.orEmpty()
            val pkgPrefix = if (pkg.isEmpty()) "" else "$pkg."
            val group = fileToGroup[file] ?: continue
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
     * comment is attached to, or `""` when no scope is known).
     */
    fun resolve(
        label: String,
        scopeFqn: String,
    ): Outcome {
        if (label.isEmpty()) return Outcome.Unresolved
        if ('.' in label) {
            return qualified.lookup(label)
        }
        bareScopeOutcome(label, scopeFqn)?.let { return it }
        globalShortTypes.lookup(label).let { if (it is Outcome.Resolved) return it }
        globalShortEnumValues.lookup(label).let { if (it is Outcome.Resolved) return it }
        return Outcome.Unresolved
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
            }
            rpcOutputOf[scopeFqn]?.let { output ->
                fieldsByMessage[output]?.let { sources += "field of output $output" to it }
            }
            return sources
        }
        return null
    }

    /**
     * Walk the parsed CommonMark fragment [root] under the given [scopeFqn] and convert every
     * resolvable `[label]` inside a [Text] node into a [Link].  Unresolved labels stay literal;
     * ambiguous labels resolve to one of the candidates (the rewrite still proceeds) but get
     * recorded as a [Failure] when [mode] is [ProtocGenMarkdown.Options.ResolveReferenceLinksMode.FAIL_ON_INVALID],
     * so the caller can fail the compile after all groups have been processed.
     */
    fun rewrite(
        root: Node,
        scopeFqn: String,
    ) {
        val pending = mutableListOf<Text>()
        root.accept(
            object : AbstractVisitor() {
                override fun visit(text: Text) {
                    if (text.parent is Link) return
                    if ('[' in text.literal && ']' in text.literal) pending += text
                }
            },
        )
        for (text in pending) rewriteText(text, scopeFqn)
    }

    private fun rewriteText(
        text: Text,
        scopeFqn: String,
    ) {
        val original = text.literal
        val matches = bracketPattern.findAll(original).toList()
        if (matches.isEmpty()) return

        val replacement = mutableListOf<Node>()
        var cursor = 0
        var changed = false
        for (m in matches) {
            val label = m.groupValues[1]
            val outcome = resolve(label, scopeFqn)
            val href =
                when (outcome) {
                    is Outcome.Resolved -> outcome.href

                    is Outcome.Ambiguous -> {
                        recordFailure(scopeFqn, label, FailureReason.Ambiguous(outcome.candidates))
                        outcome.href
                    }

                    Outcome.Unresolved -> {
                        recordFailure(scopeFqn, label, FailureReason.Unresolved)
                        continue
                    }
                }
            if (m.range.first > cursor) replacement += Text(original.substring(cursor, m.range.first))
            val link = Link(href, null)
            link.appendChild(Text(label))
            replacement += link
            cursor = m.range.last + 1
            changed = true
        }
        if (!changed) return
        if (cursor < original.length) replacement += Text(original.substring(cursor))

        for (node in replacement) text.insertBefore(node)
        text.unlink()
    }

    private fun recordFailure(
        scopeFqn: String,
        label: String,
        reason: FailureReason,
    ) {
        if (mode != ProtocGenMarkdown.Options.ResolveReferenceLinksMode.FAIL_ON_INVALID) return
        collectedFailures += Failure(
            protoFile = fileByFqn[scopeFqn] ?: "(unknown)",
            scopeFqn = scopeFqn,
            label = label,
            reason = reason,
        )
    }

    /** Drain (and clear) the failures recorded during [rewrite] calls. */
    fun drainFailures(): List<Failure> {
        val out = collectedFailures.toList()
        collectedFailures.clear()
        return out
    }

    /** Per-map `key → href` index that records every owner registering each key, so collisions
     *  produce both a warning at index time and an [Outcome.Ambiguous] at resolve time. */
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
                log.log(
                    System.Logger.Level.WARNING,
                    "protoc-gen-markdown: reference-link key '$key' in $mapLabel is ambiguous — '$owner' " +
                        "collides with a prior entry resolving to '$prior'; keeping the latest.  Use a " +
                        "qualified form to disambiguate in comments.",
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

    private companion object {
        val bracketPattern = Regex("""\[([^\[\]\r\n]+)\]""")
    }
}
