---
generated-by: https://github.com/hotelengine/protoc-gen-markdown/releases/tag/0.0.0-pre.0
protoc-gen-markdown-generated-on: 2026-01-01T00:00:00Z
protoc-gen-markdown-options: generateStableAnchors=true,generateInsertionPoints=true,minTableOfContentsHeader=2,maxTableOfContentsHeader=3,outputType=PER_FILE,includePackageIndices=false,typeSortMode=ALPHABETICAL,fileSortMode=ALPHABETICAL,rpcSortMode=ENCOUNTER,fieldSortMode=ENCOUNTER,enumValueSortMode=ENCOUNTER,resolveReferenceLinksMode=FAIL_ON_INVALID
# @@protoc_insertion_point(frontmatter)
---

# <a id="engine_protoc_markdown_example_includepackageindicesoff_core_proto"></a>engine/protoc/markdown/example/includepackageindicesoff/core.proto

___

<details>
<summary>Table of contents</summary>

- [Services](#engine_protoc_markdown_example_includepackageindicesoff_core_proto-Services)
  
  - [UserService](#engine_protoc_markdown_example_includepackageindicesoff_core_proto-Services-UserService)

- [Messages](#engine_protoc_markdown_example_includepackageindicesoff_core_proto-Messages)
  
  - [GetUserRequest](#engine_protoc_markdown_example_includepackageindicesoff_core_proto-Messages-GetUserRequest)
  
  - [User](#engine_protoc_markdown_example_includepackageindicesoff_core_proto-Messages-User)

- [Enums](#engine_protoc_markdown_example_includepackageindicesoff_core_proto-Enums)
  
  - [UserRole](#engine_protoc_markdown_example_includepackageindicesoff_core_proto-Enums-UserRole)

</details>

<!-- @@protoc_insertion_point(file_header_scope:engine/protoc/markdown/example/includepackageindicesoff/core.proto) -->

<!-- @@protoc_insertion_point(file_scope:engine/protoc/markdown/example/includepackageindicesoff/core.proto) -->

## <a id="engine_protoc_markdown_example_includepackageindicesoff_core_proto-Services"></a>Services

<!-- @@protoc_insertion_point(services_section) -->

### <a id="engine_protoc_markdown_example_includepackageindicesoff_core_proto-Services-UserService"></a>UserService

<!-- @@protoc_insertion_point(service_header_scope:engine.protoc.markdown.example.includepackageindicesoff.UserService) -->

Read and mutate user records.

<!-- @@protoc_insertion_point(service_scope:engine.protoc.markdown.example.includepackageindicesoff.UserService) -->

#### <a id="engine_protoc_markdown_example_includepackageindicesoff_core_proto-Services-UserService-RPC_Summary"></a>RPC Summary

|Name|Input|Output|Description|
|---|---|---|---|
|GetUser|[GetUserRequest](core.md#engine_protoc_markdown_example_includepackageindicesoff_core_proto-Messages-GetUserRequest)|[User](core.md#engine_protoc_markdown_example_includepackageindicesoff_core_proto-Messages-User)|Look up a user by id.|

#### <a id="engine_protoc_markdown_example_includepackageindicesoff_core_proto-Services-UserService-RPC_Details"></a>RPC Details

##### <a id="engine_protoc_markdown_example_includepackageindicesoff_core_proto-Services-UserService-RPC_Details-GetUser"></a>GetUser

___

## <a id="engine_protoc_markdown_example_includepackageindicesoff_core_proto-Messages"></a>Messages

<!-- @@protoc_insertion_point(messages_section) -->

### <a id="engine_protoc_markdown_example_includepackageindicesoff_core_proto-Messages-GetUserRequest"></a>GetUserRequest

<!-- @@protoc_insertion_point(message_header_scope:engine.protoc.markdown.example.includepackageindicesoff.GetUserRequest) -->

<!-- @@protoc_insertion_point(message_scope:engine.protoc.markdown.example.includepackageindicesoff.GetUserRequest) -->

#### <a id="engine_protoc_markdown_example_includepackageindicesoff_core_proto-Messages-GetUserRequest-Field_Summary"></a>Field Summary

|Name|Type|Description|
|---|---|---|
|id|string||

#### <a id="engine_protoc_markdown_example_includepackageindicesoff_core_proto-Messages-GetUserRequest-Field_Details"></a>Field Details

##### <a id="engine_protoc_markdown_example_includepackageindicesoff_core_proto-Messages-GetUserRequest-Field_Details-id"></a>id

### <a id="engine_protoc_markdown_example_includepackageindicesoff_core_proto-Messages-User"></a>User

<!-- @@protoc_insertion_point(message_header_scope:engine.protoc.markdown.example.includepackageindicesoff.User) -->

A user of the system.

<!-- @@protoc_insertion_point(message_scope:engine.protoc.markdown.example.includepackageindicesoff.User) -->

#### <a id="engine_protoc_markdown_example_includepackageindicesoff_core_proto-Messages-User-Field_Summary"></a>Field Summary

|Name|Type|Description|
|---|---|---|
|id|string|Stable identifier.|
|name|string|Display name.|
|role|[UserRole](core.md#engine_protoc_markdown_example_includepackageindicesoff_core_proto-Enums-UserRole)|The user's role in the system.|

#### <a id="engine_protoc_markdown_example_includepackageindicesoff_core_proto-Messages-User-Field_Details"></a>Field Details

##### <a id="engine_protoc_markdown_example_includepackageindicesoff_core_proto-Messages-User-Field_Details-id"></a>id

##### <a id="engine_protoc_markdown_example_includepackageindicesoff_core_proto-Messages-User-Field_Details-name"></a>name

##### <a id="engine_protoc_markdown_example_includepackageindicesoff_core_proto-Messages-User-Field_Details-role"></a>role

___

## <a id="engine_protoc_markdown_example_includepackageindicesoff_core_proto-Enums"></a>Enums

<!-- @@protoc_insertion_point(enums_section) -->

### <a id="engine_protoc_markdown_example_includepackageindicesoff_core_proto-Enums-UserRole"></a>UserRole

<!-- @@protoc_insertion_point(enum_header_scope:engine.protoc.markdown.example.includepackageindicesoff.UserRole) -->

The role a user is assigned.

<!-- @@protoc_insertion_point(enum_scope:engine.protoc.markdown.example.includepackageindicesoff.UserRole) -->

#### <a id="engine_protoc_markdown_example_includepackageindicesoff_core_proto-Enums-UserRole-Value_Summary"></a>Value Summary

|Name|Number|Description|
|---|---|---|
|USER\_ROLE\_UNSPECIFIED|0||
|USER\_ROLE\_MEMBER|1||
|USER\_ROLE\_ADMIN|2||

#### <a id="engine_protoc_markdown_example_includepackageindicesoff_core_proto-Enums-UserRole-Value_Details"></a>Value Details

##### <a id="engine_protoc_markdown_example_includepackageindicesoff_core_proto-Enums-UserRole-Value_Details-USER_ROLE_UNSPECIFIED"></a>USER\_ROLE\_UNSPECIFIED

##### <a id="engine_protoc_markdown_example_includepackageindicesoff_core_proto-Enums-UserRole-Value_Details-USER_ROLE_MEMBER"></a>USER\_ROLE\_MEMBER

##### <a id="engine_protoc_markdown_example_includepackageindicesoff_core_proto-Enums-UserRole-Value_Details-USER_ROLE_ADMIN"></a>USER\_ROLE\_ADMIN

<!-- @@protoc_insertion_point(file_footer) -->
