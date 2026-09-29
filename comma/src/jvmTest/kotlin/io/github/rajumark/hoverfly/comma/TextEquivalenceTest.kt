package io.github.rajumark.hoverfly.comma

import io.github.rajumark.hoverfly.comma.internal.Text
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

/** The common text rules (hand-written instead of java.util.regex) must match the 1.x ones exactly. */
class TextEquivalenceTest {
    private val pieces = listOf(
        "hi", "Rahul", "U.S.", "e.g.", "3.5", "www.", "http://", "https://", "x.com", ".in", ".info", ".org", ".net", ".comé",
        ".com_", ".com5", ".coḿ", ".com٣", "@", "a@b.com", ",", ".", "?", "!", "...", "?!", "।", "॥", "،", "؟", "۔", "。", "，",
        "？", "！", "…", "\"", "'", "(", ")", "«", "»", "-", "—", " ", "  ", "\t", "\n", " ", "　", " ", "\u0085",
        "नमस्ते", "مرحبا", "ٹھیک", "你好", "こんにちは", "😀", "Σ", "ΣΑΣ", "ß", "İ", "ﬁ", "①", "Ⅻ", "_", "\uD800",
    )

    private fun randomText(rnd: Random) = buildString { repeat(rnd.nextInt(1, 14)) { append(pieces[rnd.nextInt(pieces.size)]) } }

    @Test
    fun wordsMatch() {
        val rnd = Random(3)
        repeat(200_000) {
            val s = randomText(rnd)
            assertEquals(ReferenceText.words(s), Text.words(s), "input: ${s.map { it.code.toString(16) }}")
        }
    }

    @Test
    fun renderMatches() {
        val rnd = Random(4)
        repeat(100_000) {
            val words = Text.words(randomText(rnd) + " " + randomText(rnd))
            if (words.isEmpty()) return@repeat
            val punct = IntArray(words.size) { rnd.nextInt(5) }
            val case = IntArray(words.size) { rnd.nextInt(3) }
            assertEquals(ReferenceText.render(words, punct, case), Text.render(words, punct, case), "words: $words")
        }
    }
}
