package io.github.rajumark.hoverfly.comma.sample

import io.github.rajumark.hoverfly.comma.Comma
import kotlinx.browser.document
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLTextAreaElement
import kotlin.time.TimeSource

/** "Kotlin/JS" or "Kotlin/Wasm". */
expect val runtime: String

private val examples = listOf(
    "hi rahul are you free tomorrow i need help with the report",
    "kal milte hain ok bye take care",
    "can you pick up the kids at 5 i am stuck in a meeting",
    "bhai kahan hai tu sab wait kar rahe hain",
    "आप कैसे हैं मैं ठीक हूँ",
    "dear sir i am writing to request leave for two days thank you",
)

private fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

fun main() {
    fun el(id: String) = document.getElementById(id) as HTMLElement
    val input = document.getElementById("text") as HTMLTextAreaElement
    el("platform").textContent = "Kotlin Multiplatform · $runtime · io.github.rajumark:comma:2.0.0"

    val t0 = TimeSource.Monotonic.markNow()
    val comma = Comma()
    el("load").textContent = "Model loaded in ${t0.elapsedNow().inWholeMilliseconds} ms"

    fun render() {
        val mark = TimeSource.Monotonic.markNow()
        val r = comma.restore(input.value)
        el("out").textContent = r.ifEmpty { "—" }
        el("timing").textContent = "${mark.elapsedNow().inWholeMicroseconds} µs"
    }

    el("examples").innerHTML = examples.joinToString("") { "<button class=\"chip\">${esc(it)}</button>" }
    val list = el("examples").querySelectorAll("button")
    for (i in 0 until list.length) {
        val b = list.item(i) as HTMLElement
        b.onclick = { input.value = b.textContent ?: ""; render(); null }
    }
    input.oninput = { render(); null }
    input.value = examples[0]
    render()
}
