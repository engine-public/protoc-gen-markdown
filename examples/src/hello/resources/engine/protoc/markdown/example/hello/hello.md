---
generated-by: https://github.com/engine-public/protoc-gen-markdown/releases/tag/0.0.0-pre.0
protoc-gen-markdown-generated-on: 2026-01-01T00:00:00Z
protoc-gen-markdown-options: generateStableAnchors=true,generateInsertionPoints=true,minTableOfContentsHeader=2,maxTableOfContentsHeader=3,outputType=PER_FILE,includeIndices=true,typeSortMode=ALPHABETICAL,fileSortMode=ALPHABETICAL,rpcSortMode=ENCOUNTER,fieldSortMode=ENCOUNTER,enumValueSortMode=ENCOUNTER,resolveReferenceLinksMode=FAIL_ON_INVALID
# @@protoc_insertion_point(frontmatter)
---

# <a id="engine_protoc_markdown_example_hello_hello_proto"></a>engine/protoc/markdown/example/hello/hello.proto

___

<details>
<summary>Table of contents</summary>

- [Services](#engine_protoc_markdown_example_hello_hello_proto-Services)
  
  - [GreeterService](#engine_protoc_markdown_example_hello_hello_proto-Services-GreeterService)

- [Messages](#engine_protoc_markdown_example_hello_hello_proto-Messages)
  
  - [GreetingRequest](#engine_protoc_markdown_example_hello_hello_proto-Messages-GreetingRequest)
  
  - [GreetingResponse](#engine_protoc_markdown_example_hello_hello_proto-Messages-GreetingResponse)

- [Enums](#engine_protoc_markdown_example_hello_hello_proto-Enums)
  
  - [GreetingType](#engine_protoc_markdown_example_hello_hello_proto-Enums-GreetingType)

</details>

<!-- @@protoc_insertion_point(file_header_scope:engine/protoc/markdown/example/hello/hello.proto) -->

<!-- @@protoc_insertion_point(file_scope:engine/protoc/markdown/example/hello/hello.proto) -->

## <a id="engine_protoc_markdown_example_hello_hello_proto-Services"></a>Services

<!-- @@protoc_insertion_point(services_section) -->

### <a id="engine_protoc_markdown_example_hello_hello_proto-Services-GreeterService"></a>GreeterService

<!-- @@protoc_insertion_point(service_header_scope:engine.protoc.markdown.example.hello.GreeterService) -->

A service to say hello

<!-- @@protoc_insertion_point(service_scope:engine.protoc.markdown.example.hello.GreeterService) -->

#### <a id="engine_protoc_markdown_example_hello_hello_proto-Services-GreeterService-RPC_Summary"></a>RPC Summary

|Name|Input|Output|Description|
|---|---|---|---|
|SayHello|[GreetingRequest](hello.md#engine_protoc_markdown_example_hello_hello_proto-Messages-GreetingRequest)|[GreetingResponse](hello.md#engine_protoc_markdown_example_hello_hello_proto-Messages-GreetingResponse)|Say hello to my little friend [...](#engine_protoc_markdown_example_hello_hello_proto-Services-GreeterService-RPC_Details-SayHello)|

#### <a id="engine_protoc_markdown_example_hello_hello_proto-Services-GreeterService-RPC_Details"></a>RPC Details

##### <a id="engine_protoc_markdown_example_hello_hello_proto-Services-GreeterService-RPC_Details-SayHello"></a>SayHello

Say hello to my little friend

___

## <a id="engine_protoc_markdown_example_hello_hello_proto-Messages"></a>Messages

<!-- @@protoc_insertion_point(messages_section) -->

### <a id="engine_protoc_markdown_example_hello_hello_proto-Messages-GreetingRequest"></a>GreetingRequest

<!-- @@protoc_insertion_point(message_header_scope:engine.protoc.markdown.example.hello.GreetingRequest) -->

Minimal proto: one message and one service.

Runs the compiler at its defaults so the generated fixtures act as a baseline for the per-option suites alongside this one.

* a bulleted list
* with at least two items

<!-- @@protoc_insertion_point(message_scope:engine.protoc.markdown.example.hello.GreetingRequest) -->

#### <a id="engine_protoc_markdown_example_hello_hello_proto-Messages-GreetingRequest-Field_Summary"></a>Field Summary

|Name|Type|Description|
|---|---|---|
|name|string|A comment block for the text field. [...](#engine_protoc_markdown_example_hello_hello_proto-Messages-GreetingRequest-Field_Details-name)|
|type|[GreetingType](hello.md#engine_protoc_markdown_example_hello_hello_proto-Enums-GreetingType)|Specify the character of the greeting you will receive. [...](#engine_protoc_markdown_example_hello_hello_proto-Messages-GreetingRequest-Field_Details-type)|

#### <a id="engine_protoc_markdown_example_hello_hello_proto-Messages-GreetingRequest-Field_Details"></a>Field Details

##### <a id="engine_protoc_markdown_example_hello_hello_proto-Messages-GreetingRequest-Field_Details-name"></a>name

A comment block for the text field.

It includes two paragraphs.

> it also has a quote

[a link](https://example.com)

* and
* a
* bulleted
* list

##### <a id="engine_protoc_markdown_example_hello_hello_proto-Messages-GreetingRequest-Field_Details-type"></a>type

Specify the character of the greeting you will receive.

### <a id="engine_protoc_markdown_example_hello_hello_proto-Messages-GreetingResponse"></a>GreetingResponse

<!-- @@protoc_insertion_point(message_header_scope:engine.protoc.markdown.example.hello.GreetingResponse) -->

<!-- @@protoc_insertion_point(message_scope:engine.protoc.markdown.example.hello.GreetingResponse) -->

#### <a id="engine_protoc_markdown_example_hello_hello_proto-Messages-GreetingResponse-Field_Summary"></a>Field Summary

|Name|Type|Description|
|---|---|---|
|greeting|string||

#### <a id="engine_protoc_markdown_example_hello_hello_proto-Messages-GreetingResponse-Field_Details"></a>Field Details

##### <a id="engine_protoc_markdown_example_hello_hello_proto-Messages-GreetingResponse-Field_Details-greeting"></a>greeting

___

## <a id="engine_protoc_markdown_example_hello_hello_proto-Enums"></a>Enums

<!-- @@protoc_insertion_point(enums_section) -->

### <a id="engine_protoc_markdown_example_hello_hello_proto-Enums-GreetingType"></a>GreetingType

<!-- @@protoc_insertion_point(enum_header_scope:engine.protoc.markdown.example.hello.GreetingType) -->

Specifies how the customer will be greeted in the [GreetingResponse](hello.md#engine_protoc_markdown_example_hello_hello_proto-Messages-GreetingResponse).

<!-- @@protoc_insertion_point(enum_scope:engine.protoc.markdown.example.hello.GreetingType) -->

#### <a id="engine_protoc_markdown_example_hello_hello_proto-Enums-GreetingType-Value_Summary"></a>Value Summary

|Name|Number|Description|
|---|---|---|
|GREETING\_TYPE\_UNSPECIFIED|0|The greeting will be randomly selected from the available [GreetingType](hello.md#engine_protoc_markdown_example_hello_hello_proto-Enums-GreetingType) values. [...](#engine_protoc_markdown_example_hello_hello_proto-Enums-GreetingType-Value_Details-GREETING_TYPE_UNSPECIFIED)|
|GREETING\_TYPE\_INFORMAL|1|A greeting appropriate for formal settings. [...](#engine_protoc_markdown_example_hello_hello_proto-Enums-GreetingType-Value_Details-GREETING_TYPE_INFORMAL)|
|GREETING\_TYPE\_FORMAT|2|A greeting suited for an old friend. [...](#engine_protoc_markdown_example_hello_hello_proto-Enums-GreetingType-Value_Details-GREETING_TYPE_FORMAT)|

#### <a id="engine_protoc_markdown_example_hello_hello_proto-Enums-GreetingType-Value_Details"></a>Value Details

##### <a id="engine_protoc_markdown_example_hello_hello_proto-Enums-GreetingType-Value_Details-GREETING_TYPE_UNSPECIFIED"></a>GREETING\_TYPE\_UNSPECIFIED

The greeting will be randomly selected from the available [GreetingType](hello.md#engine_protoc_markdown_example_hello_hello_proto-Enums-GreetingType) values.

##### <a id="engine_protoc_markdown_example_hello_hello_proto-Enums-GreetingType-Value_Details-GREETING_TYPE_INFORMAL"></a>GREETING\_TYPE\_INFORMAL

A greeting appropriate for formal settings.

##### <a id="engine_protoc_markdown_example_hello_hello_proto-Enums-GreetingType-Value_Details-GREETING_TYPE_FORMAT"></a>GREETING\_TYPE\_FORMAT

A greeting suited for an old friend.

<!-- @@protoc_insertion_point(file_footer) -->
