package com.engine.protoc.markdown

import com.engine.protoc.markdown.compile.Compiler
import com.engine.protoc.util.compiler.CodeGeneratorRequestWrapper
import com.engine.protoc.util.compiler.Parameters
import com.engine.protoc.util.extensions.wrap
import com.google.protobuf.ExtensionRegistry
import com.google.protobuf.compiler.PluginProtos
import java.io.InputStream

public class ProtocGenMarkdown(
    private val request: CodeGeneratorRequestWrapper,
    private val options: Options,
) {

    /**
     * Options that influence the compiler plugin.
     *
     * Add new options as properties here, then wire each one through [Builder] so it can be parsed
     * from the `--markdown_out=key=value,…:outdir` parameter string.
     */
    public class Options private constructor(
        /**
         * Which output files the compiler emits per invocation.  Default is every [DocumentType].
         * See each enum value for what file(s) it produces and how each is named.  Restrict via the
         * protoc parameter string (`--markdown_out=documentTypes=COMPLETE:...`) or the DSL block
         * (`documentTypes = listOf(DocumentType.FILE_OVERVIEW)`).  Empty list = no output (useful
         * only in tests).
         */
        public val documentTypes: List<DocumentType>,
    ) {

        /**
         * The document-kind taxonomy.  Add a new value here, then handle it in
         * [com.engine.protoc.markdown.compile.Compiler.compile].
         */
        public enum class DocumentType {
            /**
             * One file per input `.proto`.  Contains a CommonMark document with a top-level heading
             * for the file, a section per message (each field listed under a sub-heading), a
             * section per enum, and a section per service.  Filename = the proto's relative path
             * with `.proto` swapped for `.md`.
             */
            FILE_OVERVIEW,

            /**
             * One file aggregating the entire compile scope.  Contains every top-level enum,
             * message, and service across all input protos.  Filename = the largest common
             * dotted-package prefix of the input protos + `.md`; falls back to `complete.md` if the
             * inputs share no package prefix.
             */
            COMPLETE,
        }

        public class Builder private constructor(parameters: Parameters) {

            public var documentTypes: List<DocumentType> =
                parameters.get<List<DocumentType>>("documentTypes") ?: DocumentType.entries.toList()

            public companion object {
                public fun from(parameters: Parameters): Builder = Builder(parameters)
            }

            public fun build(): Options =
                Options(
                    documentTypes = documentTypes,
                )
        }
    }

    public companion object {
        public fun from(
            input: InputStream,
            registry: ExtensionRegistry = ExtensionRegistry.newInstance(),
            block: Options.Builder.() -> Unit = {},
        ): ProtocGenMarkdown {
            val cgreq = PluginProtos.CodeGeneratorRequest.parseFrom(input, registry).wrap()
            return ProtocGenMarkdown(
                cgreq,
                Options.Builder.from(cgreq.parameters).apply(block).build(),
            )
        }
    }

    public fun compile(): PluginProtos.CodeGeneratorResponse = Compiler(request, options).compile()
}
