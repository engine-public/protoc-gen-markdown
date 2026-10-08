package com.engine.protoc.markdown.compile

import com.engine.protoc.markdown.ProtocGenMarkdown
import com.engine.protoc.util.file.FileDescriptorProtoWrapper
import com.engine.protoc.util.markdown.ReferenceElement
import com.engine.protoc.util.markdown.ReferenceIndex
import com.engine.protoc.util.markdown.ReferenceLinkMode
import com.engine.protoc.util.markdown.ReferenceLinkProcessor
import com.engine.protoc.util.markdown.ReferenceRendering
import com.engine.protoc.util.markdown.UnresolvedReferenceRendering

/**
 * Builds the reference-link processor for one output document.
 *
 * Every type, field, enum value, service, and RPC in [scopeFiles] and [peerFiles] is indexed under
 * the heading anchor this generator emits for it; [hrefFor] turns a heading path into an href
 * relative to the document being rendered.  Scope files use this run's [consolidated] layout;
 * peer files are assumed to be rendered by a sibling PER_FILE run, so their heading paths drop the
 * per-file layer, matching what `Compiler.appendTypeReference` does for transitive field types.
 * Elements of files with no output group are left unindexed.
 *
 * Unresolved references stay as literal `[label]` text so failures are visible in the output.
 */
internal fun referenceLinkProcessor(
    scopeFiles: List<FileDescriptorProtoWrapper>,
    peerFiles: List<FileDescriptorProtoWrapper>,
    consolidated: Boolean,
    fileToGroup: Map<FileDescriptorProtoWrapper, Compiler.OutputGroup>,
    peerFileToGroup: Map<FileDescriptorProtoWrapper, Compiler.OutputGroup>,
    mode: ProtocGenMarkdown.Options.ResolveReferenceLinksMode,
    overrides: Map<String, String>,
    hrefFor: (FileDescriptorProtoWrapper, List<String>) -> String,
): ReferenceLinkProcessor<String> {
    val peerSet = peerFiles.toSet()
    val index = ReferenceIndex(scopeFiles + peerFiles) { element ->
        val file = element.file
        val group = (if (file in peerSet) peerFileToGroup[file] else fileToGroup[file]) ?: return@ReferenceIndex null
        val fileConsolidated = consolidated && file !in peerSet
        val sectionBase: (String) -> List<String> = { section ->
            if (fileConsolidated) listOf(group.title, file.name ?: "(unnamed)", section) else listOf(group.title, section)
        }
        hrefFor(file, headingPath(element, sectionBase))
    }
    return ReferenceLinkProcessor(
        index = index,
        mode = mode.shared,
        overrides = overrides,
        unresolvedRendering = UnresolvedReferenceRendering.LITERAL,
    ) { ReferenceRendering.Link(it) }
}

/** The heading path of [element]'s section in the rendered document. */
private fun headingPath(
    element: ReferenceElement,
    sectionBase: (String) -> List<String>,
): List<String> =
    when (element.kind) {
        ReferenceElement.Kind.MESSAGE -> sectionBase("Messages") + element.localName
        ReferenceElement.Kind.ENUM -> sectionBase("Enums") + element.localName
        ReferenceElement.Kind.SERVICE -> sectionBase("Services") + element.localName
        ReferenceElement.Kind.FIELD -> headingPath(element.parent!!, sectionBase) + "Field Details" + element.simpleName
        ReferenceElement.Kind.ENUM_VALUE -> headingPath(element.parent!!, sectionBase) + "Value Details" + element.simpleName
        ReferenceElement.Kind.RPC -> headingPath(element.parent!!, sectionBase) + "RPC Details" + element.simpleName
    }

internal val ProtocGenMarkdown.Options.ResolveReferenceLinksMode.shared: ReferenceLinkMode
    get() = when (this) {
        ProtocGenMarkdown.Options.ResolveReferenceLinksMode.NONE -> ReferenceLinkMode.NONE
        ProtocGenMarkdown.Options.ResolveReferenceLinksMode.WARN -> ReferenceLinkMode.WARN
        ProtocGenMarkdown.Options.ResolveReferenceLinksMode.FAIL_ON_INVALID -> ReferenceLinkMode.FAIL_ON_INVALID
    }
