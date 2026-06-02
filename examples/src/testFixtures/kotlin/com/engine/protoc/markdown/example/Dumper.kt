package com.engine.protoc.markdown.example

import com.engine.protoc.markdown.ProtocGenMarkdown
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * Shared dev-tool spec for every example suite.  Runs the compiler over the suite's recorded
 * `code-generator-request.binpb` and writes each emitted `.md` file straight into the
 * suite's `src/<suite>/resources/` directory, overwriting whatever is there.  Not assertion-bearing
 * — just a one-shot regenerator.  Each example suite contributes a tiny concrete subclass under
 * its own `src/<suite>/kotlin/` so Kotest picks the spec up and runs the dump once per suite;
 * declaring this class `abstract` keeps Kotest from instantiating it directly from testFixtures.
 *
 * The compiler is invoked with a pinned [Clock] ([FIXTURE_CLOCK]) so the `generated:` field in
 * every emitted document's frontmatter is stable across regen runs; without this every
 * regeneration would churn the committed fixtures with a fresh wall-clock timestamp.
 *
 * Out path is resolved via the `dumpDir` system property (wired by `examples/build.gradle.kts`)
 * so this works regardless of the test task's working directory.
 */
public abstract class Dumper :
    FunSpec({
        test("dump documents into the suite's resources directory") {
            val req = Dumper::class.java.getResourceAsStream("/code-generator-request.binpb").shouldNotBeNull()
            val response = ProtocGenMarkdown.from(req, clock = FIXTURE_CLOCK).compile()
            val outDir = File(System.getProperty("dumpDir") ?: "src/test/resources").also { it.mkdirs() }
            for (file in response.fileList) {
                File(outDir, file.name).apply { parentFile?.mkdirs() }.writeText(file.content)
            }
        }
    }) {
    public companion object {
        /** Pinned instant used as the `generated` field for every fixture document. */
        public val FIXTURE_CLOCK: Clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC)
    }
}
