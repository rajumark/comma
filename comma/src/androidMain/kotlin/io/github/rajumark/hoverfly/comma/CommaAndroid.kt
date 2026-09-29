@file:JvmName("CommaAndroid")

package io.github.rajumark.hoverfly.comma

import android.content.Context

/** Kept so 1.x code (`Comma(context)`) still compiles; the model no longer needs a [Context]. */
@Deprecated("The model is bundled without assets now; use Comma().", ReplaceWith("Comma()"))
@Suppress("UNUSED_PARAMETER", "FunctionName")
public fun Comma(context: Context): Comma = Comma()
