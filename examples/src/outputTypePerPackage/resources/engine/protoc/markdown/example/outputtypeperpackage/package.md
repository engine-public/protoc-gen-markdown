---
generated-by: https://github.com/hotelengine/protoc-gen-markdown/releases/tag/0.0.0-pre.0
protoc-gen-markdown-generated-on: 2026-01-01T00:00:00Z
protoc-gen-markdown-options: generateStableAnchors=false,generateInsertionPoints=true,minTableOfContentsHeader=2,maxTableOfContentsHeader=4,outputType=PER_PACKAGE,typeSortMode=ALPHABETICAL,fileSortMode=ALPHABETICAL,rpcSortMode=ENCOUNTER,fieldSortMode=ENCOUNTER,enumValueSortMode=ENCOUNTER
# @@protoc_insertion_point(frontmatter)
---

# engine.protoc.markdown.example.outputtypeperpackage

<!-- @@protoc_insertion_point(file_header) -->

___

<details>
<summary>Table of contents</summary>

- [engine/protoc/markdown/example/outputtypeperpackage/core.proto](#engineprotocmarkdownexampleoutputtypeperpackagecoreproto)
  
  - [Services](#services)
    
    - [UserService](#userservice)
  
  - [Messages](#messages)
    
    - [GetUserRequest](#getuserrequest)
    
    - [User](#user)
  
  - [Enums](#enums)
    
    - [UserRole](#userrole)

- [engine/protoc/markdown/example/outputtypeperpackage/events.proto](#engineprotocmarkdownexampleoutputtypeperpackageeventsproto)
  
  - [Messages](#messages)
    
    - [Event](#event)
  
  - [Enums](#enums)
    
    - [EventKind](#eventkind)

</details>

## engine/protoc/markdown/example/outputtypeperpackage/core.proto

<!-- @@protoc_insertion_point(file_header_scope:engine/protoc/markdown/example/outputtypeperpackage/core.proto) -->

<!-- @@protoc_insertion_point(file_scope:engine/protoc/markdown/example/outputtypeperpackage/core.proto) -->

### Services

<!-- @@protoc_insertion_point(services_section) -->

#### UserService

<!-- @@protoc_insertion_point(service_header_scope:engine.protoc.markdown.example.outputtypeperpackage.UserService) -->

Read and mutate user records

<!-- @@protoc_insertion_point(service_scope:engine.protoc.markdown.example.outputtypeperpackage.UserService) -->

##### RPC Summary

|Name|Input|Output|Description|
|---|---|---|---|
|GetUser|[GetUserRequest](#getuserrequest)|[User](#user)|Look up a user by id|

___

### Messages

<!-- @@protoc_insertion_point(messages_section) -->

#### GetUserRequest

<!-- @@protoc_insertion_point(message_header_scope:engine.protoc.markdown.example.outputtypeperpackage.GetUserRequest) -->

<!-- @@protoc_insertion_point(message_scope:engine.protoc.markdown.example.outputtypeperpackage.GetUserRequest) -->

##### Field Summary

|Name|Type|Description|
|---|---|---|
|id|string||

#### User

<!-- @@protoc_insertion_point(message_header_scope:engine.protoc.markdown.example.outputtypeperpackage.User) -->

A user of the system

<!-- @@protoc_insertion_point(message_scope:engine.protoc.markdown.example.outputtypeperpackage.User) -->

##### Field Summary

|Name|Type|Description|
|---|---|---|
|id|string|Stable identifier|
|name|string|Display name|
|role|[UserRole](#userrole)|The user's role in the system|

___

### Enums

<!-- @@protoc_insertion_point(enums_section) -->

#### UserRole

<!-- @@protoc_insertion_point(enum_header_scope:engine.protoc.markdown.example.outputtypeperpackage.UserRole) -->

The role a user is assigned

<!-- @@protoc_insertion_point(enum_scope:engine.protoc.markdown.example.outputtypeperpackage.UserRole) -->

##### Value Summary

|Name|Number|Description|
|---|---|---|
|USER\_ROLE\_UNSPECIFIED|0||
|USER\_ROLE\_MEMBER|1||
|USER\_ROLE\_ADMIN|2||

___

## engine/protoc/markdown/example/outputtypeperpackage/events.proto

<!-- @@protoc_insertion_point(file_header_scope:engine/protoc/markdown/example/outputtypeperpackage/events.proto) -->

<!-- @@protoc_insertion_point(file_scope:engine/protoc/markdown/example/outputtypeperpackage/events.proto) -->

### Messages

<!-- @@protoc_insertion_point(messages_section) -->

#### Event

<!-- @@protoc_insertion_point(message_header_scope:engine.protoc.markdown.example.outputtypeperpackage.Event) -->

One audit-log entry

<!-- @@protoc_insertion_point(message_scope:engine.protoc.markdown.example.outputtypeperpackage.Event) -->

##### Field Summary

|Name|Type|Description|
|---|---|---|
|subject|[User](#user)|The user the event is about|
|kind|[EventKind](#eventkind)|What the user did|

___

### Enums

<!-- @@protoc_insertion_point(enums_section) -->

#### EventKind

<!-- @@protoc_insertion_point(enum_header_scope:engine.protoc.markdown.example.outputtypeperpackage.EventKind) -->

The kind of action recorded by an Event

<!-- @@protoc_insertion_point(enum_scope:engine.protoc.markdown.example.outputtypeperpackage.EventKind) -->

##### Value Summary

|Name|Number|Description|
|---|---|---|
|EVENT\_KIND\_UNSPECIFIED|0||
|EVENT\_KIND\_CREATED|1||
|EVENT\_KIND\_UPDATED|2||
|EVENT\_KIND\_DELETED|3||

<!-- @@protoc_insertion_point(file_footer) -->
