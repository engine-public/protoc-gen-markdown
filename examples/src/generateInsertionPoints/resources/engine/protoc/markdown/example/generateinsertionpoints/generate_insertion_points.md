---
generated-by: https://github.com/hotelengine/protoc-gen-markdown/releases/tag/0.0.0-pre.0
protoc-gen-markdown-generated-on: 2026-01-01T00:00:00Z
protoc-gen-markdown-options: generateStableAnchors=false,generateInsertionPoints=true,minTableOfContentsHeader=null,maxTableOfContentsHeader=null
# @@protoc_insertion_point(frontmatter)
---

# engine/protoc/markdown/example/generateinsertionpoints/generate\_insertion\_points.proto

<!-- @@protoc_insertion_point(file_header) -->

___

## Services

<!-- @@protoc_insertion_point(services_section) -->

### Greeter

<!-- @@protoc_insertion_point(service_header_scope:engine.protoc.markdown.example.generateinsertionpoints.Greeter) -->

Greeter is a tiny RPC so the Services section and a service\_scope pair are emitted

<!-- @@protoc_insertion_point(service_scope:engine.protoc.markdown.example.generateinsertionpoints.Greeter) -->

#### RPC Summary

|Name|Input|Output|Description|
|---|---|---|---|
|SayHello|[GreetingRequest](generate_insertion_points.md#greetingrequest)|[GreetingResponse](generate_insertion_points.md#greetingresponse)|SayHello is a single unary RPC|

## Messages

<!-- @@protoc_insertion_point(messages_section) -->

### GreetingRequest

<!-- @@protoc_insertion_point(message_header_scope:engine.protoc.markdown.example.generateinsertionpoints.GreetingRequest) -->

GreetingRequest is the request payload

<!-- @@protoc_insertion_point(message_scope:engine.protoc.markdown.example.generateinsertionpoints.GreetingRequest) -->

#### Field Summary

|Name|Type|Description|
|---|---|---|
|name|string||

### GreetingResponse

<!-- @@protoc_insertion_point(message_header_scope:engine.protoc.markdown.example.generateinsertionpoints.GreetingResponse) -->

GreetingResponse is the response payload, and also hosts a nested message and a nested
enum so the dotted-ancestor FQN form shows up in the marker names.

<!-- @@protoc_insertion_point(message_scope:engine.protoc.markdown.example.generateinsertionpoints.GreetingResponse) -->

#### Field Summary

|Name|Type|Description|
|---|---|---|
|greeting|string||

### GreetingResponse.Metadata

<!-- @@protoc_insertion_point(message_header_scope:engine.protoc.markdown.example.generateinsertionpoints.GreetingResponse.Metadata) -->

Metadata is nested so its insertion-point name is the dotted
`GreetingResponse.Metadata`.

<!-- @@protoc_insertion_point(message_scope:engine.protoc.markdown.example.generateinsertionpoints.GreetingResponse.Metadata) -->

#### Field Summary

|Name|Type|Description|
|---|---|---|
|generated\_at\_unix\_millis|int64||

## Enums

<!-- @@protoc_insertion_point(enums_section) -->

### Locale

<!-- @@protoc_insertion_point(enum_header_scope:engine.protoc.markdown.example.generateinsertionpoints.Locale) -->

Locale is a top-level enum so its insertion-point name is the bare \`Locale

<!-- @@protoc_insertion_point(enum_scope:engine.protoc.markdown.example.generateinsertionpoints.Locale) -->

#### Value Summary

|Name|Number|Description|
|---|---|---|
|LOCALE\_UNSPECIFIED|0||
|LOCALE\_EN\_US|1||
|LOCALE\_FR\_FR|2||

### GreetingResponse.Tone

<!-- @@protoc_insertion_point(enum_header_scope:engine.protoc.markdown.example.generateinsertionpoints.GreetingResponse.Tone) -->

Tone is a nested enum so its insertion-point name is the dotted
`GreetingResponse.Tone`.

<!-- @@protoc_insertion_point(enum_scope:engine.protoc.markdown.example.generateinsertionpoints.GreetingResponse.Tone) -->

#### Value Summary

|Name|Number|Description|
|---|---|---|
|TONE\_UNSPECIFIED|0||
|TONE\_FORMAL|1||
|TONE\_INFORMAL|2||

<!-- @@protoc_insertion_point(file_footer) -->
