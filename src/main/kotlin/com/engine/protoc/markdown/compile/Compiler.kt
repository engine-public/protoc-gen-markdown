package com.engine.protoc.markdown.compile

import com.engine.protoc.markdown.PlannedDocument
import com.engine.protoc.markdown.ProtocGenMarkdown
import com.engine.protoc.markdown.ProtocGenMarkdown.Options.MemberSortMode
import com.engine.protoc.markdown.ProtocGenMarkdown.Options.SortMode
import com.engine.protoc.markdown.Version
import com.engine.protoc.util.Locatable
import com.engine.protoc.util.compiler.CodeGeneratorRequestWrapper
import com.engine.protoc.util.compiler.CodeGeneratorResponseWrapper
import com.engine.protoc.util.enums.EnumDescriptorProtoWrapper
import com.engine.protoc.util.enums.EnumValueDescriptorProtoWrapper
import com.engine.protoc.util.file.FileDescriptorProtoWrapper
import com.engine.protoc.util.file.SourceCodeInfoWrapper
import com.engine.protoc.util.message.DescriptorProtoWrapper
import com.engine.protoc.util.message.FieldDescriptorProtoWrapper
import com.engine.protoc.util.service.MethodDescriptorProtoWrapper
import com.engine.protoc.util.service.ServiceDescriptorProtoWrapper
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type
import com.google.protobuf.compiler.PluginProtos
import org.commonmark.ext.front.matter.YamlFrontMatterBlock
import org.commonmark.ext.front.matter.YamlFrontMatterNode
import org.commonmark.ext.gfm.tables.TableBlock
import org.commonmark.ext.gfm.tables.TableBody
import org.commonmark.ext.gfm.tables.TableCell
import org.commonmark.ext.gfm.tables.TableHead
import org.commonmark.ext.gfm.tables.TableRow
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.node.BulletList
import org.commonmark.node.Document
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Heading
import org.commonmark.node.HtmlBlock
import org.commonmark.node.HtmlInline
import org.commonmark.node.Link
import org.commonmark.node.ListItem
import org.commonmark.node.Node
import org.commonmark.node.Paragraph
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.Text
import org.commonmark.node.ThematicBreak
import org.commonmark.parser.Parser
import org.commonmark.parser.beta.LinkProcessor
import org.commonmark.parser.beta.LinkResult
import org.commonmark.renderer.NodeRenderer
import org.commonmark.renderer.markdown.MarkdownNodeRendererContext
import org.commonmark.renderer.markdown.MarkdownNodeRendererFactory
import org.commonmark.renderer.markdown.MarkdownRenderer
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.format.DateTimeFormatter

private val log = LoggerFactory.getLogger(Compiler::class.java)

/**
 * Per compile invocation, emits one `.md` file per entry in `request.filesToGenerate`.  Each
 * document is an outline of the proto:
 *
 *  - A YAML frontmatter block at the very top — emitted unconditionally — modeled as a typed
 *    [YamlFrontMatterBlock] from `commonmark-ext-yaml-front-matter` rather than an [HtmlBlock]
 *    masquerading as YAML.  Carries three flat keys: `generated-by` (the GitHub release-tag URL
 *    encoding both plugin identity and version), `protoc-gen-markdown-generated-on` (ISO-8601
 *    instant from the injected [Clock]), and `protoc-gen-markdown-options` (every
 *    [ProtocGenMarkdown.Options] value in effect for this compile snapshotted as a
 *    comma-separated `key=value` string, defaults included).  See [frontmatterBlock] and
 *    [FrontmatterRenderer].
 *  - A `# <path>` heading carrying the proto's full relative path as protoc reports it.
 *  - A horizontal rule under the title, emitted whenever the file declares at least one service,
 *    message, or enum — independent of the Table of Contents — plus a rule before every
 *    subsequent significant section header (the per-file H2 in the consolidated `outputType`
 *    modes, and every `Services`/`Messages`/`Enums` heading), deduplicated against the
 *    under-title rule and against any immediately-preceding higher-level heading by
 *    [appendSectionRule].
 *  - A `<details><summary>Table of contents</summary>` block listing the file's headings as a
 *    nested bullet list of intra-document anchor links.  Opt-in via the `minTableOfContentsHeader`
 *    / `maxTableOfContentsHeader` options (both `null` by default — no TOC); see
 *    [renderTableOfContents] for the level-range semantics.
 *  - A `## Services` section listing each service as `### <ServiceName>`.  Under each heading:
 *    the service's leading proto comment, then a `#### RPC Summary` heading and a GFM pipe
 *    table of the service's RPCs with columns `Name | Input | Output | Description`.  Input
 *    and output types link to the file declaring them whenever the type's file is in the
 *    compile scope; client- or server-streaming markers prefix the corresponding type cell
 *    with `stream `.  The description column carries only the first paragraph of each RPC's
 *    leading comment so the pipe-table syntax stays valid; whenever the RPC has a leading
 *    comment the cell ends with a trailing `[...](#<rpc-anchor>)` link pointing at a
 *    `##### <RpcName>` expansion under the `#### RPC Details` heading emitted after the table.
 *    The expansion always re-renders the full leading comment — redundancy with the table's
 *    first-paragraph extract included — so the Details section is the canonical home of the
 *    member's full doc.  RPCs with no leading comment contribute only a heading.
 *  - A `## Messages` section listing each message as `### <Dotted.Name>`.  Under each heading:
 *    the message's leading proto comment (parsed as CommonMark so links/lists/etc. round-trip),
 *    a `#### Field Summary` heading, then a GFM pipe table of the message's fields with
 *    columns `Name | Type | Description`.  The type column links to the file containing that
 *    type (relative path) whenever the type's file is in the compile scope; out-of-scope and
 *    scalar types appear as plain text.  Repeated fields are prefixed with `repeated ` in the
 *    type cell.  The description cell carries only the first paragraph of the field's leading
 *    comment so the pipe-table syntax stays valid; whenever the field has a leading comment
 *    the cell ends with a trailing `[...](#<field-anchor>)` link pointing at a
 *    `##### <fieldName>` expansion under the `#### Field Details` heading emitted after the
 *    table.  The expansion always re-renders the full leading comment — redundancy with the
 *    table's first-paragraph extract included.  Fields with no leading comment contribute
 *    only a heading.
 *  - A `## Enums` section listing each enum as `### <Dotted.Name>`.  Under each heading:
 *    the enum's leading proto comment, then a `#### Value Summary` heading and a GFM pipe
 *    table of the enum's values with columns `Name | Number | Description`.  The description
 *    column carries only the first paragraph of each value's leading comment; whenever the
 *    value has a leading comment the cell ends with a trailing `[...](#<value-anchor>)` link
 *    pointing at a `##### <ValueName>` expansion under the `#### Value Details` heading
 *    emitted after the table.  The expansion always re-renders the full leading comment.
 *    Enum values with no leading comment contribute only a heading.
 *
 * Sections are omitted when their kind has no entries.  Map-entry synthetic messages are
 * skipped everywhere (Messages section, enum collection, type index).
 *
 * With `generateStableAnchors=true`, every heading carries an explicit `<a id="…"></a>` whose
 * id is its full ancestor-heading path joined with `-`, with characters other than ASCII
 * letters/digits/`-`/`_` replaced by `_`.  Same-named headings under different parents — two
 * `### Foo` under different message sections, two `##### text` fields under different messages
 * — get distinct ids this way.  Intra-document links (TOC entries, `[...](#…)` field-expansion
 * links) target these explicit ids rather than relying on a renderer's heading-text auto-anchor.
 *
 * With `generateStableAnchors=false` (the default), no `<a id>` element is emitted and
 * intra-document links target the GFM-style slug of the leaf heading text (lowercased, whitespace
 * → `-`, characters other than alphanumerics/`-`/`_` dropped) — the convention common renderers
 * derive from the heading text itself.  Cheaper output but vulnerable to heading-text collisions.
 *
 * The CommonMark tree is built up via [Document] + the GFM tables extension and rendered with
 * [MarkdownRenderer]; comments are parsed with [Parser] so block-level CommonMark inside a
 * comment (lists, blockquotes, fenced code, links, …) round-trips faithfully wherever it
 * appears (under headings, under field expansions).
 */
