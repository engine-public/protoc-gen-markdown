@file:OptIn(ExperimentalTime::class)

import org.gradle.internal.extensions.stdlib.capitalized
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

plugins {
    alias(libs.plugins.protobuf)
    `java-test-fixtures`
}

/*
 * Per-suite recorder options.  The shared `Dumper` in `src/testFixtures/kotlin/` always
 * compiles the recorded `CodeGeneratorRequest` at whatever options were baked into it by
 * the recorder plugin below, so this map IS the test matrix: one entry per suite, each
 * one isolating a single option from its default.  The `hello` suite passes no options
 * so its fixtures act as a defaults baseline; every other suite flips exactly one knob.
 *
 * Suite name doubles as the proto source directory under `src/<suite>/proto/` and the
 * fixture sink under `src/<suite>/resources/`.
 */
val suiteRecorderOptions =
    mapOf(
        "hello" to emptyList<String>(),
        "generateStableAnchorsOff" to listOf("generateStableAnchors=false"),
        "generateInsertionPointsOff" to listOf("generateInsertionPoints=false"),
        "tableOfContentsHeadersWide" to listOf("minTableOfContentsHeader=1", "maxTableOfContentsHeader=5"),
        "outputTypePerPackage" to listOf("outputType=PER_PACKAGE", "maxTableOfContentsHeader=4"),
        "includeIndicesOff" to listOf("includeIndices=false"),
        "outputTypeSingleFile" to listOf("outputType=SINGLE_FILE", "maxTableOfContentsHeader=4"),
        "typeSortModeEncounter" to listOf("typeSortMode=ENCOUNTER"),
        "fileSortModeEncounter" to listOf("fileSortMode=ENCOUNTER", "outputType=PER_PACKAGE"),
        "rpcSortModeAlphabetical" to listOf("rpcSortMode=ALPHABETICAL"),
        "fieldSortModeAlphabetical" to listOf("fieldSortMode=ALPHABETICAL"),
        "fieldSortModeNumber" to listOf("fieldSortMode=NUMBER"),
        "enumValueSortModeAlphabetical" to listOf("enumValueSortMode=ALPHABETICAL"),
        "enumValueSortModeNumber" to listOf("enumValueSortMode=NUMBER"),
        "resolveReferenceLinksModeNone" to listOf("resolveReferenceLinksMode=NONE"),
        "resolveReferenceLinksModeWarn" to listOf("resolveReferenceLinksMode=WARN"),
        "transitiveReferencesNone" to listOf("transitiveReferences=NONE"),
        "transitiveReferencesLinkAsPeer" to listOf("transitiveReferences=LINK_AS_PEER"),
        "transitiveReferencesIncludeFiles" to listOf("transitiveReferences=INCLUDE_FILES"),
        "referenceLink" to listOf(
            "referenceLink=ExternalSpec=/docs/external-spec",
            "referenceLink=Wrapper=/docs/override-wrapper",
            "referenceLink=engine.protoc.markdown.example.transitivereferencesshared.CoreEntity=example.com/docs/core-entity",
        ),
        /*
         * Default options.  Exercises three reference-link syntax paths that the resolver
         * recognizes alongside the canonical `[label]` shortcut:
         *  - the full reference form `[display text][label]`, where the bracketed label is
         *    the lookup key and the display text passes through unchanged;
         *  - CommonMark escaped brackets (`\[...\]`), which the parser treats as literal
         *    text and the resolver never sees;
         *  - field-scope bare-name resolution against the field's declared target type, so
         *    `[TargetType]` in a comment on a field of that type resolves locally even when
         *    another type with the same short name exists in the global compile scope.
         *
         * One `referenceLink` override exists so the full-form label `[google.rpc.Status]`
         * can resolve from a comment without `google.rpc.*` being in the compile scope —
         * matching what real-world `referenceLink` consumers do for external proto types.
         */
        "referenceLinkSyntax" to listOf(
            "referenceLink=google.rpc.Status=example.com/docs/google.rpc.Status",
        ),
        /*
         * Jekyll-plugin suites.  Dumped via `JekyllDumper` (not `Dumper`), so their fixtures are
         * the `*.frontmatter` fragments `protoc-gen-markdown-jekyll` splices into each document's
         * `frontmatter` insertion point — not full `.md` documents.  `jekyll` is the defaults
         * baseline (no `navigationParent`, so the top-tier overview gets no `parent`);
         * `jekyllNavigationParent` flips the one knob this plugin adds,
         * `jekyllVisibleNavigationDepth` caps the visible nav tree at the top tier so the
         * package and per-file pages pick up `nav_exclude: true`, and `jekyllRootNavOrder`
         * sets `nav_order` on the single top-tier root page only.
         */
        "jekyll" to emptyList(),
        "jekyllNavigationParent" to listOf("navigationParent=Reference"),
        "jekyllVisibleNavigationDepth" to listOf("visibleNavigationDepth=1"),
        "jekyllRootNavOrder" to listOf("rootNavOrder=3"),
    )

