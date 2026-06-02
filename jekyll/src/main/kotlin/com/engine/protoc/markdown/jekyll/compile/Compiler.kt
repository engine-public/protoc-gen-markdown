package com.engine.protoc.markdown.jekyll.compile

import com.engine.protoc.markdown.PlannedDocument
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
        // The set of titles other documents cite as their `parent` — i.e. the titles that have
        // children in the generated tree.  Used below to flag the deepest still-visible page whose
        // children `visibleNavigationDepth` hid, so it can declare `has_children: false`.
        val titlesWithChildren = plan.mapNotNullTo(mutableSetOf()) { it.parentTitle }
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

        // `rootDocumentTitle` retitles the single top-tier root — the one page that parents the
        // whole tree — to a human-friendly label.  Captured as the root's path, its original
        // title, and the replacement: the path picks out the page whose own `title` is replaced,
        // and the original title is what every child cites as its `parent`, so those references are
        // rewritten to the new title below and the tree stays connected.  When the top tier has more
        // than one root there is no single page to retitle, so nothing is applied and a WARN is
        // logged.
        val rootRetitle: RootRetitle? =
            options.rootDocumentTitle?.let { newTitle ->
                val roots = plan.filter { it.parentTitle == null }
                roots.singleOrNull()?.let { RootRetitle(it.path, it.title, newTitle) } ?: run {
                    log.warn(
                        "rootDocumentTitle={} ignored: the generated tree has {} top-tier root pages, not a single one to retitle",
                        newTitle,
                        roots.size,
                    )
                    null
                }
            }

        val response = PluginProtos.CodeGeneratorResponse.newBuilder()
        response.setSupportedFeatures(Long.MAX_VALUE)
        for (document in plan) {
            // The page's own title — replaced by `rootDocumentTitle` on the single retitled root.
            val title = if (rootRetitle != null && document.path == rootRetitle.path) rootRetitle.newTitle else document.title
            // A child that cited the retitled root as its `parent` follows the rename, so the
            // just-the-docs tree stays linked; `navigationParent` still backfills a root's own
            // missing `parent`.
            val resolvedParentTitle =
                if (rootRetitle != null && document.parentTitle == rootRetitle.originalTitle) {
                    rootRetitle.newTitle
                } else {
                    document.parentTitle
                }
            val parent = resolvedParentTitle ?: options.navigationParent
            val navExclude = visibleDepth != null && depthOf(document.title, parentByTitle) > visibleDepth
            // The deepest still-visible page (depth == visibleDepth) whose children the cap hid: it
            // has children, yet they all sit one level deeper and are `nav_exclude`d.  Declaring
            // `has_children: false` keeps just-the-docs from rendering an expander or on-page list for
            // those hidden pages.  A visible page can only have hidden children at exactly this depth,
            // since every child sits one level below its parent.
            val hasChildrenFalse =
                visibleDepth != null &&
                    document.title in titlesWithChildren &&
                    depthOf(document.title, parentByTitle) == visibleDepth
            val navOrder = options.rootNavOrder.takeIf { document.path == navOrderRootPath }
            val layout = layoutFor(document.kind)
            log.debug(
                "frontmatter for {}: title={}, parent={}, navExclude={}, hasChildrenFalse={}, navOrder={}, layout={}",
                document.path,
                title,
                parent,
                navExclude,
                hasChildrenFalse,
                navOrder,
                layout,
            )
            response.addFile(
                PluginProtos.CodeGeneratorResponse.File.newBuilder()
                    .setName(document.path)
                    .setInsertionPoint(FRONTMATTER_INSERTION_POINT)
                    .setContent(frontmatterFragment(title, parent, navExclude, hasChildrenFalse, navOrder, layout))
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
     * The Jekyll `layout` configured for a document of [kind], or `null` when the matching option
     * is unset.  Each document role draws from its own option —
     * [ProtocGenMarkdownJekyll.Options.overviewLayout],
     * [ProtocGenMarkdownJekyll.Options.packageIndexLayout], and
     * [ProtocGenMarkdownJekyll.Options.contentLayout] — so the overview, package-index, and content
     * tiers can carry distinct layouts (or none).
     */
    private fun layoutFor(kind: PlannedDocument.Kind): String? =
        when (kind) {
            PlannedDocument.Kind.OVERVIEW -> options.overviewLayout
            PlannedDocument.Kind.PACKAGE_INDEX -> options.packageIndexLayout
            PlannedDocument.Kind.CONTENT -> options.contentLayout
        }

    /**
     * The YAML lines spliced in immediately above the `# @@protoc_insertion_point(frontmatter)`
     * marker.  Values are double-quoted and escaped so a proto path, dotted package, or arbitrary
     * [ProtocGenMarkdownJekyll.Options.navigationParent] parses to the same string on both the
     * `title` side and any referring page's `parent` side.  When [layout] is non-null a
     * `layout: <layout>` line leads the fragment, selecting the theme layout Jekyll wraps the page
     * in (set per document role via the `*Layout` options).  When [navOrder] is non-null a
     * `nav_order: <navOrder>` line is added so just-the-docs ranks the page among its siblings (set
     * only on the single top-tier root, via [ProtocGenMarkdownJekyll.Options.rootNavOrder]).  When
     * [hasChildrenFalse] is set a bare `has_children: false` line is added — emitted only on the
     * deepest still-visible page whose children [ProtocGenMarkdownJekyll.Options.visibleNavigationDepth]
     * hid, so just-the-docs renders no expander or on-page child list for those hidden pages; every
     * other page omits the key.  When [navExclude] is set a bare `nav_exclude: true` line is added so
     * just-the-docs drops the page from the sidebar; visible pages omit the key entirely, matching the
     * theme's default of `false`.  Ends with a newline so the marker stays on its own line after the
     * splice.
     */
    private fun frontmatterFragment(
        title: String,
        parent: String?,
        navExclude: Boolean,
        hasChildrenFalse: Boolean,
        navOrder: Int?,
        layout: String?,
    ): String =
        buildString {
            if (layout != null) append("layout: ").append(yaml(layout)).append('\n')
            append("title: ").append(yaml(title)).append('\n')
            if (parent != null) append("parent: ").append(yaml(parent)).append('\n')
            if (hasChildrenFalse) append("has_children: false").append('\n')
            if (navOrder != null) append("nav_order: ").append(navOrder).append('\n')
            if (navExclude) append("nav_exclude: true").append('\n')
        }

    /** Renders [value] as a double-quoted YAML scalar, escaping backslashes and double quotes. */
    private fun yaml(value: String): String = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

    /**
     * The single top-tier root document [ProtocGenMarkdownJekyll.Options.rootDocumentTitle]
     * retitles: [path] selects the page whose own `title` becomes [newTitle], and [originalTitle]
     * is the H1 every child cites as its `parent` — rewritten to [newTitle] so the navigation tree
     * stays connected.
     */
    private data class RootRetitle(
        val path: String,
        val originalTitle: String,
        val newTitle: String,
    )

    private companion object {
        private val log = LoggerFactory.getLogger(Compiler::class.java)
        private const val FRONTMATTER_INSERTION_POINT = "frontmatter"
    }
}