internal class Compiler(
    private val request: CodeGeneratorRequestWrapper,
    private val options: ProtocGenMarkdown.Options,
    private val clock: Clock,
) {

    private val tablesExtension = TablesExtension.create()
    private val renderer: MarkdownRenderer =
        MarkdownRenderer.builder()
            .extensions(listOf(tablesExtension))
            .nodeRendererFactory(
                object : MarkdownNodeRendererFactory {
                    override fun create(context: MarkdownNodeRendererContext): NodeRenderer = FrontmatterRenderer(context, options.generateInsertionPoints)

                    override fun getSpecialCharacters(): Set<Char> = emptySet()
                },
            )
            .build()

    /**
     * Fixed [LinkProcessor] installed on the shared [parser] that delegates each parsed link
     * to whichever [ReferenceLinkResolver] owns the current document.  Constructed once per
     * compiler so the parser itself can stay a single instance even as [currentResolver] is
     * rebuilt per output document; per-comment scope flows through `setCurrentScope` /
     * `clearCurrentScope` on the active resolver, set by [appendLeadingComment] and
     * [summaryDescriptionCell] around each parse call.  When no resolver is active (NONE
     * mode), the processor returns [LinkResult.none] so the core CommonMark processor
     * handles links exactly as if no override were installed.
     */
    private val linkProcessor: LinkProcessor =
        LinkProcessor { linkInfo, scanner, ctx ->
            currentResolver?.linkProcessor?.process(linkInfo, scanner, ctx) ?: LinkResult.none()
        }
    private val parser: Parser =
        Parser.builder()
            .extensions(listOf(tablesExtension))
            .linkProcessor(linkProcessor)
            .build()

    /**
     * Every heading the compiler emits is recorded here in document order, populated as a side
     * effect of [headingOf].  Re-initialized at the start of each [outlineDocument] invocation
     * so the entries always belong to the file currently being rendered.  [renderTableOfContents]
     * is the sole consumer.
     */
    private data class HeadingRef(
        val level: Int,
        val text: String,
        val path: List<String>,
    )

    private val headings: MutableList<HeadingRef> = mutableListOf()

    /**
     * How many levels [headingOf] should add to the [HEADING_SECTION] / [HEADING_TYPE] /
     * [HEADING_FIELD_SECTION] / [HEADING_FIELD] base levels for the document currently being
     * rendered.  `0` in [ProtocGenMarkdown.Options.OutputType.PER_FILE] (the doc's L1 is the
     * file's `<path>` heading, so the body sits at L2..L5); `1` in
     * [ProtocGenMarkdown.Options.OutputType.PER_PACKAGE] /
     * [ProtocGenMarkdown.Options.OutputType.SINGLE_FILE] (the doc's L1 is the package /
     * single-file-overview label, each input file is then an L2 sub-heading, and the body
     * shifts down to L3..L6).
     * Mutated at the start of [outlineDocument] for each output group; [fixedHeading] ignores
     * it and is the way to emit headings that must sit at a known absolute level (the doc's L1
     * and the per-file L2 in consolidated modes).
     */
    private var bodyLevelShift: Int = 0

    /**
     * Per-output-document [ReferenceLinkResolver], constructed at the top of [outlineDocument]
     * with `currentMd` bound to that document's filename so the resolver can produce both bare
     * `#anchor` hrefs (target lives in the same consolidated file) and relative-path hrefs
     * (target lives in a sibling output file).  `null` when [ProtocGenMarkdown.Options.resolveReferenceLinks]
     * is off, in which case [appendLeadingComment] skips the rewrite step entirely.
     */
    private var currentResolver: ReferenceLinkResolver? = null

    /**
     * A single output `.md` file the compiler will emit: a filename plus the (one or more) input
     * proto files whose content lands in it, plus the H1 title text that heads it:
     *  - [ProtocGenMarkdown.Options.OutputType.PER_FILE] groups always contain exactly one file
     *    and the title is the file's relative path.
     *  - [ProtocGenMarkdown.Options.OutputType.PER_PACKAGE] groups (and the per-package
     *    navigation indices produced under [ProtocGenMarkdown.Options.includeIndices])
     *    share the same proto `package` and the title is the dotted package, or
     *    `Default Package` for files with no `package` directive.
     *  - [ProtocGenMarkdown.Options.OutputType.SINGLE_FILE] holds a single group containing
     *    every file in the compile request and the title is the longest common package
     *    prefix, or the label `Overview` when there is no common prefix.
     *
     * Because [outlineDocument]'s `groupPath` seeds every nested heading's path with this title,
     * the title flows through [pathAnchor] into every descendant `<a id>` — TOC entries, summary
     * table `[...](#…)` expansions, and the cross-file links from package-index pages all stay
     * consistent with the visible H1 text.
     */
    internal data class OutputGroup(
        val filename: String,
        val files: List<FileDescriptorProtoWrapper>,
        val title: String,
    )

    /** True when [outlineDocument] should emit an L2 per-file heading inside the document. */
    private val OutputGroup.consolidated: Boolean
        get() = options.outputType != ProtocGenMarkdown.Options.OutputType.PER_FILE

    internal fun compile(): PluginProtos.CodeGeneratorResponse {
        log.info("compile starting with options: {}", options)
        if (options.includeIndices && options.outputType == ProtocGenMarkdown.Options.OutputType.SINGLE_FILE) {
            log.info("includeIndices=true has no effect under outputType=SINGLE_FILE; the consolidated output is itself the aggregator")
        }
        val response = CodeGeneratorResponseWrapper()
        val collectedFailures = mutableListOf<ReferenceLinkResolver.Failure>()
        for (group in outputGroups) {
            response.addFile(group.filename, render(outlineDocument(group)))
            currentResolver?.drainFailures()?.let { collectedFailures += it }
        }
        for (group in packageIndexGroups) {
            response.addFile(group.filename, render(packageIndexDocument(group)))
        }
        overviewGroup?.let { group ->
            response.addFile(group.filename, render(overviewDocument(group)))
        }
        if (collectedFailures.isNotEmpty() &&
            options.resolveReferenceLinksMode == ProtocGenMarkdown.Options.ResolveReferenceLinksMode.FAIL_ON_INVALID
        ) {
            val summary = referenceLinkFailureSummary(collectedFailures.distinct())
            log.error("protoc-gen-markdown failed:\n{}", summary)
            response.addError("protoc-gen-markdown failed:\n$summary")
        }
        return response.build()
    }

    /**
     * The documents [compile] would emit, described as a flat list with their natural parent → child
     * hierarchy, but without rendering any Markdown.  Mirrors the exact file set and paths
     * [compile] produces — `outputGroups`, `packageIndexGroups`, and `overviewGroup` — so a sibling
     * tool can position each page in a navigation tree.  The tiers are:
     *
     *  - the `overviewGroup` (when present) is the single top-tier root (`parentTitle == null`);
     *  - under [ProtocGenMarkdown.Options.OutputType.PER_FILE], each package-index document parents
     *    onto the overview, and each per-file document parents onto its package index — or onto the
     *    overview when that package's index was dropped/absent;
     *  - under [ProtocGenMarkdown.Options.OutputType.PER_PACKAGE], each consolidated package
     *    document parents onto the overview;
     *  - under [ProtocGenMarkdown.Options.OutputType.SINGLE_FILE], the single document is itself the
     *    root.
     *
     * When a tier does not exist its children collapse onto the nearest ancestor that does, and the
     * highest surviving tier reports `parentTitle == null`.
     */
    internal fun planDocuments(): List<PlannedDocument> {
        val plan = mutableListOf<PlannedDocument>()
        val overviewTitle = overviewGroup?.title
        overviewGroup?.let {
            plan += PlannedDocument(it.filename, it.title, parentTitle = null, kind = PlannedDocument.Kind.OVERVIEW)
        }

        when (options.outputType) {
            ProtocGenMarkdown.Options.OutputType.PER_FILE -> {
                val indexTitleByPackage = HashMap<String, String>()
                for (group in packageIndexGroups) {
                    val pkg = group.files.firstOrNull()?.`package`?.value.orEmpty()
                    indexTitleByPackage[pkg] = group.title
                    plan += PlannedDocument(
                        group.filename,
                        group.title,
                        parentTitle = overviewTitle,
                        kind = PlannedDocument.Kind.PACKAGE_INDEX,
                    )
                }
                for (group in outputGroups) {
                    val pkg = group.files.first().`package`?.value.orEmpty()
                    plan += PlannedDocument(
                        group.filename,
                        group.title,
                        parentTitle = indexTitleByPackage[pkg] ?: overviewTitle,
                        kind = PlannedDocument.Kind.CONTENT,
                    )
                }
            }

            ProtocGenMarkdown.Options.OutputType.PER_PACKAGE ->
                for (group in outputGroups) {
                    plan += PlannedDocument(
                        group.filename,
                        group.title,
                        parentTitle = overviewTitle,
                        kind = PlannedDocument.Kind.CONTENT,
                    )
                }

            ProtocGenMarkdown.Options.OutputType.SINGLE_FILE ->
                for (group in outputGroups) {
                    plan += PlannedDocument(
                        group.filename,
                        group.title,
                        parentTitle = null,
                        kind = PlannedDocument.Kind.CONTENT,
                    )
                }
        }
        return plan
    }

    /**
     * Multi-line human-readable bundle of every distinct comment-reference failure collected
     * during compile.  Used twice at end-of-compile: once as the body of a `log.error` line that
     * lands in stderr (and `logFile`, when configured) so the customer sees a single
     * consolidated re-iteration alongside the per-occurrence `ERROR` lines emitted by
     * [ReferenceLinkResolver]; once as the payload of [CodeGeneratorResponse.error] so protoc
     * surfaces the same bundle when the plugin is run inside `protoc`.  Each bullet names the
     * proto file, the descriptor whose comment held the bad reference, the bracketed label, and
     * the reason (unresolved or ambiguous with the colliding candidates).  The leading count
     * line distinguishes the singular and plural cases.
     */
    private fun referenceLinkFailureSummary(failures: List<ReferenceLinkResolver.Failure>): String =
        buildString {
            append(failures.size)
            append(if (failures.size == 1) " reference-link failure under " else " reference-link failures under ")
            appendLine("resolveReferenceLinksMode=FAIL_ON_INVALID:")
            for (f in failures) {
                append("  - ")
                append(f.protoFile)
                append(" :: ")
                append(f.scopeFqn.ifEmpty { "(file scope)" })
                append(" :: [")
                append(f.label)
                append("] — ")
                when (val r = f.reason) {
                    is ReferenceLinkResolver.FailureReason.Unresolved ->
                        append("no matching type, field, enum value, or RPC in compile scope")

                    is ReferenceLinkResolver.FailureReason.Ambiguous -> {
                        append("ambiguous; candidates: ")
                        append(r.candidates.joinToString("; "))
                    }
                }
                appendLine()
            }
        }

    /**
     * Files this compile run will render output for: always the entries protoc named in
     * `filesToGenerate`, and additionally — under
     * [ProtocGenMarkdown.Options.TransitiveReferences.INCLUDE_FILES] — every transitive `.proto`
     * file referenced by at least one in-scope type, computed as the fixed point of "the file
     * declares a message or enum referenced from an already-included file."  Sort order: original
     * `filesToGenerate` order first, then any promoted transitive files by `file.name`.
     */
    private val scopeFiles: List<FileDescriptorProtoWrapper> by lazy {
        val toGenerate = request.filesToGenerate.toSet()
        val base = request.protoFiles.filter { it.name in toGenerate }
        val candidate =
            if (options.transitiveReferences != ProtocGenMarkdown.Options.TransitiveReferences.INCLUDE_FILES) {
                base
            } else {
                val byFqn = typeIndex
                val included: MutableSet<FileDescriptorProtoWrapper> = base.toMutableSet()
                var frontier: Set<FileDescriptorProtoWrapper> = base.toSet()
                while (frontier.isNotEmpty()) {
                    val next = mutableSetOf<FileDescriptorProtoWrapper>()
                    for (file in frontier) {
                        for (fqn in referencedTypeFqns(file)) {
                            val ref = byFqn[fqn] ?: continue
                            if (ref.file !in included && ref.file !in next) {
                                log.trace(
                                    "transitive file {} promoted into scope; triggered by reference to {} in {}",
                                    ref.file.name ?: "(unnamed)",
                                    fqn,
                                    file.name ?: "(unnamed)",
                                )
                                next += ref.file
                            }
                        }
                    }
                    included += next
                    frontier = next
                }
                val promoted = included.filterNot { it in base }.sortedBy { it.name ?: "" }
                base + promoted
            }
        val (kept, dropped) = candidate.partition(::hasDocumentableContent)
        for (file in dropped) {
            log.info("dropping {}: no documentable services, messages, or enums", file.name ?: "(unnamed)")
        }
        kept
    }

    /**
     * Predicate at the `scopeFiles` boundary: does this `.proto` declare anything this plugin
     * renders?  Files that fail it carry no services, no top-level non-map-entry messages, and no
     * enums — there's nothing to anchor an output `.md` to, so they are logged at INFO and
     * dropped from every downstream construct (`outputGroups`, `packageIndexGroups`,
     * `fileToGroup`).  Service-only files count as documented (the `Services` section is real
     * output).  Top-level map-entry synthetics — protoc's auto-generated wrappers for `map<K,V>`
     * fields — are not, mirroring the same exclusion the type index and rendering walks apply.
     */
    private fun hasDocumentableContent(file: FileDescriptorProtoWrapper): Boolean =
        file.services.isNotEmpty() ||
            file.messageTypes.any { it.options?.mapEntry?.value != true } ||
            file.enumTypes.isNotEmpty()

    /**
     * Files in `request.protoFiles` that ended up outside [scopeFiles] but contain at least one
     * type referenced from an in-scope file.  Populated only under
     * [ProtocGenMarkdown.Options.TransitiveReferences.LINK_AS_PEER]; empty under
     * [ProtocGenMarkdown.Options.TransitiveReferences.NONE] (no peer rendering implied) and under
     * [ProtocGenMarkdown.Options.TransitiveReferences.INCLUDE_FILES] (the transitive files got
     * promoted into [scopeFiles] instead).  Filename mirrors [perFileFilename] — the convention a
     * sibling protoc run on the dep would land on under [ProtocGenMarkdown.Options.OutputType.PER_FILE].
     */
    private val peerFileToGroup: Map<FileDescriptorProtoWrapper, OutputGroup> by lazy {
        if (options.transitiveReferences != ProtocGenMarkdown.Options.TransitiveReferences.LINK_AS_PEER) {
            return@lazy emptyMap()
        }
        val byFqn = typeIndex
        val inScopeSet = scopeFiles.toSet()
        val peers = mutableSetOf<FileDescriptorProtoWrapper>()
        for (file in scopeFiles) {
            for (fqn in referencedTypeFqns(file)) {
                val ref = byFqn[fqn] ?: continue
                if (ref.file !in inScopeSet && ref.file !in peers) {
                    log.info(
                        "transitive file {} added as peer; triggered by reference to {} in {}",
                        ref.file.name ?: "(unnamed)",
                        fqn,
                        file.name ?: "(unnamed)",
                    )
                    peers += ref.file
                }
            }
        }
        peers.associateWith { f ->
            OutputGroup(perFileFilename(f), listOf(f), titleOf(f))
        }
    }

    private fun referencedTypeFqns(file: FileDescriptorProtoWrapper): Sequence<String> =
        sequence {
            for (m in file.messageTypes) yieldAll(referencedTypeFqnsIn(m))
            for (s in file.services) {
                for (method in s.methods) {
                    method.inputType?.value?.removePrefix(".")?.takeIf { it.isNotEmpty() }
                        ?.takeUnless { it in options.referenceLink }
                        ?.let { yield(it) }
                    method.outputType?.value?.removePrefix(".")?.takeIf { it.isNotEmpty() }
                        ?.takeUnless { it in options.referenceLink }
                        ?.let { yield(it) }
                }
            }
        }

    private fun referencedTypeFqnsIn(msg: DescriptorProtoWrapper): Sequence<String> =
        sequence {
            if (msg.options?.mapEntry?.value == true) return@sequence
            for (f in msg.fields) {
                val tn = f.typeName?.value?.removePrefix(".")?.takeIf { it.isNotEmpty() } ?: continue
                if (tn in options.referenceLink) continue
                yield(tn)
            }
            for (n in msg.nestedTypes) yieldAll(referencedTypeFqnsIn(n))
        }

    /**
     * Maps every file in the compile scope to the [OutputGroup] it will be rendered under.  In
     * [ProtocGenMarkdown.Options.OutputType.PER_FILE] this is a one-to-one identity; in the
     * consolidated modes multiple files share a group.  Used by [appendTypeReference] to decide
     * whether a cross-type link can collapse to a bare `#anchor` (same group), the relative
     * `.md` path to use otherwise, and the heading-path basis the target's anchor is computed
     * from.
     */
    private val fileToGroup: Map<FileDescriptorProtoWrapper, OutputGroup> by lazy {
        outputGroups.flatMap { g -> g.files.map { it to g } }.toMap()
    }

    /**
     * The set of `.md` files this compile will produce, in the order [compile] emits them.
     *
     *  - [ProtocGenMarkdown.Options.OutputType.PER_FILE]: one group per scope file, filename
     *    derived by swapping `.proto` for `.md` on the file's relative path.  H1 title is
     *    the file's relative path.
     *  - [ProtocGenMarkdown.Options.OutputType.PER_PACKAGE]: one group per distinct
     *    `package` declared across the scope files.  When every file declaring a given
     *    non-empty package lives at the directory whose path is the package with `.` → `/`,
     *    the group's filename is `<pkg-as-dir>/package.md` (the "namespaced" case).
     *    Otherwise it is `<fully.qualified.package>.md` at the output root.  Files with no
     *    `package` directive collapse to a single group at `default.md`.  H1 title is
     *    the dotted package (or `Default Package` for the no-package group).
     *  - [ProtocGenMarkdown.Options.OutputType.SINGLE_FILE]: one group containing every
     *    scope file, named `<longest-common-package-prefix>.md` at the output root, or
     *    `overview.md` when no common prefix exists.  H1 title is the longest common
     *    package prefix, or the label `Overview` when no common prefix exists.
     */
    private val outputGroups: List<OutputGroup> by lazy {
        when (options.outputType) {
            ProtocGenMarkdown.Options.OutputType.PER_FILE ->
                scopeFiles.map { f -> OutputGroup(perFileFilename(f), listOf(f), titleOf(f)) }

            ProtocGenMarkdown.Options.OutputType.PER_PACKAGE -> {
                val byPkg = LinkedHashMap<String, MutableList<FileDescriptorProtoWrapper>>()
                for (f in scopeFiles) byPkg.getOrPut(f.`package`?.value.orEmpty()) { mutableListOf() } += f
                byPkg.map { (pkg, pkgFiles) -> packageGroup(pkg, pkgFiles) }
            }

            ProtocGenMarkdown.Options.OutputType.SINGLE_FILE -> {
                val files = scopeFiles
                val lcp = longestCommonPackagePrefix(files.map { it.`package`?.value.orEmpty() })
                val filename = lcp.ifEmpty { "overview" } + ".md"
                val title = lcp.ifEmpty { "Overview" }
                listOf(OutputGroup(filename, sortedGroupFiles(files), title))
            }
        }
    }

    private fun perFileFilename(file: FileDescriptorProtoWrapper): String = (file.name ?: "").removeSuffix(".proto") + ".md"

    /**
     * The set of filenames the per-file output groups will land at, captured for the
     * package-index collision guard.  Only populated under
     * [ProtocGenMarkdown.Options.OutputType.PER_FILE]; empty otherwise (the consolidated modes
     * don't emit per-file `.md`s alongside their package/session files, so there's nothing for
     * an index to collide with).
     */
    private val perFileFilenames: Set<String> by lazy {
        if (options.outputType == ProtocGenMarkdown.Options.OutputType.PER_FILE) {
            scopeFiles.map { perFileFilename(it) }.toSet()
        } else {
            emptySet()
        }
    }

    /**
     * Per-package navigation index files emitted alongside the per-file documents when
     * [ProtocGenMarkdown.Options.includeIndices] is on and
     * [ProtocGenMarkdown.Options.outputType] is [ProtocGenMarkdown.Options.OutputType.PER_FILE].
     * Empty otherwise — no-op under the consolidated [ProtocGenMarkdown.Options.OutputType.PER_PACKAGE]
     * (each package already has its own consolidated document) and under
     * [ProtocGenMarkdown.Options.OutputType.SINGLE_FILE] (the single document is itself the
     * aggregator).  Filename rules mirror [packageGroup]; an index whose computed filename
     * collides with a per-file output (e.g. a proto literally named `<pkg>/package.proto`) is
     * dropped with a warning so the per-file document wins.
     */
    private val packageIndexGroups: List<OutputGroup> by lazy {
        if (options.outputType != ProtocGenMarkdown.Options.OutputType.PER_FILE || !options.includeIndices) {
            return@lazy emptyList()
        }
        val byPkg = LinkedHashMap<String, MutableList<FileDescriptorProtoWrapper>>()
        for (f in scopeFiles) byPkg.getOrPut(f.`package`?.value.orEmpty()) { mutableListOf() } += f
        byPkg.mapNotNull { (pkg, pkgFiles) ->
            val group = packageGroup(pkg, pkgFiles)
            if (group.filename in perFileFilenames) {
                log.warn(
                    "skipping package index for '{}': filename {} collides with a per-file output",
                    pkg.ifEmpty { "(no package)" },
                    group.filename,
                )
                null
            } else {
                group
            }
        }
    }

    /**
     * Single navigation-only `overview.md` at the output root listing every package the compile
     * scope declared, each linked to that package's primary `.md` — the package-index document
     * under [ProtocGenMarkdown.Options.OutputType.PER_FILE] (filename via [packageGroup], same
     * one [packageIndexGroups] lands on), the consolidated package document under
     * [ProtocGenMarkdown.Options.OutputType.PER_PACKAGE] (same filename rule via [packageGroup],
     * which `outputGroups` itself uses for that mode).  Populated when
     * [ProtocGenMarkdown.Options.includeIndices] is on and [ProtocGenMarkdown.Options.outputType]
     * is not [ProtocGenMarkdown.Options.OutputType.SINGLE_FILE] (that mode's single output is
     * itself the aggregator — the no-op INFO line fires from [compile]).  When
     * `overview.md` collides with a per-file output or a consolidated/package-index output the
     * overview is dropped with a warning so the user's content wins.
     */
    private val overviewGroup: OutputGroup? by lazy {
        if (!options.includeIndices || options.outputType == ProtocGenMarkdown.Options.OutputType.SINGLE_FILE) {
            return@lazy null
        }
        if (scopeFiles.isEmpty()) return@lazy null
        val filename = "overview.md"
        val collisions = outputGroups.map { it.filename }.toSet() + packageIndexGroups.map { it.filename }.toSet()
        if (filename in collisions) {
            log.warn("skipping overview: filename {} collides with another output", filename)
            return@lazy null
        }
        val lcp = longestCommonPackagePrefix(scopeFiles.map { it.`package`?.value.orEmpty() })
        val title = lcp.ifEmpty { "Overview" }
        OutputGroup(filename, scopeFiles, title)
    }

    /**
     * Output-file shape for a single proto package's worth of files under
     * [ProtocGenMarkdown.Options.OutputType.PER_PACKAGE] (and reused by [packageIndexGroups]).
     * A non-empty package is treated as "namespaced" when every file declaring it lives at the
     * directory whose path is the package with `.` → `/` (e.g. `foo/bar/whatever.proto` for
     * `package foo.bar`); in that case the consolidated file is `<pkg-as-dir>/package.md`.
     * Otherwise the file is dropped at the output root as `<fully.qualified.package>.md`.
     * Files with no `package` directive collapse into a single `default.md` group titled
     * `Default Package`; non-empty packages produce a title of the dotted package.
     */
    private fun packageGroup(
        pkg: String,
        pkgFiles: List<FileDescriptorProtoWrapper>,
    ): OutputGroup {
        val sortedFiles = sortedGroupFiles(pkgFiles)
        if (pkg.isEmpty()) return OutputGroup("default.md", sortedFiles, "Default Package")
        val pkgAsDir = pkg.replace('.', '/')
        val namespaced =
            pkgFiles.all { f ->
                val name = f.name ?: return@all false
                name.substringBeforeLast('/', missingDelimiterValue = "") == pkgAsDir
            }
        val filename = if (namespaced) "$pkgAsDir/package.md" else "$pkg.md"
        return OutputGroup(filename, sortedFiles, pkg)
    }

    /**
     * Longest segment-wise common prefix of a list of dotted proto package names.  Splits each
     * package on `.`, walks segment-by-segment, and joins the shared prefix back with `.`.
     * Returns `""` when [pkgs] is empty, when any element is empty, or when the very first
     * segments diverge — the caller maps the empty result to the `overview` fallback.
     */
    private fun longestCommonPackagePrefix(pkgs: List<String>): String {
        if (pkgs.isEmpty()) return ""
        val splits = pkgs.map { if (it.isEmpty()) emptyList() else it.split('.') }
        val first = splits.first()
        var shared = 0
        while (shared < first.size && splits.all { it.size > shared && it[shared] == first[shared] }) shared++
        return first.take(shared).joinToString(".")
    }

    private enum class TypeKind { MESSAGE, ENUM }

    /**
     * What a TYPE_MESSAGE / TYPE_ENUM cross-reference resolves to: the file that declares the
     * type, the kind (so we know whether to anchor under the `Messages` or `Enums` section),
     * and the dotted ancestor-prefixed local name within that file (e.g. `Outer.Inner`) — the
     * exact string used as the type's leaf heading text and therefore the basis of its anchor
     * id.  The owning [OutputGroup] (looked up via [fileToGroup]) plus the [OutputGroup.title]
     * provides the rest of the ancestor heading path used for stable anchors.
     */
    private data class TypeRef(
        val file: FileDescriptorProtoWrapper,
        val kind: TypeKind,
        val localName: String,
        val inScope: Boolean,
    )

    /**
     * Fully-qualified type name → resolution metadata, across every file in
     * `request.protoFiles` (every entry protoc carried into the request, transitive
     * dependencies included).  Entries from files outside `filesToGenerate` carry
     * [TypeRef.inScope] = `false` so the resolver downstream can apply the
     * [ProtocGenMarkdown.Options.transitiveReferences] policy.  Map-entry synthetic messages
     * are not indexed (they have no user-facing `### ` heading).  On a same-FQN collision the
     * in-scope entry wins so a type that genuinely lives in `filesToGenerate` can never be
     * shadowed by an identically-named entry from a transitive file (a proto-validity bug,
     * but guarded defensively).
     */
    private val typeIndex: Map<String, TypeRef> by lazy {
        val map = mutableMapOf<String, TypeRef>()
        val toGenerate = request.filesToGenerate.toSet()
        for (file in request.protoFiles) {
            val inScope = file.name in toGenerate
            val pkg = file.`package`?.value
            val pkgPrefix = if (pkg.isNullOrEmpty()) "" else "$pkg."
            for (m in file.messageTypes) indexMessage(m, pkgPrefix, "", file, inScope, map)
            for (e in file.enumTypes) {
                val ename = e.name?.value ?: continue
                putTypeRef(map, "$pkgPrefix$ename", TypeRef(file, TypeKind.ENUM, ename, inScope))
            }
        }
        map
    }

    private fun indexMessage(
        msg: DescriptorProtoWrapper,
        pkgPrefix: String,
        localPrefix: String,
        file: FileDescriptorProtoWrapper,
        inScope: Boolean,
        map: MutableMap<String, TypeRef>,
    ) {
        if (msg.options?.mapEntry?.value == true) return
        val name = msg.name?.value ?: return
        val localName = "$localPrefix$name"
        putTypeRef(map, "$pkgPrefix$localName", TypeRef(file, TypeKind.MESSAGE, localName, inScope))
        val childLocal = "$localName."
        for (n in msg.nestedTypes) indexMessage(n, pkgPrefix, childLocal, file, inScope, map)
        for (e in msg.enumTypes) {
            val ename = e.name?.value ?: continue
            val enumLocal = "$childLocal$ename"
            putTypeRef(map, "$pkgPrefix$enumLocal", TypeRef(file, TypeKind.ENUM, enumLocal, inScope))
        }
    }

    private fun putTypeRef(
        map: MutableMap<String, TypeRef>,
        key: String,
        ref: TypeRef,
    ) {
        val existing = map[key]
        if (existing == null || (!existing.inScope && ref.inScope)) {
            map[key] = ref
        }
    }

    private fun outlineDocument(group: OutputGroup): Document {
        headings.clear()
        bodyLevelShift = if (group.consolidated) 1 else 0
        currentResolver =
            if (options.resolveReferenceLinksMode != ProtocGenMarkdown.Options.ResolveReferenceLinksMode.NONE) {
                ReferenceLinkResolver(
                    scopeFiles = scopeFiles,
                    peerFiles = peerFileToGroup.keys.toList(),
                    options = options,
                    fileToGroup = fileToGroup,
                    peerFileToGroup = peerFileToGroup,
                    mode = options.resolveReferenceLinksMode,
                    referenceLinkOverrides = options.referenceLink,
                    hrefFor = { file, path -> hrefFor(file, path, group.filename) },
                )
            } else {
                null
            }
        val doc = Document()
        val groupPath = listOf(group.title)
        doc.appendChild(frontmatterBlock())
        doc.appendChild(fixedHeading(1, group.title, groupPath))
        if (group.consolidated) {
            appendInsertionPoint(doc, "file_header")
        }

        val files = group.files.map(::fileBodyOf)
        val hasBody = files.any { it.hasBody }
        val thematicBreak: ThematicBreak? = if (hasBody) ThematicBreak().also { doc.appendChild(it) } else null

        for (body in files) appendFileBody(doc, body, group, groupPath)

        if (thematicBreak != null) renderTableOfContents(thematicBreak)

        appendInsertionPoint(doc, "file_footer")

        return doc
    }

    /**
     * A scope file with its `Messages` and `Enums` walks materialized once so [appendFileBody]
     * and the [outlineDocument] thematic-break check don't both have to walk the message tree.
     * `hasBody` is the "the file declares at least one service/message/enum" predicate that
     * gates the per-doc thematic break under the group's H1.
     */
    private class FileBody(
        val file: FileDescriptorProtoWrapper,
        val services: List<ServiceDescriptorProtoWrapper>,
        val messages: List<Pair<String, DescriptorProtoWrapper>>,
        val enums: List<Pair<String, EnumDescriptorProtoWrapper>>,
    ) {
        val hasBody: Boolean = services.isNotEmpty() || messages.isNotEmpty() || enums.isNotEmpty()
    }

    /**
     * Materialize a file's [FileBody] — the once-walked, sort-mode-applied lists of services,
     * messages, and enums [outlineDocument] and the package-index renderer both iterate over.
     * Lifted out of [outlineDocument] so [packageIndexDocument] can call it without going
     * through the per-file rendering path.
     */
    private fun fileBodyOf(file: FileDescriptorProtoWrapper): FileBody {
        val messages = sortedNamedTypes(collectMessages(file))
        val enums = sortedNamedTypes(collectEnums(file, messages))
        return FileBody(file, sortedServices(file.services), messages, enums)
    }

    /**
     * Render the navigation-only `.md` for a package index group.  The document carries the
     * shared frontmatter and a single H1 carrying the group's title
     * (the dotted package, or `Default Package` for files with no `package`
     * directive), then a `file → section → type → member` bulleted Table of Contents whose
     * every entry hyperlinks to an anchor inside one of the per-file `.md`s.  Anchor naming
     * honors [ProtocGenMarkdown.Options.generateStableAnchors] via [hrefFor].
     *
     * Built directly out of [Heading] / [BulletList] / [ListItem] nodes — no calls to
     * [headingOf] / [fixedHeading], which would scribble entries into the shared [headings]
     * list that the per-file Table of Contents consumes.  Sort order mirrors the per-file
     * documents so a member-level bullet here lines up with the matching expansion there.
     */
    private fun packageIndexDocument(group: OutputGroup): Document {
        val doc = Document()
        doc.appendChild(frontmatterBlock())
        val titlePath = listOf(group.title)
        doc.appendChild(
            Heading().apply {
                level = 1
                if (options.generateStableAnchors) appendChild(htmlInline("<a id=\"${pathAnchor(titlePath)}\"></a>"))
                appendChild(Text(group.title))
            },
        )
        appendInsertionPoint(doc, "file_header")

        val bodies = group.files.map(::fileBodyOf)
        if (bodies.any { it.hasBody }) doc.appendChild(ThematicBreak())

        val outer = BulletList()
        val currentMd = group.filename
        for (body in bodies) outer.appendChild(packageIndexFileItem(body, currentMd))
        if (outer.firstChild != null) doc.appendChild(outer)

        appendInsertionPoint(doc, "file_footer")
        return doc
    }

    /**
     * Render the navigation-only `overview.md` for [overviewGroup].  Frontmatter + an H1
     * (`# Overview` or `# <longest-common-package-prefix>`, matching
     * [ProtocGenMarkdown.Options.OutputType.SINGLE_FILE]'s title shape) + a thematic break + a
     * flat bulleted list of `[<package>](<relative-link-to-pkg-md>)` entries, one per distinct
     * proto package in [scopeFiles], sorted alphabetically by dotted name.  The link target is
     * the filename [packageGroup] produces — under `PER_FILE` that's the package index this
     * overview is the parent of, under `PER_PACKAGE` it's the consolidated package document
     * itself.  The empty package collapses to a single `(no package)` entry pointing at
     * `default.md`.
     */
    private fun overviewDocument(group: OutputGroup): Document {
        val doc = Document()
        doc.appendChild(frontmatterBlock())
        val titlePath = listOf(group.title)
        doc.appendChild(
            Heading().apply {
                level = 1
                if (options.generateStableAnchors) appendChild(htmlInline("<a id=\"${pathAnchor(titlePath)}\"></a>"))
                appendChild(Text(group.title))
            },
        )
        appendInsertionPoint(doc, "file_header")

        val byPkg = sortedMapOf<String, MutableList<FileDescriptorProtoWrapper>>()
        for (f in scopeFiles) byPkg.getOrPut(f.`package`?.value.orEmpty()) { mutableListOf() } += f

        if (byPkg.isNotEmpty()) doc.appendChild(ThematicBreak())

        val list = BulletList()
        val currentMd = group.filename
        for ((pkg, pkgFiles) in byPkg) {
            val pkgGroup = packageGroup(pkg, pkgFiles)
            val display = if (pkg.isEmpty()) "(no package)" else pkg
            val href = relativeLink(currentMd, pkgGroup.filename)
            val link = Link(href, null).apply { appendChild(Text(display)) }
            list.appendChild(ListItem().apply { appendChild(Paragraph().apply { appendChild(link) }) })
        }
        if (list.firstChild != null) doc.appendChild(list)

        appendInsertionPoint(doc, "file_footer")
        return doc
    }

    /**
     * Build the `<li>` for one input proto under a package index: a paragraph linking to the
     * file's per-file `.md` H1 anchor, followed by a nested bullet list with one item per
     * non-empty section (Services / Messages / Enums), each containing per-type bullets.
     * Members (RPCs under services, fields under messages, values under enums) are
     * intentionally omitted — the index is a navigation aid to the type, not a re-exposure of
     * the per-file Field Summary / RPC Summary / Value Summary tables.
     */
    private fun packageIndexFileItem(
        body: FileBody,
        currentMd: String,
    ): ListItem {
        val file = body.file
        val fileTitle = titleOf(file)
        val filePath = listOf(fileToGroup[file]!!.title)
        val item = ListItem().apply { appendChild(crossFileLinkParagraph(fileTitle, file, filePath, currentMd)) }

        val inner = BulletList()
        if (body.services.isNotEmpty()) {
            val sectionPath = filePath + "Services"
            val sectionItem = ListItem().apply { appendChild(crossFileLinkParagraph("Services", file, sectionPath, currentMd)) }
            val typesList = BulletList()
            for (s in body.services) {
                val sname = s.name?.value ?: "(unnamed)"
                val svcPath = sectionPath + sname
                typesList.appendChild(ListItem().apply { appendChild(crossFileLinkParagraph(sname, file, svcPath, currentMd)) })
            }
            sectionItem.appendChild(typesList)
            inner.appendChild(sectionItem)
        }
        if (body.messages.isNotEmpty()) {
            val sectionPath = filePath + "Messages"
            val sectionItem = ListItem().apply { appendChild(crossFileLinkParagraph("Messages", file, sectionPath, currentMd)) }
            val typesList = BulletList()
            for ((name, _) in body.messages) {
                val msgPath = sectionPath + name
                typesList.appendChild(ListItem().apply { appendChild(crossFileLinkParagraph(name, file, msgPath, currentMd)) })
            }
            sectionItem.appendChild(typesList)
            inner.appendChild(sectionItem)
        }
        if (body.enums.isNotEmpty()) {
            val sectionPath = filePath + "Enums"
            val sectionItem = ListItem().apply { appendChild(crossFileLinkParagraph("Enums", file, sectionPath, currentMd)) }
            val typesList = BulletList()
            for ((name, _) in body.enums) {
                val enumPath = sectionPath + name
                typesList.appendChild(ListItem().apply { appendChild(crossFileLinkParagraph(name, file, enumPath, currentMd)) })
            }
            sectionItem.appendChild(typesList)
            inner.appendChild(sectionItem)
        }
        if (inner.firstChild != null) item.appendChild(inner)
        return item
    }

    /**
     * Wrap [hrefFor] in a one-link [Paragraph] for use as a [ListItem] body in
     * [packageIndexFileItem].  Centralizes the cross-file link composition so every bullet in
     * the index — file, section, and type — goes through the same path/anchor pipeline as
     * summary-table type cells and resolved comment references.
     */
    private fun crossFileLinkParagraph(
        text: String,
        targetFile: FileDescriptorProtoWrapper,
        path: List<String>,
        currentMd: String,
    ): Paragraph {
        val link = Link(hrefFor(targetFile, path, currentMd), null).apply { appendChild(Text(text)) }
        return Paragraph().apply { appendChild(link) }
    }

    private fun appendFileBody(
        doc: Document,
        body: FileBody,
        group: OutputGroup,
        groupPath: List<String>,
    ) {
        val file = body.file
        val sci = file.sourceCodeInfo
        val currentMd = group.filename
        val pkg = file.`package`?.value.orEmpty()
        val fqn: (String) -> String = { name -> if (pkg.isEmpty()) name else "$pkg.$name" }

        val fileTitle = titleOf(file)
        val filePath: List<String> =
            if (group.consolidated) {
                appendSectionRule(doc, 2)
                val fp = groupPath + fileTitle
                doc.appendChild(fixedHeading(2, fileTitle, fp))
                fp
            } else {
                groupPath
            }
        appendInsertionPoint(doc, "file_header_scope:$fileTitle")
        appendInsertionPoint(doc, "file_scope:$fileTitle")

        val sectionLevel = HEADING_SECTION + bodyLevelShift

        if (body.services.isNotEmpty()) {
            appendSectionRule(doc, sectionLevel)
            val sectionPath = filePath + "Services"
            doc.appendChild(headingOf(HEADING_SECTION, "Services", sectionPath))
            appendInsertionPoint(doc, "services_section")
            for (s in body.services) {
                val sname = s.name?.value ?: "(unnamed)"
                val sFqn = fqn(sname)
                val svcPath = sectionPath + sname
                doc.appendChild(headingOf(HEADING_TYPE, sname, svcPath))
                appendInsertionPoint(doc, "service_header_scope:$sFqn")
                appendLeadingComment(doc, sci, s, scopeFqn = sFqn)
                appendInsertionPoint(doc, "service_scope:$sFqn")
                appendRpcTable(doc, s, sci, currentMd, svcPath, sFqn)
            }
        }

        if (body.messages.isNotEmpty()) {
            appendSectionRule(doc, sectionLevel)
            val sectionPath = filePath + "Messages"
            doc.appendChild(headingOf(HEADING_SECTION, "Messages", sectionPath))
            appendInsertionPoint(doc, "messages_section")
            for ((name, msg) in body.messages) {
                val mFqn = fqn(name)
                val msgPath = sectionPath + name
                doc.appendChild(headingOf(HEADING_TYPE, name, msgPath))
                appendInsertionPoint(doc, "message_header_scope:$mFqn")
                appendLeadingComment(doc, sci, msg, scopeFqn = mFqn)
                appendInsertionPoint(doc, "message_scope:$mFqn")
                appendFieldsTable(doc, msg, sci, currentMd, msgPath, mFqn)
            }
        }

        if (body.enums.isNotEmpty()) {
            appendSectionRule(doc, sectionLevel)
            val sectionPath = filePath + "Enums"
            doc.appendChild(headingOf(HEADING_SECTION, "Enums", sectionPath))
            appendInsertionPoint(doc, "enums_section")
            for ((name, enum) in body.enums) {
                val eFqn = fqn(name)
                val enumPath = sectionPath + name
                doc.appendChild(headingOf(HEADING_TYPE, name, enumPath))
                appendInsertionPoint(doc, "enum_header_scope:$eFqn")
                appendLeadingComment(doc, sci, enum, scopeFqn = eFqn)
                appendInsertionPoint(doc, "enum_scope:$eFqn")
                appendValuesTable(doc, enum, sci, enumPath, eFqn)
            }
        }
    }

    /**
     * Append a `___` thematic break before a significant section heading that's about to be
     * emitted at [aboutToEmitLevel] — the per-file L2 in the consolidated modes and the
     * `Services`/`Messages`/`Enums` section heading at every output type.  Skipped in two
     * cases so visually-adjacent rules don't pile up:
     *
     *  - The most recent significant child is already a [ThematicBreak] (the under-title rule,
     *    which we treat as the document's first section break — the first significant section
     *    gets no additional rule of its own).
     *  - The most recent significant child is a [Heading] at a strictly shallower level than
     *    [aboutToEmitLevel] (so e.g. the L3 `### Services` immediately following the per-file
     *    L2 `## file.proto` in a consolidated doc doesn't introduce a rule that would visually
     *    split a heading from its first sub-section).
     *
     * Trailing insertion-point HTML-comment blocks are transparent to this check so the
     * `file_header_scope:<path>` / `file_scope:<path>` markers that immediately follow a file
     * heading don't break either adjacency invariant.
     */
    private fun appendSectionRule(
        doc: Document,
        aboutToEmitLevel: Int,
    ) {
        var last: Node? = doc.lastChild
        while (last is HtmlBlock && last.literal.startsWith("<!-- @@protoc_insertion_point(")) {
            last = last.previous
        }
        when (last) {
            is ThematicBreak -> return
            is Heading -> if (last.level < aboutToEmitLevel) return
        }
        doc.appendChild(ThematicBreak())
    }

    /**
     * Splice a collapsible `<details>` block listing the file's headings into the document
     * immediately after [anchor] (the title's `___` thematic break).  Which headings appear is
     * controlled by [ProtocGenMarkdown.Options.minTableOfContentsHeader] /
     * [ProtocGenMarkdown.Options.maxTableOfContentsHeader]:
     *
     *  - both `null` → no TOC is rendered at all.  The thematic break stays put.
     *  - min `null`, max set → min is treated as 1 (the document's L1 title heading is included).
     *  - max `null`, min set → all headings at level `>= min` are included.
     *  - both set → headings whose level is in `[min, max]` inclusive are included.
     *  - min > max → a warning is logged and no TOC is rendered.  Thematic break stays put.
     *
     * The bullet hierarchy is reconstructed from heading levels via [buildHeadingBulletList].
     * Anchor targets come from [linkParagraph]/[anchorFor], so they respect
     * [ProtocGenMarkdown.Options.generateStableAnchors].
     */
    private fun renderTableOfContents(anchor: Node) {
        val minOpt = options.minTableOfContentsHeader
        val maxOpt = options.maxTableOfContentsHeader
        if (minOpt == null && maxOpt == null) return
        if (minOpt != null && maxOpt != null && minOpt > maxOpt) {
            log.warn(
                "minTableOfContentsHeader={} > maxTableOfContentsHeader={}; no table of contents will be generated",
                minOpt,
                maxOpt,
            )
            return
        }
        val effectiveMin = minOpt ?: 1
        val effectiveMax = maxOpt ?: Int.MAX_VALUE
        val filtered = headings.filter { it.level in effectiveMin..effectiveMax }
        if (filtered.isEmpty()) return

        val list = buildHeadingBulletList(filtered)
        for (node in listOf(htmlBlock("<details>\n<summary>Table of contents</summary>"), list, htmlBlock("</details>")).reversed()) {
            anchor.insertAfter(node)
        }
    }

    /**
     * Reconstruct a nested [BulletList] tree from a flat document-ordered list of [HeadingRef]
     * entries via a stack-based algorithm.  Each entry becomes a [ListItem] whose paragraph is
     * an intra-document link from [linkParagraph]; deeper-level entries nest inside the most
     * recently added shallower item.  Gaps in level (e.g. an L5 immediately after an L3 with no
     * intermediate L4) collapse naturally — the deeper entry attaches at whichever existing
     * level is shallower than it, without synthesizing intermediate bullets.
     */
    private fun buildHeadingBulletList(filtered: List<HeadingRef>): BulletList {
        val root = BulletList()
        val stack: ArrayDeque<Pair<Int, BulletList>> = ArrayDeque()
        stack.addLast(filtered.first().level to root)
        for (h in filtered) {
            while (stack.last().first > h.level) stack.removeLast()
            if (stack.last().first < h.level) {
                val parentList = stack.last().second
                val parentItem = parentList.lastChild as ListItem
                val nested = BulletList()
                parentItem.appendChild(nested)
                stack.addLast(h.level to nested)
            }
            val item = ListItem().apply { appendChild(linkParagraph(h.text, h.path)) }
            stack.last().second.appendChild(item)
        }
        return root
    }

    private fun linkParagraph(
        text: String,
        path: List<String>,
    ): Paragraph {
        val link = Link("#" + anchorFor(path), null)
        link.appendChild(Text(text))
        return Paragraph().apply { appendChild(link) }
    }

    /**
     * Pick the anchor id for [path] based on [ProtocGenMarkdown.Options.generateStableAnchors].
     * When true, returns the path-based id from [pathAnchor] — full ancestor disambiguation.
     * When false, returns the GFM-style slug of the leaf heading text from [slugify] — what the
     * renderer's heading-text auto-anchor would resolve to.
     */
    private fun anchorFor(path: List<String>): String = if (options.generateStableAnchors) pathAnchor(path) else slugify(path.last())

    /**
     * Build the anchor id for a heading from its full path of ancestor heading texts (including
     * the heading itself).  Path segments are joined with `-`; characters other than ASCII
     * letters, digits, `-`, or `_` are replaced by `_`, so paths like
     * `[engine/protoc/markdown/example/hello/hello.proto, Messages, Greeting, Field Details, text]`
     * yield `engine_protoc_markdown_example_hello_hello_proto-Messages-Greeting-Field_Details-text`.
     * Disambiguates same-named headings under different parents (the markdown default-anchor
     * problem this scheme exists to solve).
     */
    private fun pathAnchor(path: List<String>): String =
        path.joinToString("-") { segment ->
            buildString {
                for (c in segment) {
                    if (c.isLetterOrDigit() || c == '-' || c == '_') append(c) else append('_')
                }
            }
        }

    /**
     * GFM-style slug of [text]: lowercase, whitespace replaced with `-`, characters other than
     * alphanumerics/`-`/`_` dropped, leading/trailing `-` trimmed.  Mirrors the auto-anchor most
     * markdown renderers derive from heading text, so `## Field Summary` resolves to
     * `#field-summary` without any explicit anchor element.
     */
    private fun slugify(text: String): String {
        val sb = StringBuilder()
        for (c in text.lowercase()) {
            when {
                c.isLetterOrDigit() || c == '-' || c == '_' -> sb.append(c)
                c.isWhitespace() -> sb.append('-')
            }
        }
        return sb.toString().trim('-')
    }

    private fun htmlBlock(literal: String): HtmlBlock {
        val node = HtmlBlock()
        node.literal = literal
        return node
    }

    /**
     * Append a `<!-- @@protoc_insertion_point(NAME) -->` HTML-comment marker so a sibling protoc
     * plugin can splice content in immediately before the marker line via the standard
     * `CodeGeneratorResponse.File.insertion_point` mechanism.  Gated by
     * [ProtocGenMarkdown.Options.generateInsertionPoints]; a no-op when the option is off.
     * Emitted as an [HtmlBlock] so [MarkdownRenderer] keeps the marker on its own line — protoc's
     * insertion-point parser is line-oriented.
     */
    private fun appendInsertionPoint(
        doc: Document,
        name: String,
    ) {
        if (!options.generateInsertionPoints) return
        doc.appendChild(htmlBlock("<!-- @@protoc_insertion_point($name) -->"))
    }

    /**
     * Build the YAML frontmatter block emitted at the top of every generated document, modeled as
     * a [YamlFrontMatterBlock] from `commonmark-ext-yaml-front-matter` so the AST carries a typed
     * front-matter node rather than an [HtmlBlock] masquerading as YAML.  The block holds three
     * flat [YamlFrontMatterNode] children:
     *
     *  - `generated-by` — the GitHub release-tag URL.  Carries plugin identity (the path) and
     *    version (the tag) in a single value, replacing the older `generator`/`version`/`release`
     *    triple.
     *  - `protoc-gen-markdown-generated-on` — the generation instant (ISO-8601 from [clock]).
     *  - `protoc-gen-markdown-options` — every [ProtocGenMarkdown.Options] property snapshotted
     *    into a comma-separated `key=value` string in declaration order, including options left
     *    at their defaults, so the line reads like a `--markdown_out=` parameter string.
     *
     * The two plugin-namespaced keys carry the `protoc-gen-markdown-` prefix so they don't
     * collide with any top-level keys a sibling plugin might splice in at the `frontmatter`
     * insertion point (see [ProtocGenMarkdown.Options.generateInsertionPoints]).
     *
     * Actual rendering of the YAML — the `---` fences, the `key: value` lines, the optional
     * `# @@protoc_insertion_point(frontmatter)` marker before the closing fence — lives in
     * [FrontmatterRenderer], registered on [renderer] at the [MarkdownRenderer.Builder].
     */
    private fun frontmatterBlock(): YamlFrontMatterBlock {
        val version = Version.value
        val block = YamlFrontMatterBlock()
        block.appendChild(YamlFrontMatterNode("generated-by", listOf("https://github.com/hotelengine/protoc-gen-markdown/releases/tag/$version")))
        block.appendChild(YamlFrontMatterNode("protoc-gen-markdown-generated-on", listOf(DateTimeFormatter.ISO_INSTANT.format(clock.instant()))))
        block.appendChild(YamlFrontMatterNode("protoc-gen-markdown-options", listOf(formatOptions())))
        return block
    }

    /**
     * Render every [ProtocGenMarkdown.Options] property as `name=value`, joined with `,`, in
     * declaration order.  Booleans render as `true`/`false`; nullable ints render as the integer
     * or the literal `null`.  Matches the formatting accepted by the `--markdown_out=` parameter
     * parser so the resulting line is a round-trippable snapshot of the compile invocation.
     */
    private fun formatOptions(): String {
        val parts = mutableListOf<Pair<String, String>>()
        parts += "generateStableAnchors" to options.generateStableAnchors.toString()
        parts += "generateInsertionPoints" to options.generateInsertionPoints.toString()
        parts += "minTableOfContentsHeader" to (options.minTableOfContentsHeader?.toString() ?: "null")
        parts += "maxTableOfContentsHeader" to (options.maxTableOfContentsHeader?.toString() ?: "null")
        parts += "outputType" to options.outputType.name
        parts += "includeIndices" to options.includeIndices.toString()
        parts += "typeSortMode" to options.typeSortMode.name
        parts += "fileSortMode" to options.fileSortMode.name
        parts += "rpcSortMode" to options.rpcSortMode.name
        parts += "fieldSortMode" to options.fieldSortMode.name
        parts += "enumValueSortMode" to options.enumValueSortMode.name
        parts += "resolveReferenceLinksMode" to options.resolveReferenceLinksMode.name
        for ((label, url) in options.referenceLink) parts += "referenceLink" to "$label=$url"
        return parts.joinToString(",") { (k, v) -> "$k=$v" }
    }

    /**
     * `NodeRenderer` for [YamlFrontMatterBlock] and [YamlFrontMatterNode].  The
     * `commonmark-ext-yaml-front-matter` extension is parser-only — neither [MarkdownRenderer]
     * nor `HtmlRenderer` ships built-in rendering for its node types — so we register this
     * factory on the [MarkdownRenderer.Builder] to teach the renderer how to emit them.
     *
     * Layout:
     *  - For a [YamlFrontMatterBlock]: write the opening `---` fence, dispatch each
     *    [YamlFrontMatterNode] child through [context], optionally write the
     *    `# @@protoc_insertion_point(frontmatter)` marker (when [emitInsertionPoint] is true) so
     *    sibling protoc plugins can splice extra top-level YAML keys, then the closing `---`
     *    fence followed by a `block()` separator so a blank line lands between the frontmatter
     *    and the H1 title that follows.
     *  - For a [YamlFrontMatterNode]: write `<key>: <values joined with ", ">` and a newline.
     *    The plugin always passes single-element value lists, so the join is a no-op here, but
     *    multi-value support is preserved for fidelity with the node model.
     */
    private class FrontmatterRenderer(
        private val context: MarkdownNodeRendererContext,
        private val emitInsertionPoint: Boolean,
    ) : NodeRenderer {
        override fun getNodeTypes(): Set<Class<out Node>> = setOf(YamlFrontMatterBlock::class.java, YamlFrontMatterNode::class.java)

        override fun render(node: Node) {
            val w = context.writer
            when (node) {
                is YamlFrontMatterBlock -> {
                    w.raw("---")
                    w.line()
                    var child = node.firstChild
                    while (child != null) {
                        context.render(child)
                        child = child.next
                    }
                    if (emitInsertionPoint) {
                        w.raw("# @@protoc_insertion_point(frontmatter)")
                        w.line()
                    }
                    w.raw("---")
                    w.block()
                }

                is YamlFrontMatterNode -> {
                    w.raw(node.key)
                    w.raw(": ")
                    w.raw(node.values.joinToString(", "))
                    w.line()
                }
            }
        }
    }

    /** Full relative path as protoc sees it — e.g. `foo/bar/baz.proto`. */
    private fun titleOf(file: FileDescriptorProtoWrapper): String = file.name ?: "(unnamed)"

    /**
     * Depth-first walk of the file's message tree.  Each non-map-entry message contributes one
     * entry, dotted ancestor-prefixed.  Map-entry messages are skipped entirely (not yielded,
     * not descended into).
     */
    private fun collectMessages(file: FileDescriptorProtoWrapper): List<Pair<String, DescriptorProtoWrapper>> {
        val out = mutableListOf<Pair<String, DescriptorProtoWrapper>>()
        for (m in file.messageTypes) walkMessage(m, "", out)
        return out
    }

    private fun walkMessage(
        msg: DescriptorProtoWrapper,
        prefix: String,
        out: MutableList<Pair<String, DescriptorProtoWrapper>>,
    ) {
        if (msg.options?.mapEntry?.value == true) return
        val name = msg.name?.value ?: "(unnamed)"
        val qualified = if (prefix.isEmpty()) name else "$prefix.$name"
        out += qualified to msg
        for (n in msg.nestedTypes) walkMessage(n, qualified, out)
    }

    /**
     * File-level enums first (no prefix), then each non-map-entry message's nested enums in the
     * order the messages were discovered.  Walks the same tree as [collectMessages] so the
     * map-entry skip stays consistent.
     */
    private fun collectEnums(
        file: FileDescriptorProtoWrapper,
        messages: List<Pair<String, DescriptorProtoWrapper>>,
    ): List<Pair<String, EnumDescriptorProtoWrapper>> {
        val out = mutableListOf<Pair<String, EnumDescriptorProtoWrapper>>()
        for (e in file.enumTypes) out += enumName("", e) to e
        for ((prefix, msg) in messages) {
            for (e in msg.enumTypes) out += enumName(prefix, e) to e
        }
        return out
    }

    private fun enumName(
        prefix: String,
        enum: EnumDescriptorProtoWrapper,
    ): String {
        val name = enum.name?.value ?: "(unnamed)"
        return if (prefix.isEmpty()) name else "$prefix.$name"
    }

    /**
     * Per-section sorters.  Each consults the matching [ProtocGenMarkdown.Options] sort-mode
     * field and returns a new list ordered accordingly; [SortMode.ENCOUNTER] / the descriptor
     * order returns the input list unchanged.  [sortedNamedTypes] takes the already-flattened
     * `(full-dotted-name, descriptor)` pairs produced by [collectMessages] / [collectEnums],
     * so `ALPHABETICAL` lands a parent immediately before its children
     * (e.g. `Foo` < `Foo.Inner`).  [sortedFields] (consulting [ProtocGenMarkdown.Options.fieldSortMode])
     * and [sortedEnumValues] (consulting [ProtocGenMarkdown.Options.enumValueSortMode])
     * additionally honor [MemberSortMode.NUMBER] by the proto field number / enum value number.
     */
    private fun sortedServices(services: List<ServiceDescriptorProtoWrapper>): List<ServiceDescriptorProtoWrapper> =
        when (options.typeSortMode) {
            SortMode.ALPHABETICAL -> services.sortedBy { it.name?.value ?: "" }
            SortMode.ENCOUNTER -> services
        }

    private fun <T> sortedNamedTypes(items: List<Pair<String, T>>): List<Pair<String, T>> =
        when (options.typeSortMode) {
            SortMode.ALPHABETICAL -> items.sortedBy { it.first }
            SortMode.ENCOUNTER -> items
        }

    private fun sortedGroupFiles(files: List<FileDescriptorProtoWrapper>): List<FileDescriptorProtoWrapper> =
        when (options.fileSortMode) {
            SortMode.ALPHABETICAL -> files.sortedBy { it.name ?: "" }
            SortMode.ENCOUNTER -> files
        }

    private fun sortedMethods(methods: List<MethodDescriptorProtoWrapper>): List<MethodDescriptorProtoWrapper> =
        when (options.rpcSortMode) {
            SortMode.ALPHABETICAL -> methods.sortedBy { it.name?.value ?: "" }
            SortMode.ENCOUNTER -> methods
        }

    private fun sortedFields(fields: List<FieldDescriptorProtoWrapper>): List<FieldDescriptorProtoWrapper> =
        when (options.fieldSortMode) {
            MemberSortMode.ALPHABETICAL -> fields.sortedBy { it.name?.value ?: "" }
            MemberSortMode.ENCOUNTER -> fields
            MemberSortMode.NUMBER -> fields.sortedBy { it.number?.value ?: 0 }
        }

    private fun sortedEnumValues(values: List<EnumValueDescriptorProtoWrapper>): List<EnumValueDescriptorProtoWrapper> =
        when (options.enumValueSortMode) {
            MemberSortMode.ALPHABETICAL -> values.sortedBy { it.name?.value ?: "" }
            MemberSortMode.ENCOUNTER -> values
            MemberSortMode.NUMBER -> values.sortedBy { it.number?.value ?: 0 }
        }

    /**
     * Look up the leading proto comment for [locatable] and append its parsed CommonMark blocks
     * to [doc].  Treats the cleaned comment text as a CommonMark fragment, so lists, blockquotes,
     * fenced code, links, etc. round-trip through the AST and re-render correctly.
     *
     * When [ProtocGenMarkdown.Options.resolveReferenceLinks] is on and [scopeFqn] is non-empty,
     * the parsed AST is passed through [currentResolver]'s `rewrite` step before being spliced
     * into [doc] — converting shortcut-reference `[name]` patterns into real [Link] nodes when
     * the name resolves against the compile-scope's types and members.
     */
    private fun appendLeadingComment(
        doc: Document,
        sci: SourceCodeInfoWrapper?,
        locatable: Locatable,
        scopeFqn: String = "",
    ) {
        val text = sci?.findLocation(locatable)?.leadingComments?.cleaned ?: return
        if (text.isBlank()) return
        val parsed = parseUnderScope(text, scopeFqn)
        while (true) {
            val child = parsed.firstChild ?: break
            doc.appendChild(child)
        }
    }

    /**
     * Parse [text] with the active [ReferenceLinkResolver]'s scope bound to [scopeFqn] for
     * the duration of the parse, so bracketed references inside the comment resolve relative
     * to the descriptor that owns it.  Scope is cleared in a `finally` so a thrown parser
     * exception cannot leak a stale scope into the next comment.
     */
    private fun parseUnderScope(
        text: String,
        scopeFqn: String,
    ): Node {
        val resolver = currentResolver
        resolver?.setCurrentScope(scopeFqn)
        return try {
            parser.parse(text)
        } finally {
            resolver?.clearCurrentScope()
        }
    }

    // ===== Fields table =========================================================================

    private fun appendFieldsTable(
        doc: Document,
        msg: DescriptorProtoWrapper,
        sci: SourceCodeInfoWrapper?,
        currentMd: String,
        msgPath: List<String>,
        msgFqn: String,
    ) {
        if (msg.fields.isEmpty()) return

        doc.appendChild(headingOf(HEADING_FIELD_SECTION, "Field Summary", msgPath + "Field Summary"))

        val detailsPath = msgPath + "Field Details"
        val orderedFields = sortedFields(msg.fields)
        val table = TableBlock()
        val head = TableHead()
        head.appendChild(headerRow("Name", "Type", "Description"))
        table.appendChild(head)
        val body = TableBody()
        for (field in orderedFields) {
            val row = TableRow()
            row.appendChild(TableCell().apply { appendChild(Text(field.name?.value ?: "(unnamed)")) })
            row.appendChild(typeCell(field, currentMd))
            val fname = field.name?.value ?: "(unnamed)"
            row.appendChild(descriptionCell(field, sci, "$msgFqn.$fname", detailsPath))
            body.appendChild(row)
        }
        table.appendChild(body)
        doc.appendChild(table)

        doc.appendChild(headingOf(HEADING_FIELD_SECTION, "Field Details", detailsPath))
        for (field in orderedFields) {
            val fname = field.name?.value ?: "(unnamed)"
            doc.appendChild(headingOf(HEADING_FIELD, fname, detailsPath + fname))
            appendLeadingComment(doc, sci, field, scopeFqn = "$msgFqn.$fname")
        }
    }

    private fun headerRow(vararg labels: String): TableRow {
        val row = TableRow()
        for (label in labels) {
            row.appendChild(
                TableCell().apply {
                    isHeader = true
                    appendChild(Text(label))
                },
            )
        }
        return row
    }

    private fun typeCell(
        field: FieldDescriptorProtoWrapper,
        currentMd: String,
    ): TableCell {
        val cell = TableCell()
        if (field.label?.value == Label.LABEL_REPEATED) cell.appendChild(Text("repeated "))
        when (val t = field.type?.value) {
            Type.TYPE_MESSAGE, Type.TYPE_ENUM, Type.TYPE_GROUP -> appendTypeReference(cell, field.typeName?.value, currentMd)
            null -> cell.appendChild(Text("?"))
            else -> cell.appendChild(Text(t.name.removePrefix("TYPE_").lowercase()))
        }
        return cell
    }

    /**
     * Append a leaf-name reference for a fully-qualified protobuf type [fqn] (e.g.
     * `.engine.protoc.markdown.example.hello.Greeting`) to [cell].  If the type's declaring file
     * is in the compile scope, emits a [Link] to that file's `.md` (relative to [currentMd])
     * with a `#<anchor>` fragment targeting the type's heading; otherwise plain text.
     * `null`/empty FQN renders as `?`.
     *
     * When the target's [OutputGroup] is consolidated and shares its filename with [currentMd]
     * — which always happens in [ProtocGenMarkdown.Options.OutputType.SINGLE_FILE] and happens
     * for same-package references in [ProtocGenMarkdown.Options.OutputType.PER_PACKAGE] — the
     * link collapses to a bare `#anchor` instead of `self.md#anchor`.  Same-file references in
     * [ProtocGenMarkdown.Options.OutputType.PER_FILE] keep the existing `<file>.md#anchor`
     * shape so the default-mode output stays byte-identical to releases before this option
     * existed.
     *
     * The anchor scheme honors [ProtocGenMarkdown.Options.generateStableAnchors]: when on, the
     * full ancestor heading path from [pathAnchor] (which includes the group's H1 title and, in
     * consolidated modes, the per-file H2 title) so same-named types under different
     * sections/files/groups don't collide.  When off, the GFM-slug of the leaf dotted name from
     * [slugify], matching the renderer's heading-text auto-anchor — two same-named types in
     * different files of one consolidated document share that anchor and resolve to whichever
     * the renderer disambiguated first.
     */
    private fun appendTypeReference(
        cell: TableCell,
        fqn: String?,
        currentMd: String,
    ) {
        val cleaned = fqn?.removePrefix(".")
        if (cleaned.isNullOrEmpty()) {
            cell.appendChild(Text("?"))
            return
        }
        val leaf = cleaned.substringAfterLast('.').ifEmpty { "?" }
        options.referenceLink[cleaned]?.let { url ->
            val link = Link(url, null)
            link.appendChild(Text(leaf))
            cell.appendChild(link)
            return
        }
        val target = typeIndex[cleaned]
        if (target == null) {
            cell.appendChild(Text(leaf))
            return
        }
        if (!target.inScope &&
            options.transitiveReferences == ProtocGenMarkdown.Options.TransitiveReferences.NONE
        ) {
            cell.appendChild(Text(leaf))
            return
        }
        val targetGroup = fileToGroup[target.file] ?: peerFileToGroup[target.file]
        if (targetGroup == null) {
            // INCLUDE_FILES closure didn't reach this file, or LINK_AS_PEER's peer map missed
            // it (e.g., type referenced only from a transitive file the closure didn't pull
            // in).  Fall through to plain text rather than emit a broken link.
            cell.appendChild(Text(leaf))
            return
        }
        val section = if (target.kind == TypeKind.MESSAGE) "Messages" else "Enums"
        val isPeer = !target.inScope
        val targetHeadingPath =
            when {
                isPeer ->
                    // Peer is assumed to render under PER_FILE conventions regardless of this
                    // run's outputType: the heading path is the per-file H1 title, the
                    // Messages/Enums section, and the dotted local name.
                    listOf(targetGroup.title, section, target.localName)

                targetGroup.consolidated ->
                    listOf(targetGroup.title, titleOf(target.file), section, target.localName)

                else ->
                    listOf(targetGroup.title, section, target.localName)
            }
        val href = hrefFor(target.file, targetHeadingPath, currentMd)
        val link = Link(href, null)
        link.appendChild(Text(leaf))
        cell.appendChild(link)
    }

    /**
     * Compute the href for a target whose heading lives at [targetHeadingPath] inside
     * [targetFile]'s output group, viewed from a document being rendered into [currentMd].
     * Collapses to a bare `#anchor` when the target and current document are the same
     * consolidated output file; otherwise builds a relative-path + fragment.  Anchor naming
     * honors [ProtocGenMarkdown.Options.generateStableAnchors] via [anchorFor].
     *
     * Shared by [appendTypeReference] (field-type cells, RPC-input/output cells) and
     * [ReferenceLinkResolver] (comment-body references) so the two paths can't drift on the
     * "where does this target live" rules.
     */
    internal fun hrefFor(
        targetFile: FileDescriptorProtoWrapper,
        targetHeadingPath: List<String>,
        currentMd: String,
    ): String {
        val anchor = anchorFor(targetHeadingPath)
        val targetGroup = fileToGroup[targetFile] ?: peerFileToGroup[targetFile]!!
        return if (targetGroup.consolidated && targetGroup.filename == currentMd) {
            "#$anchor"
        } else {
            relativeLink(currentMd, targetGroup.filename) + "#" + anchor
        }
    }

    // ===== RPC table ============================================================================

    private fun appendRpcTable(
        doc: Document,
        service: ServiceDescriptorProtoWrapper,
        sci: SourceCodeInfoWrapper?,
        currentMd: String,
        svcPath: List<String>,
        serviceFqn: String,
    ) {
        if (service.methods.isEmpty()) return
        doc.appendChild(headingOf(HEADING_FIELD_SECTION, "RPC Summary", svcPath + "RPC Summary"))

        val detailsPath = svcPath + "RPC Details"
        val orderedMethods = sortedMethods(service.methods)
        val table = TableBlock()
        val head = TableHead()
        head.appendChild(headerRow("Name", "Input", "Output", "Description"))
        table.appendChild(head)
        val body = TableBody()
        for (method in orderedMethods) {
            val row = TableRow()
            row.appendChild(TableCell().apply { appendChild(Text(method.name?.value ?: "(unnamed)")) })
            row.appendChild(rpcTypeCell(method.inputType?.value, method.clientStreaming?.value == true, currentMd))
            row.appendChild(rpcTypeCell(method.outputType?.value, method.serverStreaming?.value == true, currentMd))
            val mname = method.name?.value ?: "(unnamed)"
            row.appendChild(summaryDescriptionCell(sci, method, "$serviceFqn.$mname", mname, detailsPath))
            body.appendChild(row)
        }
        table.appendChild(body)
        doc.appendChild(table)

        doc.appendChild(headingOf(HEADING_FIELD_SECTION, "RPC Details", detailsPath))
        for (method in orderedMethods) {
            val mname = method.name?.value ?: "(unnamed)"
            doc.appendChild(headingOf(HEADING_FIELD, mname, detailsPath + mname))
            appendLeadingComment(doc, sci, method, scopeFqn = "$serviceFqn.$mname")
        }
    }

    private fun rpcTypeCell(
        fqn: String?,
        streaming: Boolean,
        currentMd: String,
    ): TableCell {
        val cell = TableCell()
        if (streaming) cell.appendChild(Text("stream "))
        appendTypeReference(cell, fqn, currentMd)
        return cell
    }

    // ===== Enum values table ====================================================================

    private fun appendValuesTable(
        doc: Document,
        enum: EnumDescriptorProtoWrapper,
        sci: SourceCodeInfoWrapper?,
        enumPath: List<String>,
        enumFqn: String,
    ) {
        if (enum.values.isEmpty()) return
        doc.appendChild(headingOf(HEADING_FIELD_SECTION, "Value Summary", enumPath + "Value Summary"))

        val detailsPath = enumPath + "Value Details"
        val orderedValues = sortedEnumValues(enum.values)
        val table = TableBlock()
        val head = TableHead()
        head.appendChild(headerRow("Name", "Number", "Description"))
        table.appendChild(head)
        val body = TableBody()
        for (value in orderedValues) {
            val vname = value.name?.value ?: "(unnamed)"
            val row = TableRow()
            row.appendChild(TableCell().apply { appendChild(Text(vname)) })
            row.appendChild(TableCell().apply { appendChild(Text(value.number?.value?.toString() ?: "?")) })
            row.appendChild(summaryDescriptionCell(sci, value, "$enumFqn.$vname", vname, detailsPath))
            body.appendChild(row)
        }
        table.appendChild(body)
        doc.appendChild(table)

        doc.appendChild(headingOf(HEADING_FIELD_SECTION, "Value Details", detailsPath))
        for (value in orderedValues) {
            val vname = value.name?.value ?: "(unnamed)"
            doc.appendChild(headingOf(HEADING_FIELD, vname, detailsPath + vname))
            appendLeadingComment(doc, sci, value, scopeFqn = "$enumFqn.$vname")
        }
    }

    /**
     * Build the field's description cell from its leading proto comment.
     *
     * The cell carries only the first paragraph's inline content so the GFM pipe-table syntax
     * stays valid (one row per line), followed by a trailing `[...](#<field-anchor>)` link into
     * the field's `##### <fieldName>` expansion under `#### Field Details`.  The link is emitted
     * whenever the field has any leading comment — even when the comment is single-paragraph and
     * the Summary cell already shows the same content — so the Details section is the canonical
     * place the full comment lives.
     *
     * The anchor is the explicit path id of the field's `##### <fieldName>` expansion (parent
     * message path + `Field Details` + field name, sanitized via [pathAnchor]).  Soft line
     * breaks inside the first paragraph collapse to spaces and hard line breaks to `<br>` so
     * no literal newline ever lands inside a pipe-table cell.
     */
    private fun descriptionCell(
        field: FieldDescriptorProtoWrapper,
        sci: SourceCodeInfoWrapper?,
        scopeFqn: String,
        detailsPath: List<String>,
    ): TableCell = summaryDescriptionCell(sci, field, scopeFqn, field.name?.value ?: "(unnamed)", detailsPath)

    /**
     * Shared first-paragraph-only description cell for summary tables (Field Summary, RPC
     * Summary, Value Summary).  Parses [locatable]'s cleaned leading proto comment, lifts the
     * first paragraph's inline children into a [TableCell], and — whenever any comment exists —
     * appends a `[...](#…)` link pointing at the matching `##### <elementName>` expansion under
     * the corresponding Details section.  Empty cells are returned unchanged when the locatable
     * has no leading comment at all.
     *
     * Soft line breaks collapse to spaces and hard line breaks to `<br>` (via [replaceLineBreaks])
     * so no literal newline ever lands inside a pipe-table cell.
     */
    private fun summaryDescriptionCell(
        sci: SourceCodeInfoWrapper?,
        locatable: Locatable,
        scopeFqn: String,
        elementName: String,
        detailsPath: List<String>,
    ): TableCell {
        val cell = TableCell()
        val raw = sci?.findLocation(locatable)?.leadingComments?.cleaned
        if (raw.isNullOrBlank()) return cell
        val parsed = parseUnderScope(raw, scopeFqn)
        val firstBlock = parsed.firstChild ?: return cell
        if (firstBlock is Paragraph) {
            while (true) {
                val inline = firstBlock.firstChild ?: break
                cell.appendChild(inline)
            }
        }
        if (cell.firstChild != null) cell.appendChild(Text(" "))
        val anchor = "#" + anchorFor(detailsPath + elementName)
        val link = Link(anchor, null).apply { appendChild(Text("...")) }
        cell.appendChild(link)
        replaceLineBreaks(cell)
        return cell
    }

    /**
     * Walk [root]'s inline subtree and replace every soft/hard line break in place — soft breaks
     * become a single space (CommonMark treats them as equivalent), hard breaks become `<br>` —
     * so no literal newline ever lands inside a pipe-table cell.
     */
    private fun replaceLineBreaks(root: Node) {
        var child = root.firstChild
        while (child != null) {
            val next = child.next
            when (child) {
                is SoftLineBreak -> {
                    child.insertAfter(Text(" "))
                    child.unlink()
                }

                is HardLineBreak -> {
                    child.insertAfter(htmlInline("<br>"))
                    child.unlink()
                }

                else -> replaceLineBreaks(child)
            }
            child = next
        }
    }

    private fun htmlInline(literal: String): HtmlInline {
        val node = HtmlInline()
        node.literal = literal
        return node
    }

    /**
     * POSIX-style relative link from [from] (a `.md` file path) to [to] (another `.md` file path).
     * Both are slash-separated; the result uses `..` segments to ascend from [from]'s directory
     * to the longest shared ancestor, then descends to [to].  Same-file inputs yield the bare
     * filename (a valid self-link).
     */
    private fun relativeLink(
        from: String,
        to: String,
    ): String {
        val fromDirs = from.split('/').dropLast(1)
        val toParts = to.split('/')
        var shared = 0
        while (shared < fromDirs.size && shared < toParts.size - 1 && fromDirs[shared] == toParts[shared]) {
            shared++
        }
        val ups = List(fromDirs.size - shared) { ".." }
        val downs = toParts.drop(shared)
        return (ups + downs).joinToString("/").ifEmpty { to.substringAfterLast('/') }
    }

    /**
     * Build a body heading whose absolute level is [baseLevel] plus the current
     * [bodyLevelShift], with the visible [text], and record a [HeadingRef] for the Table of
     * Contents to consume later.  [baseLevel] is the level the heading sits at in
     * [ProtocGenMarkdown.Options.OutputType.PER_FILE] mode (so [HEADING_SECTION] = 2,
     * [HEADING_TYPE] = 3, etc.); in the consolidated modes the shift adds one so the same call
     * sites emit headings one level deeper to make room for the per-file L2 layer.  When
     * [ProtocGenMarkdown.Options.generateStableAnchors] is true the heading is prefixed by an
     * inline empty `<a>` carrying the path-based anchor id derived from [path] via [pathAnchor]
     * — what intra-document links target so that same-named headings under different parents
     * (e.g. two `### Foo` under different messages) stay distinct.  When false the anchor
     * element is omitted; links rely on the renderer's heading-text auto-anchor (see [slugify]).
     */
    private fun headingOf(
        baseLevel: Int,
        text: String,
        path: List<String>,
    ): Heading = fixedHeading(baseLevel + bodyLevelShift, text, path)

    /**
     * Build a heading at an absolute [level] (no [bodyLevelShift] applied) with the visible
     * [text] and record a [HeadingRef] for the Table of Contents to consume later.  Used for
     * the doc's H1 (always level 1, regardless of mode) and the per-file H2 in the
     * consolidated modes — both layers that sit outside the per-file body whose levels [headingOf]
     * shifts.
     */
    private fun fixedHeading(
        level: Int,
        text: String,
        path: List<String>,
    ): Heading {
        headings += HeadingRef(level, text, path)
        return Heading().apply {
            this.level = level
            if (options.generateStableAnchors) appendChild(htmlInline("<a id=\"${pathAnchor(path)}\"></a>"))
            appendChild(Text(text))
        }
    }

    private fun render(doc: Document): String = renderer.render(doc)

    private companion object {
        const val HEADING_SECTION = 2
        const val HEADING_TYPE = 3
        const val HEADING_FIELD_SECTION = 4
        const val HEADING_FIELD = 5
    }
}
