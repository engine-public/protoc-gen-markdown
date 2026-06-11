package com.engine.protoc.markdown

import com.engine.protoc.markdown.compile.Compiler
import com.engine.protoc.util.compiler.CodeGeneratorRequestWrapper
import com.engine.protoc.util.compiler.Parameters
import com.engine.protoc.util.extensions.wrap
import com.google.protobuf.ExtensionRegistry
import com.google.protobuf.compiler.PluginProtos
import java.io.InputStream
import java.time.Clock

public class ProtocGenMarkdown(
    private val request: CodeGeneratorRequestWrapper,
    private val options: Options,
    private val clock: Clock = Clock.systemUTC(),
) {

    /**
     * Options that influence the compiler plugin.
     *
     * Add new options as properties here, then wire each one through [Builder] so it can be parsed
     * from the `--markdown_out=key=value,…:outdir` parameter string.
     */
    public class Options private constructor(
        /**
         * When true, every emitted heading is prefixed with an inline empty `<a id="…"></a>`
         * whose id is the heading's full ancestor-path joined with `-` (non-alphanumeric chars
         * other than `-`/`_` replaced with `_`).  Intra-document links (the Table of Contents,
         * the `[...](#…)` field-/RPC-/value-expansion links inside summary tables) target those
         * path-based ids, so same-named headings under different parents — e.g. two `### Foo`
         * under different message sections — stay distinct.
         *
         * When false (default), no `<a id>` element is emitted; headings render as plain
         * `## Heading text` and intra-document links target the renderer's default auto-anchor
         * derived from the heading text (lowercased, whitespace → `-`, non-alphanumeric dropped).
         * Cheaper output, but headings whose text collides — two nested `Outer.Inner` messages
         * named the same under different scopes, or two fields named the same under different
         * messages — will resolve to whichever the renderer disambiguated first.  Flip this on
         * when collisions matter; leave it off when they don't.
         */
        public val generateStableAnchors: Boolean,
        /**
         * When true, the renderer emits `<!-- @@protoc_insertion_point(NAME) -->` HTML-comment
         * markers at a fixed catalog of scopes so sibling protoc plugins can splice content into
         * the generated Markdown via the standard protoc plugin insertion-point mechanism (a
         * `CodeGeneratorResponse.File` whose `insertion_point` matches `NAME` and whose `content`
         * is inserted immediately before the marker line).  The markers render as HTML comments,
         * so they are invisible in the rendered document.
         *
         * The catalog, in document order:
         *  - `frontmatter` — the last line inside the YAML frontmatter block, before the closing
         *    `---` fence.  Wrapped as a `#`-prefixed YAML comment rather than an HTML comment so
         *    YAML parsers ignore it; the marker line still matches protoc's insertion-point
         *    parser.  Injected content becomes additional top-level YAML keys; the plugin's own
         *    keys carry a `protoc-gen-markdown-` namespace prefix to avoid collisions.
         *  - `file_header` — directly after the L1 `# <path>` title.
         *  - `services_section` — directly after `## Services`, when the section is emitted.
         *  - `service_header_scope:<fqsn>` — after each `### ServiceName`, before its leading
         *    proto comment.
         *  - `service_scope:<fqsn>` — after the leading comment, before `#### RPC Summary`.
         *  - `messages_section` — directly after `## Messages`, when the section is emitted.
         *  - `message_header_scope:<fqmn>` — after each `### MessageName`, before its leading
         *    proto comment.
         *  - `message_scope:<fqmn>` — after the leading comment, before `#### Field Summary`.
         *  - `enums_section` — directly after `## Enums`, when the section is emitted.
         *  - `enum_header_scope:<fqen>` — after each `### EnumName`, before its leading proto
         *    comment.
         *  - `enum_scope:<fqen>` — after the leading comment, before `#### Value Summary`.
         *  - `file_footer` — at the very end of the document.
         *
         * Type names are fully qualified: the proto file's `package` joined with the dotted
         * ancestor-prefixed type name (so `Outer.Inner` in `package foo.bar` becomes
         * `foo.bar.Outer.Inner`).  When the file has no `package` directive the bare dotted name
         * is used (no leading dot).
         *
         * Default `false` — the markers are not emitted at all, and the output is byte-identical
         * to a build with this option absent (apart from this option line in the YAML
         * frontmatter).
         */
        public val generateInsertionPoints: Boolean,
        /**
         * Lower bound (inclusive) of the heading levels that appear in the Table of Contents.
         * Heading levels are 1 (`#`) through 5 (`#####`); the file path is L1, the section names
         * (`Services`/`Messages`/`Enums`) are L2, each service/message/enum name is L3, the
         * `Field Summary` / `Field Details` / `RPC Summary` / `RPC Details` / `Value Summary` /
         * `Value Details` sub-section headers are L4, and the individual `##### <name>` headings
         * inside the `Details` sub-sections are L5.
         *
         * Default `null`.  Behavior when combined with [maxTableOfContentsHeader]:
         *  - both `null` → no Table of Contents is rendered at all.
         *  - this `null`, [maxTableOfContentsHeader] set → treated as `1` (so the TOC starts from
         *    the L1 file-path heading).
         *  - this set, [maxTableOfContentsHeader] `null` → all headings at level ≥ this value are
         *    included; max is treated as unbounded.
         *  - both set → headings whose level falls in `[this, maxTableOfContentsHeader]`
         *    (inclusive on both ends) are included.
         *  - this > [maxTableOfContentsHeader] → a warning is logged and no TOC is rendered.
         *
         * Must parse as an integer when provided via the parameter string; non-integer values
         * throw [NumberFormatException] from the parameter parser.
         */
        public val minTableOfContentsHeader: Int?,
        /**
         * Upper bound (inclusive) of the heading levels that appear in the Table of Contents.
         * See [minTableOfContentsHeader] for level semantics and the interaction matrix.  Default
         * `null` (treated as unbounded when [minTableOfContentsHeader] is set; both `null` means
         * no TOC at all).  Must parse as an integer when provided.
         */
        public val maxTableOfContentsHeader: Int?,
    ) {

        public class Builder private constructor(parameters: Parameters) {

            public var generateStableAnchors: Boolean = parameters.get<Boolean>("generateStableAnchors") ?: false

            public var generateInsertionPoints: Boolean = parameters.get<Boolean>("generateInsertionPoints") ?: false

            public var minTableOfContentsHeader: Int? = parameters.get<Int>("minTableOfContentsHeader")

            public var maxTableOfContentsHeader: Int? = parameters.get<Int>("maxTableOfContentsHeader")

            public companion object {
                public fun from(parameters: Parameters): Builder = Builder(parameters)
            }

            public fun build(): Options =
                Options(
                    generateStableAnchors = generateStableAnchors,
                    generateInsertionPoints = generateInsertionPoints,
                    minTableOfContentsHeader = minTableOfContentsHeader,
                    maxTableOfContentsHeader = maxTableOfContentsHeader,
                )
        }
    }

    public companion object {
        public fun from(
            input: InputStream,
            registry: ExtensionRegistry = ExtensionRegistry.newInstance(),
            clock: Clock = Clock.systemUTC(),
            block: Options.Builder.() -> Unit = {},
        ): ProtocGenMarkdown {
            val cgreq = PluginProtos.CodeGeneratorRequest.parseFrom(input, registry).wrap()
            return ProtocGenMarkdown(
                cgreq,
                Options.Builder.from(cgreq.parameters).apply(block).build(),
                clock,
            )
        }
    }

    public fun compile(): PluginProtos.CodeGeneratorResponse = Compiler(request, options, clock).compile()
}
