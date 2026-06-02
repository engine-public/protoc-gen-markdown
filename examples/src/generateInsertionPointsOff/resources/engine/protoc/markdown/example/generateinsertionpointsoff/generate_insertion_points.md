---
generated-by: https://github.com/hotelengine/protoc-gen-markdown/releases/tag/0.0.0-pre.0
protoc-gen-markdown-generated-on: 2026-01-01T00:00:00Z
protoc-gen-markdown-options: generateStableAnchors=true,generateInsertionPoints=false,minTableOfContentsHeader=2,maxTableOfContentsHeader=3,outputType=PER_FILE,includeIndices=true,typeSortMode=ALPHABETICAL,fileSortMode=ALPHABETICAL,rpcSortMode=ENCOUNTER,fieldSortMode=ENCOUNTER,enumValueSortMode=ENCOUNTER,resolveReferenceLinksMode=FAIL_ON_INVALID
---

# <a id="engine_protoc_markdown_example_generateinsertionpointsoff_generate_insertion_points_proto"></a>engine/protoc/markdown/example/generateinsertionpointsoff/generate\_insertion\_points.proto

___

<details>
<summary>Table of contents</summary>

- [Services](#engine_protoc_markdown_example_generateinsertionpointsoff_generate_insertion_points_proto-Services)
  
  - [Greeter](#engine_protoc_markdown_example_generateinsertionpointsoff_generate_insertion_points_proto-Services-Greeter)

- [Messages](#engine_protoc_markdown_example_generateinsertionpointsoff_generate_insertion_points_proto-Messages)
  
  - [GreetingRequest](#engine_protoc_markdown_example_generateinsertionpointsoff_generate_insertion_points_proto-Messages-GreetingRequest)
  
  - [GreetingResponse](#engine_protoc_markdown_example_generateinsertionpointsoff_generate_insertion_points_proto-Messages-GreetingResponse)
  
  - [GreetingResponse.Metadata](#engine_protoc_markdown_example_generateinsertionpointsoff_generate_insertion_points_proto-Messages-GreetingResponse_Metadata)

- [Enums](#engine_protoc_markdown_example_generateinsertionpointsoff_generate_insertion_points_proto-Enums)
  
  - [GreetingResponse.Tone](#engine_protoc_markdown_example_generateinsertionpointsoff_generate_insertion_points_proto-Enums-GreetingResponse_Tone)
  
  - [Locale](#engine_protoc_markdown_example_generateinsertionpointsoff_generate_insertion_points_proto-Enums-Locale)

</details>

## <a id="engine_protoc_markdown_example_generateinsertionpointsoff_generate_insertion_points_proto-Services"></a>Services

### <a id="engine_protoc_markdown_example_generateinsertionpointsoff_generate_insertion_points_proto-Services-Greeter"></a>Greeter

Greeter is a tiny RPC so the Services section and a service\_scope pair are emitted.

#### <a id="engine_protoc_markdown_example_generateinsertionpointsoff_generate_insertion_points_proto-Services-Greeter-RPC_Summary"></a>RPC Summary

|Name|Input|Output|Description|
|---|---|---|---|
|SayHello|[GreetingRequest](generate_insertion_points.md#engine_protoc_markdown_example_generateinsertionpointsoff_generate_insertion_points_proto-Messages-GreetingRequest)|[GreetingResponse](generate_insertion_points.md#engine_protoc_markdown_example_generateinsertionpointsoff_generate_insertion_points_proto-Messages-GreetingResponse)|SayHello is a single unary RPC. [...](#engine_protoc_markdown_example_generateinsertionpointsoff_generate_insertion_points_proto-Services-Greeter-RPC_Details-SayHello)|

#### <a id="engine_protoc_markdown_example_generateinsertionpointsoff_generate_insertion_points_proto-Services-Greeter-RPC_Details"></a>RPC Details

##### <a id="engine_protoc_markdown_example_generateinsertionpointsoff_generate_insertion_points_proto-Services-Greeter-RPC_Details-SayHello"></a>SayHello

SayHello is a single unary RPC.

___

## <a id="engine_protoc_markdown_example_generateinsertionpointsoff_generate_insertion_points_proto-Messages"></a>Messages

### <a id="engine_protoc_markdown_example_generateinsertionpointsoff_generate_insertion_points_proto-Messages-GreetingRequest"></a>GreetingRequest

GreetingRequest is the request payload.

#### <a id="engine_protoc_markdown_example_generateinsertionpointsoff_generate_insertion_points_proto-Messages-GreetingRequest-Field_Summary"></a>Field Summary

|Name|Type|Description|
|---|---|---|
|name|string||

#### <a id="engine_protoc_markdown_example_generateinsertionpointsoff_generate_insertion_points_proto-Messages-GreetingRequest-Field_Details"></a>Field Details

##### <a id="engine_protoc_markdown_example_generateinsertionpointsoff_generate_insertion_points_proto-Messages-GreetingRequest-Field_Details-name"></a>name

### <a id="engine_protoc_markdown_example_generateinsertionpointsoff_generate_insertion_points_proto-Messages-GreetingResponse"></a>GreetingResponse

GreetingResponse is the response payload, and also hosts a nested message and a nested
enum so the dotted-ancestor FQN form shows up in the marker names.

#### <a id="engine_protoc_markdown_example_generateinsertionpointsoff_generate_insertion_points_proto-Messages-GreetingResponse-Field_Summary"></a>Field Summary

|Name|Type|Description|
|---|---|---|
|greeting|string||

#### <a id="engine_protoc_markdown_example_generateinsertionpointsoff_generate_insertion_points_proto-Messages-GreetingResponse-Field_Details"></a>Field Details

##### <a id="engine_protoc_markdown_example_generateinsertionpointsoff_generate_insertion_points_proto-Messages-GreetingResponse-Field_Details-greeting"></a>greeting

### <a id="engine_protoc_markdown_example_generateinsertionpointsoff_generate_insertion_points_proto-Messages-GreetingResponse_Metadata"></a>GreetingResponse.Metadata

Metadata is nested so its insertion-point name is the dotted
`GreetingResponse.Metadata`.

#### <a id="engine_protoc_markdown_example_generateinsertionpointsoff_generate_insertion_points_proto-Messages-GreetingResponse_Metadata-Field_Summary"></a>Field Summary

|Name|Type|Description|
|---|---|---|
|generated\_at\_unix\_millis|int64||

#### <a id="engine_protoc_markdown_example_generateinsertionpointsoff_generate_insertion_points_proto-Messages-GreetingResponse_Metadata-Field_Details"></a>Field Details

##### <a id="engine_protoc_markdown_example_generateinsertionpointsoff_generate_insertion_points_proto-Messages-GreetingResponse_Metadata-Field_Details-generated_at_unix_millis"></a>generated\_at\_unix\_millis

___

## <a id="engine_protoc_markdown_example_generateinsertionpointsoff_generate_insertion_points_proto-Enums"></a>Enums

### <a id="engine_protoc_markdown_example_generateinsertionpointsoff_generate_insertion_points_proto-Enums-GreetingResponse_Tone"></a>GreetingResponse.Tone

Tone is a nested enum so its insertion-point name is the dotted
`GreetingResponse.Tone`.

#### <a id="engine_protoc_markdown_example_generateinsertionpointsoff_generate_insertion_points_proto-Enums-GreetingResponse_Tone-Value_Summary"></a>Value Summary

|Name|Number|Description|
|---|---|---|
|TONE\_UNSPECIFIED|0||
|TONE\_FORMAL|1||
|TONE\_INFORMAL|2||

#### <a id="engine_protoc_markdown_example_generateinsertionpointsoff_generate_insertion_points_proto-Enums-GreetingResponse_Tone-Value_Details"></a>Value Details

##### <a id="engine_protoc_markdown_example_generateinsertionpointsoff_generate_insertion_points_proto-Enums-GreetingResponse_Tone-Value_Details-TONE_UNSPECIFIED"></a>TONE\_UNSPECIFIED

##### <a id="engine_protoc_markdown_example_generateinsertionpointsoff_generate_insertion_points_proto-Enums-GreetingResponse_Tone-Value_Details-TONE_FORMAL"></a>TONE\_FORMAL

##### <a id="engine_protoc_markdown_example_generateinsertionpointsoff_generate_insertion_points_proto-Enums-GreetingResponse_Tone-Value_Details-TONE_INFORMAL"></a>TONE\_INFORMAL

### <a id="engine_protoc_markdown_example_generateinsertionpointsoff_generate_insertion_points_proto-Enums-Locale"></a>Locale

Locale is a top-level enum so its insertion-point name is the bare `Locale`.

#### <a id="engine_protoc_markdown_example_generateinsertionpointsoff_generate_insertion_points_proto-Enums-Locale-Value_Summary"></a>Value Summary

|Name|Number|Description|
|---|---|---|
|LOCALE\_UNSPECIFIED|0||
|LOCALE\_EN\_US|1||
|LOCALE\_FR\_FR|2||

#### <a id="engine_protoc_markdown_example_generateinsertionpointsoff_generate_insertion_points_proto-Enums-Locale-Value_Details"></a>Value Details

##### <a id="engine_protoc_markdown_example_generateinsertionpointsoff_generate_insertion_points_proto-Enums-Locale-Value_Details-LOCALE_UNSPECIFIED"></a>LOCALE\_UNSPECIFIED

##### <a id="engine_protoc_markdown_example_generateinsertionpointsoff_generate_insertion_points_proto-Enums-Locale-Value_Details-LOCALE_EN_US"></a>LOCALE\_EN\_US

##### <a id="engine_protoc_markdown_example_generateinsertionpointsoff_generate_insertion_points_proto-Enums-Locale-Value_Details-LOCALE_FR_FR"></a>LOCALE\_FR\_FR
