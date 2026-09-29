package io.github.rajumark.hoverfly.comma.internal

/**
 * Taking text apart into lowercase words, and putting it back together with marks and capitals. Must behave exactly
 * like the reference implementation; ParityTest checks the words and the rendered output on every test vector.
 *
 * Strings are walked by code point, and whitespace follows Python's str.isspace(). The 1.x version used
 * java.util.regex and java.lang.Character; the same rules are written out here with [UnicodeTables], so every
 * platform gives identical results.
 */
internal object Text {
    const val O = 0
    const val COMMA = 1
    const val PERIOD = 2
    const val QUESTION = 3
    const val EXCLAMATION = 4
    const val LOWER = 0
    const val CAP = 1
    const val UPPER = 2

    /** Trailing marks -> label; the last mark wins ("really?!" -> EXCLAMATION, "wait..." -> PERIOD). */
    private val MARK: Map<Int, Int> = buildMap {
        for (c in ",،、，;؛:") put(c.code, COMMA)
        for (c in ".।॥۔。…|") put(c.code, PERIOD)
        for (c in "?؟？") put(c.code, QUESTION)
        for (c in "!！") put(c.code, EXCLAMATION)
    }
    private const val ASCII_MARKS = ",?!;:"

    /** Stripped from both ends of a word but never predicted. */
    private val DECOR: Set<Int> = "\"'“”‘’«»„‚()[]{}<>*_-–—~`´¿¡•·/\\".map { it.code }.toSet()
    private val TLDS = listOf(".com", ".in", ".org", ".net")

    /**
     * The 1.x regex `(https?://|www\.|@|\.(com|in|org|net)\b)` with Python's `\b` (the reference): after the
     * top-level domain comes the end or a character that is not `[\p{L}\p{N}_]`. (1.x itself differed here between
     * Android's ICU regex and Java 19+, whose `\b` is ASCII-only; now every platform follows the reference.)
     */
    private fun isUrlish(tok: String): Boolean {
        if ("http://" in tok || "https://" in tok || "www." in tok || '@' in tok) return true
        for (tld in TLDS) {
            var i = tok.indexOf(tld)
            while (i >= 0) {
                val end = i + tld.length
                if (end >= tok.length || !UnicodeTables.contains(UnicodeTables.WORD, tok.cpAt(end))) return true
                i = tok.indexOf(tld, i + 1)
            }
        }
        return false
    }

    fun isPySpace(cp: Int): Boolean = UnicodeTables.contains(UnicodeTables.SPACE, cp)

    fun codePoints(s: String): IntArray {
        val out = IntArray(s.cpCount())
        var i = 0
        var k = 0
        while (i < s.length) {
            val cp = s.cpAt(i)
            out[k++] = cp
            i += cpLen(cp)
        }
        return out
    }

    private fun str(cps: IntArray, from: Int, to: Int): String {
        val sb = StringBuilder()
        for (k in from until to) sb.appendCp(cps[k])
        return sb.toString()
    }

    /** '"Rahul,"' -> ("Rahul", COMMA). Abbreviations and numbers keep their inner dots ("U.S." stays, O). */
    private fun splitWord(tok: String): Pair<String, Int> {
        if (isUrlish(tok)) {  // links, emails, handles: only a mark at the very end is punctuation
            val cps = codePoints(tok)
            if (cps.size > 1 && ASCII_MARKS.indexOf(cps.last().toChar()) >= 0 && cps.last() < 128) {
                return str(cps, 0, cps.size - 1) to MARK.getValue(cps.last())
            }
            return tok to O
        }
        val cps = codePoints(tok)
        var i = 0
        var j = cps.size
        while (i < j && cps[i] in DECOR) i++
        var label = O
        while (j > i && (cps[j - 1] in MARK || cps[j - 1] in DECOR)) {
            val m = MARK[cps[j - 1]]
            if (m != null && label == O) label = m
            j--
        }
        val core = str(cps, i, j)
        // "U.S." / "e.g." / "3.5": a dot inside the word means the final dot belongs to it
        if (label == PERIOD && core.contains('.') && j < cps.size && cps[j] == '.'.code && core.cpCount() <= 6) {
            return "$core." to O
        }
        return core to label
    }