/*
 * Source directory holding the transitive-only `core.proto` that the three
 * `transitiveReferences*` suites import without listing in `filesToGenerate`.  Wired into each
 * suite's `generate<Suite>Proto` via `addIncludeDir(...)` below so protoc resolves the
 * import without compiling the file itself.
 */
val transitiveReferencesSharedProto = layout.projectDirectory.dir("src/transitiveReferencesShared/proto")

dependencies {
    testFixturesImplementation(projects.protocGenMarkdown)
    // JekyllDumper runs the Jekyll plugin; the dependency reaches each Jekyll suite's runtime
    // classpath transitively through its `implementation(testFixtures(project()))`.
    testFixturesImplementation(projects.protocGenMarkdownJekyll)
    testFixturesImplementation(libs.protobuf.java)
    testFixturesImplementation(libs.bundles.test.kotest)
}

testing {
    suites {
        /*
         * Each test suite is its own protoc compilation run, so the recorder produces a
         * distinct CodeGeneratorRequest per suite.  The shared block below wires the binpb
         * output of `generate<Suite>Proto` into `process<Suite>Resources`, making the
         * recorded CGR available on each suite's test classpath as `/code-generator-request.binpb`.
         */
        withType<JvmTestSuite> {
            val testSuiteName = this.name

            dependencies {
                implementation(projects.protocGenMarkdown)
                /*
                 * The root project's deps are `implementation` scope, so protobuf-java
                 * isn't on consumers' compile classpath despite leaking through the
                 * public API of ProtocGenMarkdown (compile() returns PluginProtos.CodeGeneratorResponse).
                 * Pull it in explicitly per suite.
                 */
                implementation(libs.protobuf.java)
                /*
                 * Pulls in the abstract `Dumper` base from testFixtures.  Each suite ships
                 * a tiny concrete subclass under `src/<suite>/kotlin/` so Kotest discovers
                 * the spec and runs the dump once per suite.
                 */
                implementation(testFixtures(project()))
            }

            tasks.named("process${testSuiteName.capitalized()}Resources", ProcessResources::class) {
                dependsOn("generate${testSuiteName.capitalized()}Proto")
                from(
                    project.layout.buildDirectory
                        .dir("generated/sources/proto/$testSuiteName/recorder")
                        .map { it.file("code-generator-request.binpb") },
                )
            }

            /*
             * The Dumper test writes regenerated reference .md files into the suite's
             * resources directory.  Pass the absolute path as a system property so the dumper
             * doesn't have to guess at the test task's working directory.
             */
            targets.all {
                testTask.configure {
                    systemProperty(
                        "dumpDir",
                        layout.projectDirectory.dir("src/$testSuiteName/resources").asFile.absolutePath,
                    )
                }
            }

            tasks.named("check") {
                dependsOn(this@withType)
            }
        }

        /*
         * One JvmTestSuite per entry in `suiteRecorderOptions`.  Adding a new example is
         * just a map entry plus the matching `src/<suite>/proto/` and Dumper subclass.
         */
        suiteRecorderOptions.keys.forEach { suiteName -> register<JvmTestSuite>(suiteName) }
    }
}

protobuf {
    protoc {
        artifact = libs.tools.protoc.compiler.get().toString()
    }
    plugins {
        create("recorder") {
            artifact = libs.tools.protoc.recorder.get().toString()
        }
    }
    generateProtoTasks {
        all().all {
            val suiteName = this.sourceSet.name
            val opts = suiteRecorderOptions[suiteName] ?: return@all
            /*
             * Matches every per-suite generateProto task.  The `main` and `testFixtures`
             * source sets aren't in `suiteRecorderOptions`, so they short-circuit above.
             */
            if (name == "generate${suiteName.capitalized()}Proto") {
                if ((suiteName.startsWith("transitiveReferences") && suiteName != "transitiveReferencesShared") ||
                    suiteName == "referenceLink"
                ) {
                    /*
                     * Add the shared transitive `core.proto` to protoc's include path
                     * without listing it in `filesToGenerate` — so it surfaces in the
                     * recorded `CodeGeneratorRequest.protoFiles` as a transitive dep but
                     * the plugin under test never sees it in `filesToGenerate`.
                     *
                     * Disable the built-in Java codegen here: the consumer protos
                     * reference types from the transitive `core.proto`, but the
                     * resulting Java sources for `consumer.proto` would reference
                     * Java classes that the suite never compiles (since `core.proto`
                     * isn't in `filesToGenerate`).  The recorder still runs and
                     * captures the CGR — which is the only output these suites
                     * actually need.
                     */
                    addIncludeDir(files(transitiveReferencesSharedProto))
                    builtins.removeIf { it.name == "java" }
                }
                plugins {
                    create("recorder") {
                        option("logLevel=TRACE")
                        option("logFile=${project.layout.buildDirectory.dir("logs/${Clock.System.now().epochSeconds}").map { it.file("${suiteName}.txt") }.get().asFile.absolutePath}")
                        opts.forEach { option(it) }
                    }
                }
            }
        }
    }
}
