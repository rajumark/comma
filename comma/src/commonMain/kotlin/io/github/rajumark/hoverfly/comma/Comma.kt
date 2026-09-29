package io.github.rajumark.hoverfly.comma

import io.github.rajumark.hoverfly.comma.internal.Featurizer
import io.github.rajumark.hoverfly.comma.internal.Network
import io.github.rajumark.hoverfly.comma.internal.SentencePiece
import io.github.rajumark.hoverfly.comma.internal.Text
import io.github.rajumark.hoverfly.comma.internal.readModelFile

/**
 * On-device punctuation and capitalisation: turns voice-typed or unpunctuated text into readable sentences.
 *
 * ```
 * Comma().use { comma ->
 *     comma.restore("hi rahul are you free tomorrow i need help with the report")
 *     // "Hi Rahul, are you free tomorrow? I need help with the report."
 * }
 * ```
 *
 * Adds commas, full stops, question marks, exclamation marks and capitals, in each script's own form (a full stop
 * is "।" in Hindi, Marathi, Bengali and Punjabi, "۔" in Urdu). English, Hinglish, Hindi and other Indian languages,
 * plus about 15 European and other languages written with spaces between words.
 *
 * Everything runs locally on Android, iOS, macOS, the JVM and the web: the model ships inside the library, there is no network, no permission and no
 * dependency. Creating an instance reads the model (tens of ms), so create it off the main thread and keep it
 * around; [restore] takes a few milliseconds for a chat message and is safe to call from several threads.
 */
public class Comma internal constructor(open: (String) -> ByteArray) : AutoCloseable {

    /** Loads the model bundled in the library. */
    public constructor() : this(::readModelFile)

    private var network: Network? = Network(open("comma.bin"))
    private val featurizer = Featurizer(SentencePiece(open("spm_pieces.tsv").decodeToString()))

    init {
        // The first calls run interpreted; pay that here (off the UI thread) instead of on the first message.
        repeat(WARM_UP) { restore("warm up the model number $it how are you") }
    }

    /**
     * Returns [text] with punctuation and capitals restored. Punctuation already in [text] is replaced by the
     * model's, and the text is re-cased. Whitespace is normalised to single spaces. A blank text returns "".
     */
    public fun restore(text: String): String {
        val words = Text.words(text)
        if (words.isEmpty()) return ""
        val (punct, case) = tagWords(words)
        return Text.render(words, punct, case)
    }

    /** Releases the model (about 25 MB of heap). The instance cannot be used afterwards. */
    override fun close() {
        network = null
    }

    /** Per-word (punctuation, casing) labels. Long text is cut into overlapping windows; each word takes the
     *  prediction of the window where it has the most context on both sides. */
    internal fun tagWords(words: List<String>): Pair<IntArray, IntArray> {
        val net = checkNotNull(network) { "Comma is closed" }
        val punct = IntArray(words.size)
        val case = IntArray(words.size)
        val best = IntArray(words.size) { -1 }
        for ((s, e) in windows(words, net.maxTokens)) {
            val f = featurizer.featurize(words.subList(s, e), net.maxTokens)
            val tags = net.tag(f.tokIds, f.wordIds)
            val n = e - s
            val owner = f.owner
            for (i in owner.indices) {
                val wk = owner[i]
                val g = s + wk
                val centre = minOf(wk + 1, n - wk)
                val last = i + 1 == owner.size || owner[i + 1] != wk
                val first = i == 0 || owner[i - 1] != wk
                if (centre >= best[g] || (last && best[g] < 0)) {
                    if (last) punct[g] = tags.punct[i]
                    if (first) case[g] = tags.case[i]
                    if (last) best[g] = centre
                }
            }
        }
        return punct to case
    }

    /** Labels from a single window (the first MAX_TOKENS tokens), as the reference test vectors are made. */
    internal fun tagFirstWindow(words: List<String>): Pair<IntArray, IntArray> {
        val net = checkNotNull(network) { "Comma is closed" }
        val f = featurizer.featurize(words, net.maxTokens)
        val tags = net.tag(f.tokIds, f.wordIds)
        val n = if (f.owner.isEmpty()) 0 else f.owner.last() + 1
        val punct = IntArray(n)
        val case = IntArray(n)
        for (i in f.owner.indices) {
            val k = f.owner[i]
            if (i + 1 == f.owner.size || f.owner[i + 1] != k) punct[k] = tags.punct[i]
            if (i == 0 || f.owner[i - 1] != k) case[k] = tags.case[i]
        }
        return punct to case
    }

    private fun windows(words: List<String>, maxTokens: Int): List<Pair<Int, Int>> {
        val nTok = IntArray(words.size) { featurizer.tokenCount(words[it]) }
        val out = ArrayList<Pair<Int, Int>>()
        var s = 0
        while (s < words.size) {
            var e = s
            var t = 0
            while (e < words.size && t + nTok[e] <= maxTokens) { t += nTok[e]; e++ }
            e = maxOf(e, s + 1)
            out.add(s to e)
            if (e >= words.size) break
            s = maxOf(e - OVERLAP, s + 1)
        }
        return out
    }

    internal companion object {
        const val WARM_UP = 20
        const val OVERLAP = 12
    }
}
