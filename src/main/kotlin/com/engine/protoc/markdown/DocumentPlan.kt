package com.engine.protoc.markdown

/**
 * One document the compiler will emit, described independently of its rendered Markdown body.
 *
 * Produced by [ProtocGenMarkdown.plan] so a sibling tool (notably the `protoc-gen-markdown-jekyll`
 * plugin) can reconstruct the exact set of output files, their paths, and the natural parent → child
 * hierarchy between them without re-deriving the layout rules.  The layout is the same one
 * [ProtocGenMarkdown.compile] renders, so a plan computed from the same request and [Options]
 * lines up one-to-one with the emitted `.md` files.
 *
 * @property path the output-relative path the document lands at (e.g. `foo/bar/baz.md`), identical
 *   to the name the rendered file is emitted under.
 * @property title the document's H1 title — the same string rendered as its top heading.
 * @property parentTitle the [title] of this document's natural parent within the generated set, or
 *   `null` when the document is a top-tier root (the overview, or — when no overview exists — the
 *   highest tier that does).  Children of a tier that does not exist collapse onto the nearest
 *   ancestor that does.
 * @property kind which of the three document roles this page fills.  Lets a sibling tool treat the
 *   overview, the package-index pages, and the content pages differently without re-deriving the
 *   layout rules — [parentTitle] alone cannot tell them apart (e.g. the overview and a
 *   [Kind.CONTENT] root both report `parentTitle == null`).
 */
public data class PlannedDocument(
    val path: String,
    val title: String,
    val parentTitle: String?,
    val kind: Kind,
) {
    /**
     * The role a [PlannedDocument] fills within the generated set, matching the core compiler's
     * three internal buckets.
     */
    public enum class Kind {
        /** The single overview / landing page that sits above every other generated document. */
        OVERVIEW,

        /**
         * A package-index page — one per distinct package, emitted only under
         * [ProtocGenMarkdown.Options.OutputType.PER_FILE] with indices enabled.
         */
        PACKAGE_INDEX,

        /**
         * A content page carrying the rendered type documentation: one per input file under
         * [ProtocGenMarkdown.Options.OutputType.PER_FILE], one per package under
         * [ProtocGenMarkdown.Options.OutputType.PER_PACKAGE], or the lone document under
         * [ProtocGenMarkdown.Options.OutputType.SINGLE_FILE].
         */
        CONTENT,
    }
}
