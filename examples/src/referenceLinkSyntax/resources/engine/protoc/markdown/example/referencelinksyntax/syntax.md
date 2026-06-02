---
generated-by: https://github.com/hotelengine/protoc-gen-markdown/releases/tag/0.0.0-pre.0
protoc-gen-markdown-generated-on: 2026-01-01T00:00:00Z
protoc-gen-markdown-options: generateStableAnchors=true,generateInsertionPoints=true,minTableOfContentsHeader=2,maxTableOfContentsHeader=3,outputType=PER_FILE,includeIndices=true,typeSortMode=ALPHABETICAL,fileSortMode=ALPHABETICAL,rpcSortMode=ENCOUNTER,fieldSortMode=ENCOUNTER,enumValueSortMode=ENCOUNTER,resolveReferenceLinksMode=FAIL_ON_INVALID,referenceLink=google.rpc.Status=example.com/docs/google.rpc.Status
# @@protoc_insertion_point(frontmatter)
---

# <a id="engine_protoc_markdown_example_referencelinksyntax_syntax_proto"></a>engine/protoc/markdown/example/referencelinksyntax/syntax.proto

___

<details>
<summary>Table of contents</summary>

- [Services](#engine_protoc_markdown_example_referencelinksyntax_syntax_proto-Services)
  
  - [ExampleService](#engine_protoc_markdown_example_referencelinksyntax_syntax_proto-Services-ExampleService)

- [Messages](#engine_protoc_markdown_example_referencelinksyntax_syntax_proto-Messages)
  
  - [Holder](#engine_protoc_markdown_example_referencelinksyntax_syntax_proto-Messages-Holder)
  
  - [Holder.Responsive](#engine_protoc_markdown_example_referencelinksyntax_syntax_proto-Messages-Holder_Responsive)
  
  - [Responsive](#engine_protoc_markdown_example_referencelinksyntax_syntax_proto-Messages-Responsive)
  
  - [SubmitRequest](#engine_protoc_markdown_example_referencelinksyntax_syntax_proto-Messages-SubmitRequest)
  
  - [SubmitResponse](#engine_protoc_markdown_example_referencelinksyntax_syntax_proto-Messages-SubmitResponse)

</details>

<!-- @@protoc_insertion_point(file_header_scope:engine/protoc/markdown/example/referencelinksyntax/syntax.proto) -->

<!-- @@protoc_insertion_point(file_scope:engine/protoc/markdown/example/referencelinksyntax/syntax.proto) -->

## <a id="engine_protoc_markdown_example_referencelinksyntax_syntax_proto-Services"></a>Services

<!-- @@protoc_insertion_point(services_section) -->

### <a id="engine_protoc_markdown_example_referencelinksyntax_syntax_proto-Services-ExampleService"></a>ExampleService

<!-- @@protoc_insertion_point(service_header_scope:engine.protoc.markdown.example.referencelinksyntax.ExampleService) -->

Stand-in for a typical RPC that surfaces a structured error.

<!-- @@protoc_insertion_point(service_scope:engine.protoc.markdown.example.referencelinksyntax.ExampleService) -->

#### <a id="engine_protoc_markdown_example_referencelinksyntax_syntax_proto-Services-ExampleService-RPC_Summary"></a>RPC Summary

|Name|Input|Output|Description|
|---|---|---|---|
|Submit|[SubmitRequest](syntax.md#engine_protoc_markdown_example_referencelinksyntax_syntax_proto-Messages-SubmitRequest)|[SubmitResponse](syntax.md#engine_protoc_markdown_example_referencelinksyntax_syntax_proto-Messages-SubmitResponse)|Submit a payload. [...](#engine_protoc_markdown_example_referencelinksyntax_syntax_proto-Services-ExampleService-RPC_Details-Submit)|

#### <a id="engine_protoc_markdown_example_referencelinksyntax_syntax_proto-Services-ExampleService-RPC_Details"></a>RPC Details

##### <a id="engine_protoc_markdown_example_referencelinksyntax_syntax_proto-Services-ExampleService-RPC_Details-Submit"></a>Submit

Submit a payload.

In case of error, the status will contain an error detail within the
[Status.details](example.com/docs/google.rpc.Status) field.

___

## <a id="engine_protoc_markdown_example_referencelinksyntax_syntax_proto-Messages"></a>Messages

<!-- @@protoc_insertion_point(messages_section) -->

### <a id="engine_protoc_markdown_example_referencelinksyntax_syntax_proto-Messages-Holder"></a>Holder

<!-- @@protoc_insertion_point(message_header_scope:engine.protoc.markdown.example.referencelinksyntax.Holder) -->

Exercises three reference-link syntax forms the resolver supports beyond the canonical
`[label]` shortcut:

 1. CommonMark full reference form `[display text][label]`.
    See the `submit` RPC below: `[Status.details][google.rpc.Status]` is rendered with
    `Status.details` as the visible link text and the URL drawn from the
    `referenceLink=google.rpc.Status=...` override the suite passes — the display text is
    NOT itself resolved.
 2. Backslash-escaped brackets `\[...\]`.
    See [Holder.tags](syntax.md#engine_protoc_markdown_example_referencelinksyntax_syntax_proto-Messages-Holder-Field_Details-tags): the escaped `\["a", "b"\]` in the comment survives as literal text
    in the rendered output, with the brackets preserved and no resolver attempt.
 3. Field-scope bare-name resolution against the field's declared type.
    See [Holder.responsive](syntax.md#engine_protoc_markdown_example_referencelinksyntax_syntax_proto-Messages-Holder-Field_Details-responsive): although the package declares two `Responsive` messages (one
    nested inside `Holder`, one at the top level here), `[Responsive]` in the field's
    comment resolves to the specific message the field's type points at — local context
    wins over the otherwise-ambiguous global short name.

<!-- @@protoc_insertion_point(message_scope:engine.protoc.markdown.example.referencelinksyntax.Holder) -->

#### <a id="engine_protoc_markdown_example_referencelinksyntax_syntax_proto-Messages-Holder-Field_Summary"></a>Field Summary

|Name|Type|Description|
|---|---|---|
|responsive|[Responsive](syntax.md#engine_protoc_markdown_example_referencelinksyntax_syntax_proto-Messages-Responsive)|The shape this holder wraps.  Resolves to the [Responsive](syntax.md#engine_protoc_markdown_example_referencelinksyntax_syntax_proto-Messages-Responsive) declared at the top of this file (the field's declared type), even though there is also a nested `Holder.Responsive` sharing the same short name. [...](#engine_protoc_markdown_example_referencelinksyntax_syntax_proto-Messages-Holder-Field_Details-responsive)|
|tags|repeated string|Tags categorizing the held content. Example: \["a", "b"\] [...](#engine_protoc_markdown_example_referencelinksyntax_syntax_proto-Messages-Holder-Field_Details-tags)|

#### <a id="engine_protoc_markdown_example_referencelinksyntax_syntax_proto-Messages-Holder-Field_Details"></a>Field Details

##### <a id="engine_protoc_markdown_example_referencelinksyntax_syntax_proto-Messages-Holder-Field_Details-responsive"></a>responsive

The shape this holder wraps.  Resolves to the [Responsive](syntax.md#engine_protoc_markdown_example_referencelinksyntax_syntax_proto-Messages-Responsive) declared at the top of
this file (the field's declared type), even though there is also a nested
`Holder.Responsive` sharing the same short name.

##### <a id="engine_protoc_markdown_example_referencelinksyntax_syntax_proto-Messages-Holder-Field_Details-tags"></a>tags

Tags categorizing the held content.
Example: \["a", "b"\]

### <a id="engine_protoc_markdown_example_referencelinksyntax_syntax_proto-Messages-Holder_Responsive"></a>Holder.Responsive

<!-- @@protoc_insertion_point(message_header_scope:engine.protoc.markdown.example.referencelinksyntax.Holder.Responsive) -->

A nested message that shadows the top-level `Responsive` short name.  Used to prove
that bare-name resolution from `[Holder.responsive]`'s field comment picks the
declared target rather than this ambiguity-introducing sibling.

<!-- @@protoc_insertion_point(message_scope:engine.protoc.markdown.example.referencelinksyntax.Holder.Responsive) -->

#### <a id="engine_protoc_markdown_example_referencelinksyntax_syntax_proto-Messages-Holder_Responsive-Field_Summary"></a>Field Summary

|Name|Type|Description|
|---|---|---|
|label|string||

#### <a id="engine_protoc_markdown_example_referencelinksyntax_syntax_proto-Messages-Holder_Responsive-Field_Details"></a>Field Details

##### <a id="engine_protoc_markdown_example_referencelinksyntax_syntax_proto-Messages-Holder_Responsive-Field_Details-label"></a>label

### <a id="engine_protoc_markdown_example_referencelinksyntax_syntax_proto-Messages-Responsive"></a>Responsive

<!-- @@protoc_insertion_point(message_header_scope:engine.protoc.markdown.example.referencelinksyntax.Responsive) -->

The shape `Holder.responsive` points at.

<!-- @@protoc_insertion_point(message_scope:engine.protoc.markdown.example.referencelinksyntax.Responsive) -->

#### <a id="engine_protoc_markdown_example_referencelinksyntax_syntax_proto-Messages-Responsive-Field_Summary"></a>Field Summary

|Name|Type|Description|
|---|---|---|
|id|string||

#### <a id="engine_protoc_markdown_example_referencelinksyntax_syntax_proto-Messages-Responsive-Field_Details"></a>Field Details

##### <a id="engine_protoc_markdown_example_referencelinksyntax_syntax_proto-Messages-Responsive-Field_Details-id"></a>id

### <a id="engine_protoc_markdown_example_referencelinksyntax_syntax_proto-Messages-SubmitRequest"></a>SubmitRequest

<!-- @@protoc_insertion_point(message_header_scope:engine.protoc.markdown.example.referencelinksyntax.SubmitRequest) -->

<!-- @@protoc_insertion_point(message_scope:engine.protoc.markdown.example.referencelinksyntax.SubmitRequest) -->

#### <a id="engine_protoc_markdown_example_referencelinksyntax_syntax_proto-Messages-SubmitRequest-Field_Summary"></a>Field Summary

|Name|Type|Description|
|---|---|---|
|payload|string||

#### <a id="engine_protoc_markdown_example_referencelinksyntax_syntax_proto-Messages-SubmitRequest-Field_Details"></a>Field Details

##### <a id="engine_protoc_markdown_example_referencelinksyntax_syntax_proto-Messages-SubmitRequest-Field_Details-payload"></a>payload

### <a id="engine_protoc_markdown_example_referencelinksyntax_syntax_proto-Messages-SubmitResponse"></a>SubmitResponse

<!-- @@protoc_insertion_point(message_header_scope:engine.protoc.markdown.example.referencelinksyntax.SubmitResponse) -->

<!-- @@protoc_insertion_point(message_scope:engine.protoc.markdown.example.referencelinksyntax.SubmitResponse) -->

#### <a id="engine_protoc_markdown_example_referencelinksyntax_syntax_proto-Messages-SubmitResponse-Field_Summary"></a>Field Summary

|Name|Type|Description|
|---|---|---|
|result|string||

#### <a id="engine_protoc_markdown_example_referencelinksyntax_syntax_proto-Messages-SubmitResponse-Field_Details"></a>Field Details

##### <a id="engine_protoc_markdown_example_referencelinksyntax_syntax_proto-Messages-SubmitResponse-Field_Details-result"></a>result

<!-- @@protoc_insertion_point(file_footer) -->
