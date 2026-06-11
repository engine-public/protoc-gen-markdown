---
generated-by: https://github.com/hotelengine/protoc-gen-markdown/releases/tag/0.0.0-pre.0
protoc-gen-markdown-generated-on: 2026-01-01T00:00:00Z
protoc-gen-markdown-options: generateStableAnchors=true,generateInsertionPoints=false,minTableOfContentsHeader=null,maxTableOfContentsHeader=null
---

# <a id="engine_protoc_markdown_example_generatestableanchors_generate_stable_anchors_proto"></a>engine/protoc/markdown/example/generatestableanchors/generate\_stable\_anchors.proto

___

## <a id="engine_protoc_markdown_example_generatestableanchors_generate_stable_anchors_proto-Messages"></a>Messages

### <a id="engine_protoc_markdown_example_generatestableanchors_generate_stable_anchors_proto-Messages-First"></a>First

Two messages exposing a same-named `text` field whose multi-paragraph leading comment
forces a `##### text` heading under Field Details.  With generateStableAnchors=true the
two `##### text` headings get distinct path-anchor ids (one under First, one under Second);
with the default false they would collide on the renderer's `#text` slug.

#### <a id="engine_protoc_markdown_example_generatestableanchors_generate_stable_anchors_proto-Messages-First-Field_Summary"></a>Field Summary

|Name|Type|Description|
|---|---|---|
|text|string|First message's text field. [...](#engine_protoc_markdown_example_generatestableanchors_generate_stable_anchors_proto-Messages-First-Field_Details-text)|
|second|[Second](generate_stable_anchors.md#engine_protoc_markdown_example_generatestableanchors_generate_stable_anchors_proto-Messages-Second)||

#### <a id="engine_protoc_markdown_example_generatestableanchors_generate_stable_anchors_proto-Messages-First-Field_Details"></a>Field Details

##### <a id="engine_protoc_markdown_example_generatestableanchors_generate_stable_anchors_proto-Messages-First-Field_Details-text"></a>text

First message's text field.

Multi-paragraph so a Field Details expansion is forced.

### <a id="engine_protoc_markdown_example_generatestableanchors_generate_stable_anchors_proto-Messages-Second"></a>Second

#### <a id="engine_protoc_markdown_example_generatestableanchors_generate_stable_anchors_proto-Messages-Second-Field_Summary"></a>Field Summary

|Name|Type|Description|
|---|---|---|
|text|string|Second message's text field. [...](#engine_protoc_markdown_example_generatestableanchors_generate_stable_anchors_proto-Messages-Second-Field_Details-text)|

#### <a id="engine_protoc_markdown_example_generatestableanchors_generate_stable_anchors_proto-Messages-Second-Field_Details"></a>Field Details

##### <a id="engine_protoc_markdown_example_generatestableanchors_generate_stable_anchors_proto-Messages-Second-Field_Details-text"></a>text

Second message's text field.

Multi-paragraph so a Field Details expansion is forced — collides with First's `text`
under the default slugified anchor scheme.
