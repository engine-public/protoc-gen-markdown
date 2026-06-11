---
generated-by: https://github.com/hotelengine/protoc-gen-markdown/releases/tag/0.0.0-pre.0
protoc-gen-markdown-generated-on: 2026-01-01T00:00:00Z
protoc-gen-markdown-options: generateStableAnchors=false,generateInsertionPoints=true,minTableOfContentsHeader=2,maxTableOfContentsHeader=3,outputType=PER_FILE,includePackageIndices=true,typeSortMode=ALPHABETICAL,fileSortMode=ALPHABETICAL,rpcSortMode=ENCOUNTER,fieldSortMode=ENCOUNTER,enumValueSortMode=ENCOUNTER,resolveReferenceLinksMode=FAIL_ON_INVALID
# @@protoc_insertion_point(frontmatter)
---

# engine/protoc/markdown/example/generatestableanchorsoff/generate\_stable\_anchors.proto

___

<details>
<summary>Table of contents</summary>

- [Messages](#messages)
  
  - [First](#first)
  
  - [Second](#second)

</details>

<!-- @@protoc_insertion_point(file_header_scope:engine/protoc/markdown/example/generatestableanchorsoff/generate_stable_anchors.proto) -->

<!-- @@protoc_insertion_point(file_scope:engine/protoc/markdown/example/generatestableanchorsoff/generate_stable_anchors.proto) -->

## Messages

<!-- @@protoc_insertion_point(messages_section) -->

### First

<!-- @@protoc_insertion_point(message_header_scope:engine.protoc.markdown.example.generatestableanchorsoff.First) -->

Two messages exposing a same-named `text` field whose multi-paragraph leading comment
forces a `##### text` heading under Field Details.  With the default
generateStableAnchors=true the two `##### text` headings get distinct path-anchor ids
(one under First, one under Second); with this suite's generateStableAnchors=false they
collide on the renderer's `#text` slug.

<!-- @@protoc_insertion_point(message_scope:engine.protoc.markdown.example.generatestableanchorsoff.First) -->

#### Field Summary

|Name|Type|Description|
|---|---|---|
|text|string|First message's text field. [...](#text)|
|second|[Second](generate_stable_anchors.md#second)||

#### Field Details

##### text

First message's text field.

Multi-paragraph so a Field Details expansion is forced.

##### second

### Second

<!-- @@protoc_insertion_point(message_header_scope:engine.protoc.markdown.example.generatestableanchorsoff.Second) -->

<!-- @@protoc_insertion_point(message_scope:engine.protoc.markdown.example.generatestableanchorsoff.Second) -->

#### Field Summary

|Name|Type|Description|
|---|---|---|
|text|string|Second message's text field. [...](#text)|

#### Field Details

##### text

Second message's text field.

Multi-paragraph so a Field Details expansion is forced — collides with First's `text`
under the default slugified anchor scheme.

<!-- @@protoc_insertion_point(file_footer) -->
