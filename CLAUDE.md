# CLAUDE.md

Guidance for Claude Code when working in this repository.

For project overview, what the plugin renders, usage, and the full compiler-options table, read [README.md](README.md).
For build commands, the native-image / reflection-metadata workflow, code style, the example-suite pipeline, and the PR process, read [CONTRIBUTING.md](CONTRIBUTING.md).

## Working Style

**Never silently defer scope.**
When a request reasonably implies multiple sub-pieces, do not unilaterally decide to ship some now and "leave the rest as a follow-up."
If something looks deferrable, ask the user before narrowing the scope — explicitly, before reporting the work as done.

When in doubt: ask, even if it feels obvious.
Scope calls belong to the user.

## Code Navigation

- [`Main.kt`](src/main/kotlin/com/engine/protoc/markdown/Main.kt) — process entry point.
  Wires `System.in` → `ProtocGenMarkdown.from(...).compile()` → `System.out`. Exits 1 if the response carries an `error`.
- [`ProtocGenMarkdown.kt`](src/main/kotlin/com/engine/protoc/markdown/ProtocGenMarkdown.kt) — public façade.
  Houses the `Options` class (every plugin option lives here as a property with KDoc) and the `Options.Builder` that parses each option from the `--markdown_out=key=value,…:outdir` parameter string.
  The `from(...)` factory parses the `CodeGeneratorRequest` and applies the DSL block; the instance-level `compile()` delegates to the internal `Compiler` below.
- [`compile/Compiler.kt`](src/main/kotlin/com/engine/protoc/markdown/compile/Compiler.kt) — internal compiler.
  Owns all rendering.  Builds a [commonmark-java](https://github.com/commonmark/commonmark-java) `Document` tree per output file and renders it via `MarkdownRenderer`.
  Extend this class — not `ProtocGenMarkdown` — when adding compiler features.

The `protoc-utils` library hides almost all of the raw descriptor handling: every descriptor type has a `*Wrapper` (`FileDescriptorProtoWrapper`, `ServiceDescriptorProtoWrapper`, `DescriptorProtoWrapper` for messages, etc.), every scalar field is exposed as a `SyntaxElement<T>` paired with its `SourceCodeInfo.Location`, and message/enum/file options that carry custom extensions can be queried with `findExtension(...)`.
Walk these wrappers from `CodeGeneratorRequestWrapper.filesToGenerate` when implementing the compiler.

## Adding a Compiler Option

1. Declare a property on `Options` with KDoc describing the semantics, the default, and any rendering-shape implication.
2. Add a matching `var` on `Options.Builder` initialized via `parameters.get<T>("name") ?: default`.
3. Pass it through `Builder.build()`.
4. Wire the new behavior into `Compiler` where rendering happens.
5. Add a new example suite isolating the option from its default value — see [CONTRIBUTING.md § Adding a New Example](CONTRIBUTING.md#adding-a-new-example).
   Every other option has its own suite; the matrix is exhaustive on purpose.
6. Update the alphabetized options table in [README.md](README.md) — the option name links to its KDoc line, so keep the `#L<number>` anchor accurate.

## Working Reminders

- After any change to a Kotlin source file, run `./gradlew ktlintFormat` before declaring the task done.
  CI runs `ktlintCheck` and will fail otherwise.
- When you add a plugin option, the README options table, the suite matrix in [`examples/build.gradle.kts`](examples/build.gradle.kts), and the per-suite `Dumper` subclass must all be updated together.
- When you add a new proto type or new Kotlin reflection usage, regenerate native-image metadata per the steps in [CONTRIBUTING.md](CONTRIBUTING.md#graalvm-native-image-and-reflection-metadata).
- Build markdown structurally via commonmark-java's `Node` types (`Heading`, `Paragraph`, `BulletList`, `ListItem`, `Text`, `Code`, …) rather than hand-formatting strings — the renderer handles escaping, list-item indentation, and reflow.
- Markdown files use one sentence per line — see [CONTRIBUTING.md](CONTRIBUTING.md#markdown-style).
