package com.engine.protoc.markdown.compile

import com.engine.protoc.markdown.ProtocGenMarkdown
import com.engine.protoc.markdown.ProtocGenMarkdown.Options.DocumentType
import com.engine.protoc.markdown.Version
import com.engine.protoc.util.compiler.CodeGeneratorRequestWrapper
import com.engine.protoc.util.compiler.CodeGeneratorResponseWrapper
import com.engine.protoc.util.enums.EnumDescriptorProtoWrapper
import com.engine.protoc.util.file.FileDescriptorProtoWrapper
import com.engine.protoc.util.message.DescriptorProtoWrapper
import com.engine.protoc.util.message.FieldDescriptorProtoWrapper
import com.engine.protoc.util.service.ServiceDescriptorProtoWrapper
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type
import com.google.protobuf.compiler.PluginProtos
import org.commonmark.node.BulletList
import org.commonmark.node.Code
import org.commonmark.node.Document
import org.commonmark.node.Heading
import org.commonmark.node.ListItem
import org.commonmark.node.Paragraph
import org.commonmark.node.Text
import org.commonmark.renderer.markdown.MarkdownRenderer

/**
 * Per compile invocation, emits one or more `.md` files based on
 * [ProtocGenMarkdown.Options.documentTypes] — see [DocumentType] for what each kind produces and
 * how its filename is formed.  Every document is built up as a CommonMark [Document] tree and
 * rendered via [MarkdownRenderer], so the output is guaranteed to be parseable by the same
 * library that produced it.
 *
 * Sections within each document:
 *  - top-level `# <title>` heading with a leading attribution paragraph;
 *  - one `## <enum-name>` section per top-level enum, with values as a bullet list;
 *  - one `## <message-name>` section per top-level message, with fields as a bullet list
 *    (`<type> <name>` per item; `repeated` is rendered with a trailing `[]`);
 *  - one `## <service-name>` section per service, with methods as a bullet list
 *    (`RpcName(Request) Response`, with a `stream` marker on either side for streaming RPCs).
 *
 * For [DocumentType.COMPLETE], each input proto becomes a `## <file-name>` section and its types
 * shift down to `### <type-name>` to keep the heading hierarchy stable.
 *
 * Nested messages, oneofs, maps, well-known-type suppression, deprecated styling, and per-message
 * focus documents are not yet handled — this is the initial skeleton; extend by adding rendering
 * helpers below and surfacing new options on [ProtocGenMarkdown.Options].
 */
