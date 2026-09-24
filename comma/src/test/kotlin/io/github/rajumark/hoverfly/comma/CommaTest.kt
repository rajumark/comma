package io.github.rajumark.hoverfly.comma

import io.github.rajumark.hoverfly.comma.internal.Text
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommaTest {
    private val comma = ParityTest.testComma()

    @Test
    fun restoresEnglishChat() {
        val out = comma.restore("hi rahul are you free tomorrow i need help with the report")
        println(out)
        assertTrue(out, out.startsWith("Hi") && out.contains("Rahul"))
        assertTrue(out, out.contains("?"))
        assertTrue(out, out.endsWith("."))
    }

    @Test
    fun usesDandaForHindi() {
        val out = comma.restore("आप कैसे हैं मैं ठीक हूँ")
        println(out)
        assertTrue(out, out.endsWith("।"))
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
        assertTrue(out, out.last() in ".?!")
    }

    @Test(expected = IllegalStateException::class)
    fun closedInstanceThrows() {
        val c = ParityTest.testComma()
        c.close()
        c.restore("hello")
    }

    @Test
    fun latency() {
        val texts = listOf("hi rahul are you free tomorrow i need help with the report", "kal milte hain ok bye take care",
            "can you pick up the kids at 5 i am stuck in a meeting", "आप कैसे हैं मैं ठीक हूँ",
            "thanks so much see you on monday")
        repeat(300) { comma.restore(texts[it % texts.size]) }
        val n = 2000
        val t0 = System.nanoTime()
        repeat(n) { comma.restore(texts[it % texts.size]) }
        val ms = (System.nanoTime() - t0) / 1e6 / n
        println("JVM latency: %.3f ms per message".format(ms))
        assertTrue("too slow: $ms ms", ms < 20.0)
    }
}
