package com.engine.protoc.markdown.example

import com.engine.protoc.markdown.jekyll.ProtocGenMarkdownJekyll
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import java.io.File

/**
 * Shared dev-tool spec for every Jekyll example suite, mirroring [Dumper] for the core plugin.
 * Runs `protoc-gen-markdown-jekyll` over the suite's recorded `code-generator-request.binpb` and
 * writes each emitted insertion-point fragment into the suite's `src/<suite>/resources/` directory.
 *
 * Unlike [Dumper], the plugin's outputs are not standalone documents — each is the small YAML
 * fragment spliced into a document's `frontmatter` insertion point.  They are written to
 * `<file.name>.frontmatter` (e.g. `foo/bar/baz.md.frontmatter`) so a regen `git diff` shows exactly
 * which `title:` / `parent:` keys land on which generated document, without masquerading as the
 * full `.md` the core plugin produces for the same path.
 *
 * Declaring this class `abstract` keeps Kotest from instantiating it directly from testFixtures;
 * each suite contributes a tiny concrete subclass under its own `src/<suite>/kotlin/`.
 */
public abstract class JekyllDumper :
    FunSpec({
        test("dump jekyll frontmatter fragments into the suite's resources directory") {
            val req = JekyllDumper::class.java.getResourceAsStream("/code-generator-request.binpb").shouldNotBeNull()
            val response = ProtocGenMarkdownJekyll.from(req, clock = Dumper.FIXTURE_CLOCK).compile()
            val outDir = File(System.getProperty("dumpDir") ?: "src/test/resources").also { it.mkdirs() }
            for (file in response.fileList) {
                File(outDir, "${file.name}.frontmatter").apply { parentFile?.mkdirs() }.writeText(file.content)
            }
        }
    })