internal class Compiler(
    private val request: CodeGeneratorRequestWrapper,
    private val options: ProtocGenMarkdown.Options,
) {

    private val renderer: MarkdownRenderer = MarkdownRenderer.builder().build()

    internal fun compile(): PluginProtos.CodeGeneratorResponse {
        val response = CodeGeneratorResponseWrapper()
        if (DocumentType.FILE_OVERVIEW in options.documentTypes) {
            for (file in scopeFiles) response.addFile(overviewFilename(file), render(overviewDocument(file)))
        }
        if (DocumentType.COMPLETE in options.documentTypes) {
            response.addFile(completeFilename(), render(completeDocument()))
        }
        return response.build()
    }

    // ===== Scope =================================================================================

    private val scopeFiles: List<FileDescriptorProtoWrapper> by lazy {
        val toGenerate = request.filesToGenerate.toSet()
        request.protoFiles.filter {
            it.name in toGenerate &&
                (it.messageTypes.isNotEmpty() || it.enumTypes.isNotEmpty() || it.services.isNotEmpty())
        }
    }

    // ===== Filenames =============================================================================

    private fun overviewFilename(file: FileDescriptorProtoWrapper): String = (file.name ?: "").removeSuffix(".proto") + ".md"

    private fun completeFilename(): String = (commonPackagePrefix().ifEmpty { "complete" }) + ".md"

    /** Longest dotted prefix shared by every scope file's package; `""` if none or no packages. */
    private fun commonPackagePrefix(): String {
        val parts = scopeFiles.mapNotNull { it.`package`?.value?.takeIf(String::isNotEmpty)?.split('.') }
        if (parts.isEmpty()) return ""
        val limit = parts.minOf { it.size }
        val keep = (0 until limit).takeWhile { i -> parts.all { it[i] == parts[0][i] } }
        return keep.joinToString(".") { parts[0][it] }
    }

    // ===== Documents ============================================================================

    private fun overviewDocument(file: FileDescriptorProtoWrapper): Document {
        val doc = newDocument(file.name ?: "(unnamed)")
        appendTypes(doc, file.enumTypes, file.messageTypes, file.services, HEADING_TYPE_AT_TOP)
        return doc
    }

    private fun completeDocument(): Document {
        val prefix = commonPackagePrefix()
        val title = if (prefix.isEmpty()) "Complete schema" else "Complete schema: $prefix"
        val doc = newDocument(title)
        for (file in scopeFiles) {
            doc.appendChild(headingOf(HEADING_FILE_IN_COMPLETE, file.name ?: "(unnamed)"))
            appendTypes(doc, file.enumTypes, file.messageTypes, file.services, HEADING_TYPE_IN_COMPLETE)
        }
        return doc
    }

    private fun newDocument(title: String): Document {
        val doc = Document()
        doc.appendChild(headingOf(HEADING_TOP, title))
        doc.appendChild(paragraphOf(attribution()))
        return doc
    }

    private fun appendTypes(
        doc: Document,
        enums: List<EnumDescriptorProtoWrapper>,
        messages: List<DescriptorProtoWrapper>,
        services: List<ServiceDescriptorProtoWrapper>,
        headingLevel: Int,
    ) {
        for (e in enums) appendEnumSection(doc, e, headingLevel)
        for (m in messages) {
            if (m.options?.mapEntry?.value == true) continue
            appendMessageSection(doc, m, headingLevel)
        }
        for (s in services) appendServiceSection(doc, s, headingLevel)
    }

    // ===== Type sections =========================================================================

    private fun appendEnumSection(
        doc: Document,
        enum: EnumDescriptorProtoWrapper,
        level: Int,
    ) {
        val name = enum.name?.value ?: "(unnamed enum)"
        doc.appendChild(headingOf(level, "enum $name"))
        val list = BulletList()
        for (v in enum.values) {
            val vname = v.name?.value ?: continue
            list.appendChild(textListItem(vname))
        }
        doc.appendChild(list)
    }

    private fun appendMessageSection(
        doc: Document,
        msg: DescriptorProtoWrapper,
        level: Int,
    ) {
        val name = msg.name?.value ?: "(unnamed message)"
        doc.appendChild(headingOf(level, "message $name"))
        val list = BulletList()
        for (field in msg.fields) list.appendChild(fieldListItem(msg, field))
        doc.appendChild(list)
    }

    private fun appendServiceSection(
        doc: Document,
        service: ServiceDescriptorProtoWrapper,
        level: Int,
    ) {
        val name = service.name?.value ?: "(unnamed service)"
        doc.appendChild(headingOf(level, "service $name"))
        val list = BulletList()
        for (m in service.methods) {
            val mname = m.name?.value ?: continue
            val input = typeLabel(m.inputType?.value).let { if (m.clientStreaming?.value == true) "stream $it" else it }
            val output = typeLabel(m.outputType?.value).let { if (m.serverStreaming?.value == true) "stream $it" else it }
            list.appendChild(codeListItem("$mname($input) $output"))
        }
        doc.appendChild(list)
    }

    // ===== Field rendering =======================================================================

    private fun fieldListItem(
        msg: DescriptorProtoWrapper,
        field: FieldDescriptorProtoWrapper,
    ): ListItem {
        val item = ListItem()
        val para = Paragraph()
        para.appendChild(Code(fieldTypeLabel(msg, field)))
        para.appendChild(Text(" " + (field.name?.value ?: "?")))
        item.appendChild(para)
        return item
    }

    /** In-list field-line type label: `Base[]` for repeated, plain `Base` otherwise. */
    private fun fieldTypeLabel(
        msg: DescriptorProtoWrapper,
        field: FieldDescriptorProtoWrapper,
    ): String {
        @Suppress("UNUSED_PARAMETER")
        val parent = msg // reserved for future map-entry resolution
        val base = scalarOrRefLabel(field)
        return if (field.label?.value == Label.LABEL_REPEATED) "$base[]" else base
    }

    private fun scalarOrRefLabel(field: FieldDescriptorProtoWrapper): String =
        when (val t = field.type?.value) {
            Type.TYPE_MESSAGE, Type.TYPE_ENUM, Type.TYPE_GROUP -> typeLabel(field.typeName?.value)
            null -> "?"
            else -> t.name.removePrefix("TYPE_").lowercase()
        }

    /** Drop the package qualifier from a protobuf FQN, leaving the leaf name. */
    private fun typeLabel(fqn: String?): String = fqn?.substringAfterLast('.') ?: "?"

    // ===== Node helpers =========================================================================

    private fun headingOf(
        level: Int,
        text: String,
    ): Heading =
        Heading().apply {
            this.level = level
            appendChild(Text(text))
        }

    private fun paragraphOf(text: String): Paragraph = Paragraph().apply { appendChild(Text(text)) }

    private fun textListItem(text: String): ListItem {
        val item = ListItem()
        item.appendChild(paragraphOf(text))
        return item
    }

    private fun codeListItem(text: String): ListItem {
        val item = ListItem()
        item.appendChild(Paragraph().apply { appendChild(Code(text)) })
        return item
    }

    // ===== Render ===============================================================================

    private fun attribution(): String = "Generated by protoc-gen-markdown ${Version.value} — $RELEASE_URL_PREFIX/${Version.value}"

    private fun render(doc: Document): String = renderer.render(doc)

    private companion object {
        const val HEADING_TOP = 1
        const val HEADING_TYPE_AT_TOP = 2
        const val HEADING_FILE_IN_COMPLETE = 2
        const val HEADING_TYPE_IN_COMPLETE = 3
        const val RELEASE_URL_PREFIX = "https://github.com/hotelengine/protoc-gen-markdown/releases/tag"
    }
}
