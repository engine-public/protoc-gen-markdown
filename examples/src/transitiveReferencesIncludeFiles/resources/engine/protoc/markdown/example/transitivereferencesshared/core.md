---
generated-by: https://github.com/hotelengine/protoc-gen-markdown/releases/tag/0.0.0-pre.0
protoc-gen-markdown-generated-on: 2026-01-01T00:00:00Z
protoc-gen-markdown-options: generateStableAnchors=true,generateInsertionPoints=true,minTableOfContentsHeader=2,maxTableOfContentsHeader=3,outputType=PER_FILE,includeIndices=true,typeSortMode=ALPHABETICAL,fileSortMode=ALPHABETICAL,rpcSortMode=ENCOUNTER,fieldSortMode=ENCOUNTER,enumValueSortMode=ENCOUNTER,resolveReferenceLinksMode=FAIL_ON_INVALID
# @@protoc_insertion_point(frontmatter)
---

# <a id="engine_protoc_markdown_example_transitivereferencesshared_core_proto"></a>engine/protoc/markdown/example/transitivereferencesshared/core.proto

___

<details>
<summary>Table of contents</summary>

- [Messages](#engine_protoc_markdown_example_transitivereferencesshared_core_proto-Messages)
  
  - [CoreEntity](#engine_protoc_markdown_example_transitivereferencesshared_core_proto-Messages-CoreEntity)

- [Enums](#engine_protoc_markdown_example_transitivereferencesshared_core_proto-Enums)
  
  - [CoreStatus](#engine_protoc_markdown_example_transitivereferencesshared_core_proto-Enums-CoreStatus)

</details>

<!-- @@protoc_insertion_point(file_header_scope:engine/protoc/markdown/example/transitivereferencesshared/core.proto) -->

<!-- @@protoc_insertion_point(file_scope:engine/protoc/markdown/example/transitivereferencesshared/core.proto) -->

## <a id="engine_protoc_markdown_example_transitivereferencesshared_core_proto-Messages"></a>Messages

<!-- @@protoc_insertion_point(messages_section) -->

### <a id="engine_protoc_markdown_example_transitivereferencesshared_core_proto-Messages-CoreEntity"></a>CoreEntity

<!-- @@protoc_insertion_point(message_header_scope:engine.protoc.markdown.example.transitivereferencesshared.CoreEntity) -->

A type that lives in the transitive-only dependency.  None of the three
transitiveReferences-example suites place this file in `filesToGenerate`; it is
added to protoc's `--proto_path` via an `addIncludeDir` call in `examples/build.gradle.kts`,
so it surfaces in the request as a transitive dep that consumer.proto imports.

<!-- @@protoc_insertion_point(message_scope:engine.protoc.markdown.example.transitivereferencesshared.CoreEntity) -->

#### <a id="engine_protoc_markdown_example_transitivereferencesshared_core_proto-Messages-CoreEntity-Field_Summary"></a>Field Summary

|Name|Type|Description|
|---|---|---|
|name|string|A label assigned by the originating service.|
|description|string|Free-form description.|

#### <a id="engine_protoc_markdown_example_transitivereferencesshared_core_proto-Messages-CoreEntity-Field_Details"></a>Field Details

##### <a id="engine_protoc_markdown_example_transitivereferencesshared_core_proto-Messages-CoreEntity-Field_Details-name"></a>name

##### <a id="engine_protoc_markdown_example_transitivereferencesshared_core_proto-Messages-CoreEntity-Field_Details-description"></a>description

___

## <a id="engine_protoc_markdown_example_transitivereferencesshared_core_proto-Enums"></a>Enums

<!-- @@protoc_insertion_point(enums_section) -->

### <a id="engine_protoc_markdown_example_transitivereferencesshared_core_proto-Enums-CoreStatus"></a>CoreStatus

<!-- @@protoc_insertion_point(enum_header_scope:engine.protoc.markdown.example.transitivereferencesshared.CoreStatus) -->

Lifecycle state of a [CoreEntity](core.md#engine_protoc_markdown_example_transitivereferencesshared_core_proto-Messages-CoreEntity).

<!-- @@protoc_insertion_point(enum_scope:engine.protoc.markdown.example.transitivereferencesshared.CoreStatus) -->

#### <a id="engine_protoc_markdown_example_transitivereferencesshared_core_proto-Enums-CoreStatus-Value_Summary"></a>Value Summary

|Name|Number|Description|
|---|---|---|
|CORE\_STATUS\_UNSPECIFIED|0||
|CORE\_STATUS\_ACTIVE|1||
|CORE\_STATUS\_ARCHIVED|2||

#### <a id="engine_protoc_markdown_example_transitivereferencesshared_core_proto-Enums-CoreStatus-Value_Details"></a>Value Details

##### <a id="engine_protoc_markdown_example_transitivereferencesshared_core_proto-Enums-CoreStatus-Value_Details-CORE_STATUS_UNSPECIFIED"></a>CORE\_STATUS\_UNSPECIFIED

##### <a id="engine_protoc_markdown_example_transitivereferencesshared_core_proto-Enums-CoreStatus-Value_Details-CORE_STATUS_ACTIVE"></a>CORE\_STATUS\_ACTIVE

##### <a id="engine_protoc_markdown_example_transitivereferencesshared_core_proto-Enums-CoreStatus-Value_Details-CORE_STATUS_ARCHIVED"></a>CORE\_STATUS\_ARCHIVED

<!-- @@protoc_insertion_point(file_footer) -->
