# protoc-gen-markdown-jekyll

A companion `protoc` compiler plugin that injects [Jekyll](https://jekyllrb.com/) navigation frontmatter into the Markdown documents produced by [`protoc-gen-markdown`](../README.md).
It does not generate documents of its own.
Instead it runs in the same `protoc` invocation and, for each document the core plugin emits, splices a `title` and (where one applies) a `parent` key into that document's `frontmatter` insertion point — the keys [just-the-docs](https://just-the-docs.com) and compatible themes use to assemble a sidebar navigation tree.
Like the core plugin it compiles to a native binary via GraalVM so it can be used directly in a `protoc` invocation without a JVM on `PATH`.

## How it works

The core plugin emits a `# @@protoc_insertion_point(frontmatter)` marker inside every document's YAML frontmatter (see [`generateInsertionPoints`](../README.md#compiler-options)).
This plugin targets that marker via the standard `CodeGeneratorResponse.File.insertion_point` mechanism, so `protoc` itself splices the injected keys in immediately above the marker line.

Because each `protoc` plugin only receives the proto descriptors and its own parameter string — never the other plugins' generated files — this plugin reconstructs the core plugin's document layout itself.
It does so by reusing the core plugin directly: it parses the same layout options and calls `protoc-gen-markdown`'s render-free `ProtocGenMarkdown.plan()`, which returns the exact set of output paths, their H1 titles, and the natural parent → child hierarchy between them.
The injected frontmatter therefore lines up one-to-one with the documents the core plugin renders, with no duplicated or drifting layout logic.

### What it injects

For each planned document the plugin injects:

- `title` — the document's H1 title, identical to the heading the core plugin renders (a proto file's relative path for per-file pages, the dotted package name for package-index pages, the longest common package prefix or `Overview` for the overview page).
- `parent` — the `title` of the document's natural parent, so just-the-docs nests it correctly.
  The natural parentage mirrors the core plugin's own structure: the `overview` page sits at the top (when it exists), per-package index pages are children of the overview, and per-proto-file pages are children of their package's index page — each tier conditional on its existing under the active `outputType` / `includeIndices`.
  A document in a tier whose natural parent does not exist collapses onto the nearest ancestor that does.

