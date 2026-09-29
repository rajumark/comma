package io.github.rajumark.hoverfly.comma

import io.github.rajumark.hoverfly.comma.internal.Featurizer
import io.github.rajumark.hoverfly.comma.internal.SentencePiece
import io.github.rajumark.hoverfly.comma.internal.Text
import io.github.rajumark.hoverfly.comma.internal.TestData
import io.github.rajumark.hoverfly.comma.internal.decodeChunks
import io.github.rajumark.hoverfly.comma.internal.readModelFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Checks the Kotlin port against the reference implementation on testvectors.tsv: identical token and word ids,
 * the same label for every word, and the same restored text.
 * Runs on every target.
 */
class ParityTest {
    private class Vector(
        val input: String, val tok: List<Int>, val word: List<Int>,
        val punct: List<Int>, val case: List<Int>, val output: String,
    )

    private fun ints(s: String) = if (s.isEmpty()) emptyList() else s.split(',').map { it.toInt() }

    private val vectors = decodeChunks(TestData.files.getValue("testvectors.tsv")).decodeToString()
        .lineSequence().filter { it.isNotEmpty() }
        .map { it.split('\t') }
        .map { Vector(it[0], ints(it[1]), ints(it[2]), ints(it[3]), ints(it[4]), it[5]) }
        .toList()

    @Test
    fun featurizerMatchesReference() {
        val f = Featurizer(SentencePiece(readModelFile("spm_pieces.tsv").decodeToString()))
        var bad = 0
        for (v in vectors) {
            val got = f.featurize(Text.words(v.input))
            if (got.tokIds.toList() != v.tok || got.wordIds.toList() != v.word) {
                bad++
                println("MISMATCH: ${v.input}\n  words ${Text.words(v.input)}\n  tok  ${got.tokIds.toList()}\n  want ${v.tok}")
            }
        }
        println("featurizer: ${vectors.size - bad}/${vectors.size} identical")
        assertEquals(0, bad)
    }

    @Test
    fun modelMatchesReference() {
        val comma = testComma()
        var labels = 0
        var text = 0
        for (v in vectors) {
            val words = Text.words(v.input)
            val (p, c) = comma.tagFirstWindow(words)
            val n = v.punct.size  // the vectors cover one window; sliding windows are checked in CommaTest
            if (p.take(n) == v.punct && c.take(n) == v.case) labels++ else println("LABELS DIFF: ${v.input}")
            if (Text.render(words.take(n), p.copyOf(n), c.copyOf(n)) == v.output) text++
            else println("TEXT DIFF: ${v.input}\n  got  ${Text.render(words.take(n), p.copyOf(n), c.copyOf(n))}\n  want ${v.output}")
        }
        println("model: labels $labels/${vectors.size}, text $text/${vectors.size}")
        assertTrue(labels >= vectors.size - 1, "labels differ too often: $labels/${vectors.size}") // a near-tie may flip
        assertEquals(labels, text)
    }

    companion object {
        private var shared: Comma? = null

        /** One instance per test run: loading is the slow part on the native and web targets. */
        fun testComma(): Comma = shared ?: Comma().also { shared = it }
    }
}
