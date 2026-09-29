package io.github.rajumark.hoverfly.comma.internal

/**
 * Words -> model inputs. Each word is encoded with SentencePiece on its own (so a piece never spans two words);
 * every token also carries its whole word's hashed id. Must produce exactly the reference ids (ParityTest).
 */
internal class Featurizer(private val sp: SentencePiece) {

    /** tokIds / wordIds per token, and the index of the word each token belongs to. */
    class Features(val tokIds: IntArray, val wordIds: IntArray, val owner: IntArray)

    fun featurize(words: List<String>, maxTokens: Int = MAX_TOKENS): Features {
        val tok = ArrayList<Int>()
        val wid = ArrayList<Int>()
        val own = ArrayList<Int>()
        outer@ for ((k, w) in words.withIndex()) {
            val h = wordHash(w)
            for (t in sp.encode(w)) {
                if (tok.size >= maxTokens) break@outer
                tok.add(t); wid.add(h); own.add(k)
            }
        }
        return Features(tok.toIntArray(), wid.toIntArray(), own.toIntArray())
    }

    /** Number of tokens of one word (at least 1), for cutting long text into windows. */
    fun tokenCount(word: String): Int = maxOf(1, sp.encode(word).size)

    companion object {
        const val MAX_TOKENS = 128
        const val N_BUCKETS = 1 shl 15

        fun fnv1a(s: String): Long {
            var h = 0x811C9DC5L
            for (b in s.encodeToByteArray()) {
                h = h xor (b.toLong() and 0xFF)
                h = (h * 0x01000193L) and 0xFFFFFFFFL
            }
            return h
        }

        fun wordHash(w: String): Int = (fnv1a("w:$w") % (N_BUCKETS - 1) + 1).toInt()
    }
}
