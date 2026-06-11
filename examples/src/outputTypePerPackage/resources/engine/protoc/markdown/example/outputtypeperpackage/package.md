---
generated-by: https://github.com/hotelengine/protoc-gen-markdown/releases/tag/0.0.0-pre.0
protoc-gen-markdown-generated-on: 2026-01-01T00:00:00Z
protoc-gen-markdown-options: generateStableAnchors=true,generateInsertionPoints=true,minTableOfContentsHeader=2,maxTableOfContentsHeader=4,outputType=PER_PACKAGE,includeIndices=true,typeSortMode=ALPHABETICAL,fileSortMode=ALPHABETICAL,rpcSortMode=ENCOUNTER,fieldSortMode=ENCOUNTER,enumValueSortMode=ENCOUNTER,resolveReferenceLinksMode=FAIL_ON_INVALID
# @@protoc_insertion_point(frontmatter)
---

# <a id="engine_protoc_markdown_example_outputtypeperpackage"></a>engine.protoc.markdown.example.outputtypeperpackage

<!-- @@protoc_insertion_point(file_header) -->

___

<details>
<summary>Table of contents</summary>

- [engine/protoc/markdown/example/outputtypeperpackage/core.proto](#engine_protoc_markdown_example_outputtypeperpackage-engine_protoc_markdown_example_outputtypeperpackage_core_proto)
  
  - [Services](#engine_protoc_markdown_example_outputtypeperpackage-engine_protoc_markdown_example_outputtypeperpackage_core_proto-Services)
    
    - [UserService](#engine_protoc_markdown_example_outputtypeperpackage-engine_protoc_markdown_example_outputtypeperpackage_core_proto-Services-UserService)
  
  - [Messages](#engine_protoc_markdown_example_outputtypeperpackage-engine_protoc_markdown_example_outputtypeperpackage_core_proto-Messages)
    
    - [GetUserRequest](#engine_protoc_markdown_example_outputtypeperpackage-engine_protoc_markdown_example_outputtypeperpackage_core_proto-Messages-GetUserRequest)
    
    - [User](#engine_protoc_markdown_example_outputtypeperpackage-engine_protoc_markdown_example_outputtypeperpackage_core_proto-Messages-User)
  
  - [Enums](#engine_protoc_markdown_example_outputtypeperpackage-engine_protoc_markdown_example_outputtypeperpackage_core_proto-Enums)
    
    - [UserRole](#engine_protoc_markdown_example_outputtypeperpackage-engine_protoc_markdown_example_outputtypeperpackage_core_proto-Enums-UserRole)

- [engine/protoc/markdown/example/outputtypeperpackage/events.proto](#engine_protoc_markdown_example_outputtypeperpackage-engine_protoc_markdown_example_outputtypeperpackage_events_proto)
  
  - [Messages](#engine_protoc_markdown_example_outputtypeperpackage-engine_protoc_markdown_example_outputtypeperpackage_events_proto-Messages)
    
    - [Event](#engine_protoc_markdown_example_outputtypeperpackage-engine_protoc_markdown_example_outputtypeperpackage_events_proto-Messages-Event)
  
  - [Enums](#engine_protoc_markdown_example_outputtypeperpackage-engine_protoc_markdown_example_outputtypeperpackage_events_proto-Enums)
    
    - [EventKind](#engine_protoc_markdown_example_outputtypeperpackage-engine_protoc_markdown_example_outputtypeperpackage_events_proto-Enums-EventKind)

</details>

## <a id="engine_protoc_markdown_example_outputtypeperpackage-engine_protoc_markdown_example_outputtypeperpackage_core_proto"></a>engine/protoc/markdown/example/outputtypeperpackage/core.proto

<!-- @@protoc_insertion_point(file_header_scope:engine/protoc/markdown/example/outputtypeperpackage/core.proto) -->

<!-- @@protoc_insertion_point(file_scope:engine/protoc/markdown/example/outputtypeperpackage/core.proto) -->

### <a id="engine_protoc_markdown_example_outputtypeperpackage-engine_protoc_markdown_example_outputtypeperpackage_core_proto-Services"></a>Services

<!-- @@protoc_insertion_point(services_section) -->

#### <a id="engine_protoc_markdown_example_outputtypeperpackage-engine_protoc_markdown_example_outputtypeperpackage_core_proto-Services-UserService"></a>UserService

<!-- @@protoc_insertion_point(service_header_scope:engine.protoc.markdown.example.outputtypeperpackage.UserService) -->

Read and mutate user records.

<!-- @@protoc_insertion_point(service_scope:engine.protoc.markdown.example.outputtypeperpackage.UserService) -->

##### <a id="engine_protoc_markdown_example_outputtypeperpackage-engine_protoc_markdown_example_outputtypeperpackage_core_proto-Services-UserService-RPC_Summary"></a>RPC Summary

|Name|Input|Output|Description|
|---|---|---|---|
|GetUser|[GetUserRequest](#engine_protoc_markdown_example_outputtypeperpackage-engine_protoc_markdown_example_outputtypeperpackage_core_proto-Messages-GetUserRequest)|[User](#engine_protoc_markdown_example_outputtypeperpackage-engine_protoc_markdown_example_outputtypeperpackage_core_proto-Messages-User)|Look up a user by id. [...](#engine_protoc_markdown_example_outputtypeperpackage-engine_protoc_markdown_example_outputtypeperpackage_core_proto-Services-UserService-RPC_Details-GetUser)|

##### <a id="engine_protoc_markdown_example_outputtypeperpackage-engine_protoc_markdown_example_outputtypeperpackage_core_proto-Services-UserService-RPC_Details"></a>RPC Details

###### <a id="engine_protoc_markdown_example_outputtypeperpackage-engine_protoc_markdown_example_outputtypeperpackage_core_proto-Services-UserService-RPC_Details-GetUser"></a>GetUser

Look up a user by id.

___

### <a id="engine_protoc_markdown_example_outputtypeperpackage-engine_protoc_markdown_example_outputtypeperpackage_core_proto-Messages"></a>Messages

<!-- @@protoc_insertion_point(messages_section) -->

#### <a id="engine_protoc_markdown_example_outputtypeperpackage-engine_protoc_markdown_example_outputtypeperpackage_core_proto-Messages-GetUserRequest"></a>GetUserRequest

<!-- @@protoc_insertion_point(message_header_scope:engine.protoc.markdown.example.outputtypeperpackage.GetUserRequest) -->

<!-- @@protoc_insertion_point(message_scope:engine.protoc.markdown.example.outputtypeperpackage.GetUserRequest) -->

##### <a id="engine_protoc_markdown_example_outputtypeperpackage-engine_protoc_markdown_example_outputtypeperpackage_core_proto-Messages-GetUserRequest-Field_Summary"></a>Field Summary

|Name|Type|Description|
|---|---|---|
|id|string||

##### <a id="engine_protoc_markdown_example_outputtypeperpackage-engine_protoc_markdown_example_outputtypeperpackage_core_proto-Messages-GetUserRequest-Field_Details"></a>Field Details

###### <a id="engine_protoc_markdown_example_outputtypeperpackage-engine_protoc_markdown_example_outputtypeperpackage_core_proto-Messages-GetUserRequest-Field_Details-id"></a>id

#### <a id="engine_protoc_markdown_example_outputtypeperpackage-engine_protoc_markdown_example_outputtypeperpackage_core_proto-Messages-User"></a>User

<!-- @@protoc_insertion_point(message_header_scope:engine.protoc.markdown.example.outputtypeperpackage.User) -->

A user of the system.

<!-- @@protoc_insertion_point(message_scope:engine.protoc.markdown.example.outputtypeperpackage.User) -->

##### <a id="engine_protoc_markdown_example_outputtypeperpackage-engine_protoc_markdown_example_outputtypeperpackage_core_proto-Messages-User-Field_Summary"></a>Field Summary

|Name|Type|Description|
|---|---|---|
|id|string|Stable identifier. [...](#engine_protoc_markdown_example_outputtypeperpackage-engine_protoc_markdown_example_outputtypeperpackage_core_proto-Messages-User-Field_Details-id)|
|name|string|Display name. [...](#engine_protoc_markdown_example_outputtypeperpackage-engine_protoc_markdown_example_outputtypeperpackage_core_proto-Messages-User-Field_Details-name)|
|role|[UserRole](#engine_protoc_markdown_example_outputtypeperpackage-engine_protoc_markdown_example_outputtypeperpackage_core_proto-Enums-UserRole)|The user's role in the system. [...](#engine_protoc_markdown_example_outputtypeperpackage-engine_protoc_markdown_example_outputtypeperpackage_core_proto-Messages-User-Field_Details-role)|

##### <a id="engine_protoc_markdown_example_outputtypeperpackage-engine_protoc_markdown_example_outputtypeperpackage_core_proto-Messages-User-Field_Details"></a>Field Details

###### <a id="engine_protoc_markdown_example_outputtypeperpackage-engine_protoc_markdown_example_outputtypeperpackage_core_proto-Messages-User-Field_Details-id"></a>id

Stable identifier.

###### <a id="engine_protoc_markdown_example_outputtypeperpackage-engine_protoc_markdown_example_outputtypeperpackage_core_proto-Messages-User-Field_Details-name"></a>name

Display name.

###### <a id="engine_protoc_markdown_example_outputtypeperpackage-engine_protoc_markdown_example_outputtypeperpackage_core_proto-Messages-User-Field_Details-role"></a>role

The user's role in the system.

___

### <a id="engine_protoc_markdown_example_outputtypeperpackage-engine_protoc_markdown_example_outputtypeperpackage_core_proto-Enums"></a>Enums

<!-- @@protoc_insertion_point(enums_section) -->

#### <a id="engine_protoc_markdown_example_outputtypeperpackage-engine_protoc_markdown_example_outputtypeperpackage_core_proto-Enums-UserRole"></a>UserRole

<!-- @@protoc_insertion_point(enum_header_scope:engine.protoc.markdown.example.outputtypeperpackage.UserRole) -->

The role a user is assigned.

<!-- @@protoc_insertion_point(enum_scope:engine.protoc.markdown.example.outputtypeperpackage.UserRole) -->

##### <a id="engine_protoc_markdown_example_outputtypeperpackage-engine_protoc_markdown_example_outputtypeperpackage_core_proto-Enums-UserRole-Value_Summary"></a>Value Summary

|Name|Number|Description|
|---|---|---|
|USER\_ROLE\_UNSPECIFIED|0||
|USER\_ROLE\_MEMBER|1||
|USER\_ROLE\_ADMIN|2||

##### <a id="engine_protoc_markdown_example_outputtypeperpackage-engine_protoc_markdown_example_outputtypeperpackage_core_proto-Enums-UserRole-Value_Details"></a>Value Details

###### <a id="engine_protoc_markdown_example_outputtypeperpackage-engine_protoc_markdown_example_outputtypeperpackage_core_proto-Enums-UserRole-Value_Details-USER_ROLE_UNSPECIFIED"></a>USER\_ROLE\_UNSPECIFIED

###### <a id="engine_protoc_markdown_example_outputtypeperpackage-engine_protoc_markdown_example_outputtypeperpackage_core_proto-Enums-UserRole-Value_Details-USER_ROLE_MEMBER"></a>USER\_ROLE\_MEMBER

###### <a id="engine_protoc_markdown_example_outputtypeperpackage-engine_protoc_markdown_example_outputtypeperpackage_core_proto-Enums-UserRole-Value_Details-USER_ROLE_ADMIN"></a>USER\_ROLE\_ADMIN

___

## <a id="engine_protoc_markdown_example_outputtypeperpackage-engine_protoc_markdown_example_outputtypeperpackage_events_proto"></a>engine/protoc/markdown/example/outputtypeperpackage/events.proto

<!-- @@protoc_insertion_point(file_header_scope:engine/protoc/markdown/example/outputtypeperpackage/events.proto) -->

<!-- @@protoc_insertion_point(file_scope:engine/protoc/markdown/example/outputtypeperpackage/events.proto) -->

### <a id="engine_protoc_markdown_example_outputtypeperpackage-engine_protoc_markdown_example_outputtypeperpackage_events_proto-Messages"></a>Messages

<!-- @@protoc_insertion_point(messages_section) -->

#### <a id="engine_protoc_markdown_example_outputtypeperpackage-engine_protoc_markdown_example_outputtypeperpackage_events_proto-Messages-Event"></a>Event

<!-- @@protoc_insertion_point(message_header_scope:engine.protoc.markdown.example.outputtypeperpackage.Event) -->

One audit-log entry.

<!-- @@protoc_insertion_point(message_scope:engine.protoc.markdown.example.outputtypeperpackage.Event) -->

##### <a id="engine_protoc_markdown_example_outputtypeperpackage-engine_protoc_markdown_example_outputtypeperpackage_events_proto-Messages-Event-Field_Summary"></a>Field Summary

|Name|Type|Description|
|---|---|---|
|subject|[User](#engine_protoc_markdown_example_outputtypeperpackage-engine_protoc_markdown_example_outputtypeperpackage_core_proto-Messages-User)|The user the event is about. [...](#engine_protoc_markdown_example_outputtypeperpackage-engine_protoc_markdown_example_outputtypeperpackage_events_proto-Messages-Event-Field_Details-subject)|
|kind|[EventKind](#engine_protoc_markdown_example_outputtypeperpackage-engine_protoc_markdown_example_outputtypeperpackage_events_proto-Enums-EventKind)|What the user did. [...](#engine_protoc_markdown_example_outputtypeperpackage-engine_protoc_markdown_example_outputtypeperpackage_events_proto-Messages-Event-Field_Details-kind)|

##### <a id="engine_protoc_markdown_example_outputtypeperpackage-engine_protoc_markdown_example_outputtypeperpackage_events_proto-Messages-Event-Field_Details"></a>Field Details

###### <a id="engine_protoc_markdown_example_outputtypeperpackage-engine_protoc_markdown_example_outputtypeperpackage_events_proto-Messages-Event-Field_Details-subject"></a>subject

The user the event is about.

###### <a id="engine_protoc_markdown_example_outputtypeperpackage-engine_protoc_markdown_example_outputtypeperpackage_events_proto-Messages-Event-Field_Details-kind"></a>kind

What the user did.

___

### <a id="engine_protoc_markdown_example_outputtypeperpackage-engine_protoc_markdown_example_outputtypeperpackage_events_proto-Enums"></a>Enums

<!-- @@protoc_insertion_point(enums_section) -->

#### <a id="engine_protoc_markdown_example_outputtypeperpackage-engine_protoc_markdown_example_outputtypeperpackage_events_proto-Enums-EventKind"></a>EventKind

<!-- @@protoc_insertion_point(enum_header_scope:engine.protoc.markdown.example.outputtypeperpackage.EventKind) -->

The kind of action recorded by an Event.

<!-- @@protoc_insertion_point(enum_scope:engine.protoc.markdown.example.outputtypeperpackage.EventKind) -->

##### <a id="engine_protoc_markdown_example_outputtypeperpackage-engine_protoc_markdown_example_outputtypeperpackage_events_proto-Enums-EventKind-Value_Summary"></a>Value Summary

|Name|Number|Description|
|---|---|---|
|EVENT\_KIND\_UNSPECIFIED|0||
|EVENT\_KIND\_CREATED|1||
|EVENT\_KIND\_UPDATED|2||
|EVENT\_KIND\_DELETED|3||

##### <a id="engine_protoc_markdown_example_outputtypeperpackage-engine_protoc_markdown_example_outputtypeperpackage_events_proto-Enums-EventKind-Value_Details"></a>Value Details

###### <a id="engine_protoc_markdown_example_outputtypeperpackage-engine_protoc_markdown_example_outputtypeperpackage_events_proto-Enums-EventKind-Value_Details-EVENT_KIND_UNSPECIFIED"></a>EVENT\_KIND\_UNSPECIFIED

###### <a id="engine_protoc_markdown_example_outputtypeperpackage-engine_protoc_markdown_example_outputtypeperpackage_events_proto-Enums-EventKind-Value_Details-EVENT_KIND_CREATED"></a>EVENT\_KIND\_CREATED

###### <a id="engine_protoc_markdown_example_outputtypeperpackage-engine_protoc_markdown_example_outputtypeperpackage_events_proto-Enums-EventKind-Value_Details-EVENT_KIND_UPDATED"></a>EVENT\_KIND\_UPDATED

###### <a id="engine_protoc_markdown_example_outputtypeperpackage-engine_protoc_markdown_example_outputtypeperpackage_events_proto-Enums-EventKind-Value_Details-EVENT_KIND_DELETED"></a>EVENT\_KIND\_DELETED

<!-- @@protoc_insertion_point(file_footer) -->
