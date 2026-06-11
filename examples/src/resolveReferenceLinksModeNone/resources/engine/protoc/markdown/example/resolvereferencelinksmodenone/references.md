---
generated-by: https://github.com/hotelengine/protoc-gen-markdown/releases/tag/0.0.0-pre.0
protoc-gen-markdown-generated-on: 2026-01-01T00:00:00Z
protoc-gen-markdown-options: generateStableAnchors=true,generateInsertionPoints=true,minTableOfContentsHeader=2,maxTableOfContentsHeader=3,outputType=PER_FILE,includePackageIndices=true,typeSortMode=ALPHABETICAL,fileSortMode=ALPHABETICAL,rpcSortMode=ENCOUNTER,fieldSortMode=ENCOUNTER,enumValueSortMode=ENCOUNTER,resolveReferenceLinksMode=NONE
# @@protoc_insertion_point(frontmatter)
---

# <a id="engine_protoc_markdown_example_resolvereferencelinksmodenone_references_proto"></a>engine/protoc/markdown/example/resolvereferencelinksmodenone/references.proto

___

<details>
<summary>Table of contents</summary>

- [Messages](#engine_protoc_markdown_example_resolvereferencelinksmodenone_references_proto-Messages)
  
  - [Existing](#engine_protoc_markdown_example_resolvereferencelinksmodenone_references_proto-Messages-Existing)
  
  - [Referencer](#engine_protoc_markdown_example_resolvereferencelinksmodenone_references_proto-Messages-Referencer)

</details>

<!-- @@protoc_insertion_point(file_header_scope:engine/protoc/markdown/example/resolvereferencelinksmodenone/references.proto) -->

<!-- @@protoc_insertion_point(file_scope:engine/protoc/markdown/example/resolvereferencelinksmodenone/references.proto) -->

## <a id="engine_protoc_markdown_example_resolvereferencelinksmodenone_references_proto-Messages"></a>Messages

<!-- @@protoc_insertion_point(messages_section) -->

### <a id="engine_protoc_markdown_example_resolvereferencelinksmodenone_references_proto-Messages-Existing"></a>Existing

<!-- @@protoc_insertion_point(message_header_scope:engine.protoc.markdown.example.resolvereferencelinksmodenone.Existing) -->

A type that exists in scope so its name `[Existing]` would resolve under
`resolveReferenceLinksMode=WARN` or the default `FAIL_ON_INVALID`.

<!-- @@protoc_insertion_point(message_scope:engine.protoc.markdown.example.resolvereferencelinksmodenone.Existing) -->

#### <a id="engine_protoc_markdown_example_resolvereferencelinksmodenone_references_proto-Messages-Existing-Field_Summary"></a>Field Summary

|Name|Type|Description|
|---|---|---|
|label|string||

#### <a id="engine_protoc_markdown_example_resolvereferencelinksmodenone_references_proto-Messages-Existing-Field_Details"></a>Field Details

##### <a id="engine_protoc_markdown_example_resolvereferencelinksmodenone_references_proto-Messages-Existing-Field_Details-label"></a>label

### <a id="engine_protoc_markdown_example_resolvereferencelinksmodenone_references_proto-Messages-Referencer"></a>Referencer

<!-- @@protoc_insertion_point(message_header_scope:engine.protoc.markdown.example.resolvereferencelinksmodenone.Referencer) -->

Comments here contain shortcut-reference syntax that WOULD resolve under
`resolveReferenceLinksMode=WARN` or the default `FAIL_ON_INVALID`.  See \[Existing\]
for the type that would have linked, and \[Missing\] for the name that doesn't exist
in scope.  With this suite's `resolveReferenceLinksMode=NONE`, both stay literal
`[…]` text; compare against the `hello` suite fixture for the on-by-default
rendering.

<!-- @@protoc_insertion_point(message_scope:engine.protoc.markdown.example.resolvereferencelinksmodenone.Referencer) -->

#### <a id="engine_protoc_markdown_example_resolvereferencelinksmodenone_references_proto-Messages-Referencer-Field_Summary"></a>Field Summary

|Name|Type|Description|
|---|---|---|
|note|string||

#### <a id="engine_protoc_markdown_example_resolvereferencelinksmodenone_references_proto-Messages-Referencer-Field_Details"></a>Field Details

##### <a id="engine_protoc_markdown_example_resolvereferencelinksmodenone_references_proto-Messages-Referencer-Field_Details-note"></a>note

<!-- @@protoc_insertion_point(file_footer) -->
