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
 */
public data class PlannedDocument(
    val path: String,
    val title: String,
    val parentTitle: String?,
)
