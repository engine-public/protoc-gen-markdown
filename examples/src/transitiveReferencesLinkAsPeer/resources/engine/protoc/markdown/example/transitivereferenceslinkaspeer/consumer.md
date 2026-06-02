---
generated-by: https://github.com/hotelengine/protoc-gen-markdown/releases/tag/0.0.0-pre.0
protoc-gen-markdown-generated-on: 2026-01-01T00:00:00Z
protoc-gen-markdown-options: generateStableAnchors=true,generateInsertionPoints=true,minTableOfContentsHeader=2,maxTableOfContentsHeader=3,outputType=PER_FILE,includeIndices=true,typeSortMode=ALPHABETICAL,fileSortMode=ALPHABETICAL,rpcSortMode=ENCOUNTER,fieldSortMode=ENCOUNTER,enumValueSortMode=ENCOUNTER,resolveReferenceLinksMode=FAIL_ON_INVALID
# @@protoc_insertion_point(frontmatter)
---

# <a id="engine_protoc_markdown_example_transitivereferenceslinkaspeer_consumer_proto"></a>engine/protoc/markdown/example/transitivereferenceslinkaspeer/consumer.proto

___

<details>
<summary>Table of contents</summary>

- [Messages](#engine_protoc_markdown_example_transitivereferenceslinkaspeer_consumer_proto-Messages)
  
  - [Consumer](#engine_protoc_markdown_example_transitivereferenceslinkaspeer_consumer_proto-Messages-Consumer)

</details>

<!-- @@protoc_insertion_point(file_header_scope:engine/protoc/markdown/example/transitivereferenceslinkaspeer/consumer.proto) -->

<!-- @@protoc_insertion_point(file_scope:engine/protoc/markdown/example/transitivereferenceslinkaspeer/consumer.proto) -->

## <a id="engine_protoc_markdown_example_transitivereferenceslinkaspeer_consumer_proto-Messages"></a>Messages

<!-- @@protoc_insertion_point(messages_section) -->

### <a id="engine_protoc_markdown_example_transitivereferenceslinkaspeer_consumer_proto-Messages-Consumer"></a>Consumer

<!-- @@protoc_insertion_point(message_header_scope:engine.protoc.markdown.example.transitivereferenceslinkaspeer.Consumer) -->

Exercises transitiveReferences=LINK\_AS\_PEER (the default) — a reference to [CoreEntity](../transitivereferencesshared/core.md#File__engine_protoc_markdown_example_transitivereferencesshared_core_proto-Messages-CoreEntity),
which lives in the transitive-only `core.proto`, is linked to where the peer `.md`
would land if a sibling protoc run rendered `core.proto` with the same options.  No
peer `.md` is emitted by this suite, so the link target won't exist in this suite's
own output set — but the link shape is the one a parallel render would land on.

<!-- @@protoc_insertion_point(message_scope:engine.protoc.markdown.example.transitivereferenceslinkaspeer.Consumer) -->

#### <a id="engine_protoc_markdown_example_transitivereferenceslinkaspeer_consumer_proto-Messages-Consumer-Field_Summary"></a>Field Summary

|Name|Type|Description|
|---|---|---|
|entity|[CoreEntity](../transitivereferencesshared/core.md#File__engine_protoc_markdown_example_transitivereferencesshared_core_proto-Messages-CoreEntity)|The entity this consumer wraps; see [CoreEntity](../transitivereferencesshared/core.md#File__engine_protoc_markdown_example_transitivereferencesshared_core_proto-Messages-CoreEntity).|
|status|[CoreStatus](../transitivereferencesshared/core.md#File__engine_protoc_markdown_example_transitivereferencesshared_core_proto-Enums-CoreStatus)|Lifecycle status; see [CoreStatus](../transitivereferencesshared/core.md#File__engine_protoc_markdown_example_transitivereferencesshared_core_proto-Enums-CoreStatus).|

#### <a id="engine_protoc_markdown_example_transitivereferenceslinkaspeer_consumer_proto-Messages-Consumer-Field_Details"></a>Field Details

##### <a id="engine_protoc_markdown_example_transitivereferenceslinkaspeer_consumer_proto-Messages-Consumer-Field_Details-entity"></a>entity

##### <a id="engine_protoc_markdown_example_transitivereferenceslinkaspeer_consumer_proto-Messages-Consumer-Field_Details-status"></a>status

<!-- @@protoc_insertion_point(file_footer) -->
