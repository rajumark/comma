package io.github.rajumark.hoverfly.comma.sample

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.rajumark.hoverfly.comma.Comma
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { CommaTheme { CommaScreen() } }
    }
}

private val EXAMPLES = listOf(
    "hi rahul are you free tomorrow i need help with the report",
    "kal milte hain ok bye take care",
    "can you pick up the kids at 5 i am stuck in a meeting",
    "bhai kahan hai tu sab wait kar rahe hain",
    "आप कैसे हैं मैं ठीक हूँ",
    "dear sir i am writing to request leave for two days thank you",
)

/** Result of one inference, with its wall-clock time. */
private class Result(val text: String, val micros: Long)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CommaScreen() {
    val context = LocalContext.current.applicationContext

    // Loading reads the model: do it once, off the main thread.
    val comma by produceState<Comma?>(null) {
        value = withContext(Dispatchers.Default) { Comma(context) }
        awaitDispose { value?.close() }
    }
    var input by remember { mutableStateOf(EXAMPLES[0]) }

    val result by produceState<Result?>(null, comma, input) {
        val c = comma ?: return@produceState
        value = withContext(Dispatchers.Default) {
            val t0 = System.nanoTime()
            val r = c.restore(input)
            Result(r, (System.nanoTime() - t0) / 1000)
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Comma") }) }) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Voice-typed or unpunctuated text") },
                minLines = 3,
                trailingIcon = {
                    if (input.isNotEmpty()) IconButton(onClick = { input = "" }) { Icon(Icons.Filled.Clear, "Clear") }
                },
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
                    Text(
                        "${r.micros} µs",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Text("Try", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EXAMPLES.forEach { SuggestionChip(onClick = { input = it }, label = { Text(it, maxLines = 1) }) }
            }
            Text(
                "Runs on this device. No network, no permission.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun CommaTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val ctx = LocalContext.current
    val colors = when {
        Build.VERSION.SDK_INT >= 31 -> if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialTheme(colorScheme = colors, content = content)
}
