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

- A top-level `# <path>` heading — the proto's full relative path as protoc reports it (e.g. `foo/bar/baz.proto`).
- A horizontal rule under the title, emitted whenever the file declares at least one service, message, or enum.
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
| [`generateStableAnchors`](src/main/kotlin/com/engine/protoc/markdown/ProtocGenMarkdown.kt#L39) | boolean | `false` | When `true`, every heading is prefixed with an inline `<a id="…"></a>` whose id is its full ancestor-heading path, and intra-document links (TOC, `[...](#…)` summary-table expansions) target those ids — so same-named headings under different parents (e.g. two `### Outer.Inner` messages, two same-named fields under different messages) stay distinct.  When `false`, no anchor element is emitted and links target the renderer's heading-text auto-anchor (lowercased, whitespace → `-`, non-alphanumeric dropped); cheaper output but vulnerable to heading-text collisions. |
| [`maxTableOfContentsHeader`](src/main/kotlin/com/engine/protoc/markdown/ProtocGenMarkdown.kt#L68) | int | `null` | Upper bound (inclusive) of heading levels included in the Table of Contents.  See `minTableOfContentsHeader` for the full interaction matrix.  `null` is treated as unbounded when min is set; both `null` means no TOC at all. |
| [`minTableOfContentsHeader`](src/main/kotlin/com/engine/protoc/markdown/ProtocGenMarkdown.kt#L61) | int | `null` | Lower bound (inclusive) of heading levels included in the Table of Contents.  Heading levels are L1 (file path) / L2 (`Services` / `Messages` / `Enums`) / L3 (each service/message/enum name) / L4 (Field/RPC/Value Summary + Details headers) / L5 (individual field/value/RPC names).  Both `null` (the default) → no TOC.  Only `max` set → min treated as `1`.  Only `min` set → max unbounded.  Both set → headings in `[min, max]` inclusive.  `min > max` logs a warning and skips the TOC.  The `___` thematic break under the title is always emitted when the file has at least one service/message/enum, independent of the TOC. |

See [CLAUDE.md](CLAUDE.md#adding-a-compiler-option) for the steps to add a new option.

## Related Projects

- [hotelengine/protoc-utils](https://github.com/HotelEngine/protoc-utils) — shared protoc plugin utilities (descriptor wrappers, comment parsing, parameter handling) and the `recorder` plugin used by this project's example suite.
- [hotelengine/protoc-gen-mermaid](https://github.com/hotelengine/protoc-gen-mermaid) — sibling plugin generating Mermaid class diagrams from the same descriptors; the layout and conventions of this repo follow it closely.
- [hotelengine/protoc-gen-openapi](https://github.com/hotelengine/protoc-gen-openapi) — sibling plugin generating OpenAPI 3.1 documents from the same descriptors.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for build commands, the native-image / reflection metadata workflow, the example-suite mechanics, and the PR process.
