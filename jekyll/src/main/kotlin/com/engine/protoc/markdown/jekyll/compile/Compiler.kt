package com.engine.protoc.markdown.jekyll.compile

import com.engine.protoc.markdown.ProtocGenMarkdown
import com.engine.protoc.markdown.jekyll.ProtocGenMarkdownJekyll
import com.engine.protoc.util.compiler.CodeGeneratorRequestWrapper
import com.google.protobuf.compiler.PluginProtos
import org.slf4j.LoggerFactory
import java.time.Clock

/**
 * Internal compiler for the Jekyll plugin.
 *
 * Reconstructs the document layout the core `protoc-gen-markdown` plugin emits — via
 * [ProtocGenMarkdown.plan], driven by the core [markdownOptions] parsed from the same parameter
 * string — and, for each planned document, emits a `CodeGeneratorResponse.File` targeting that
 * document's `frontmatter` insertion point.  The injected fragment carries a `title` (the
 * document's H1) and, where one applies, a `parent` (its natural parent's title, or
 * [ProtocGenMarkdownJekyll.Options.navigationParent] for the top tier) — the keys
 * [just-the-docs](https://just-the-docs.com) and compatible themes use to build a navigation tree.
 *
 * Extend this class — not [ProtocGenMarkdownJekyll] — when adding compiler features.
 */
internal class Compiler(
    private val request: CodeGeneratorRequestWrapper,
    private val options: ProtocGenMarkdownJekyll.Options,
    private val markdownOptions: ProtocGenMarkdown.Options,
    private val clock: Clock = Clock.systemUTC(),
) {

    internal fun compile(): PluginProtos.CodeGeneratorResponse {
        log.info("compile starting with options: {}", options)
        val plan = ProtocGenMarkdown(request, markdownOptions, clock).plan()

        // Depth (1-based) of each document within the generated tree, keyed by its title — the
        // same string that referring pages cite as their `parent`.  Used only to resolve
        // `visibleNavigationDepth`; computed once and shared across the loop below.
        val parentByTitle = plan.associate { it.title to it.parentTitle }
        val visibleDepth = options.visibleNavigationDepth
        if (visibleDepth != null) {
            val maxDepth = plan.maxOfOrNull { depthOf(it.title, parentByTitle) } ?: 0
            if (visibleDepth > maxDepth) {
                log.warn(
                    "visibleNavigationDepth={} exceeds the {} navigation level(s) in the generated tree; no pages are excluded",
                    visibleDepth,
                    maxDepth,
                )
            }
        }

        // `nav_order` lands on the single top-tier root — the one page (overview, or the lone
        // document of a SINGLE_FILE / single-package / single-file layout) that parents the whole
        // tree.  When the top tier has more than one root there is no single page to rank, so the
        // key is emitted nowhere and a WARN is logged.
        val navOrderRootPath =
            options.rootNavOrder?.let { navOrder ->
                val roots = plan.filter { it.parentTitle == null }
                roots.singleOrNull()?.path ?: run {
                    log.warn(
                        "rootNavOrder={} ignored: the generated tree has {} top-tier root pages, not a single one to rank",
                        navOrder,
                        roots.size,
                    )
                    null
                }
            }

        val response = PluginProtos.CodeGeneratorResponse.newBuilder()
        for (document in plan) {
            val parent = document.parentTitle ?: options.navigationParent
            val navExclude = visibleDepth != null && depthOf(document.title, parentByTitle) > visibleDepth
            val navOrder = options.rootNavOrder.takeIf { document.path == navOrderRootPath }
            log.debug(
                "frontmatter for {}: title={}, parent={}, navExclude={}, navOrder={}",
                document.path,
                document.title,
                parent,
                navExclude,
                navOrder,
            )
            response.addFile(
                PluginProtos.CodeGeneratorResponse.File.newBuilder()
                    .setName(document.path)
                    .setInsertionPoint(FRONTMATTER_INSERTION_POINT)
                    .setContent(frontmatterFragment(document.title, parent, navExclude, navOrder))
                    .build(),
            )
        }
        return response.build()
    }

    /**
     * The 1-based depth of the document titled [title] within the generated tree, found by
     * walking the `parentTitle` chain in [parentByTitle] up to a root (a `null` parent).  A
     * top-tier root is depth `1`, its children depth `2`, and so on.  The visited set guards
     * against a cyclic chain so a malformed plan cannot loop forever.
     */
    private fun depthOf(
        title: String,
        parentByTitle: Map<String, String?>,
    ): Int {
        var depth = 1
        val seen = mutableSetOf(title)
        var parent = parentByTitle[title]
        while (parent != null && seen.add(parent)) {
            depth++
            parent = parentByTitle[parent]
        }
        return depth
    }

    /**
     * The YAML lines spliced in immediately above the `# @@protoc_insertion_point(frontmatter)`
     * marker.  Values are double-quoted and escaped so a proto path, dotted package, or arbitrary
     * [ProtocGenMarkdownJekyll.Options.navigationParent] parses to the same string on both the
     * `title` side and any referring page's `parent` side.  When [navOrder] is non-null a
     * `nav_order: <navOrder>` line is added so just-the-docs ranks the page among its siblings (set
     * only on the single top-tier root, via [ProtocGenMarkdownJekyll.Options.rootNavOrder]).  When
     * [navExclude] is set a bare `nav_exclude: true` line is added so just-the-docs drops the page
     * from the sidebar; visible pages omit the key entirely, matching the theme's default of
     * `false`.  Ends with a newline so the marker stays on its own line after the splice.
     */
    private fun frontmatterFragment(
        title: String,
        parent: String?,
        navExclude: Boolean,
        navOrder: Int?,
    ): String =
        buildString {
            append("title: ").append(yaml(title)).append('\n')
            if (parent != null) append("parent: ").append(yaml(parent)).append('\n')
            if (navOrder != null) append("nav_order: ").append(navOrder).append('\n')
            if (navExclude) append("nav_exclude: true").append('\n')
        }

    /** Renders [value] as a double-quoted YAML scalar, escaping backslashes and double quotes. */
    private fun yaml(value: String): String = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

    private companion object {
        private val log = LoggerFactory.getLogger(Compiler::class.java)
        private const val FRONTMATTER_INSERTION_POINT = "frontmatter"
    }
}
