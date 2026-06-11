# protoc-gen-markdown

A `protoc` compiler plugin that turns protobuf message, enum, and service definitions into [CommonMark](https://commonmark.org/) markdown documents.
Documents are built up as a [commonmark-java](https://github.com/commonmark/commonmark-java) AST and rendered with `org.commonmark.renderer.markdown.MarkdownRenderer`, so every output file round-trips cleanly through the same parser that produced it.
Each compile invocation emits one `.md` per input proto containing an outline of the file's services, messages, and enums.
The plugin compiles to a native binary via GraalVM so it can be used directly in a `protoc` invocation without a JVM on `PATH`.

## Subprojects

| path | description |
|---|---|
| **root** (`src/`) | The plugin executable. Reads `CodeGeneratorRequest` from stdin and writes `CodeGeneratorResponse` to stdout, per the protoc plugin protocol. |
| [`examples/`](examples/) | Acceptance test suite. Each example fixes one compiler option to a non-default value and dumps the resulting documents as checked-in reference fixtures. |

## What it renders

For every file in the compile request, the plugin emits one `.md` at the same relative path with `.proto` swapped for `.md`.
Each document is an outline:

- A YAML frontmatter block at the very top — always emitted — modeled as a typed [`YamlFrontMatterBlock`](https://github.com/commonmark/commonmark-java/tree/main/commonmark-ext-yaml-front-matter) in the AST and carrying three flat keys: `generated-by` (the matching GitHub release-tag URL — name and version collapsed into one link), `protoc-gen-markdown-generated-on` (the generation instant, ISO-8601 UTC), and `protoc-gen-markdown-options` (every compiler option in effect for that compile snapshotted as a comma-separated `key=value` string in declaration order, defaults included).  The two plugin-namespaced keys carry the `protoc-gen-markdown-` prefix so sibling protoc plugins splicing extra top-level YAML keys at the `frontmatter` insertion point (see `generateInsertionPoints`) don't collide with anything the plugin owns.
- A top-level `# <path>` heading — the proto's full relative path as protoc reports it (e.g. `foo/bar/baz.proto`).
- A horizontal rule under the title, emitted whenever the file declares at least one service, message, or enum, plus an additional rule before each significant section header — the per-file `## <path>` heading in the consolidated `outputType` modes and each `## Services` / `## Messages` / `## Enums` heading at every output type — deduplicated so a rule isn't emitted immediately after the under-title break or between a heading and its first sub-section.
- Optionally — opt-in via the `minTableOfContentsHeader` / `maxTableOfContentsHeader` options — a `<details>` block whose summary is "Table of contents" and whose body lists the file's headings as a nested bullet list of intra-document links.  Which heading levels appear is controlled by the option range; both options `null` (the default) suppresses the TOC entirely.
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

```kotlin
plugins {
    id("com.google.protobuf") version "0.9.6"
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
  https://github.com/hotelengine/protoc-gen-markdown/releases/download/<version>/protoc-gen-markdown-<os>-<arch>.exe
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
| [`enumValueSortMode`](src/main/kotlin/com/engine/protoc/markdown/ProtocGenMarkdown.kt#L235) | enum | `ENCOUNTER` | Sort order for the values within each enum — drives the `#### Value Summary` table rows and the matching `Value Details` heading order.  `ENCOUNTER` (default) preserves proto declaration order, which in proto3 always leads with the zero value.  `ALPHABETICAL` sorts by value name.  `NUMBER` sorts by the integer value. |
| [`fieldSortMode`](src/main/kotlin/com/engine/protoc/markdown/ProtocGenMarkdown.kt#L221) | enum | `ENCOUNTER` | Sort order for the fields within each message — drives the `#### Field Summary` table rows and the matching `Field Details` heading order.  `ENCOUNTER` (default) preserves proto declaration order, which usually mirrors the field-number tags authors picked.  `ALPHABETICAL` sorts by field name.  `NUMBER` sorts by the proto field number (the integer tag). |
| [`fileSortMode`](src/main/kotlin/com/engine/protoc/markdown/ProtocGenMarkdown.kt#L193) | enum | `ALPHABETICAL` | Sort order for the per-file L2 sub-sections inside a consolidated [`outputType=PER_PACKAGE`](#compiler-options) / `PER_SESSION` document.  `ALPHABETICAL` (default) sorts by the proto's full relative path (`file.name`).  `ENCOUNTER` preserves the order protoc walked the input list.  No-op under `PER_FILE`. |
| [`generateInsertionPoints`](src/main/kotlin/com/engine/protoc/markdown/ProtocGenMarkdown.kt#L92) | boolean | `false` | When `true`, emits `@@protoc_insertion_point(NAME)` markers at a fixed catalog of scopes (an in-YAML `frontmatter` line, the document header/footer, two markers under each file heading, each section heading, and two markers per service/message/enum) so sibling protoc plugins can splice content into the generated Markdown via the standard `CodeGeneratorResponse.File.insertion_point` mechanism.  The `frontmatter` marker is wrapped as a `#` YAML comment so it doesn't break YAML parsing; every other marker is wrapped in an HTML comment so it's invisible in rendered Markdown.  Type names in the markers are fully qualified (proto `package` joined with the dotted ancestor-prefixed name).  See [Pairing with other protoc plugins](#pairing-with-other-protoc-plugins) for the full marker catalog. |
| [`generateStableAnchors`](src/main/kotlin/com/engine/protoc/markdown/ProtocGenMarkdown.kt#L41) | boolean | `false` | When `true`, every heading is prefixed with an inline `<a id="…"></a>` whose id is its full ancestor-heading path, and intra-document links (TOC, `[...](#…)` summary-table expansions) target those ids — so same-named headings under different parents (e.g. two `### Outer.Inner` messages, two same-named fields under different messages) stay distinct.  When `false`, no anchor element is emitted and links target the renderer's heading-text auto-anchor (lowercased, whitespace → `-`, non-alphanumeric dropped); cheaper output but vulnerable to heading-text collisions. |
| [`maxTableOfContentsHeader`](src/main/kotlin/com/engine/protoc/markdown/ProtocGenMarkdown.kt#L121) | int | `null` | Upper bound (inclusive) of heading levels included in the Table of Contents.  See `minTableOfContentsHeader` for the full interaction matrix.  `null` is treated as unbounded when min is set; both `null` means no TOC at all. |
| [`minTableOfContentsHeader`](src/main/kotlin/com/engine/protoc/markdown/ProtocGenMarkdown.kt#L114) | int | `null` | Lower bound (inclusive) of heading levels included in the Table of Contents.  Heading levels are L1 (file path) / L2 (`Services` / `Messages` / `Enums`) / L3 (each service/message/enum name) / L4 (Field/RPC/Value Summary + Details headers) / L5 (individual field/value/RPC names).  Both `null` (the default) → no TOC.  Only `max` set → min treated as `1`.  Only `min` set → max unbounded.  Both set → headings in `[min, max]` inclusive.  `min > max` logs a warning and skips the TOC.  The `___` thematic break under the title is always emitted when the file has at least one service/message/enum, independent of the TOC. |
| [`outputType`](src/main/kotlin/com/engine/protoc/markdown/ProtocGenMarkdown.kt#L160) | enum | `PER_FILE` | Selects how input files are mapped to output files.  `PER_FILE` (the default) emits one `.md` per input proto, mirroring the input layout.  `PER_PACKAGE` consolidates every file declaring the same proto package into one `.md` — `<pkg-as-dir>/package.md` when every such file lives at the directory whose path equals the package's dotted name with `.` → `/`, otherwise `<fully.qualified.package>.md` at the output root — with the package name as the H1, each input file as an H2, and the existing per-file headings shifted down one level.  `PER_SESSION` consolidates every file in the compile request into one `.md` at the output root, named `<longest-common-package-prefix>.md` (or `overview.md` when no common prefix exists).  Cross-type references whose source and target end up in the same consolidated file collapse to bare `#anchor` links; same-file references in `PER_FILE` keep the existing `<file>.md#anchor` shape. |
| [`rpcSortMode`](src/main/kotlin/com/engine/protoc/markdown/ProtocGenMarkdown.kt#L205) | enum | `ENCOUNTER` | Sort order for the RPCs within each service — drives both the `#### RPC Summary` table rows and the `#### RPC Details` heading order.  `ENCOUNTER` (default) preserves proto declaration order, which is typically how authors group related calls.  `ALPHABETICAL` sorts by method name. |
| [`typeSortMode`](src/main/kotlin/com/engine/protoc/markdown/ProtocGenMarkdown.kt#L178) | enum | `ALPHABETICAL` | Sort order for the services / messages / enums lists under each `## Services` / `## Messages` / `## Enums` section.  `ALPHABETICAL` (default) sorts each list by name; for messages and enums (collected via a depth-first descriptor walk), the flat list is sorted by full dotted name so parents naturally precede their children (`Foo`, `Foo.Bar`, `Foo.Baz`, `Quux`).  `ENCOUNTER` preserves the proto declaration order — nested messages immediately follow their parent. |

See [CLAUDE.md](CLAUDE.md#adding-a-compiler-option) for the steps to add a new option.

## Pairing with other protoc plugins

Two design choices make this plugin easy to combine with sibling protoc plugins that need to enrich the generated Markdown or generate alongside it:

1. **Deterministic output filenames.**  For any compile request, the set of `.md` paths this plugin produces is a pure function of the input file list, those files' proto `package` directives, and the `outputType` option — no descriptor traversal needed.  A sibling plugin can predict the same paths and emit `CodeGeneratorResponse.File` entries that line up with them.
2. **Named insertion points.**  When `generateInsertionPoints=true` (default off), the plugin emits a fixed catalog of `<!-- @@protoc_insertion_point(NAME) -->` markers.  Sibling plugins target these names via the standard `CodeGeneratorResponse.File.insertion_point` / `content` fields, and `protoc` itself splices the content in immediately before the marker line.

### Output filenames

**`PER_FILE`** (default) — one `.md` per input proto, mirroring the input directory layout.  `foo/bar/baz.proto` → `foo/bar/baz.md`.

**`PER_PACKAGE`** — every input file declaring the same proto `package` consolidates into one `.md`.  Output location depends on the package's layout:
- *Namespaced* — every file declaring `foo.bar` lives under `foo/bar/`.  Output: `foo/bar/package.md`.
- *Non-namespaced* — at least one file declaring `foo.bar` lives outside `foo/bar/`.  Output at the output root: `foo.bar.md`.
- *No package* — files declaring no `package` directive group under the title `(no package)` at the output root: `default.md`.

**`PER_SESSION`** — every input file in the compile request consolidates into a single `.md` at the output root.  The basename is the longest dotted package prefix shared across all files (e.g. files declaring `foo.bar.baz` and `foo.bar.qux` → `foo.bar.md`), or `overview.md` when there is no common prefix.

### Insertion-point catalog

Markers are listed in document order.  All are wrapped in HTML comments — invisible in the rendered document — except `frontmatter`, which is wrapped as a `#` YAML comment so YAML parsers ignore it while protoc's marker parser still matches.  In the `name` column, `<file>` is the proto file's full relative path as protoc reports it (e.g. `foo/bar/baz.proto`); `<fqn>` is the fully-qualified type name (proto `package` joined with the dotted ancestor-prefixed type name — `foo.bar.Outer.Inner` for `Outer.Inner` declared in `package foo.bar`).

| name | position | emitted |
|---|---|---|
| `frontmatter` | last line inside the YAML block, before the closing `---` fence | always |
| `file_header` | directly after the L1 group title | `PER_PACKAGE` and `PER_SESSION` only — the L1 there is the package or session title, not a file.  In `PER_FILE` the L1 IS the file heading, so `file_header_scope:<file>` covers the same spot instead |
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

When `generateInsertionPoints=false` (the default) none of these markers are emitted and the output is byte-identical to a build with this option absent.

## Related Projects

- [hotelengine/protoc-utils](https://github.com/HotelEngine/protoc-utils) — shared protoc plugin utilities (descriptor wrappers, comment parsing, parameter handling) and the `recorder` plugin used by this project's example suite.
- [hotelengine/protoc-gen-mermaid](https://github.com/hotelengine/protoc-gen-mermaid) — sibling plugin generating Mermaid class diagrams from the same descriptors; the layout and conventions of this repo follow it closely.
- [hotelengine/protoc-gen-openapi](https://github.com/hotelengine/protoc-gen-openapi) — sibling plugin generating OpenAPI 3.1 documents from the same descriptors.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for build commands, the native-image / reflection metadata workflow, the example-suite mechanics, and the PR process.