    /** Python str.split() on NFKC text: runs of whitespace separate words. */
    private fun whitespaceSplit(t: String): List<String> {
        val out = ArrayList<String>()
        val sb = StringBuilder()
        var i = 0
        while (i < t.length) {
            val cp = t.cpAt(i)
            if (isPySpace(cp)) {
                if (sb.isNotEmpty()) { out.add(sb.toString()); sb.setLength(0) }
            } else sb.appendCp(cp)
            i += cpLen(cp)
        }
        if (sb.isNotEmpty()) out.add(sb.toString())
        return out
    }

    /** Text -> the lowercase words the model reads (predicted punctuation removed). */
    fun words(text: String): List<String> {
        val out = ArrayList<String>()
        for (tok in whitespaceSplit(nfkc(text))) {
            val core = splitWord(tok).first
            if (core.isNotEmpty()) out.add(core.lowercase())
        }
        return out
    }

    // --- rendering ------------------------------------------------------------------------------------
    private const val LATIN = 0
    private const val DANDA = 1
    private const val ARABIC = 2
    private const val URDU = 3
    private const val CJK = 4

    private fun script(word: String): Int {
        for (cp in codePoints(word)) {
            if (cp in 0x0900..0x097F || cp in 0x0980..0x09FF || cp in 0x0A00..0x0A7F) return DANDA
            if (cp in 0x0600..0x06FF || cp in 0x0750..0x077F) return ARABIC
            if (cp in 0x3040..0x30FF || cp in 0x3400..0x9FFF) return CJK
            if (UnicodeTables.contains(UnicodeTables.LETTER, cp)) return LATIN
        }
        return LATIN
    }

    private fun isCjk(c: Int) = c in 0x3040..0x30FF || c in 0x3400..0x9FFF

    private val URDU_LETTERS = "ٹڈڑںےھۓ"

    private val MARKS = arrayOf(
        arrayOf("", ",", ".", "?", "!"),
        arrayOf("", ",", "।", "?", "!"),
        arrayOf("", "،", ".", "؟", "!"),
        arrayOf("", "،", "۔", "؟", "!"),
        arrayOf("", "，", "。", "？", "！"),
    )

    fun applyCase(word: String, case: Int): String = when (case) {
        UPPER -> word.uppercase()
        CAP -> if (word.isEmpty()) word else {
            val cp = word.cpAt(0)
            val n = cpLen(cp)
            cpToString(cp).uppercase() + word.substring(n)
        }
        else -> word
    }

    fun render(words: List<String>, punct: IntArray, case: IntArray): String {
        val urdu = words.any { w -> w.any { URDU_LETTERS.indexOf(it) >= 0 } }
        val sb = StringBuilder()
        for ((k, w) in words.withIndex()) {
            var s = script(w)
            if (s == ARABIC && urdu) s = URDU
            if (k > 0) sb.append(' ')
            sb.append(applyCase(w, case[k]))
            if (punct[k] != O) sb.append(MARKS[s][punct[k]])
        }
        // CJK marks need no space after them (1.x: replace "([，。？！])\\s+(?=[\\u3040-\\u30ff\\u3400-\\u9fff])" with "$1";
        // words hold no whitespace, so the only whitespace here is the single joining space)
        val out = StringBuilder(sb.length)
        for (i in sb.indices) {
            if (sb[i] == ' ' && i > 0 && sb[i - 1] in "，。？！" && i + 1 < sb.length && isCjk(sb[i + 1].code)) continue
            out.append(sb[i])
        }
        return out.toString()
    }
}
