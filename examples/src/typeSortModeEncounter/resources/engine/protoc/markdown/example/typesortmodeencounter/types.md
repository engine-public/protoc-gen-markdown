---
generated-by: https://github.com/hotelengine/protoc-gen-markdown/releases/tag/0.0.0-pre.0
protoc-gen-markdown-generated-on: 2026-01-01T00:00:00Z
protoc-gen-markdown-options: generateStableAnchors=false,generateInsertionPoints=false,minTableOfContentsHeader=null,maxTableOfContentsHeader=null,outputType=PER_FILE,typeSortMode=ENCOUNTER,fileSortMode=ALPHABETICAL,rpcSortMode=ENCOUNTER,fieldSortMode=ENCOUNTER,enumValueSortMode=ENCOUNTER
---

# engine/protoc/markdown/example/typesortmodeencounter/types.proto

___

## Services

### ZebraService

#### RPC Summary

|Name|Input|Output|Description|
|---|---|---|---|
|Stride|[ZebraSignal](types.md#zebrasignal)|[ZebraSignal](types.md#zebrasignal)||

### ApeService

#### RPC Summary

|Name|Input|Output|Description|
|---|---|---|---|
|Wave|[ApeSignal](types.md#apesignal)|[ApeSignal](types.md#apesignal)||

___

## Messages

### ZebraSignal

Contains a nested message and a nested enum so the alphabetical (flatten-by-full-dotted-name)
traversal can be contrasted with the depth-first declaration order recorded here.

#### Field Summary

|Name|Type|Description|
|---|---|---|
|text|string||

### ZebraSignal.Stripe

#### Field Summary

|Name|Type|Description|
|---|---|---|
|width|int32||

### ApeSignal

#### Field Summary

|Name|Type|Description|
|---|---|---|
|text|string||

___

## Enums

### ZebraSpecies

#### Value Summary

|Name|Number|Description|
|---|---|---|
|ZEBRA\_SPECIES\_UNSPECIFIED|0||

### ApeSpecies

#### Value Summary

|Name|Number|Description|
|---|---|---|
|APE\_SPECIES\_UNSPECIFIED|0||

### ZebraSignal.Pattern

#### Value Summary

|Name|Number|Description|
|---|---|---|
|ZEBRA\_PATTERN\_UNSPECIFIED|0||
