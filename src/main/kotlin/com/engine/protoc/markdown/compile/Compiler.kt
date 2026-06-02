package com.engine.protoc.markdown.compile

import com.engine.protoc.markdown.ProtocGenMarkdown
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
import org.commonmark.renderer.NodeRenderer
import org.commonmark.renderer.markdown.MarkdownNodeRendererContext
import org.commonmark.renderer.markdown.MarkdownNodeRendererFactory
import org.commonmark.renderer.markdown.MarkdownRenderer
import java.time.Clock
import java.time.format.DateTimeFormatter

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
 *    message, or enum — independent of the Table of Contents.
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
 *    leading comment so the pipe-table syntax stays valid; when an RPC's comment has additional
 *    content, a trailing `[...](#<rpc-anchor>)` link points at a `##### <RpcName>` expansion
 *    grouped under a `#### RPC Details` heading emitted after the table.  `RPC Details` is
 *    omitted when no RPC needs an expansion.
 *  - A `## Messages` section listing each message as `### <Dotted.Name>`.  Under each heading:
 *    the message's leading proto comment (parsed as CommonMark so links/lists/etc. round-trip),
 *    a `#### Field Summary` heading, then a GFM pipe table of the message's fields with
 *    columns `Name | Type | Description`.  The type column links to the file containing that
 *    type (relative path) whenever the type's file is in the compile scope; out-of-scope and
 *    scalar types appear as plain text.  Repeated fields are prefixed with `repeated ` in the
 *    type cell.  The description cell carries only the first paragraph of the field's leading
 *    comment so the pipe-table syntax stays valid; if the comment has additional content
 *    beyond that first paragraph, a trailing `[...](#<field-anchor>)` link points at a
 *    `##### <fieldName>` expansion grouped under a `#### Field Details` heading emitted after
 *    the table.  `Field Details` is omitted when no field needs an expansion.
 *  - A `## Enums` section listing each enum as `### <Dotted.Name>`.  Under each heading:
 *    the enum's leading proto comment, then a `#### Value Summary` heading and a GFM pipe
 *    table of the enum's values with columns `Name | Number | Description`.  The description
 *    column carries only the first paragraph of each value's leading comment; when a value's
 *    comment has additional content, a trailing `[...](#<value-anchor>)` link points at a
 *    `##### <ValueName>` expansion grouped under a `#### Value Details` heading emitted after
 *    the table.  `Value Details` is omitted when no value needs an expansion.
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
    private val parser: Parser = Parser.builder().extensions(listOf(tablesExtension)).build()
    private val log: System.Logger = System.getLogger("com.engine.protoc.markdown")

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

    internal fun compile(): PluginProtos.CodeGeneratorResponse {
        val response = CodeGeneratorResponseWrapper()
        for (file in scopeFiles) response.addFile(outlineFilename(file), render(outlineDocument(file)))
        return response.build()
    }

    private val scopeFiles: List<FileDescriptorProtoWrapper> by lazy {
        val toGenerate = request.filesToGenerate.toSet()
        request.protoFiles.filter { it.name in toGenerate }
    }

    private enum class TypeKind { MESSAGE, ENUM }

    /**
     * What a TYPE_MESSAGE / TYPE_ENUM cross-reference resolves to: the file that declares the
     * type, the kind (so we know whether to anchor under the `Messages` or `Enums` L2 section),
     * and the dotted ancestor-prefixed local name within that file (e.g. `Outer.Inner`) — the
     * exact string used as the type's L3 heading text and therefore the basis of its anchor id.
     */
    private data class TypeRef(
        val file: FileDescriptorProtoWrapper,
        val kind: TypeKind,
        val localName: String,
    )

    /**
     * Fully-qualified type name → resolution metadata, across every file in the compile scope.
     * Used to turn TYPE_MESSAGE / TYPE_ENUM field references into intra/inter-file links that
     * land on the correct heading anchor (see [appendTypeReference]).  Map-entry synthetic
     * messages are not indexed (they have no user-facing `### ` heading and fields that
     * reference them render as plain text leaf names).
     */
    private val typeIndex: Map<String, TypeRef> by lazy {
        val map = mutableMapOf<String, TypeRef>()
        for (file in scopeFiles) {
            val pkg = file.`package`?.value
            val pkgPrefix = if (pkg.isNullOrEmpty()) "" else "$pkg."
            for (m in file.messageTypes) indexMessage(m, pkgPrefix, "", file, map)
            for (e in file.enumTypes) {
                val ename = e.name?.value ?: continue
                map["$pkgPrefix$ename"] = TypeRef(file, TypeKind.ENUM, ename)
            }
        }
        map
    }

    private fun indexMessage(
        msg: DescriptorProtoWrapper,
        pkgPrefix: String,
        localPrefix: String,
        file: FileDescriptorProtoWrapper,
        map: MutableMap<String, TypeRef>,
    ) {
        if (msg.options?.mapEntry?.value == true) return
        val name = msg.name?.value ?: return
        val localName = "$localPrefix$name"
        map["$pkgPrefix$localName"] = TypeRef(file, TypeKind.MESSAGE, localName)
        val childLocal = "$localName."
        for (n in msg.nestedTypes) indexMessage(n, pkgPrefix, childLocal, file, map)
        for (e in msg.enumTypes) {
            val ename = e.name?.value ?: continue
            val enumLocal = "$childLocal$ename"
            map["$pkgPrefix$enumLocal"] = TypeRef(file, TypeKind.ENUM, enumLocal)
        }
    }

    private fun outlineFilename(file: FileDescriptorProtoWrapper): String = (file.name ?: "").removeSuffix(".proto") + ".md"

    private fun outlineDocument(file: FileDescriptorProtoWrapper): Document {
        headings.clear()
        val doc = Document()
        val sci = file.sourceCodeInfo
        val currentMd = outlineFilename(file)
        val title = titleOf(file)
        val titlePath = listOf(title)
        val pkg = file.`package`?.value.orEmpty()
        val fqn: (String) -> String = { name -> if (pkg.isEmpty()) name else "$pkg.$name" }
        doc.appendChild(frontmatterBlock())
        doc.appendChild(headingOf(HEADING_TOP, title, titlePath))
        appendInsertionPoint(doc, "file_header")

        val services = file.services
        val messages = collectMessages(file)
        val enums = collectEnums(file, messages)
        val hasBody = services.isNotEmpty() || messages.isNotEmpty() || enums.isNotEmpty()
        val thematicBreak: ThematicBreak? = if (hasBody) ThematicBreak().also { doc.appendChild(it) } else null

        if (services.isNotEmpty()) {
            val sectionPath = titlePath + "Services"
            doc.appendChild(headingOf(HEADING_SECTION, "Services", sectionPath))
            appendInsertionPoint(doc, "services_section")
            for (s in services) {
                val sname = s.name?.value ?: "(unnamed)"
                val svcPath = sectionPath + sname
                doc.appendChild(headingOf(HEADING_TYPE, sname, svcPath))
                appendInsertionPoint(doc, "service_header_scope:${fqn(sname)}")
                appendLeadingComment(doc, sci, s)
                appendInsertionPoint(doc, "service_scope:${fqn(sname)}")
                appendRpcTable(doc, s, sci, currentMd, svcPath)
            }
        }

        if (messages.isNotEmpty()) {
            val sectionPath = titlePath + "Messages"
            doc.appendChild(headingOf(HEADING_SECTION, "Messages", sectionPath))
            appendInsertionPoint(doc, "messages_section")
            for ((name, msg) in messages) {
                val msgPath = sectionPath + name
                doc.appendChild(headingOf(HEADING_TYPE, name, msgPath))
                appendInsertionPoint(doc, "message_header_scope:${fqn(name)}")
                appendLeadingComment(doc, sci, msg)
                appendInsertionPoint(doc, "message_scope:${fqn(name)}")
                appendFieldsTable(doc, msg, sci, currentMd, msgPath)
            }
        }

        if (enums.isNotEmpty()) {
            val sectionPath = titlePath + "Enums"
            doc.appendChild(headingOf(HEADING_SECTION, "Enums", sectionPath))
            appendInsertionPoint(doc, "enums_section")
            for ((name, enum) in enums) {
                val enumPath = sectionPath + name
                doc.appendChild(headingOf(HEADING_TYPE, name, enumPath))
                appendInsertionPoint(doc, "enum_header_scope:${fqn(name)}")
                appendLeadingComment(doc, sci, enum)
                appendInsertionPoint(doc, "enum_scope:${fqn(name)}")
                appendValuesTable(doc, enum, sci, enumPath)
            }
        }

        if (thematicBreak != null) renderTableOfContents(thematicBreak)

        appendInsertionPoint(doc, "file_footer")

        return doc
    }

    /**
     * Splice a collapsible `<details>` block listing the file's headings into the document
     * immediately after [anchor] (the title's `___` thematic break).  Which headings appear is
     * controlled by [ProtocGenMarkdown.Options.minTableOfContentsHeader] /
     * [ProtocGenMarkdown.Options.maxTableOfContentsHeader]:
     *
     *  - both `null` → no TOC is rendered at all.  The thematic break stays put.
     *  - min `null`, max set → min is treated as 1 (the L1 file-path heading is included).
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
            log.log(
                System.Logger.Level.WARNING,
                "minTableOfContentsHeader=$minOpt > maxTableOfContentsHeader=$maxOpt; no table of contents will be generated",
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
    private fun formatOptions(): String =
        listOf(
            "generateStableAnchors" to options.generateStableAnchors.toString(),
            "generateInsertionPoints" to options.generateInsertionPoints.toString(),
            "minTableOfContentsHeader" to (options.minTableOfContentsHeader?.toString() ?: "null"),
            "maxTableOfContentsHeader" to (options.maxTableOfContentsHeader?.toString() ?: "null"),
        ).joinToString(",") { (k, v) -> "$k=$v" }

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
     * Look up the leading proto comment for [locatable] and append its parsed CommonMark blocks
     * to [doc].  Treats the cleaned comment text as a CommonMark fragment, so lists, blockquotes,
     * fenced code, links, etc. round-trip through the AST and re-render correctly.
     */
    private fun appendLeadingComment(
        doc: Document,
        sci: SourceCodeInfoWrapper?,
        locatable: Locatable,
    ) {
        val text = sci?.findLocation(locatable)?.leadingComments?.cleaned ?: return
        if (text.isBlank()) return
        val parsed = parser.parse(text)
        while (true) {
            val child = parsed.firstChild ?: break
            doc.appendChild(child)
        }
    }

    // ===== Fields table =========================================================================

    private fun appendFieldsTable(
        doc: Document,
        msg: DescriptorProtoWrapper,
        sci: SourceCodeInfoWrapper?,
        currentMd: String,
        msgPath: List<String>,
    ) {
        if (msg.fields.isEmpty()) return

        doc.appendChild(headingOf(HEADING_FIELD_SECTION, "Field Summary", msgPath + "Field Summary"))

        val detailsPath = msgPath + "Field Details"
        val expanded = mutableListOf<FieldDescriptorProtoWrapper>()
        val table = TableBlock()
        val head = TableHead()
        head.appendChild(headerRow("Name", "Type", "Description"))
        table.appendChild(head)
        val body = TableBody()
        for (field in msg.fields) {
            val row = TableRow()
            row.appendChild(TableCell().apply { appendChild(Text(field.name?.value ?: "(unnamed)")) })
            row.appendChild(typeCell(field, currentMd))
            val (cell, needsExpansion) = descriptionCell(field, sci, detailsPath)
            row.appendChild(cell)
            if (needsExpansion) expanded += field
            body.appendChild(row)
        }
        table.appendChild(body)
        doc.appendChild(table)

        if (expanded.isNotEmpty()) {
            doc.appendChild(headingOf(HEADING_FIELD_SECTION, "Field Details", detailsPath))
            for (field in expanded) {
                val fname = field.name?.value ?: "(unnamed)"
                doc.appendChild(headingOf(HEADING_FIELD, fname, detailsPath + fname))
                appendLeadingComment(doc, sci, field)
            }
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
     * with a `#<anchor>` fragment targeting the type's L3 heading; otherwise plain text.
     * `null`/empty FQN renders as `?`.
     *
     * The anchor scheme honors [ProtocGenMarkdown.Options.generateStableAnchors]: when on, the
     * path-based id from [pathAnchor] (`<file-slug>-<section>-<localName>`) so same-named types
     * under different sections / files don't collide; when off, the GFM-slug of the local dotted
     * name from [slugify], matching the renderer's heading-text auto-anchor.
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
        val target = typeIndex[cleaned]
        if (target != null) {
            val section = if (target.kind == TypeKind.MESSAGE) "Messages" else "Enums"
            val anchor =
                if (options.generateStableAnchors) {
                    pathAnchor(listOf(titleOf(target.file), section, target.localName))
                } else {
                    slugify(target.localName)
                }
            val link = Link(relativeLink(currentMd, outlineFilename(target.file)) + "#" + anchor, null)
            link.appendChild(Text(leaf))
            cell.appendChild(link)
        } else {
            cell.appendChild(Text(leaf))
        }
    }

    // ===== RPC table ============================================================================

    private fun appendRpcTable(
        doc: Document,
        service: ServiceDescriptorProtoWrapper,
        sci: SourceCodeInfoWrapper?,
        currentMd: String,
        svcPath: List<String>,
    ) {
        if (service.methods.isEmpty()) return
        doc.appendChild(headingOf(HEADING_FIELD_SECTION, "RPC Summary", svcPath + "RPC Summary"))

        val detailsPath = svcPath + "RPC Details"
        val expanded = mutableListOf<MethodDescriptorProtoWrapper>()
        val table = TableBlock()
        val head = TableHead()
        head.appendChild(headerRow("Name", "Input", "Output", "Description"))
        table.appendChild(head)
        val body = TableBody()
        for (method in service.methods) {
            val row = TableRow()
            row.appendChild(TableCell().apply { appendChild(Text(method.name?.value ?: "(unnamed)")) })
            row.appendChild(rpcTypeCell(method.inputType?.value, method.clientStreaming?.value == true, currentMd))
            row.appendChild(rpcTypeCell(method.outputType?.value, method.serverStreaming?.value == true, currentMd))
            val (cell, needsExpansion) = summaryDescriptionCell(sci, method, method.name?.value ?: "(unnamed)", detailsPath)
            row.appendChild(cell)
            if (needsExpansion) expanded += method
            body.appendChild(row)
        }
        table.appendChild(body)
        doc.appendChild(table)

        if (expanded.isNotEmpty()) {
            doc.appendChild(headingOf(HEADING_FIELD_SECTION, "RPC Details", detailsPath))
            for (method in expanded) {
                val mname = method.name?.value ?: "(unnamed)"
                doc.appendChild(headingOf(HEADING_FIELD, mname, detailsPath + mname))
                appendLeadingComment(doc, sci, method)
            }
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
    ) {
        if (enum.values.isEmpty()) return
        doc.appendChild(headingOf(HEADING_FIELD_SECTION, "Value Summary", enumPath + "Value Summary"))

        val detailsPath = enumPath + "Value Details"
        val expanded = mutableListOf<EnumValueDescriptorProtoWrapper>()
        val table = TableBlock()
        val head = TableHead()
        head.appendChild(headerRow("Name", "Number", "Description"))
        table.appendChild(head)
        val body = TableBody()
        for (value in enum.values) {
            val vname = value.name?.value ?: "(unnamed)"
            val row = TableRow()
            row.appendChild(TableCell().apply { appendChild(Text(vname)) })
            row.appendChild(TableCell().apply { appendChild(Text(value.number?.value?.toString() ?: "?")) })
            val (cell, needsExpansion) = summaryDescriptionCell(sci, value, vname, detailsPath)
            row.appendChild(cell)
            if (needsExpansion) expanded += value
            body.appendChild(row)
        }
        table.appendChild(body)
        doc.appendChild(table)

        if (expanded.isNotEmpty()) {
            doc.appendChild(headingOf(HEADING_FIELD_SECTION, "Value Details", detailsPath))
            for (value in expanded) {
                val vname = value.name?.value ?: "(unnamed)"
                doc.appendChild(headingOf(HEADING_FIELD, vname, detailsPath + vname))
                appendLeadingComment(doc, sci, value)
            }
        }
    }

    /**
     * Build the field's description cell from its leading proto comment.
     *
     * The cell carries only the first paragraph's inline content so the GFM pipe-table syntax
     * stays valid (one row per line).  If the parsed comment has any block beyond that first
     * paragraph — additional paragraphs, lists, blockquotes, code, etc. — the cell also gets
     * a trailing `[...](#<field-anchor>)` link and the function returns `true` so the caller
     * can emit a `#### <fieldName>` expansion with the full comment underneath after the table.
     *
     * The anchor is the explicit path id of the field's `##### <fieldName>` expansion (parent
     * message path + `Field Details` + field name, sanitized via [pathAnchor]).  Soft line
     * breaks inside the first paragraph collapse to spaces and hard line breaks to `<br>` so
     * no literal newline ever lands inside a pipe-table cell.
     */
    private fun descriptionCell(
        field: FieldDescriptorProtoWrapper,
        sci: SourceCodeInfoWrapper?,
        detailsPath: List<String>,
    ): Pair<TableCell, Boolean> = summaryDescriptionCell(sci, field, field.name?.value ?: "(unnamed)", detailsPath)

    /**
     * Shared first-paragraph-only description cell for summary tables (Field Summary, RPC Summary).
     * Parses [locatable]'s cleaned leading proto comment, lifts the first paragraph's inline
     * children into a [TableCell], and — if there's any content beyond that first paragraph —
     * appends a `[...](#…)` link pointing at the matching `##### <elementName>` expansion under
     * the corresponding Details section and returns `true` so the caller can emit it.
     *
     * Soft line breaks collapse to spaces and hard line breaks to `<br>` (via [replaceLineBreaks])
     * so no literal newline ever lands inside a pipe-table cell.
     */
    private fun summaryDescriptionCell(
        sci: SourceCodeInfoWrapper?,
        locatable: Locatable,
        elementName: String,
        detailsPath: List<String>,
    ): Pair<TableCell, Boolean> {
        val cell = TableCell()
        val raw = sci?.findLocation(locatable)?.leadingComments?.cleaned
        if (raw.isNullOrBlank()) return cell to false
        val parsed = parser.parse(raw)
        val firstBlock = parsed.firstChild ?: return cell to false
        val needsExpansion = firstBlock !is Paragraph || firstBlock.next != null
        if (firstBlock is Paragraph) {
            while (true) {
                val inline = firstBlock.firstChild ?: break
                cell.appendChild(inline)
            }
        }
        if (needsExpansion) {
            if (cell.firstChild != null) cell.appendChild(Text(" "))
            val anchor = "#" + anchorFor(detailsPath + elementName)
            val link = Link(anchor, null).apply { appendChild(Text("...")) }
            cell.appendChild(link)
        }
        replaceLineBreaks(cell)
        return cell to needsExpansion
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
     * Build a heading at [level] with the visible [text] and record a [HeadingRef] for the
     * Table of Contents to consume later.  When
     * [ProtocGenMarkdown.Options.generateStableAnchors] is true the heading is prefixed by an
     * inline empty `<a>` carrying the path-based anchor id derived from [path] via [pathAnchor]
     * — what intra-document links target so that same-named headings under different parents
     * (e.g. two `### Foo` under different messages) stay distinct.  When false the anchor
     * element is omitted; links rely on the renderer's heading-text auto-anchor (see [slugify]).
     */
    private fun headingOf(
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
        const val HEADING_TOP = 1
        const val HEADING_SECTION = 2
        const val HEADING_TYPE = 3
        const val HEADING_FIELD_SECTION = 4
        const val HEADING_FIELD = 5
    }
}
