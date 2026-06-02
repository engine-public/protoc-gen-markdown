package com.engine.protoc.markdown

import com.engine.protoc.markdown.compile.Compiler
import com.engine.protoc.util.compiler.CodeGeneratorRequestWrapper
import com.engine.protoc.util.compiler.Parameters
import com.engine.protoc.util.extensions.wrap
import com.google.protobuf.ExtensionRegistry
import com.google.protobuf.compiler.PluginProtos
import org.apache.logging.log4j.core.appender.ConsoleAppender
import org.apache.logging.log4j.core.config.Configurator
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory
import org.slf4j.event.Level
import java.io.InputStream
import java.time.Clock
import java.util.concurrent.atomic.AtomicBoolean
import org.apache.logging.log4j.Level as Log4jLevel

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
    public data class Options(
        /**
         * When true (default), every emitted heading is prefixed with an inline empty
         * `<a id="…"></a>` whose id is the heading's full ancestor-path joined with `-`
         * (non-alphanumeric chars other than `-`/`_` replaced with `_`).  Intra-document links
         * (the Table of Contents, the `[...](#…)` field-/RPC-/value-expansion links inside
         * summary tables) target those path-based ids, so same-named headings under different
         * parents — e.g. two `### Foo` under different message sections — stay distinct.
         *
         * When false, no `<a id>` element is emitted; headings render as plain
         * `## Heading text` and intra-document links target the renderer's default auto-anchor
         * derived from the heading text (lowercased, whitespace → `-`, non-alphanumeric dropped).
         * Cheaper output, but headings whose text collides — two nested `Outer.Inner` messages
         * named the same under different scopes, or two fields named the same under different
         * messages — will resolve to whichever the renderer disambiguated first.  Flip this off
         * when collisions don't matter; leave it on when they do.
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
         *  - `file_header` — directly after the L1 group title.  Emitted only in the consolidated
         *    [OutputType.PER_PACKAGE] / [OutputType.SINGLE_FILE] modes where the L1 is the
         *    package or session title, not a file; in [OutputType.PER_FILE] the L1 IS the file
         *    heading and the keyed `file_header_scope:<file>` covers the same spot instead.
         *  - `file_header_scope:<file>` — directly after each file's heading (the L1 in
         *    [OutputType.PER_FILE], each per-file L2 in the consolidated modes).  Key is the
         *    proto file's full relative path as protoc reports it (e.g. `foo/bar/baz.proto`).
         *  - `file_scope:<file>` — immediately after `file_header_scope:<file>`.  The plugin
         *    renders no file-level leading comment, so the two markers are adjacent; the pair
         *    mirrors the `_header_scope` / `_scope` shape used for services, messages, and enums.
         *  - `services_section` — directly after the `Services` section heading, when the section
         *    is emitted.
         *  - `service_header_scope:<fqsn>` — after each `### ServiceName`, before its leading
         *    proto comment.
         *  - `service_scope:<fqsn>` — after the leading comment, before `#### RPC Summary`.
         *  - `messages_section` — directly after the `Messages` section heading, when the section
         *    is emitted.
         *  - `message_header_scope:<fqmn>` — after each `### MessageName`, before its leading
         *    proto comment.
         *  - `message_scope:<fqmn>` — after the leading comment, before `#### Field Summary`.
         *  - `enums_section` — directly after the `Enums` section heading, when the section is
         *    emitted.
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
         * Default `true`.  When set to `false` the markers are not emitted at all, and the
         * output is byte-identical to a build with this option absent (apart from this option
         * line in the YAML frontmatter).
         */
        public val generateInsertionPoints: Boolean,
        /**
         * Lower bound (inclusive) of the heading levels that appear in the Table of Contents.
         * Heading levels are 1 (`#`) through 5 (`#####`); the document's title (the file
         * `<path>` in [OutputType.PER_FILE], the dotted `<pkg>` in [OutputType.PER_PACKAGE] and
         * package indices, the `<prefix>` or `Overview` in [OutputType.SINGLE_FILE]) is L1, the section
         * names (`Services`/`Messages`/`Enums`) are L2, each service/message/enum name is L3, the
         * `Field Summary` / `Field Details` / `RPC Summary` / `RPC Details` / `Value Summary` /
         * `Value Details` sub-section headers are L4, and the individual `##### <name>` headings
         * inside the `Details` sub-sections are L5.
         *
         * Default `2` — paired with [maxTableOfContentsHeader]'s default of `3`, the TOC covers
         * the per-file section headings (`Services` / `Messages` / `Enums`) and the
         * per-service/message/enum L3 headings.  Behavior when combined with [maxTableOfContentsHeader]:
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
         * See [minTableOfContentsHeader] for level semantics and the interaction matrix.
         * Default `3` — paired with [minTableOfContentsHeader]'s default of `2`, the TOC covers
         * the section / type-name layer.  `null` (treated as unbounded when
         * [minTableOfContentsHeader] is set; both `null` means no TOC at all) widens or
         * disables the TOC.  Must parse as an integer when provided.
         */
        public val maxTableOfContentsHeader: Int?,
        /**
         * Selects how input files are mapped to output files.
         *
         *  - [OutputType.PER_FILE] (the default) — one `.md` per input proto, mirroring the input
         *    directory layout.  The L1 heading is `# <full relative path>`; every other
         *    heading sits one level deeper, exactly as documented under the headings section
         *    above.
         *  - [OutputType.PER_PACKAGE] — every input file declaring the same proto `package` is
         *    consolidated into one `.md`.  The file's L1 heading is `# <dotted
         *    package>`; each input file becomes an L2 heading carrying its full relative path;
         *    the existing `## Services` / `## Messages` / `## Enums` section headers shift down
         *    to L3, type names to L4, the `Field Summary` / `RPC Summary` / `Value Summary`
         *    sub-section headers to L5, and the individual `##### <name>` headings to L6.
         *    Output location depends on whether the package is "namespaced": when every file
         *    declaring the package lives at the directory whose path equals the package's dotted
         *    name with `.` → `/` (e.g. all files declaring `foo.bar` live under `foo/bar/`), the
         *    consolidated file is dropped at `<pkg-as-dir>/package.md`.  Otherwise it is dropped
         *    at the output root with the name `<fully.qualified.package>.md`.  Files declaring
         *    no `package` directive group under the title `# Default Package` at `default.md`.
         *  - [OutputType.SINGLE_FILE] — every input file in the compile request is consolidated
         *    into a single `.md` at the output root.  The L1 heading is `# <longest
         *    common package prefix>` (e.g. `# foo.bar` for files declaring `foo.bar.baz`
         *    and `foo.bar.qux`), or `# Overview` when there is no common prefix.  The
         *    filename is `<longest-common-package-prefix>.md`, or `overview.md` when no common
         *    prefix exists.  Per-file L2 sub-sections and the level-shift rules from PER_PACKAGE
         *    apply identically.
         *
         * Cross-type references whose source and target end up in the same output file collapse
         * to a bare `#anchor` link; references that cross output-file boundaries keep their
         * relative `.md#anchor` form.  When [generateStableAnchors] is `true` the anchor includes
         * the full ancestor heading path (so the group-title L1 and per-file L2 segments
         * disambiguate two same-named types declared in different files of the same package);
         * when `false`, the renderer's heading-text auto-anchor still resolves leaf names only,
         * so same-named types in different files of one consolidated document will collide
         * unless stable anchors are turned on.
         *
         * Default [OutputType.PER_FILE].  Must parse as one of the enum names (case-insensitive)
         * when provided via the parameter string.
         */
        public val outputType: OutputType,
        /**
         * Controls whether the compiler emits navigation-only aggregator documents alongside the
         * primary per-`outputType` output set.  Two layers of aggregation, both gated by this
         * single switch:
         *
         *  - **Package indices** — under [OutputType.PER_FILE] only, one extra `.md` per distinct
         *    proto `package` declared across the compile-scope files, titled `# <dotted
         *    package>` (or `# Default Package` for files with no `package` directive).  Body
         *    is a bulleted Table of Contents listing every file, section (`Services` /
         *    `Messages` / `Enums`), type, and member in that package, each entry hyperlinked to
         *    the corresponding heading anchor inside the per-file `.md`s.  The index filename
         *    mirrors [OutputType.PER_PACKAGE]'s rules so it sits where a consolidated package
         *    document would have sat: `<pkg-as-dir>/package.md` when every file declaring the
         *    package lives at the directory whose path is the package name with `.` → `/`,
         *    otherwise `<fully.qualified.package>.md` at the output root.  Files with no
         *    `package` directive collapse into a single `default.md` index.  Cross-file links
         *    honor [generateStableAnchors]; member entry paths mirror the per-file Details
         *    headings so each link lands on the same heading the in-file `[...](#…)` expansion
         *    targets.  Sort order matches the per-file documents (driven by [fileSortMode] /
         *    [typeSortMode] / [rpcSortMode] / [fieldSortMode] / [enumValueSortMode]).  Under
         *    [OutputType.PER_PACKAGE] no package indices are emitted — each package's
         *    consolidated `.md` already plays the indexing role.
         *
         *  - **Overview** — under [OutputType.PER_FILE] and [OutputType.PER_PACKAGE], a single
         *    `overview.md` at the output root with `# Overview` (or `# <longest common
         *    package prefix>` when one exists) whose body is a flat bulleted list of every
         *    package, each entry linked to the package's primary `.md` (the package index under
         *    `PER_FILE`; the consolidated package document under `PER_PACKAGE`).  Acts as the
         *    parent of the package indices under `PER_FILE`, and as the equivalent navigation
         *    hub under `PER_PACKAGE` where each package is itself a single document.
         *
         * When the filename `overview.md` collides with a per-file or per-package output, the
         * overview is skipped with a warning so the user's content wins (same strategy as the
         * package-index collision guard).
         *
         * Under [OutputType.SINGLE_FILE] this option is a no-op: the single consolidated `.md`
         * is itself the aggregator.  When set to `true` together with `SINGLE_FILE`, an INFO
         * log line records that the option had no effect.
         *
         * Default `true`.
         */
        public val includeIndices: Boolean,
        /**
         * Threshold at which the plugin emits log records via SLF4J.  Accepts any value of
         * [org.slf4j.event.Level] (`TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR`); a record is
         * emitted when its level is greater than or equal to this threshold.  Defaults to
         * `ERROR` so the plugin is quiet by default but still surfaces error-level reports.
         *
         * The option is realised at runtime by programmatically reconfiguring the Log4j 2
         * `Configuration` after [Options] is built, so it controls every logger the plugin
         * (and its dependencies) creates.
         *
         * Passed via `--markdown_out=logLevel=DEBUG:outdir` (case-insensitive).
         */
        public val logLevel: Level,
        /**
         * Optional path to a file that receives timestamped log records in addition to the
         * stderr console output.  The stderr `Console` appender is always attached — its lines
         * are prefixed with `[protoc-gen-markdown]` so they stand out from other compiler
         * output protoc may multiplex on the same stream.  When this option is set, a `File`
         * appender is *also* attached at the given path with a `%d{HH:mm:ss.SSS}`-prefixed
         * pattern.
         *
         * Passed via `--markdown_out=logFile=/tmp/protoc.log:outdir`.
         */
        public val logFile: String?,
        /**
         * Sort order for the per-file lists of services, messages, and enums under each
         * `## Services` / `## Messages` / `## Enums` section.
         *
         *  - [SortMode.ALPHABETICAL] (default) — services / messages / enums each sort by name.
         *    For messages and enums (which collect via a depth-first walk of the descriptor
         *    tree), the resulting flat list sorts by full dotted name (`Foo`, `Foo.Bar`,
         *    `Foo.Baz`, `Quux`) so parents naturally precede their children.
         *  - [SortMode.ENCOUNTER] — proto declaration order: services in the order they appear
         *    in the `.proto` file; messages and enums via depth-first traversal of the
         *    descriptor tree, so nested messages immediately follow their parent.
         *
         * Default [SortMode.ALPHABETICAL].  Sort is case-sensitive (UTF-16 codepoint order); in
         * proto convention service/message/enum names are PascalCase so case sensitivity rarely
         * matters within a single scope.  Must parse as one of the enum names (case-insensitive)
         * when provided via the parameter string.
         */
        public val typeSortMode: SortMode,
        /**
         * Sort order for the per-file L2 sub-sections inside a consolidated
         * [OutputType.PER_PACKAGE] or [OutputType.SINGLE_FILE] document.
         *
         *  - [SortMode.ALPHABETICAL] (default) — files sort by their full relative path
         *    (`file.name`, e.g. `foo/bar/baz.proto`).
         *  - [SortMode.ENCOUNTER] — files appear in the order protoc walked them in the compile
         *    request.
         *
         * No-op under [OutputType.PER_FILE] (each output document holds exactly one file).
         *
         * Default [SortMode.ALPHABETICAL].  Must parse as one of the enum names (case-insensitive)
         * when provided via the parameter string.
         */
        public val fileSortMode: SortMode,
        /**
         * Sort order for the RPCs within each service — drives both the `#### RPC Summary`
         * table rows and the per-RPC headings grouped under `#### RPC Details`.
         *
         *  - [SortMode.ALPHABETICAL] — RPCs sort by name.
         *  - [SortMode.ENCOUNTER] (default) — RPCs appear in proto declaration order, which
         *    is typically how authors group related calls in the source file.
         *
         * Default [SortMode.ENCOUNTER].  Must parse as one of the enum names (case-insensitive)
         * when provided via the parameter string.
         */
        public val rpcSortMode: SortMode,
        /**
         * Sort order for the fields within each message — drives both the `#### Field Summary`
         * table rows and the per-field headings grouped under `#### Field Details`.
         *
         *  - [MemberSortMode.ALPHABETICAL] — fields sort by name.
         *  - [MemberSortMode.ENCOUNTER] (default) — fields appear in proto declaration order,
         *    which usually mirrors the field-number tag order authors picked when designing
         *    the message.
         *  - [MemberSortMode.NUMBER] — fields sort by their proto field number (the integer
         *    tag); often the same shape as `ENCOUNTER` but enforced.
         *
         * See [enumValueSortMode] for the equivalent over enum values.  Default
         * [MemberSortMode.ENCOUNTER].  Must parse as one of the enum names (case-insensitive)
         * when provided via the parameter string.
         */
        public val fieldSortMode: MemberSortMode,
        /**
         * Sort order for the values within each enum — drives both the `#### Value Summary`
         * table rows and the per-value headings grouped under `#### Value Details`.
         *
         *  - [MemberSortMode.ALPHABETICAL] — values sort by name.
         *  - [MemberSortMode.ENCOUNTER] (default) — values appear in proto declaration order
         *    (which, in proto3, must lead with the zero value but is otherwise authored).
         *  - [MemberSortMode.NUMBER] — values sort by their integer value.
         *
         * See [fieldSortMode] for the equivalent over message fields.  Default
         * [MemberSortMode.ENCOUNTER].  Must parse as one of the enum names (case-insensitive)
         * when provided via the parameter string.
         */
        public val enumValueSortMode: MemberSortMode,
        /**
         * Controls how CommonMark reference-link syntax inside proto leading comments — e.g.
         * `[GreetingResponse]`, `[GreetingRequest.name]`, `[GreeterService.SayHello]` — is
         * handled when rendering each comment block.  Both the shortcut form (`[label]`)
         * and the full form (`[display text][label]`) are recognized; in the full form the
         * label inside the second brackets is the lookup key and the visible link text
         * stays as the first brackets' content (so `[Status.details][google.rpc.Status]`
         * pairs nicely with a [referenceLink] entry for `google.rpc.Status`).  Escaped
         * brackets (`\[…\]`) follow CommonMark semantics — the parser sees them as literal
         * text and the resolver is never invoked, so the brackets pass through unchanged.
         *
         *  - [ResolveReferenceLinksMode.NONE] — references are not rewritten; the parsed
         *    CommonMark AST is appended as-is and unresolved shortcut references survive as
         *    literal `[name]` text.  Use this when the surrounding documentation system or a
         *    downstream pipeline takes over reference resolution.
         *  - [ResolveReferenceLinksMode.WARN] — references resolve against the types, fields,
         *    enum values, and RPCs in the compile scope and are rewritten into real Markdown
         *    links to the target's heading anchor.  Unresolved names stay literal.  Every
         *    comment-level reference that fails to resolve, or that resolves through an
         *    ambiguous short-name collision, logs at `warn` naming the request site (proto
         *    file, comment scope, label) and the candidates / reason.
         *  - [ResolveReferenceLinksMode.FAIL_ON_INVALID] (default) — same rewrite path as
         *    [WARN], but each failing comment-level reference logs at `error` with the
         *    request-site details, and the plugin additionally **collects** every failure and
         *    at the end of compilation sets `CodeGeneratorResponse.error` so protoc fails the
         *    run.  Reference-driven: collisions that are never used in any comment do not
         *    cause a failure (they only emit a `debug` line at index time).
         *
         * The resolver honors the comment's own anchor descriptor for bare-name lookups, so
         * authors can write `[name]` inside a comment on `message User` and have it find
         * `User.name` without the explicit qualifier.  Field- and RPC-scoped lookups also
         * include the descriptor's declared target type by short name — `[Foo]` in a comment
         * on a field of type `Foo` resolves to that specific target even when other types
         * sharing the short name exist in the global scope.  The structural per-member
         * `Field Details` / `RPC Details` / `Value Details` headings are emitted regardless
         * of this option (they exist to anchor cross-references but are useful on their own
         * as deep-link targets).
         *
         * Default [ResolveReferenceLinksMode.FAIL_ON_INVALID].  Must parse as one of the enum
         * names (case-insensitive) when provided via the parameter string.
         */
        public val resolveReferenceLinksMode: ResolveReferenceLinksMode,
        /**
         * Controls how references — both field-type cells in summary tables and
         * `[label]` shortcut-references inside proto leading comments — are handled when the
         * target message or enum lives in a *transitive* `.proto` file: one that the
         * `CodeGeneratorRequest` includes (because some `filesToGenerate` file imports it) but
         * which protoc did not ask this plugin to generate output for.
         *
         *  - [TransitiveReferences.NONE] — transitive types are treated as unknown.  Field-type
         *    cells render the leaf name as plain text; comment `[label]` references resolve as
         *    if the type did not exist, which (depending on [resolveReferenceLinksMode]) leaves
         *    the literal `[name]` in place, logs a `warn`, or fails the build.
         *  - [TransitiveReferences.LINK_AS_PEER] (default) — transitive types are linked as if
         *    the consumer ran this same plugin (with the same [outputType], [generateStableAnchors],
         *    etc.) on the transitive `.proto` files in a *parallel* protoc invocation that landed
         *    its output alongside this one.  The href follows the same `<path>.md#<anchor>`
         *    convention that an intra-scope cross-file link uses, but no peer `.md` is emitted by
         *    *this* run — the link will only resolve at view time when the peer documents exist
         *    on disk.  Best fit for monorepo workflows that document each proto package in its
         *    own `:gen` task but render the resulting markdown into one shared output tree.
         *  - [TransitiveReferences.INCLUDE_FILES] — every transitive `.proto` file referenced by
         *    at least one in-scope type is promoted into the generated output set, exactly as if
         *    it had appeared in `filesToGenerate`.  The resulting bundle is self-contained: every
         *    field-type link and every successfully-resolved comment reference points at content
         *    this run actually emitted.  Output set grows transparently with the dependency
         *    closure; under the consolidated [OutputType.PER_PACKAGE] / [OutputType.SINGLE_FILE]
         *    modes this can pull in additional packages / inflate the session title accordingly.
         *
         * Well-known protos (`google.protobuf.*`) are stripped from the request by protoc, so
         * neither mode can link them — they always render as plain text regardless of this
         * setting.
         *
         * Default [TransitiveReferences.LINK_AS_PEER].  Must parse as one of the enum names
         * (case-insensitive) when provided via the parameter string.
         */
        public val transitiveReferences: TransitiveReferences,
        /**
         * Per-label URL overrides applied to two reference sites: shortcut-reference syntax
         * inside proto leading comments, and field-type / RPC input-output cells whose declared
         * type FQN matches a map key.  Each entry maps a bracketed label exactly as it appears
         * in the comment (`[CoreEntity]`, `[Order.id]`, `[google.protobuf.Timestamp]`) — or, for
         * the field/RPC cell path, the fully-qualified type name with no leading dot
         * (`google.rpc.Status`) — to the URL the rewritten link should target.  When either
         * resolver encounters a key that matches the requested label, the override URL wins
         * unconditionally — no compile-scope, peer, or transitive lookup is performed for that
         * label.  As a side effect, a reference whose only would-be target lives in a
         * transitive dependency is not enough to pull that dependency in as a peer or promote
         * it under [TransitiveReferences.INCLUDE_FILES]: the override answers the link without
         * consulting the transitive index, and that single reference no longer counts toward
         * the file's promotion criteria.  (A file pulled in by *other*, non-overridden
         * references still gets promoted normally.)
         *
         * Labels not present in this map fall through to the normal resolution path, including
         * [resolveReferenceLinksMode]'s WARN / FAIL_ON_INVALID handling for unresolved or
         * ambiguous comment references.  Pair an override with
         * [ResolveReferenceLinksMode.FAIL_ON_INVALID] (the default) to keep comment links
         * tightly checked while still pointing a known-external label at an external URL.
         *
         * Passed via repeated entries in the parameter string, each of the form
         * `referenceLink=<label>=<URL>`; the `Parameters` parser splits on `=` with a limit
         * of two, so the URL may contain additional `=` characters.  Entries with no `=`
         * separator are rejected with an [IllegalArgumentException] from the parameter
         * parser.  The map preserves entry order (parameter-string order) so the frontmatter
         * snapshot round-trips it deterministically.
         *
         * URLs containing `:` cannot be passed inside `--markdown_out=…:<outdir>` because
         * protoc splits options from the output directory on the first `:`.  Two URL shapes
         * sidestep the issue:
         *
         *  - Site-relative paths — `referenceLink=CoreEntity=/docs/core-entity`.  Resolves
         *    relative to the host serving the rendered Markdown; ideal when the docs are
         *    deployed alongside an existing site (`example.com/docs/core-entity`).
         *  - Scheme-stripped absolute URLs — `referenceLink=CoreEntity=example.com/docs/core-entity`.
         *    Useful when targeting a different host; the rendered link is protocol-relative,
         *    so the browser will follow it with whatever scheme served the surrounding page.
         *
         * For values that genuinely need an explicit scheme (`https://...`), pass them through
         * `--markdown_opt=referenceLink=<label>=<URL>` instead (one `--markdown_opt` per
         * entry).  The Gradle `protobuf` plugin's `option(...)` DSL currently routes through
         * `--markdown_out=…`, so `:`-bearing URLs go via a manual protoc invocation with
         * `--markdown_opt`, or use one of the two URL shapes above.
         *
         * Default: empty map — no overrides applied; every comment reference goes through the
         * normal resolver.
         */
        public val referenceLink: Map<String, String>,
    ) {

        /** Output-file shape selected by [Options.outputType]. */
        public enum class OutputType { PER_FILE, PER_PACKAGE, SINGLE_FILE }

        /**
         * Two-value sort order shared by [Options.typeSortMode], [Options.fileSortMode], and
         * [Options.rpcSortMode].  See [Options.MemberSortMode] for the field- and enum-value
         * variant that adds a [MemberSortMode.NUMBER] mode.
         */
        public enum class SortMode { ALPHABETICAL, ENCOUNTER }

        /**
         * Sort order selected by [Options.fieldSortMode] / [Options.enumValueSortMode] for
         * message fields and enum values respectively.  Adds [NUMBER] — sort by the proto
         * field number / enum value number — to the two modes of [SortMode].
         */
        public enum class MemberSortMode { ALPHABETICAL, ENCOUNTER, NUMBER }

        /**
         * Behavior selected by [Options.resolveReferenceLinksMode] for the shortcut-reference
         * resolver that rewrites `[label]` patterns inside proto leading comments.  See the
         * property's KDoc for the full semantics of each value.
         */
        public enum class ResolveReferenceLinksMode { NONE, WARN, FAIL_ON_INVALID }

        /**
         * Behavior selected by [Options.transitiveReferences] for messages and enums that live in
         * `.proto` files which the request pulled in as transitive dependencies of
         * `filesToGenerate` but did not itself ask the plugin to render.  See the property's
         * KDoc for the full semantics of each value.
         */
        public enum class TransitiveReferences { NONE, LINK_AS_PEER, INCLUDE_FILES }

        public class Builder private constructor(parameters: Parameters) {

            public var generateStableAnchors: Boolean = parameters.get<Boolean>("generateStableAnchors") ?: true

            public var generateInsertionPoints: Boolean = parameters.get<Boolean>("generateInsertionPoints") ?: true

            public var minTableOfContentsHeader: Int? = parameters.get<Int>("minTableOfContentsHeader") ?: 2

            public var maxTableOfContentsHeader: Int? = parameters.get<Int>("maxTableOfContentsHeader") ?: 3

            public var outputType: OutputType = parameters.get<OutputType>("outputType") ?: OutputType.PER_FILE

            public var includeIndices: Boolean = parameters.get<Boolean>("includeIndices") ?: true

            public var logLevel: Level = parameters.get<Level>("logLevel") ?: Level.ERROR

            public var logFile: String? = parameters.get<String>("logFile")

            public var typeSortMode: SortMode = parameters.get<SortMode>("typeSortMode") ?: SortMode.ALPHABETICAL

            public var fileSortMode: SortMode = parameters.get<SortMode>("fileSortMode") ?: SortMode.ALPHABETICAL

            public var rpcSortMode: SortMode = parameters.get<SortMode>("rpcSortMode") ?: SortMode.ENCOUNTER

            public var fieldSortMode: MemberSortMode = parameters.get<MemberSortMode>("fieldSortMode") ?: MemberSortMode.ENCOUNTER

            public var enumValueSortMode: MemberSortMode = parameters.get<MemberSortMode>("enumValueSortMode") ?: MemberSortMode.ENCOUNTER

            public var resolveReferenceLinksMode: ResolveReferenceLinksMode =
                parameters.get<ResolveReferenceLinksMode>("resolveReferenceLinksMode") ?: ResolveReferenceLinksMode.FAIL_ON_INVALID

            public var transitiveReferences: TransitiveReferences =
                parameters.get<TransitiveReferences>("transitiveReferences") ?: TransitiveReferences.LINK_AS_PEER

            public var referenceLink: Map<String, String> =
                parameters.get<List<String>>("referenceLink")?.let { entries ->
                    val map = LinkedHashMap<String, String>()
                    for (entry in entries) {
                        val idx = entry.indexOf('=')
                        require(idx > 0 && idx < entry.length - 1) {
                            "referenceLink entry must be of the form <label>=<URL>, got `$entry`"
                        }
                        map[entry.substring(0, idx)] = entry.substring(idx + 1)
                    }
                    map
                } ?: emptyMap()

            public companion object {
                public fun from(parameters: Parameters): Builder = Builder(parameters)
            }

            public fun build(): Options =
                Options(
                    generateStableAnchors = generateStableAnchors,
                    generateInsertionPoints = generateInsertionPoints,
                    minTableOfContentsHeader = minTableOfContentsHeader,
                    maxTableOfContentsHeader = maxTableOfContentsHeader,
                    outputType = outputType,
                    includeIndices = includeIndices,
                    logLevel = logLevel,
                    logFile = logFile,
                    typeSortMode = typeSortMode,
                    fileSortMode = fileSortMode,
                    rpcSortMode = rpcSortMode,
                    fieldSortMode = fieldSortMode,
                    enumValueSortMode = enumValueSortMode,
                    resolveReferenceLinksMode = resolveReferenceLinksMode,
                    transitiveReferences = transitiveReferences,
                    referenceLink = referenceLink,
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
            val options = Options.Builder.from(cgreq.parameters).apply(block).build()
            applyLoggingConfiguration(options)
            return ProtocGenMarkdown(cgreq, options, clock)
        }

        /**
         * Reconfigures the Log4j 2 `Configuration` from [Options.logLevel] and [Options.logFile].
         *
         * Appenders are attached to the `com.engine` logger only, so downstream dependencies'
         * loggers stay silent regardless of their own level.  A stderr `Console` appender is
         * always attached, prefixed with `[protoc-gen-markdown]` so its records stand out from
         * other compiler output protoc may multiplex on the same stream.  When [Options.logFile]
         * is non-null a `File` appender is also attached, writing timestamped records to the
         * given path.  The root logger is silenced with `Level.OFF` to discard anything emitted
         * outside the `com.engine` tree.  Invoked from [from] immediately after [Options] is
         * built so subsequent `LoggerFactory.getLogger` calls observe the resolved configuration.
         */
        private fun applyLoggingConfiguration(options: Options) {
            val cb = ConfigurationBuilderFactory.newConfigurationBuilder()
            cb.setStatusLevel(Log4jLevel.OFF)

            cb.add(
                cb.newAppender("stderr", "Console")
                    .addAttribute("target", ConsoleAppender.Target.SYSTEM_ERR)
                    .add(
                        cb.newLayout("PatternLayout")
                            .addAttribute("pattern", "[protoc-gen-markdown] %-5level %logger{36} - %msg%n"),
                    ),
            )

            val engine =
                cb.newLogger("com.engine", options.logLevel.toLog4j())
                    .addAttribute("additivity", false)
                    .add(cb.newAppenderRef("stderr"))

            options.logFile?.let { path ->
                cb.add(
                    cb.newAppender("file", "File")
                        .addAttribute("fileName", path)
                        .add(
                            cb.newLayout("PatternLayout")
                                .addAttribute("pattern", "%d{HH:mm:ss.SSS} %-5level %logger{36} - %msg%n"),
                        ),
                )
                engine.add(cb.newAppenderRef("file"))
            }

            cb.add(engine)
            cb.add(cb.newRootLogger(Log4jLevel.OFF))
            val config = cb.build(false)
            // First call in this JVM: `initialize` so a fresh LoggerContext
            // starts with our configuration directly, bypassing Log4j's default
            // config-file probing (~24 file paths) that breaks under
            // native-image's strict missing-resource registration.
            // Subsequent calls (test harnesses re-entering `from(...)` with
            // different `logLevel` / `logFile`): `reconfigure` swaps in the
            // freshly-built configuration.  Calling `reconfigure` on the same
            // config object that was just installed by `initialize` triggers a
            // start-then-immediate-stop sequence inside log4j2 that leaves
            // every appender in the `stopped` state, silently dropping all
            // subsequent events — the gate below avoids that.
            if (configurationInitialized.compareAndSet(false, true)) {
                Configurator.initialize(config)
            } else {
                Configurator.reconfigure(config)
            }
        }

        private val configurationInitialized = AtomicBoolean(false)

        private fun Level.toLog4j(): Log4jLevel =
            when (this) {
                Level.ERROR -> Log4jLevel.ERROR
                Level.WARN -> Log4jLevel.WARN
                Level.INFO -> Log4jLevel.INFO
                Level.DEBUG -> Log4jLevel.DEBUG
                Level.TRACE -> Log4jLevel.TRACE
            }
    }

    public fun compile(): PluginProtos.CodeGeneratorResponse = Compiler(request, options, clock).compile()

    /**
     * The documents [compile] would emit for this request and [Options], described as a flat list
     * with their natural parent → child hierarchy but without rendering any Markdown.  The paths and
     * titles line up one-to-one with the `.md` files [compile] produces, so a sibling tool — notably
     * the `protoc-gen-markdown-jekyll` plugin — can position each page in a navigation tree without
     * re-deriving the layout rules.  See [PlannedDocument].
     */
    public fun plan(): List<PlannedDocument> = Compiler(request, options, clock).planDocuments()
}
