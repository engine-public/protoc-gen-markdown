package com.engine.protoc.markdown.example

import com.engine.protoc.markdown.ProtocGenMarkdown
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Concrete Kotest spec for the `referenceLinkSyntax` suite — see [Dumper] for what runs.
 * Adds a guard that `CodeGeneratorResponse.error` is empty so a regression that turns one
 * of the three exercised forms (full reference, escaped brackets, field-target bare name)
 * back into a `FAIL_ON_INVALID` failure trips this suite even when the fixture files would
 * still render successfully.
 */
public class ReferenceLinkSyntaxDumper : Dumper() {
    init {
        test("compile produces no reference-link failures") {
            val req = Dumper::class.java.getResourceAsStream("/code-generator-request.binpb").shouldNotBeNull()
            val response = ProtocGenMarkdown.from(req, clock = FIXTURE_CLOCK).compile()
            response.error shouldBe ""
        }
    }
}
