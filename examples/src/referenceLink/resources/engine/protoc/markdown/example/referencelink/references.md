---
generated-by: https://github.com/hotelengine/protoc-gen-markdown/releases/tag/0.0.0-pre.0
protoc-gen-markdown-generated-on: 2026-01-01T00:00:00Z
protoc-gen-markdown-options: generateStableAnchors=true,generateInsertionPoints=true,minTableOfContentsHeader=2,maxTableOfContentsHeader=3,outputType=PER_FILE,includeIndices=true,typeSortMode=ALPHABETICAL,fileSortMode=ALPHABETICAL,rpcSortMode=ENCOUNTER,fieldSortMode=ENCOUNTER,enumValueSortMode=ENCOUNTER,resolveReferenceLinksMode=FAIL_ON_INVALID,referenceLink=ExternalSpec=example.com/docs/external-spec,referenceLink=Wrapper=/docs/override-wrapper,referenceLink=engine.protoc.markdown.example.transitivereferencesshared.CoreEntity=example.com/docs/core-entity
# @@protoc_insertion_point(frontmatter)
---

# <a id="engine_protoc_markdown_example_referencelink_references_proto"></a>engine/protoc/markdown/example/referencelink/references.proto

___

<details>
<summary>Table of contents</summary>

- [Messages](#engine_protoc_markdown_example_referencelink_references_proto-Messages)
  
  - [Wrapper](#engine_protoc_markdown_example_referencelink_references_proto-Messages-Wrapper)

</details>

<!-- @@protoc_insertion_point(file_header_scope:engine/protoc/markdown/example/referencelink/references.proto) -->

<!-- @@protoc_insertion_point(file_scope:engine/protoc/markdown/example/referencelink/references.proto) -->

## <a id="engine_protoc_markdown_example_referencelink_references_proto-Messages"></a>Messages

<!-- @@protoc_insertion_point(messages_section) -->

### <a id="engine_protoc_markdown_example_referencelink_references_proto-Messages-Wrapper"></a>Wrapper

<!-- @@protoc_insertion_point(message_header_scope:engine.protoc.markdown.example.referencelink.Wrapper) -->

Exercises the `referenceLink` option at every site the override fires: bracketed labels
in proto leading comments, and field-type cells whose declared type FQN matches an
override key.  This suite supplies three overrides — `[ExternalSpec]` (a label that
exists nowhere in scope, given here as a scheme-stripped absolute URL
`example.com/docs/external-spec`; without the override the default FAIL\_ON\_INVALID
`resolveReferenceLinksMode` would fail the run), `[Wrapper]` (the in-scope message
below, given here as a site-relative path `/docs/override-wrapper`, which would
otherwise resolve to its own heading), and the transitive-only type
`engine.protoc.markdown.example.transitivereferencesshared.CoreEntity` (matched against
the `core` field's declared type FQN, pointing at `example.com/docs/core-entity`); all
three render with the override URLs regardless of what the resolver would otherwise
find.  The `core` field also demonstrates the side effect: without the override the
default `transitiveReferences=LINK_AS_PEER` would add the shared `core.proto` as a
peer file, but the FQN override suppresses that promotion so no peer document is
produced for this suite.  See [ExternalSpec](example.com/docs/external-spec) and [Wrapper](/docs/override-wrapper) in the rendered output.

<!-- @@protoc_insertion_point(message_scope:engine.protoc.markdown.example.referencelink.Wrapper) -->

#### <a id="engine_protoc_markdown_example_referencelink_references_proto-Messages-Wrapper-Field_Summary"></a>Field Summary

|Name|Type|Description|
|---|---|---|
|name|string|Free-form label; see [Wrapper](/docs/override-wrapper) for context on the surrounding message. [...](#engine_protoc_markdown_example_referencelink_references_proto-Messages-Wrapper-Field_Details-name)|
|core|[CoreEntity](example.com/docs/core-entity)|The transitive type whose field-type cell points at the override URL. [...](#engine_protoc_markdown_example_referencelink_references_proto-Messages-Wrapper-Field_Details-core)|

#### <a id="engine_protoc_markdown_example_referencelink_references_proto-Messages-Wrapper-Field_Details"></a>Field Details

##### <a id="engine_protoc_markdown_example_referencelink_references_proto-Messages-Wrapper-Field_Details-name"></a>name

Free-form label; see [Wrapper](/docs/override-wrapper) for context on the surrounding message.

##### <a id="engine_protoc_markdown_example_referencelink_references_proto-Messages-Wrapper-Field_Details-core"></a>core

The transitive type whose field-type cell points at the override URL.

<!-- @@protoc_insertion_point(file_footer) -->
