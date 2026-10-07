import java.util.function.Predicate

plugins {
    application
    alias(libs.plugins.graalvm.native)
    alias(libs.plugins.osdetector)
}

description = "protoc compiler that injects Jekyll navigation keys into protoc-gen-markdown frontmatter"

dependencies {
    // The Jekyll plugin reuses the core plugin's option parsing and its render-free
    // ProtocGenMarkdown.plan() to reconstruct the exact document layout (paths, titles,
    // parentage) the core plugin emits, so the navigation frontmatter it injects lines up
    // one-to-one with those documents.
    implementation(projects.protocGenMarkdown)
    implementation(libs.engine.protoc.utils)
    implementation(libs.protobuf.java)

    // Compiler code calls SLF4J 2.x (LoggerFactory.getLogger, log.warn(...))
    // directly, so slf4j-api sits on the compile classpath. Ship Log4j 2 as
    // the binding so the plugin's logLevel / logFile options can be applied
    // programmatically via the Configurator API: log4j-core is used directly
    // in applyLoggingConfiguration() (ConfigurationBuilder, Configurator,
    // ConsoleAppender.Target), and log4j-slf4j2-impl is the runtime bridge
    // from SLF4J 2.x to log4j-core. log4j-core 2.25.0+ ships its own GraalVM
    // native-image reachability metadata, so no hand-rolled reflect/resource
    // config is required for the core appender path.
    implementation(libs.slf4j.api)
    implementation(libs.log4j.api)
    implementation(libs.log4j.core)
    runtimeOnly(libs.log4j.slf4j2.impl)

    // GraalVM hosted API used by native-image Feature classes under
    // com.engine.protoc.markdown.jekyll.nativeimage. Compile-only — the
    // org.graalvm.nativeimage module is provided by the GraalVM JDK at
    // native-image build time and is not shipped to consumers.
    compileOnly(libs.graalvm.sdk)
}

application {
    mainClass.set("com.engine.protoc.markdown.jekyll.MainKt")
}

graalvmNative {
    toolchainDetection = false
    binaries {
        named("main") {
            // GraalVM auto-appends .exe on Windows; everywhere else we add it
            // explicitly so every published native artifact ends in .exe (the
            // io.grpc:protoc-gen-grpc-java convention).
            val exeSuffix = if (osdetector.os == "windows") "" else ".exe"
            imageName = "${project.name}-${osdetector.os}-${osdetector.arch}$exeSuffix"
            mainClass = application.mainClass
            sharedLibrary = false
            resources.autodetect()
            fallback = false
        }
        all {
            verbose = true
            javaLauncher.set(javaToolchains.launcherFor {
                languageVersion.set(JavaLanguageVersion.of(21))
                vendor.set(JvmVendorSpec.GRAAL_VM)
            })
            buildArgs.add("-H:+UnlockExperimentalVMOptions")
            buildArgs.add("-H:ThrowMissingRegistrationErrors=")
            // protobuf-java RuntimeVersion.<clinit> hits String.format → CLDR bundle lookup
            // for the runtime default locale, which can be anything on a user machine.
            buildArgs.add("-H:+IncludeAllLocales")
        }
    }
    agent {
        enabled = true
        /*
         * Scope the native-image-agent to the `run` task only.  By default the plugin attaches
         * the agent as a JVMTI agent to every JavaExec/Test task, which conflicts with the IDE
         * debugger's own JVMTI agent and aborts test runs with `JVMTI_ERROR_NOT_AVAILABLE`.
         * `metadataCopy` only consumes output from `run` anyway, so instrumenting other tasks
         * provides no benefit.
         */
        tasksToInstrumentPredicate.set(Predicate<Task> { it.name == "run" })
        metadataCopy {
            inputTaskNames.add("run")
            outputDirectories.add("src/main/resources/META-INF/native-image/com.engine/protoc-gen-markdown-jekyll")
            mergeWithExisting = true
        }
    }
    metadataRepository {
        enabled = true
        version = "0.3.24"
    }
}

