# protoc-gen-markdown

A `protoc` compiler plugin that turns protobuf message, enum, and service definitions into [CommonMark](https://commonmark.org/) markdown documents.
Documents are built up as a [commonmark-java](https://github.com/commonmark/commonmark-java) AST and rendered with `org.commonmark.renderer.markdown.MarkdownRenderer`, so every output file round-trips cleanly through the same parser that produced it.
Each compile invocation emits one `.md` per input proto containing an outline of the file's services, messages, and enums, plus — by default — one navigation-only index `.md` per distinct proto package and an `overview.md` aggregating them (see [`includeIndices`](#compiler-options)).
The plugin compiles to a native binary via GraalVM so it can be used directly in a `protoc` invocation without a JVM on `PATH`.

## Subprojects

| path | description |
|---|---|
| **root** (`src/`) | The plugin executable. Reads `CodeGeneratorRequest` from stdin and writes `CodeGeneratorResponse` to stdout, per the protoc plugin protocol. |
| [`jekyll/`](jekyll/README.md) | Companion plugin `protoc-gen-markdown-jekyll`. Runs alongside this plugin and injects [Jekyll](https://jekyllrb.com/) navigation keys (`title`, `parent`) into each generated document's `frontmatter` insertion point, turning the output into a [just-the-docs](https://just-the-docs.com)-style navigation tree. See its [README](jekyll/README.md). |
| [`examples/`](examples/) | Acceptance test suite. Each example fixes one compiler option to a non-default value and dumps the resulting documents as checked-in reference fixtures. |

## What it renders

For every file in the compile request, the plugin emits one `.md` at the same relative path with `.proto` swapped for `.md`.
Each document is an outline:

- A YAML frontmatter block at the very top — always emitted — modeled as a typed [`YamlFrontMatterBlock`](https://github.com/commonmark/commonmark-java/tree/main/commonmark-ext-yaml-front-matter) in the AST and carrying three flat keys: `generated-by` (the matching GitHub release-tag URL — name and version collapsed into one link), `protoc-gen-markdown-generated-on` (the generation instant, ISO-8601 UTC), and `protoc-gen-markdown-options` (every compiler option in effect for that compile snapshotted as a comma-separated `key=value` string in declaration order, defaults included).  The two plugin-namespaced keys carry the `protoc-gen-markdown-` prefix so sibling protoc plugins splicing extra top-level YAML keys at the `frontmatter` insertion point (see `generateInsertionPoints`) don't collide with anything the plugin owns.
- A top-level heading — the proto's full relative path as protoc reports it (e.g. `# foo/bar/baz.proto`).  Package-index `.md`s use the dotted package as the heading `# <dotted package>` (or `# Default Package` for files with no `package` directive); consolidated `outputType=PER_PACKAGE` documents use the same form, and `outputType=SINGLE_FILE` uses `# <longest common package prefix>` (or the bare `# Overview` when no common prefix exists).
- A horizontal rule under the title, emitted whenever the file declares at least one service, message, or enum, plus an additional rule before each significant section header — the per-file `## <path>` heading in the consolidated `outputType` modes and each `## Services` / `## Messages` / `## Enums` heading at every output type — deduplicated so a rule isn't emitted immediately after the under-title break or between a heading and its first sub-section.
- A `<details>` block whose summary is "Table of contents" and whose body lists the file's headings as a nested bullet list of intra-document links.  Which heading levels appear is controlled by the `minTableOfContentsHeader` / `maxTableOfContentsHeader` range; defaults to `[2, 3]` (sections and the type-name layer).  Set both to `null` to suppress the TOC entirely.
- A `## Services` section, emitted only if the file declares at least one service, with one `### <ServiceName>` per service.
  Under each service: the service's leading comment, then `#### RPC Summary` + a GFM table of RPCs (Name / Input / Output / Description); input and output types link to the file declaring them whenever in scope, and client/server-streaming RPCs prefix the corresponding cell with `stream `.
  When any RPC's leading comment has content beyond a single paragraph, the description cell gets a trailing `[...](#rpc-anchor)` link and the full comment renders under a `##### <RpcName>` heading grouped beneath `#### RPC Details`.
- A `## Messages` section, emitted only if the file declares at least one non-map-entry message, with one `### <MessageName>` per message.
  Under each message: the message's leading comment, then `#### Field Summary` + a GFM table of fields (Name / Type / Description); when any field's leading comment has content beyond a single paragraph, the description cell gets a trailing `[...](#field-anchor)` link and the full comment renders under a `##### <fieldName>` heading grouped beneath `#### Field Details`.
  Nested messages use dotted ancestor-prefixed names (`Outer.Inner`).
- A `## Enums` section, emitted only if the file declares at least one enum (top-level or nested in a message), with one `### <EnumName>` per enum.
  Under each enum: the enum's leading comment, then `#### Value Summary` + a GFM table of values (Name / Number / Description); when any value's leading comment has content beyond a single paragraph, the description cell gets a trailing `[...](#value-anchor)` link and the full comment renders under a `##### <ValueName>` heading grouped beneath `#### Value Details`.
  Enums nested in a message use the same dotted-name convention as nested messages.

No body content is rendered under the `### ` entries — they are name-only outline headings.
Synthetic `map<K,V>` entry messages are skipped everywhere.

See [the compile/ source](src/main/kotlin/com/engine/protoc/markdown/compile/Compiler.kt) for the extension points.

## Usage

The plugin executable must be named `protoc-gen-markdown` on `PATH` (or pointed at explicitly — see the Gradle example below).
Options are passed as `--markdown_out=<comma-separated-options>:<outdir>`.

### Gradle

Configure the [`protobuf-gradle-plugin`](https://github.com/google/protobuf-gradle-plugin) to invoke `protoc-gen-markdown` as a code-generation plugin.

Released artifacts are published to [GitHub Packages](https://github.com/engine-public/protoc-gen-markdown/packages).
GitHub Packages requires authentication to download from a Maven repository — even for public repositories — so consumers need a [GitHub personal access token](https://github.com/settings/tokens) with the `read:packages` scope.

```kotlin
plugins {
    id("com.google.protobuf") version "0.9.6"
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/engine-public/protoc-gen-markdown")
        credentials {
            username = providers.gradleProperty("gpr.user").orElse(providers.environmentVariable("GITHUB_ACTOR")).get()
            password = providers.gradleProperty("gpr.key").orElse(providers.environmentVariable("GITHUB_TOKEN")).get()
        }
    }
}

protobuf {
    plugins {
        create("markdown") {
            artifact = "com.engine:protoc-gen-markdown:<version>:${osdetector.classifier}@exe"
        }
    }
    generateProtoTasks {
        all().all {
            plugins {
                create("markdown") {}
            }
        }
    }
}
```

The published artifact is POM-only with one classified `.exe` per platform (`linux-x86_64`, `linux-aarch_64`, `osx-aarch_64`, `windows-x86_64`), mirroring the `io.grpc:protoc-gen-grpc-java` convention.
Use the `com.google.osdetector` Gradle plugin to pick the right classifier at resolve time.

### Bash

```bash
# one-time: download the native binary for your platform from the GitHub release
curl -L -o protoc-gen-markdown \
  https://github.com/engine-public/protoc-gen-markdown/releases/download/<version>/protoc-gen-markdown-<os>-<arch>.exe
chmod +x protoc-gen-markdown
mv protoc-gen-markdown /usr/local/bin/

protoc \
  --proto_path=path/to/your/protos \
  --markdown_out=./build/markdown \
  src/main/proto/example/v1/service.proto
```

## Compiler Options

All options are defined on [`ProtocGenMarkdown.Options`](src/main/kotlin/com/engine/protoc/markdown/ProtocGenMarkdown.kt).
Click the option name to jump to its KDoc for full semantics.

| name | type | default | summary |
|---|---|---|---|
| [`enumValueSortMode`](src/main/kotlin/com/engine/protoc/markdown/ProtocGenMarkdown.kt#L303) | enum | `ENCOUNTER` | Sort order for the values within each enum — drives the `#### Value Summary` table rows and the matching `Value Details` heading order.  `ENCOUNTER` (default) preserves proto declaration order, which in proto3 always leads with the zero value.  `ALPHABETICAL` sorts by value name.  `NUMBER` sorts by the integer value. |
| [`fieldSortMode`](src/main/kotlin/com/engine/protoc/markdown/ProtocGenMarkdown.kt#L289) | enum | `ENCOUNTER` | Sort order for the fields within each message — drives the `#### Field Summary` table rows and the matching `Field Details` heading order.  `ENCOUNTER` (default) preserves proto declaration order, which usually mirrors the field-number tags authors picked.  `ALPHABETICAL` sorts by field name.  `NUMBER` sorts by the proto field number (the integer tag). |
| [`fileSortMode`](src/main/kotlin/com/engine/protoc/markdown/ProtocGenMarkdown.kt#L261) | enum | `ALPHABETICAL` | Sort order for the per-file L2 sub-sections inside a consolidated [`outputType=PER_PACKAGE`](#compiler-options) / `SINGLE_FILE` document.  `ALPHABETICAL` (default) sorts by the proto's full relative path (`file.name`).  `ENCOUNTER` preserves the order protoc walked the input list.  No-op under `PER_FILE`. |
| [`generateInsertionPoints`](src/main/kotlin/com/engine/protoc/markdown/ProtocGenMarkdown.kt#L97) | boolean | `true` | When `true` (default), emits `@@protoc_insertion_point(NAME)` markers at a fixed catalog of scopes (an in-YAML `frontmatter` line, the document header/footer, two markers under each file heading, each section heading, and two markers per service/message/enum) so sibling protoc plugins can splice content into the generated Markdown via the standard `CodeGeneratorResponse.File.insertion_point` mechanism.  The `frontmatter` marker is wrapped as a `#` YAML comment so it doesn't break YAML parsing; every other marker is wrapped in an HTML comment so it's invisible in rendered Markdown.  Type names in the markers are fully qualified (proto `package` joined with the dotted ancestor-prefixed name).  See [Pairing with other protoc plugins](#pairing-with-other-protoc-plugins) for the full marker catalog. |
| [`generateStableAnchors`](src/main/kotlin/com/engine/protoc/markdown/ProtocGenMarkdown.kt#L46) | boolean | `true` | When `true` (default), every heading is prefixed with an inline `<a id="…"></a>` whose id is its full ancestor-heading path, and intra-document links (TOC, `[...](#…)` summary-table expansions) target those ids — so same-named headings under different parents (e.g. two `### Outer.Inner` messages, two same-named fields under different messages) stay distinct.  When `false`, no anchor element is emitted and links target the renderer's heading-text auto-anchor (lowercased, whitespace → `-`, non-alphanumeric dropped); cheaper output but vulnerable to heading-text collisions. |
| [`includeIndices`](src/main/kotlin/com/engine/protoc/markdown/ProtocGenMarkdown.kt#L204) | boolean | `true` | Controls navigation-only aggregator documents emitted alongside the primary per-`outputType` output.  Two layers, both gated by this switch.  **Package indices** — under `outputType=PER_FILE` only, one extra `.md` per distinct proto package titled `# <dotted package>`: a `file → section → type → member` bulleted Table of Contents whose entries link to anchors in the per-file `.md`s.  Filename mirrors the `PER_PACKAGE` rules (`<pkg-as-dir>/package.md` namespaced; `<fully.qualified.package>.md` at root otherwise; `default.md` titled `# Default Package` for files with no `package` directive).  Anchor naming honors `generateStableAnchors`.  Member entry paths match the per-file Details headings.  **Overview** — under `outputType=PER_FILE` and `PER_PACKAGE`, a single `overview.md` at the output root with `# Overview` (or `# <longest common package prefix>` when one exists) whose body is a flat bulleted list of every package, each linked to that package's primary `.md` (the package index under `PER_FILE`; the consolidated package document under `PER_PACKAGE`).  Filenames that collide with another emitted output are dropped with a warning so the user's content wins.  No-op under `outputType=SINGLE_FILE` (the single consolidated document is itself the aggregator) — set to `true` together with `SINGLE_FILE` and an INFO line records that it had no effect. |
| [`logFile`](src/main/kotlin/com/engine/protoc/markdown/ProtocGenMarkdown.kt#L228) | string | — | File path the SLF4J binding writes records to. When unset, records go to standard error. |
| [`logLevel`](src/main/kotlin/com/engine/protoc/markdown/ProtocGenMarkdown.kt#L217) | enum | `ERROR` | SLF4J threshold (`TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR`) applied to every logger the plugin and its dependencies create. |
| [`maxTableOfContentsHeader`](src/main/kotlin/com/engine/protoc/markdown/ProtocGenMarkdown.kt#L132) | int | `3` | Upper bound (inclusive) of heading levels included in the Table of Contents.  See `minTableOfContentsHeader` for the full interaction matrix.  `null` is treated as unbounded when min is set; both `null` means no TOC at all. |
| [`minTableOfContentsHeader`](src/main/kotlin/com/engine/protoc/markdown/ProtocGenMarkdown.kt#L123) | int | `2` | Lower bound (inclusive) of heading levels included in the Table of Contents.  Heading levels are L1 (the document's title — the file `<path>` / the dotted `<pkg>` / the `<prefix>` or `Overview`) / L2 (`Services` / `Messages` / `Enums`) / L3 (each service/message/enum name) / L4 (Field/RPC/Value Summary + Details headers) / L5 (individual field/value/RPC names).  Paired with `maxTableOfContentsHeader`'s default of `3` the TOC covers the section and type-name layers.  Both `null` → no TOC.  Only `max` set → min treated as `1`.  Only `min` set → max unbounded.  Both set → headings in `[min, max]` inclusive.  `min > max` logs a warning and skips the TOC.  The `___` thematic break under the title is always emitted when the file has at least one service/message/enum, independent of the TOC. |
| [`outputType`](src/main/kotlin/com/engine/protoc/markdown/ProtocGenMarkdown.kt#L172) | enum | `PER_FILE` | Selects how input files are mapped to output files.  `PER_FILE` (the default) emits one `.md` per input proto, mirroring the input layout, with H1 `# <path>`.  `PER_PACKAGE` consolidates every file declaring the same proto package into one `.md` — `<pkg-as-dir>/package.md` when every such file lives at the directory whose path equals the package's dotted name with `.` → `/`, otherwise `<fully.qualified.package>.md` at the output root — with H1 `# <dotted package>` (`# Default Package` for files with no `package` directive), each input file as an H2, and the existing per-file headings shifted down one level.  `SINGLE_FILE` consolidates every file in the compile request into one `.md` at the output root, named `<longest-common-package-prefix>.md` (or `overview.md` when no common prefix exists), with H1 `# <longest common prefix>` (or the bare `# Overview` when no common prefix exists).  Cross-type references whose source and target end up in the same consolidated file collapse to bare `#anchor` links; same-file references in `PER_FILE` keep the existing `<file>.md#anchor` shape. |
| [`referenceLink`](src/main/kotlin/com/engine/protoc/markdown/ProtocGenMarkdown.kt#L432) | map (repeatable `label=URL`) | — | Per-label URL overrides applied to two reference sites: shortcut-reference syntax inside proto leading comments, and field-type / RPC input-output cells whose declared type FQN matches a map key.  Each entry maps a bracketed label exactly as it appears in the comment (`[CoreEntity]`, `[Order.id]`, `[google.protobuf.Timestamp]`) — or, for the field/RPC cell path, the fully-qualified type name with no leading dot (`google.rpc.Status`) — to the URL the rewritten link should target.  When a matching key is found the override URL wins unconditionally, with no compile-scope, peer, or transitive lookup performed for that reference — so a reference whose only would-be target lives in a transitive dependency is not enough on its own to pull that dependency in as a peer or promote it under `transitiveReferences=INCLUDE_FILES`; a file pulled in by other, non-overridden references still gets promoted normally.  Labels not present in the map fall through to the normal resolver and remain subject to `resolveReferenceLinksMode` (WARN / FAIL_ON_INVALID).  Passed as repeated `referenceLink=<label>=<URL>` entries; the parameter parser splits the value on `=` with a limit of two, so the URL may contain additional `=` characters.  URLs containing `:` collide with protoc's `--markdown_out=opts:outdir` splitter — pass site-relative paths (`/docs/foo`) or scheme-stripped values (`example.com/docs/foo`) inside `--markdown_out`, or use `--markdown_opt=` for `:`-bearing URLs.  Default empty. |
| [`resolveReferenceLinksMode`](src/main/kotlin/com/engine/protoc/markdown/ProtocGenMarkdown.kt#L336) | enum | `FAIL_ON_INVALID` | Controls how CommonMark reference-link syntax inside proto leading comments — e.g. `[GreetingResponse]`, `[GreetingRequest.name]`, `[GreeterService.SayHello]`, `[GreetingType.GREETING_TYPE_FORMAL]` — is handled.  Both the shortcut form (`[label]`) and the full form (`[display text][label]`) are recognized; in the full form the bracketed label is the lookup key and the display text passes through unchanged.  Escaped brackets (`\[…\]`) follow CommonMark semantics — the parser treats them as literal text and the resolver never fires.  `NONE` skips the rewrite entirely (references stay literal).  `WARN` resolves references against the types, fields, enum values, and RPCs in the compile scope and rewrites them into real Markdown links; each comment-level reference that fails to resolve, or that resolves through an ambiguous short-name collision, logs at `warn` naming the request site (proto file, comment scope, label) and the candidates / reason.  `FAIL_ON_INVALID` (default) runs the same rewrite path but logs each failing reference at `error` and additionally **collects** them, setting `CodeGeneratorResponse.error` at end-of-compile so protoc fails the run.  Logging is reference-driven: index-time collisions that no comment depends on only emit a `debug` line.  The resolver honors the comment's own anchor descriptor for bare-name lookups, so authors can write `[name]` inside a comment on `message User` to find `User.name`; field- and RPC-scoped lookups also include the descriptor's declared target type by short name so `[Foo]` in a comment on a field of type `Foo` resolves locally even when other `Foo`s exist in the global scope. |
| [`rpcSortMode`](src/main/kotlin/com/engine/protoc/markdown/ProtocGenMarkdown.kt#L273) | enum | `ENCOUNTER` | Sort order for the RPCs within each service — drives both the `#### RPC Summary` table rows and the `#### RPC Details` heading order.  `ENCOUNTER` (default) preserves proto declaration order, which is typically how authors group related calls.  `ALPHABETICAL` sorts by method name. |
| [`transitiveReferences`](src/main/kotlin/com/engine/protoc/markdown/ProtocGenMarkdown.kt#L372) | enum | `LINK_AS_PEER` | Controls how messages / enums that live in a transitive `.proto` file — one the request pulled in as a dependency of `filesToGenerate` but didn't itself ask to render — are linked.  `NONE` treats transitive types as unknown: field-type cells render the leaf name as plain text and comment `[label]` references resolve as if the type did not exist.  `LINK_AS_PEER` (default) emits links as if a sibling protoc run rendered the transitive `.proto` files (with the same options) into a peer output tree — link shape matches the same plugin's `PER_FILE` `<path>.md#anchor` convention; no peer `.md` is emitted by this run.  `INCLUDE_FILES` promotes every transitive `.proto` from which at least one type is referenced into the generated output set, so links resolve to content this run actually emitted; the bundle becomes self-contained at the cost of growing the output with the dependency closure.  Well-known protos (`google.protobuf.*`) are stripped from the request by protoc, so neither mode can link them. |
| [`typeSortMode`](src/main/kotlin/com/engine/protoc/markdown/ProtocGenMarkdown.kt#L246) | enum | `ALPHABETICAL` | Sort order for the services / messages / enums lists under each `## Services` / `## Messages` / `## Enums` section.  `ALPHABETICAL` (default) sorts each list by name; for messages and enums (collected via a depth-first descriptor walk), the flat list is sorted by full dotted name so parents naturally precede their children (`Foo`, `Foo.Bar`, `Foo.Baz`, `Quux`).  `ENCOUNTER` preserves the proto declaration order — nested messages immediately follow their parent. |

See [CLAUDE.md](CLAUDE.md#adding-a-compiler-option) for the steps to add a new option.

## Pairing with other protoc plugins

Two design choices make this plugin easy to combine with sibling protoc plugins that need to enrich the generated Markdown or generate alongside it:

1. **Deterministic output filenames.**  For any compile request, the set of `.md` paths this plugin produces is a pure function of the input file list, those files' proto `package` directives, and the `outputType` option — no descriptor traversal needed.  A sibling plugin can predict the same paths and emit `CodeGeneratorResponse.File` entries that line up with them.
2. **Named insertion points.**  When `generateInsertionPoints=true` (default on), the plugin emits a fixed catalog of `<!-- @@protoc_insertion_point(NAME) -->` markers.  Sibling plugins target these names via the standard `CodeGeneratorResponse.File.insertion_point` / `content` fields, and `protoc` itself splices the content in immediately before the marker line.  A splicing sibling **must run after this plugin** — `protoc` applies responses in command-line order and can only insert into a file an earlier plugin already wrote; otherwise it fails with `Tried to insert into file that doesn't exist`.  With the [`protobuf-gradle-plugin`](https://github.com/google/protobuf-gradle-plugin) that order is the **alphabetical order of the registered plugin names**, not `create(...)` declaration order, so name the sibling to sort after `markdown` (e.g. `markdownJekyll`, not `jekyll`).

The bundled [`protoc-gen-markdown-jekyll`](jekyll/README.md) plugin is the reference example of both: it reconstructs the exact output filenames and reuses the same layout planner to inject Jekyll navigation keys at the `frontmatter` insertion point. **Read more: [`jekyll/README.md`](jekyll/README.md).**

### Output filenames

**`PER_FILE`** (default) — one `.md` per input proto, mirroring the input directory layout.  `foo/bar/baz.proto` → `foo/bar/baz.md`.
When `includeIndices=true` (the default), one extra navigation-only `.md` is also emitted per distinct proto package, using the same filename rules as `PER_PACKAGE` below, plus an `overview.md` at the output root aggregating those package indices.

**`PER_PACKAGE`** — every input file declaring the same proto `package` consolidates into one `.md`.  Output location depends on the package's layout:
- *Namespaced* — every file declaring `foo.bar` lives under `foo/bar/`.  Output: `foo/bar/package.md`.
- *Non-namespaced* — at least one file declaring `foo.bar` lives outside `foo/bar/`.  Output at the output root: `foo.bar.md`.
- *No package* — files declaring no `package` directive group under the title `(no package)` at the output root: `default.md`.

When `includeIndices=true` (the default), an `overview.md` is also emitted at the output root linking to every package's consolidated document.

**`SINGLE_FILE`** — every input file in the compile request consolidates into a single `.md` at the output root.  The basename is the longest dotted package prefix shared across all files (e.g. files declaring `foo.bar.baz` and `foo.bar.qux` → `foo.bar.md`), or `overview.md` when there is no common prefix.  `includeIndices` is a no-op in this mode (the single document IS the aggregator); setting it to `true` here emits an INFO line and otherwise has no effect.

### Insertion-point catalog

Markers are listed in document order.  All are wrapped in HTML comments — invisible in the rendered document — except `frontmatter`, which is wrapped as a `#` YAML comment so YAML parsers ignore it while protoc's marker parser still matches.  In the `name` column, `<file>` is the proto file's full relative path as protoc reports it (e.g. `foo/bar/baz.proto`); `<fqn>` is the fully-qualified type name (proto `package` joined with the dotted ancestor-prefixed type name — `foo.bar.Outer.Inner` for `Outer.Inner` declared in `package foo.bar`).

| name | position | emitted |
|---|---|---|
| `frontmatter` | last line inside the YAML block, before the closing `---` fence | always |
| `file_header` | directly after the L1 group title | `PER_PACKAGE` and `SINGLE_FILE` only — the L1 there is the package title (`PER_PACKAGE`) or the single-file overview title — the longest common package prefix, or `Overview` (`SINGLE_FILE`), not a file.  In `PER_FILE` the L1 IS the file heading, so `file_header_scope:<file>` covers the same spot instead |
| `file_header_scope:<file>` | directly after each file's heading (the L1 in `PER_FILE`, each per-file L2 in the consolidated modes) | every output type, once per input file |
| `file_scope:<file>` | immediately after `file_header_scope:<file>` (the plugin renders no file-level leading comment, so the two markers are adjacent) | every output type, once per input file |
| `services_section` | directly after the `Services` section heading | per file, only when the file declares ≥1 service |
| `service_header_scope:<fqn>` | after each `### ServiceName`, before its leading proto comment | per service |
| `service_scope:<fqn>` | after the service's leading comment, before `#### RPC Summary` | per service |
| `messages_section` | directly after the `Messages` section heading | per file, only when the file declares ≥1 non-`map`-entry message |
| `message_header_scope:<fqn>` | after each `### MessageName`, before its leading proto comment | per message |
| `message_scope:<fqn>` | after the message's leading comment, before `#### Field Summary` | per message |
| `enums_section` | directly after the `Enums` section heading | per file, only when the file declares ≥1 enum |
| `enum_header_scope:<fqn>` | after each `### EnumName`, before its leading proto comment | per enum |
| `enum_scope:<fqn>` | after the enum's leading comment, before `#### Value Summary` | per enum |
| `file_footer` | at the very end of the document | always |

Set `generateInsertionPoints=false` to suppress every marker and produce output byte-identical to a build with this option absent.

Package-index files (emitted when `includeIndices=true` under `outputType=PER_FILE`) and `overview.md` (emitted when `includeIndices=true` under `PER_FILE` or `PER_PACKAGE`) also carry `frontmatter`, `file_header`, and `file_footer` markers — none of the per-type or per-section markers apply because their bodies are bulleted Tables of Contents rather than per-file outlines.

## Related Projects

- [engine-public/protoc-utils](https://github.com/engine-public/protoc-utils) — shared protoc plugin utilities (descriptor wrappers, comment parsing, parameter handling) and the `recorder` plugin used by this project's example suite.
- [engine-public/protoc-gen-mermaid](https://github.com/engine-public/protoc-gen-mermaid) — sibling plugin generating Mermaid class diagrams from the same descriptors; the layout and conventions of this repo follow it closely.
- [engine-public/protoc-gen-openapi](https://github.com/engine-public/protoc-gen-openapi) — sibling plugin generating OpenAPI 3.1 documents from the same descriptors.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for build commands, the native-image / reflection metadata workflow, the example-suite mechanics, and the PR process.
