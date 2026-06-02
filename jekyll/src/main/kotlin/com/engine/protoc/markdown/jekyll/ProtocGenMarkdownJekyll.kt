package com.engine.protoc.markdown.jekyll

import com.engine.protoc.markdown.ProtocGenMarkdown
import com.engine.protoc.markdown.jekyll.compile.Compiler
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

public class ProtocGenMarkdownJekyll(
    private val request: CodeGeneratorRequestWrapper,
    private val options: Options,
    private val markdownOptions: ProtocGenMarkdown.Options,
    private val clock: Clock = Clock.systemUTC(),
) {

    /**
     * Options that influence the compiler plugin.
     *
     * Add new options as properties here, then wire each one through [Builder] so it can be parsed
     * from the `--markdown_jekyll_out=key=value,…:outdir` parameter string.
     */
    public data class Options(
        /**
         * Threshold at which the plugin emits log records via SLF4J.  Accepts any value of
         * [org.slf4j.event.Level] (`TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR`); a record is
         * emitted when its level is greater than or equal to this threshold.  Defaults to
         * `WARN` so the plugin is quiet by default but still surfaces warning- and error-level
         * reports.
         *
         * The option is realised at runtime by programmatically reconfiguring the Log4j 2
         * `Configuration` after [Options] is built, so it controls every logger the plugin
         * (and its dependencies) creates.
         *
         * Passed via `--markdown_jekyll_out=logLevel=DEBUG:outdir` (case-insensitive).
         */
        public val logLevel: Level,
        /**
         * Optional path to a file that receives timestamped log records in addition to the
         * stderr console output.  The stderr `Console` appender is always attached — its lines
         * are prefixed with `[protoc-gen-markdown-jekyll]` so they stand out from other compiler
         * output protoc may multiplex on the same stream.  When this option is set, a `File`
         * appender is *also* attached at the given path with a `%d{HH:mm:ss.SSS}`-prefixed
         * pattern.
         *
         * Passed via `--markdown_jekyll_out=logFile=/tmp/protoc.log:outdir`.
         */
        public val logFile: String?,
        /**
         * Optional `parent` assigned to the top-most tier of generated documents — the overview
         * page when one exists, otherwise whichever tier is highest (the package pages, or the
         * per-file pages when neither overview nor package pages exist).  When set, every top-tier
         * document receives `parent: <navigationParent>` in its frontmatter, slotting the whole
         * generated tree beneath a hand-authored Jekyll page of that title.  Defaults to `null`,
         * leaving the top tier with no `parent` (a set of Jekyll top-level pages).
         *
         * Documents below the top tier always parent onto their natural ancestor (per-file pages
         * onto their package page, package pages onto the overview); this option only re-parents
         * the roots.
         *
         * Passed via `--markdown_jekyll_out=navigationParent=Reference:outdir`.
         */
        public val navigationParent: String?,
        /**
         * Optional cap on how many levels of the generated navigation tree stay visible in the
         * Jekyll sidebar.  Every document whose depth exceeds this value receives
         * `nav_exclude: true` in its frontmatter — just-the-docs drops it from the navigation
         * while leaving the page itself reachable.  Depth is 1-based over the generated tree
         * only (the top-most generated tier is level 1, its children level 2, and so on); a
         * hand-authored [navigationParent] page does *not* count as a level.
         *
         * `0` excludes every generated page, `1` keeps only the top-most tier, `N` keeps tiers
         * `1..N`.  When the value exceeds the number of levels the generated tree actually has,
         * the plugin logs a `WARN` record and excludes nothing.  Defaults to `null`, which leaves
         * the whole tree visible (no `nav_exclude` keys emitted) — note `0` is a meaningful value,
         * so absence rather than a sentinel signals "disabled".
         *
         * Passed via `--markdown_jekyll_out=visibleNavigationDepth=1:outdir`.
         */
        public val visibleNavigationDepth: Int?,
        /**
         * Optional `nav_order` assigned to the single top-tier root page — the one document that
         * parents the whole generated tree.  That is the overview page when one exists, and
         * otherwise whatever lone page forms the top tier: the single document under
         * [ProtocGenMarkdown.Options.OutputType.SINGLE_FILE], the one package document under
         * [ProtocGenMarkdown.Options.OutputType.PER_PACKAGE] when the compile scope holds a single
         * package and no overview, or the one per-file document under
         * [ProtocGenMarkdown.Options.OutputType.PER_FILE] when it holds a single file and no
         * overview.  When set, that page receives `nav_order: <rootNavOrder>` in its
         * frontmatter, letting just-the-docs and compatible themes rank the whole reference tree
         * among its sibling top-level pages by an explicit order rather than the theme's default
         * title-alphabetical one.  Every other generated document omits the key.
         *
         * Applies only when the top tier is a single page.  When the generated tree has more than
         * one top-tier root (e.g. several per-file or per-package documents with no overview) there
         * is no single page to rank: the key is emitted nowhere and a `WARN` is logged.  Defaults to
         * `null`, leaving the root with no `nav_order` key.
         *
         * Passed via `--markdown_jekyll_out=rootNavOrder=1:outdir`.
         */
        public val rootNavOrder: Int?,
    ) {

        public class Builder private constructor(parameters: Parameters) {

            public var logLevel: Level = parameters.get<Level>("logLevel") ?: Level.WARN

            public var logFile: String? = parameters.get<String>("logFile")

            public var navigationParent: String? = parameters.get<String>("navigationParent")

            public var visibleNavigationDepth: Int? = parameters.get<Int>("visibleNavigationDepth")

            public var rootNavOrder: Int? = parameters.get<Int>("rootNavOrder")

            public companion object {
                public fun from(parameters: Parameters): Builder = Builder(parameters)
            }

            public fun build(): Options =
                Options(
                    logLevel = logLevel,
                    logFile = logFile,
                    navigationParent = navigationParent,
                    visibleNavigationDepth = visibleNavigationDepth,
                    rootNavOrder = rootNavOrder,
                )
        }
    }

    public companion object {
        public fun from(
            input: InputStream,
            registry: ExtensionRegistry = ExtensionRegistry.newInstance(),
            clock: Clock = Clock.systemUTC(),
            block: Options.Builder.() -> Unit = {},
        ): ProtocGenMarkdownJekyll {
            val cgreq = PluginProtos.CodeGeneratorRequest.parseFrom(input, registry).wrap()
            val options = Options.Builder.from(cgreq.parameters).apply(block).build()
            // Parse the core plugin's layout-affecting options (outputType, includeIndices,
            // transitiveReferences, …) from the *same* parameter string, so the document layout
            // reconstructed below matches the one the core plugin emitted.  Callers must pass the
            // core layout options to `--markdown_jekyll_out` as well as `--markdown_out`.
            val markdownOptions = ProtocGenMarkdown.Options.Builder.from(cgreq.parameters).build()
            applyLoggingConfiguration(options)
            return ProtocGenMarkdownJekyll(cgreq, options, markdownOptions, clock)
        }

        /**
         * Reconfigures the Log4j 2 `Configuration` from [Options.logLevel] and [Options.logFile].
         *
         * Appenders are attached to the `com.engine` logger only, so downstream dependencies'
         * loggers stay silent regardless of their own level.  A stderr `Console` appender is
         * always attached, prefixed with `[protoc-gen-markdown-jekyll]` so its records stand out
         * from other compiler output protoc may multiplex on the same stream.  When
         * [Options.logFile] is non-null a `File` appender is also attached, writing timestamped
         * records to the given path.  The root logger is silenced with `Level.OFF` to discard
         * anything emitted outside the `com.engine` tree.  Invoked from [from] immediately after
         * [Options] is built so subsequent `LoggerFactory.getLogger` calls observe the resolved
         * configuration.
         */
        private fun applyLoggingConfiguration(options: Options) {
            val cb = ConfigurationBuilderFactory.newConfigurationBuilder()
            cb.setStatusLevel(Log4jLevel.OFF)

            cb.add(
                cb.newAppender("stderr", "Console")
                    .addAttribute("target", ConsoleAppender.Target.SYSTEM_ERR)
                    .add(
                        cb.newLayout("PatternLayout")
                            .addAttribute("pattern", "[protoc-gen-markdown-jekyll] %-5level %logger{36} - %msg%n"),
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

    public fun compile(): PluginProtos.CodeGeneratorResponse = Compiler(request, options, markdownOptions, clock).compile()
}