/*
 * Per-platform native binaries are published to Maven Central as classified
 * artifacts on a POM-only artifact (no main jar, mirroring io.grpc:protoc-gen-grpc-java).
 * Every binary uses the .exe extension regardless of host OS, so the artifact
 * coordinates can be resolved with `:<classifier>@exe` on every platform.
 */
val classifiedNativeArtifacts = listOf(
    "linux-x86_64",
    "linux-aarch_64",
    "osx-aarch_64",
    "windows-x86_64",
)

val nativeBinariesDir: Provider<File> = providers
    .environmentVariable("ENGINE_NATIVE_BIN_DIR")
    .map { rootProject.layout.projectDirectory.dir(it).asFile }
    .orElse(layout.buildDirectory.dir("native/nativeCompile").map { it.asFile })

publishing {
    publications {
        create<MavenPublication>("maven") {
            // intentionally no `from(components["java"])` — the plugin ships
            // only the classified native binaries below, and the main pom is
            // <packaging>pom</packaging>.
            artifact(layout.buildDirectory.file("reports/cyclonedx-direct/bom.json")) {
                classifier = "cyclonedx"
                extension = "json"
                builtBy(tasks.named("cyclonedxDirectBom"))
            }
            pom {
                name.set(project.name)
                packaging = "pom"
                inceptionYear.set("2026")
                licenses {
                    license {
                        name.set("Apache-2.0")
                        url.set("https://github.com/engine-public/protoc-gen-markdown/blob/${version}/LICENSE")
                    }
                }
                developers {
                    developer {
                        organizationUrl.set("https://github.com/engine-public")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/engine-public/protoc-gen-markdown.git")
                    developerConnection.set("scm:git:https://github.com/engine-public/protoc-gen-markdown.git")
                    url.set("https://github.com/engine-public/protoc-gen-markdown")
                }
            }
        }
    }
}

afterEvaluate {
    val pub = publishing.publications.getByName<MavenPublication>("maven")
    pub.pom {
        description.set(project.description)
        url.set("https://github.com/engine-public/protoc-gen-markdown/blob/${version}/README.md")
    }

    val binDir = nativeBinariesDir.get()
    val localClassifier = "${osdetector.os}-${osdetector.arch}"
    val nativeCompileTask = tasks.named("nativeCompile")
    val localBinary = layout.buildDirectory.file(
        "native/nativeCompile/${project.name}-$localClassifier.exe",
    )

    classifiedNativeArtifacts.forEach { classifier ->
        // CI release staging produces version-tagged file names; prefer that
        // form when present so all classifiers attach to the publication.
        val stagedFile = binDir.resolve("${project.name}-${project.version}-$classifier.exe")
        when {
            stagedFile.exists() -> pub.artifact(stagedFile) {
                this.classifier = classifier
                this.extension = "exe"
            }
            classifier == localClassifier -> pub.artifact(localBinary) {
                this.classifier = classifier
                this.extension = "exe"
                // Build the binary on demand so publishToMavenLocal triggers
                // nativeCompile automatically.
                builtBy(nativeCompileTask)
            }
            else -> logger.info(
                "No native binary for classifier '{}'; skipping artifact.",
                classifier,
            )
        }
    }
}

gradle.taskGraph.whenReady {
    gradle.taskGraph.allTasks.forEach {
        if (project.hasProperty("codeql")) {
            if (it.name.startsWith("nativeCompile")) {
                logger.quiet("Disabling ${it.path} due to codeql run.")
                it.enabled = false
            }
        }
    }
}

val versionResourceDir = layout.buildDirectory.dir("generated/resources/version")

val writeVersionResource =
    tasks.register("writeVersionResource") {
        group = "build"
        description = "Writes project.version to a namespaced classpath resource."

        val versionString = project.version.toString()
        val outDir = versionResourceDir

        inputs.property("version", versionString)
        outputs.dir(outDir)

        doLast {
            val file = outDir.get().file("META-INF/com.engine.protoc.markdown.jekyll/version").asFile
            file.parentFile.mkdirs()
            file.writeText(versionString)
        }
    }

sourceSets.named("main") {
    resources.srcDir(writeVersionResource.map { versionResourceDir })
}
