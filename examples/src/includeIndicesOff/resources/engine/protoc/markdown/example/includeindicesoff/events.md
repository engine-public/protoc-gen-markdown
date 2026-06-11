---
generated-by: https://github.com/hotelengine/protoc-gen-markdown/releases/tag/0.0.0-pre.0
protoc-gen-markdown-generated-on: 2026-01-01T00:00:00Z
protoc-gen-markdown-options: generateStableAnchors=true,generateInsertionPoints=true,minTableOfContentsHeader=2,maxTableOfContentsHeader=3,outputType=PER_FILE,includeIndices=false,typeSortMode=ALPHABETICAL,fileSortMode=ALPHABETICAL,rpcSortMode=ENCOUNTER,fieldSortMode=ENCOUNTER,enumValueSortMode=ENCOUNTER,resolveReferenceLinksMode=FAIL_ON_INVALID
# @@protoc_insertion_point(frontmatter)
---

# <a id="engine_protoc_markdown_example_includeindicesoff_events_proto"></a>engine/protoc/markdown/example/includeindicesoff/events.proto

___

<details>
<summary>Table of contents</summary>

- [Messages](#engine_protoc_markdown_example_includeindicesoff_events_proto-Messages)
  
  - [Event](#engine_protoc_markdown_example_includeindicesoff_events_proto-Messages-Event)

- [Enums](#engine_protoc_markdown_example_includeindicesoff_events_proto-Enums)
  
  - [EventKind](#engine_protoc_markdown_example_includeindicesoff_events_proto-Enums-EventKind)

</details>

<!-- @@protoc_insertion_point(file_header_scope:engine/protoc/markdown/example/includeindicesoff/events.proto) -->

<!-- @@protoc_insertion_point(file_scope:engine/protoc/markdown/example/includeindicesoff/events.proto) -->

## <a id="engine_protoc_markdown_example_includeindicesoff_events_proto-Messages"></a>Messages

<!-- @@protoc_insertion_point(messages_section) -->

### <a id="engine_protoc_markdown_example_includeindicesoff_events_proto-Messages-Event"></a>Event

<!-- @@protoc_insertion_point(message_header_scope:engine.protoc.markdown.example.includeindicesoff.Event) -->

One audit-log entry.

<!-- @@protoc_insertion_point(message_scope:engine.protoc.markdown.example.includeindicesoff.Event) -->

#### <a id="engine_protoc_markdown_example_includeindicesoff_events_proto-Messages-Event-Field_Summary"></a>Field Summary

|Name|Type|Description|
|---|---|---|
|subject|[User](core.md#engine_protoc_markdown_example_includeindicesoff_core_proto-Messages-User)|The user the event is about.|
|kind|[EventKind](events.md#engine_protoc_markdown_example_includeindicesoff_events_proto-Enums-EventKind)|What the user did.|

#### <a id="engine_protoc_markdown_example_includeindicesoff_events_proto-Messages-Event-Field_Details"></a>Field Details

##### <a id="engine_protoc_markdown_example_includeindicesoff_events_proto-Messages-Event-Field_Details-subject"></a>subject

##### <a id="engine_protoc_markdown_example_includeindicesoff_events_proto-Messages-Event-Field_Details-kind"></a>kind

___

## <a id="engine_protoc_markdown_example_includeindicesoff_events_proto-Enums"></a>Enums

<!-- @@protoc_insertion_point(enums_section) -->

### <a id="engine_protoc_markdown_example_includeindicesoff_events_proto-Enums-EventKind"></a>EventKind

<!-- @@protoc_insertion_point(enum_header_scope:engine.protoc.markdown.example.includeindicesoff.EventKind) -->

The kind of action recorded by an Event.

<!-- @@protoc_insertion_point(enum_scope:engine.protoc.markdown.example.includeindicesoff.EventKind) -->

#### <a id="engine_protoc_markdown_example_includeindicesoff_events_proto-Enums-EventKind-Value_Summary"></a>Value Summary

|Name|Number|Description|
|---|---|---|
|EVENT\_KIND\_UNSPECIFIED|0||
|EVENT\_KIND\_CREATED|1||
|EVENT\_KIND\_UPDATED|2||
|EVENT\_KIND\_DELETED|3||

#### <a id="engine_protoc_markdown_example_includeindicesoff_events_proto-Enums-EventKind-Value_Details"></a>Value Details

##### <a id="engine_protoc_markdown_example_includeindicesoff_events_proto-Enums-EventKind-Value_Details-EVENT_KIND_UNSPECIFIED"></a>EVENT\_KIND\_UNSPECIFIED

##### <a id="engine_protoc_markdown_example_includeindicesoff_events_proto-Enums-EventKind-Value_Details-EVENT_KIND_CREATED"></a>EVENT\_KIND\_CREATED

##### <a id="engine_protoc_markdown_example_includeindicesoff_events_proto-Enums-EventKind-Value_Details-EVENT_KIND_UPDATED"></a>EVENT\_KIND\_UPDATED

##### <a id="engine_protoc_markdown_example_includeindicesoff_events_proto-Enums-EventKind-Value_Details-EVENT_KIND_DELETED"></a>EVENT\_KIND\_DELETED

<!-- @@protoc_insertion_point(file_footer) -->