The top-most tier (the overview, or — when no overview exists — whichever tier is highest) has no natural parent.
Set [`navigationParent`](#options) to give it one: a hand-authored Jekyll page title that the whole generated tree slots beneath.

## Deployment contract

The document layout depends on the core plugin's `outputType`, `includeIndices`, and `transitiveReferences` options.
Since each plugin sees only its own parameter string, **you must pass those same layout options to `--markdown_jekyll_out` that you pass to `--markdown_out`.**
If they disagree, the reconstructed layout — and therefore the injected `parent` keys and the insertion-point file paths — will not match the documents the core plugin actually emitted.
Both plugins must also write to the **same output directory** so the injected fragments land in the documents the core plugin produced.

## Usage

The plugin executable must be named `protoc-gen-markdown-jekyll` on `PATH` (or pointed at explicitly).
Options are passed as `--markdown_jekyll_out=<comma-separated-options>:<outdir>`.

### Gradle

Configure the [`protobuf-gradle-plugin`](https://github.com/google/protobuf-gradle-plugin) to invoke both plugins into the same output directory, with the layout options mirrored.

```kotlin
plugins {
    id("com.google.protobuf") version "0.9.6"
}

protobuf {
    plugins {
        create("markdown") {
            artifact = "com.engine:protoc-gen-markdown:<version>:${osdetector.classifier}@exe"
        }
        create("markdown_jekyll") {
            artifact = "com.engine:protoc-gen-markdown-jekyll:<version>:${osdetector.classifier}@exe"
        }
    }
    generateProtoTasks {
        all().all {
            plugins {
                // Mirror the layout options across both plugins.
                create("markdown") { option("outputType=PER_PACKAGE") }
                create("markdown_jekyll") { option("outputType=PER_PACKAGE"); option("navigationParent=Reference") }
            }
        }
    }
}
```

The published artifact is POM-only with one classified `.exe` per platform (`linux-x86_64`, `linux-aarch_64`, `osx-aarch_64`, `windows-x86_64`), mirroring the `io.grpc:protoc-gen-grpc-java` convention.

### Bash

```bash
# one-time: download the native binary for your platform from the GitHub release
curl -L -o protoc-gen-markdown-jekyll \
  https://github.com/hotelengine/protoc-gen-markdown/releases/download/<version>/protoc-gen-markdown-jekyll-<os>-<arch>.exe
chmod +x protoc-gen-markdown-jekyll
mv protoc-gen-markdown-jekyll /usr/local/bin/

# both plugins generate into ./build/markdown with matching layout options
protoc \
  --proto_path=path/to/your/protos \
  --markdown_out=./build/markdown \
  --markdown_jekyll_out=navigationParent=Reference:./build/markdown \
  src/main/proto/example/v1/service.proto
```

## Options

All options are defined on [`ProtocGenMarkdownJekyll.Options`](src/main/kotlin/com/engine/protoc/markdown/jekyll/ProtocGenMarkdownJekyll.kt).
Click the option name to jump to its KDoc for full semantics.
In addition to these, pass the core plugin's layout options (`outputType`, `includeIndices`, `transitiveReferences`) — see [Deployment contract](#deployment-contract).

| name | type | default | summary |
|---|---|---|---|
| [`logFile`](src/main/kotlin/com/engine/protoc/markdown/jekyll/ProtocGenMarkdownJekyll.kt#L56) | string | — | File path the SLF4J binding writes records to, in addition to the stderr console appender. When unset, records go to standard error only. |
| [`logLevel`](src/main/kotlin/com/engine/protoc/markdown/jekyll/ProtocGenMarkdownJekyll.kt#L45) | enum | `WARN` | SLF4J threshold (`TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR`) applied to every logger the plugin and its dependencies create. |
| [`navigationParent`](src/main/kotlin/com/engine/protoc/markdown/jekyll/ProtocGenMarkdownJekyll.kt#L71) | string | — | `parent` assigned to the top-most tier of generated documents — the overview page when one exists, otherwise the highest tier that does (the package pages, or the per-file pages when neither overview nor package pages exist).  When set, every top-tier document receives `parent: <navigationParent>`, slotting the whole generated tree beneath a hand-authored Jekyll page of that title.  Documents below the top tier always parent onto their natural ancestor.  Defaults to unset, leaving the top tier with no `parent`. |
| [`rootNavOrder`](src/main/kotlin/com/engine/protoc/markdown/jekyll/ProtocGenMarkdownJekyll.kt#L109) | int | — | `nav_order` assigned to the single top-tier root page — the overview, or the lone page of a `SINGLE_FILE` / single-package / single-file layout — ranking the whole generated tree among its sibling top-level pages instead of the theme's default title-alphabetical order. Every other generated document omits the key. When the top tier has more than one root, nothing is emitted and a `WARN` is logged. Defaults to unset, leaving the root with no `nav_order`. |
| [`visibleNavigationDepth`](src/main/kotlin/com/engine/protoc/markdown/jekyll/ProtocGenMarkdownJekyll.kt#L88) | int | — | Caps how many levels of the generated tree stay visible in the sidebar. Every document deeper than this 1-based depth gets `nav_exclude: true` (just-the-docs drops it from the nav but keeps it reachable). `0` excludes every page, `1` keeps only the top tier, `N` keeps tiers `1..N`. Depth counts generated levels only — a `navigationParent` page does not count. When the value exceeds the levels the tree has, a `WARN` is logged and nothing is excluded. Defaults to unset, leaving the whole tree visible. |

## Related Projects

- [`protoc-gen-markdown`](../README.md) — the core plugin this one enriches; its `frontmatter` insertion point and deterministic output filenames are what make this pairing possible (see [Pairing with other protoc plugins](../README.md#pairing-with-other-protoc-plugins)).
- [hotelengine/protoc-utils](https://github.com/HotelEngine/protoc-utils) — shared protoc plugin utilities (descriptor wrappers, comment parsing, parameter handling) and the `recorder` plugin used by the example suite.

## Contributing

See [CONTRIBUTING.md](../CONTRIBUTING.md) for build commands, the native-image / reflection metadata workflow, the example-suite mechanics, and the PR process.
