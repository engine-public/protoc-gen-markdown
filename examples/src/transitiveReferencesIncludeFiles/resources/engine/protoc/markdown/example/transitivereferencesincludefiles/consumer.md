---
generated-by: https://github.com/engine-public/protoc-gen-markdown/releases/tag/0.0.0-pre.0
protoc-gen-markdown-generated-on: 2026-01-01T00:00:00Z
protoc-gen-markdown-options: generateStableAnchors=true,generateInsertionPoints=true,minTableOfContentsHeader=2,maxTableOfContentsHeader=3,outputType=PER_FILE,includeIndices=true,typeSortMode=ALPHABETICAL,fileSortMode=ALPHABETICAL,rpcSortMode=ENCOUNTER,fieldSortMode=ENCOUNTER,enumValueSortMode=ENCOUNTER,resolveReferenceLinksMode=FAIL_ON_INVALID
# @@protoc_insertion_point(frontmatter)
---

# <a id="engine_protoc_markdown_example_transitivereferencesincludefiles_consumer_proto"></a>engine/protoc/markdown/example/transitivereferencesincludefiles/consumer.proto

___

<details>
<summary>Table of contents</summary>

- [Messages](#engine_protoc_markdown_example_transitivereferencesincludefiles_consumer_proto-Messages)
  
  - [Consumer](#engine_protoc_markdown_example_transitivereferencesincludefiles_consumer_proto-Messages-Consumer)

</details>

<!-- @@protoc_insertion_point(file_header_scope:engine/protoc/markdown/example/transitivereferencesincludefiles/consumer.proto) -->

<!-- @@protoc_insertion_point(file_scope:engine/protoc/markdown/example/transitivereferencesincludefiles/consumer.proto) -->

## <a id="engine_protoc_markdown_example_transitivereferencesincludefiles_consumer_proto-Messages"></a>Messages

<!-- @@protoc_insertion_point(messages_section) -->

### <a id="engine_protoc_markdown_example_transitivereferencesincludefiles_consumer_proto-Messages-Consumer"></a>Consumer

<!-- @@protoc_insertion_point(message_header_scope:engine.protoc.markdown.example.transitivereferencesincludefiles.Consumer) -->

Exercises transitiveReferences=INCLUDE\_FILES — references to [CoreEntity](../transitivereferencesshared/core.md#engine_protoc_markdown_example_transitivereferencesshared_core_proto-Messages-CoreEntity) and
[CoreStatus](../transitivereferencesshared/core.md#engine_protoc_markdown_example_transitivereferencesshared_core_proto-Enums-CoreStatus) resolve to the transitive `core.proto`, and that file is promoted into the
generated output set, so `core.md` is emitted alongside this suite's `consumer.md` and
every link resolves to actual content this run wrote.

<!-- @@protoc_insertion_point(message_scope:engine.protoc.markdown.example.transitivereferencesincludefiles.Consumer) -->

#### <a id="engine_protoc_markdown_example_transitivereferencesincludefiles_consumer_proto-Messages-Consumer-Field_Summary"></a>Field Summary

|Name|Type|Description|
|---|---|---|
|entity|[CoreEntity](../transitivereferencesshared/core.md#engine_protoc_markdown_example_transitivereferencesshared_core_proto-Messages-CoreEntity)|The entity this consumer wraps; see [CoreEntity](../transitivereferencesshared/core.md#engine_protoc_markdown_example_transitivereferencesshared_core_proto-Messages-CoreEntity). [...](#engine_protoc_markdown_example_transitivereferencesincludefiles_consumer_proto-Messages-Consumer-Field_Details-entity)|
|status|[CoreStatus](../transitivereferencesshared/core.md#engine_protoc_markdown_example_transitivereferencesshared_core_proto-Enums-CoreStatus)|Lifecycle status; see [CoreStatus](../transitivereferencesshared/core.md#engine_protoc_markdown_example_transitivereferencesshared_core_proto-Enums-CoreStatus). [...](#engine_protoc_markdown_example_transitivereferencesincludefiles_consumer_proto-Messages-Consumer-Field_Details-status)|

#### <a id="engine_protoc_markdown_example_transitivereferencesincludefiles_consumer_proto-Messages-Consumer-Field_Details"></a>Field Details

##### <a id="engine_protoc_markdown_example_transitivereferencesincludefiles_consumer_proto-Messages-Consumer-Field_Details-entity"></a>entity

The entity this consumer wraps; see [CoreEntity](../transitivereferencesshared/core.md#engine_protoc_markdown_example_transitivereferencesshared_core_proto-Messages-CoreEntity).

##### <a id="engine_protoc_markdown_example_transitivereferencesincludefiles_consumer_proto-Messages-Consumer-Field_Details-status"></a>status

Lifecycle status; see [CoreStatus](../transitivereferencesshared/core.md#engine_protoc_markdown_example_transitivereferencesshared_core_proto-Enums-CoreStatus).

<!-- @@protoc_insertion_point(file_footer) -->
