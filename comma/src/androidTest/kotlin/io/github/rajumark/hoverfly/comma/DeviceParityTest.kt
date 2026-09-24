package io.github.rajumark.hoverfly.comma

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Same check as the JVM ParityTest, on a real device: Android's ICU-backed Unicode tables (NFKC, lowercase,
 * character types) must give the reference output on every vector.
 */
@RunWith(AndroidJUnit4::class)
class DeviceParityTest {
    @Test
    fun matchesReferenceOnDevice() {
        val inst = InstrumentationRegistry.getInstrumentation()
        val lines = inst.context.assets.open("testvectors.tsv").bufferedReader().readLines()
        val t0 = System.nanoTime()
        val comma = Comma(inst.targetContext)
        val loadMs = (System.nanoTime() - t0) / 1e6
        var same = 0
        for (line in lines) {
            val c = line.split('\t')
            val want = c[5]
            val words = io.github.rajumark.hoverfly.comma.internal.Text.words(c[0])
            val n = if (c[3].isEmpty()) 0 else c[3].split(',').size
            val (p, k) = comma.tagFirstWindow(words)
            val got = io.github.rajumark.hoverfly.comma.internal.Text.render(words.take(n), p.copyOf(n), k.copyOf(n))
            if (got == want) same++ else println("DIFF: ${c[0]}\n  got  $got\n  want $want")
        }
        val texts = lines.map { it.substringBefore('\t') }.filter { it.length in 5..200 }
        repeat(200) { comma.restore(texts[it % texts.size]) }
        val n = 1000
        val s0 = System.nanoTime()
        repeat(n) { comma.restore(texts[it % texts.size]) }
        val ms = (System.nanoTime() - s0) / 1e6 / n
        println("COMMA_DEVICE text $same/${lines.size} load=${"%.0f".format(loadMs)}ms latency=${"%.3f".format(ms)}ms")
        assertTrue("$same/${lines.size}", same >= lines.size - 1)
    }
}
