package io.github.rajumark.hoverfly.comma.internal

import io.github.rajumark.hoverfly.comma.Comma
import java.text.Normalizer

internal actual fun nfkc(s: String): String = Normalizer.normalize(s, Normalizer.Form.NFKC)

// The model ships as Java resources in the jar/AAR (src/modelData), so no Context or copy is needed.
internal actual fun readModelFile(name: String): ByteArray =
    Comma::class.java.getResourceAsStream("/io/github/rajumark/hoverfly/comma/model/$name")?.use { it.readBytes() }
        ?: error("Comma model file $name is missing from the library jar")
