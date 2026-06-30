---
generated-by: https://github.com/engine-public/protoc-gen-markdown/releases/tag/0.0.0-pre.0
protoc-gen-markdown-generated-on: 2026-01-01T00:00:00Z
protoc-gen-markdown-options: generateStableAnchors=true,generateInsertionPoints=true,minTableOfContentsHeader=2,maxTableOfContentsHeader=3,outputType=PER_FILE,includeIndices=true,typeSortMode=ALPHABETICAL,fileSortMode=ALPHABETICAL,rpcSortMode=ENCOUNTER,fieldSortMode=ENCOUNTER,enumValueSortMode=ENCOUNTER,resolveReferenceLinksMode=WARN
# @@protoc_insertion_point(frontmatter)
---

# <a id="engine_protoc_markdown_example_resolvereferencelinksmodewarn_references_proto"></a>engine/protoc/markdown/example/resolvereferencelinksmodewarn/references.proto

___

<details>
<summary>Table of contents</summary>

- [Messages](#engine_protoc_markdown_example_resolvereferencelinksmodewarn_references_proto-Messages)
  
  - [ReferencedType](#engine_protoc_markdown_example_resolvereferencelinksmodewarn_references_proto-Messages-ReferencedType)
  
  - [Wrapper](#engine_protoc_markdown_example_resolvereferencelinksmodewarn_references_proto-Messages-Wrapper)

</details>

<!-- @@protoc_insertion_point(file_header_scope:engine/protoc/markdown/example/resolvereferencelinksmodewarn/references.proto) -->

<!-- @@protoc_insertion_point(file_scope:engine/protoc/markdown/example/resolvereferencelinksmodewarn/references.proto) -->

## <a id="engine_protoc_markdown_example_resolvereferencelinksmodewarn_references_proto-Messages"></a>Messages

<!-- @@protoc_insertion_point(messages_section) -->

### <a id="engine_protoc_markdown_example_resolvereferencelinksmodewarn_references_proto-Messages-ReferencedType"></a>ReferencedType

<!-- @@protoc_insertion_point(message_header_scope:engine.protoc.markdown.example.resolvereferencelinksmodewarn.ReferencedType) -->

Exercises resolveReferenceLinksMode=WARN — a shortcut reference that doesn't resolve
(`[Missing]` below) is left literal and a warning is logged, but the compile still
succeeds.  Under the default FAIL\_ON\_INVALID the same content renders identically but
`CodeGeneratorResponse.error` would be set so protoc fails the run.

<!-- @@protoc_insertion_point(message_scope:engine.protoc.markdown.example.resolvereferencelinksmodewarn.ReferencedType) -->

#### <a id="engine_protoc_markdown_example_resolvereferencelinksmodewarn_references_proto-Messages-ReferencedType-Field_Summary"></a>Field Summary

|Name|Type|Description|
|---|---|---|
|label|string||

#### <a id="engine_protoc_markdown_example_resolvereferencelinksmodewarn_references_proto-Messages-ReferencedType-Field_Details"></a>Field Details

##### <a id="engine_protoc_markdown_example_resolvereferencelinksmodewarn_references_proto-Messages-ReferencedType-Field_Details-label"></a>label

### <a id="engine_protoc_markdown_example_resolvereferencelinksmodewarn_references_proto-Messages-Wrapper"></a>Wrapper

<!-- @@protoc_insertion_point(message_header_scope:engine.protoc.markdown.example.resolvereferencelinksmodewarn.Wrapper) -->

See [ReferencedType](references.md#engine_protoc_markdown_example_resolvereferencelinksmodewarn_references_proto-Messages-ReferencedType) for the shape of the entry this message wraps; also mentions
\[Missing\] which does NOT exist in scope and stays literal under WARN.

<!-- @@protoc_insertion_point(message_scope:engine.protoc.markdown.example.resolvereferencelinksmodewarn.Wrapper) -->

#### <a id="engine_protoc_markdown_example_resolvereferencelinksmodewarn_references_proto-Messages-Wrapper-Field_Summary"></a>Field Summary

|Name|Type|Description|
|---|---|---|
|inner|[ReferencedType](references.md#engine_protoc_markdown_example_resolvereferencelinksmodewarn_references_proto-Messages-ReferencedType)|The wrapped [ReferencedType](references.md#engine_protoc_markdown_example_resolvereferencelinksmodewarn_references_proto-Messages-ReferencedType) instance. [...](#engine_protoc_markdown_example_resolvereferencelinksmodewarn_references_proto-Messages-Wrapper-Field_Details-inner)|

#### <a id="engine_protoc_markdown_example_resolvereferencelinksmodewarn_references_proto-Messages-Wrapper-Field_Details"></a>Field Details

##### <a id="engine_protoc_markdown_example_resolvereferencelinksmodewarn_references_proto-Messages-Wrapper-Field_Details-inner"></a>inner

The wrapped [ReferencedType](references.md#engine_protoc_markdown_example_resolvereferencelinksmodewarn_references_proto-Messages-ReferencedType) instance.

<!-- @@protoc_insertion_point(file_footer) -->
