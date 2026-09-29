package io.github.rajumark.hoverfly.comma.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.rajumark.hoverfly.comma.Comma
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.TimeSource

private val examples = listOf(
    "hi rahul are you free tomorrow i need help with the report",
    "kal milte hain ok bye take care",
    "can you pick up the kids at 5 i am stuck in a meeting",
    "bhai kahan hai tu sab wait kar rahe hain",
    "आप कैसे हैं मैं ठीक हूँ",
    "dear sir i am writing to request leave for two days thank you",
)

/** Result of one inference, with its wall-clock time. */
private class Result(val text: String, val micros: Long)

/** The whole demo: unpunctuated text in, restored text out. [platform] is shown so screenshots say where they ran. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun App(platform: String) {
    MaterialTheme(colorScheme = lightColorScheme()) {
        Surface(Modifier.fillMaxSize()) {
            // Loading reads the model: do it once, off the main thread.
            val comma by produceState<Comma?>(null) { value = withContext(Dispatchers.Default) { Comma() } }
            var input by remember { mutableStateOf(examples[0]) }

            val result by produceState<Result?>(null, comma, input) {
                val c = comma ?: return@produceState
                value = withContext(Dispatchers.Default) {
                    val t0 = TimeSource.Monotonic.markNow()
                    val r = c.restore(input)
                    Result(r, t0.elapsedNow().inWholeMicroseconds)
                }
            }

            Column(
                Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("Comma", style = MaterialTheme.typography.headlineLarge)
                Text(
                    "Kotlin Multiplatform · $platform · io.github.rajumark:comma:$COMMA_VERSION",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Voice-typed or unpunctuated text") },
                    minLines = 3,
                )
                Column(
                    Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(20.dp)).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Restored", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    val r = result
                    if (comma == null || r == null) {
                        Box(Modifier.fillMaxWidth().height(48.dp), Alignment.Center) { CircularProgressIndicator() }
                    } else {
                        Text(r.text.ifEmpty { "—" }, style = MaterialTheme.typography.titleLarge)
                        Text("${r.micros} µs", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text("Try", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    examples.forEach { SuggestionChip(onClick = { input = it }, label = { Text(it, maxLines = 1) }) }
                }
                Text(
                    "Runs on this device. No network, no permission.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

const val COMMA_VERSION = "2.0.0"
