---
generated-by: https://github.com/hotelengine/protoc-gen-markdown/releases/tag/0.0.0-pre.0
protoc-gen-markdown-generated-on: 2026-01-01T00:00:00Z
protoc-gen-markdown-options: generateStableAnchors=false,generateInsertionPoints=false,minTableOfContentsHeader=null,maxTableOfContentsHeader=null
---

# engine/protoc/markdown/example/hello/hello.proto

___

## Services

### GreeterService

A service to say hello

#### RPC Summary

|Name|Input|Output|Description|
|---|---|---|---|
|SayHello|[GreetingRequest](hello.md#greetingrequest)|[GreetingResponse](hello.md#greetingresponse)|Say hello to my little friend|

## Messages

### GreetingRequest

Minimal proto: one message and one service.

Runs the compiler at its defaults so the generated fixtures act as a baseline for the per-option suites alongside this one.

 * a bulleted list
 * with at least two items

#### Field Summary

|Name|Type|Description|
|---|---|---|
|name|string|A comment block for the text field. [...](#name)|
|type|[GreetingType](hello.md#greetingtype)|Specify the character of the greeting you will receive|

#### Field Details

##### name

A comment block for the text field.

It includes two paragraphs.

> it also has a quote

[a link](https://example.com)

 * and
 * a
 * bulleted
 * list

### GreetingResponse

#### Field Summary

|Name|Type|Description|
|---|---|---|
|greeting|string||

## Enums

### GreetingType

Specifies how the customer will be greeted

#### Value Summary

|Name|Number|Description|
|---|---|---|
|GREETING\_TYPE\_UNSPECIFIED|0|The greeting will be randomly selected|
|GREETING\_TYPE\_INFORMAL|1|A greeting appropriate for formal settings|
|GREETING\_TYPE\_FORMAT|2|A greeting suited for an old friend|
