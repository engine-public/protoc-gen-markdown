# protoc-gen-markdown

A `protoc` compiler plugin that turns protobuf message, enum, and service definitions into [CommonMark](https://commonmark.org/) markdown documents.
Documents are built up as a [commonmark-java](https://github.com/commonmark/commonmark-java) AST and rendered with `org.commonmark.renderer.markdown.MarkdownRenderer`, so every output file round-trips cleanly through the same parser that produced it.
Each compile invocation can emit a per-proto overview document and/or a single aggregate document across the whole compile scope — selected by the [`documentTypes`](#compiler-options) option.
The plugin compiles to a native binary via GraalVM so it can be used directly in a `protoc` invocation without a JVM on `PATH`.

## Subprojects

| path | description |
|---|---|
| **root** (`src/`) | The plugin executable. Reads `CodeGeneratorRequest` from stdin and writes `CodeGeneratorResponse` to stdout, per the protoc plugin protocol. |
| [`examples/`](examples/) | Acceptance test suite. Each example fixes one compiler option to a non-default value and dumps the resulting documents as checked-in reference fixtures. |

## What it renders

Every document is a CommonMark file containing:

- A top-level `# <title>` heading naming the input proto (or, for `COMPLETE`, the common package prefix).
- An attribution paragraph identifying the plugin version that produced the document.
- One `## enum <name>` section per top-level enum, with values as a bullet list.
- One `## message <name>` section per top-level message, with fields as a bullet list — each item is `` `<type>` <name> ``, with a trailing `[]` on the type for `repeated` fields.
- One `## service <name>` section per service, with methods as a bullet list — each item is `` `RpcName(Request) Response` ``, prefixed with `stream ` on either side for client/server-streaming RPCs.

For `COMPLETE` documents, each input proto becomes a `## <file>` section and its enum/message/service sections shift down to `###` so the heading hierarchy stays consistent.

Nested messages, oneofs, maps, well-known-type suppression, and deprecated styling are not yet handled — this is the initial skeleton.
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
                create("markdown") {
                    option("documentTypes=FILE_OVERVIEW,COMPLETE")
                    // other options as desired
                }
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
  --markdown_out=documentTypes=FILE_OVERVIEW:./build/markdown \
  src/main/proto/example/v1/service.proto
```

## Compiler Options

All options are defined on [`ProtocGenMarkdown.Options`](src/main/kotlin/com/engine/protoc/markdown/ProtocGenMarkdown.kt).
Click the option name to jump to its KDoc for full semantics.

| name | type | default | summary |
|---|---|---|---|
| [`documentTypes`](src/main/kotlin/com/engine/protoc/markdown/ProtocGenMarkdown.kt#L36) | enum list | every value | Which output files the compiler emits per invocation: `FILE_OVERVIEW` (one `.md` per input proto), `COMPLETE` (one aggregate `.md` across the whole compile scope). |

Enum-valued options accept their values case-insensitively.
List-valued options accept comma-separated values inside the protoc parameter string.

## Related Projects

- [hotelengine/protoc-utils](https://github.com/HotelEngine/protoc-utils) — shared protoc plugin utilities (descriptor wrappers, comment parsing, parameter handling) and the `recorder` plugin used by this project's example suite.
- [hotelengine/protoc-gen-mermaid](https://github.com/hotelengine/protoc-gen-mermaid) — sibling plugin generating Mermaid class diagrams from the same descriptors; the layout and conventions of this repo follow it closely.
- [hotelengine/protoc-gen-openapi](https://github.com/hotelengine/protoc-gen-openapi) — sibling plugin generating OpenAPI 3.1 documents from the same descriptors.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for build commands, the native-image / reflection metadata workflow, the example-suite mechanics, and the PR process.
