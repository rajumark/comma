package io.github.rajumark.hoverfly.comma

import io.github.rajumark.hoverfly.comma.internal.Text
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.Test
import kotlin.time.TimeSource
import kotlin.test.assertFailsWith

class CommaTest {
    private val comma = ParityTest.testComma()

    @Test
    fun restoresEnglishChat() {
        val out = comma.restore("hi rahul are you free tomorrow i need help with the report")
        println(out)
        assertTrue(out.startsWith("Hi") && out.contains("Rahul"), out)
        assertTrue(out.contains("?"), out)
        assertTrue(out.endsWith("."), out)
    }

    @Test
    fun usesDandaForHindi() {
        val out = comma.restore("आप कैसे हैं मैं ठीक हूँ")
        println(out)
        assertTrue(out.endsWith("।"), out)
    }

    @Test
    fun blankGivesEmpty() {
        assertEquals("", comma.restore(""))
        assertEquals("", comma.restore("   \n\t "))
    }

    @Test
    fun replacesExistingPunctuation() {
        assertEquals(comma.restore("ok bye take care"), comma.restore("OK, bye!!! take care..."))
    }

    @Test
    fun longTextIsCoveredEntirely() {
        val text = (1..60).joinToString(" ") { "this is sentence number $it and it goes on" }
        val out = comma.restore(text)
        assertEquals(Text.words(text).size, out.split(' ').size)
        assertTrue(out.last() in ".?!", out)
    }

    @Test
    fun closedInstanceThrows() {
        val c = Comma()
        c.close()
        assertFailsWith<IllegalStateException> { c.restore("hello") }
    }

    @Test
    fun latency() {
        val texts = listOf("hi rahul are you free tomorrow i need help with the report", "kal milte hain ok bye take care",
            "can you pick up the kids at 5 i am stuck in a meeting", "आप कैसे हैं मैं ठीक हूँ",
            "thanks so much see you on monday")
        repeat(300) { comma.restore(texts[it % texts.size]) }
        val n = 2000
        val t0 = TimeSource.Monotonic.markNow()
        repeat(n) { comma.restore(texts[it % texts.size]) }
        val ms = t0.elapsedNow().inWholeNanoseconds / 1e6 / n
        println("latency: ${fmt(ms, 3)} ms per message")
        assertTrue(ms < 200, "too slow: $ms ms") // generous: Kotlin/Native test binaries are unoptimized debug builds
        val l0 = TimeSource.Monotonic.markNow()
        Comma().close()
        println("load: ${l0.elapsedNow().inWholeMilliseconds} ms")
    }
}
