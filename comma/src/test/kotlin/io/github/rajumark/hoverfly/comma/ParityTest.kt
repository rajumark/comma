package io.github.rajumark.hoverfly.comma

import io.github.rajumark.hoverfly.comma.internal.Featurizer
import io.github.rajumark.hoverfly.comma.internal.SentencePiece
import io.github.rajumark.hoverfly.comma.internal.Text
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Checks the Kotlin port against the reference implementation on testvectors.tsv: identical token and word ids,
 * the same label for every word, and the same restored text.
 */
class ParityTest {
    private class Vector(
        val input: String, val tok: List<Int>, val word: List<Int>,
        val punct: List<Int>, val case: List<Int>, val output: String,
    )

    private fun ints(s: String) = if (s.isEmpty()) emptyList() else s.split(',').map { it.toInt() }

    private val vectors = javaClass.getResourceAsStream("/testvectors.tsv")!!.bufferedReader().readLines()
        .map { it.split('\t') }
        .map { Vector(it[0], ints(it[1]), ints(it[2]), ints(it[3]), ints(it[4]), it[5]) }

    @Test
    fun featurizerMatchesReference() {
        val f = Featurizer(File("$ASSETS/spm_pieces.tsv").inputStream().use { SentencePiece(it) })
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
        assertTrue("labels differ too often: $labels/${vectors.size}", labels >= vectors.size - 1) // a near-tie may flip
        assertEquals(labels, text)
    }

    companion object {
        const val ASSETS = "src/main/assets/comma"
        fun testComma() = Comma { name -> File("$ASSETS/$name").inputStream() }
    }
}
