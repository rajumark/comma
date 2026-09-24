# Comma ✍️

By Hoverfly. On-device punctuation and capitalisation for Android. It turns voice-typed or unpunctuated text into
readable sentences: commas, full stops, question marks, exclamation marks and capitals.

```kotlin
import io.github.rajumark.hoverfly.comma.Comma

Comma(context).use { comma ->
    comma.restore("can you pick up the kids at 5 i am stuck in a meeting")
    // "Can you pick up the kids at 5? I am stuck in a meeting."
    comma.restore("आप कैसे हैं मैं ठीक हूँ")
    // "आप कैसे हैं? मैं ठीक हूँ।"
}
```

- **Made for Indian text.** English, Hinglish and Tanglish chat, Hindi, Bengali, Marathi, Gujarati, Punjabi, Tamil,
  Telugu, Kannada, Malayalam and Urdu, plus Spanish, French, German, Portuguese, Italian, Russian and Indonesian.
- **Each script's own marks.** A full stop is `।` in Hindi, Marathi, Bengali and Punjabi and `۔` in Urdu; Urdu
  questions end in `؟`.
- **No dependencies.** Inference is plain Kotlin. There is no ONNX Runtime, TFLite, ML Kit or native code.
- **Private and offline.** The model ships inside the AAR. There is no network, no permission and no telemetry.
- **Fast enough for every message.** About 10 ms per message on an Android emulator; the model loads in about 250 ms.
- **minSdk 21.** Works from Kotlin and Java.

## Install

Available via [JitPack](https://jitpack.io/#rajumark/comma):

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

// build.gradle.kts
dependencies {
    implementation("com.github.rajumark:comma:v1.0.0")
}
```

## Screenshots

The sample app on an emulator. Every result is computed on the device.

| English | Hindi | Hinglish |
|---|---|---|
| ![English](docs/screenshots/comma-english.png) | ![Hindi](docs/screenshots/comma-hindi.png) | ![Hinglish](docs/screenshots/comma-hinglish.png) |
| "Can you pick up the kids at 5? I am stuck in a meeting." | "आप कैसे हैं? मैं ठीक हूँ।" | "Kal milte hain, OK, bye. Take care." |

## Use

```kotlin
val comma = Comma(context)        // loads the model: do it off the main thread, keep one instance

comma.restore("hi rahul are you free tomorrow i need help with the report")
// "Hi, Rahul. Are you free tomorrow? I need help with the report."

comma.restore("OK, bye!!! take care...")   // existing punctuation is replaced, the text re-cased
comma.restore("   ")                        // ""

comma.close()                     // frees the model's heap memory
```

`restore()` is thread-safe. Long text (a whole voice note) is handled in overlapping windows, so every word gets
context on both sides.

With coroutines:

```kotlin
val comma = withContext(Dispatchers.Default) { Comma(context) }
val text = withContext(Dispatchers.Default) { comma.restore(transcript) }
```

From Java:

```java
try (Comma comma = new Comma(context)) {
    String text = comma.restore("kal milte hain ok bye take care");
}
```

### API

| | |
|---|---|
| `Comma(context)` | Loads the bundled model. `Closeable`. |
| `restore(text)` | Returns the text with punctuation and capitals restored. `""` for blank text. |

## Quality

Word-level F1 for the four marks (punctuation) and for capitalised words (capitals), measured on held-out test sets
never used for training, and on 70 hand-written messages in 15 languages.

| | Comma | XLM-R punctuation + true-case (47 languages) | XLM-R base, punctuation only |
|---|---|---|---|
| Hinglish chat, punctuation / capitals | **0.83 / 0.92** | 0.49 / 0.70 | 0.41 / — |
| English chat, punctuation / capitals | **0.86 / 0.96** | 0.72 / 0.91 | 0.75 / — |
| Hand-written messages, punctuation / capitals | **0.74 / 0.89** | 0.70 / 0.75 | 0.69 / — |
| Hand-written messages fully right | **43%** | 26% | — |
| Formal text, 18 languages, punctuation / capitals | 0.65 / **0.79** | **0.76** / 0.66 | 0.73 / — |
| Size | **7.8 MB** | 1.1 GB | 1.1 GB |
| Latency, one message, 1 CPU thread (laptop) | **~0.3 ms** | ~39 ms | ~22 ms |

Comma is clearly better on chat and messages, and capitalises better everywhere. On long formal text (news,
encyclopedia prose) the much larger XLM-R models place commas better.

**Where it falls short:** commas in long formal sentences; "!" vs "." is a judgement call it sometimes gets
differently; some Kannada and Malayalam answers after a question also get a "?". Chinese, Japanese and Thai are not
supported (they don't separate words with spaces).

## Sample app

`sample/` is a Jetpack Compose (Material 3) demo: type or pick unpunctuated text and see it restored, with the time
it took.

```bash
./gradlew :sample:installDebug
```

## Project layout

```
comma/                the library (AAR)
  src/main/assets/comma/        comma.bin (int8 weights) · spm_pieces.tsv (tokenizer)
  src/main/kotlin/io/github/rajumark/hoverfly/comma/          public API: Comma
  src/main/kotlin/io/github/rajumark/hoverfly/comma/internal/ Text, Featurizer, SentencePiece, Network (the model in plain Kotlin)
  src/test/           JVM tests: parity with the reference on 94 vectors, API, long text, latency
  src/androidTest/    the same parity check on a real device (Android ICU)
sample/               demo app
```

## Tests

```bash
./gradlew :comma:testDebugUnitTest                        # JVM: parity + API
./gradlew :comma:connectedDebugAndroidTest                # on a connected device/emulator
```

The parity tests require identical token ids, the same label for every word and the same restored text as the
reference implementation on all 94 vectors (JVM and emulator).

## How it works

Every word is split into SentencePiece tokens, and each token also carries a hash of its whole word. A small
bidirectional transformer (4 layers, 7M parameters, int8 weights with one scale per row) reads the text and predicts,
for each word, the mark after it and its casing. Long text is cut into overlapping windows of 128 tokens.

## Publishing

See [PUBLISHING.md](PUBLISHING.md).

## Pricing & license

**Free for up to 10,000 monthly active devices.** You don't need an API key, an account or a license file: add the dependency and ship. It works in commercial apps too, with no limit on how often each device runs it.

| | Community | Commercial | Custom models |
|---|---|---|---|
| **Price** | Free | Contact us | Contact us |
| **For** | Products with up to 10,000 monthly active devices per platform | Products above 10,000 monthly active devices on any platform | A model trained for your own language, domain or task |
| **Includes** | Commercial use, unlimited calls, no key or sign-up | One license per product per model, direct support, early access to updates | Designed and trained by Hoverfly, shipped as a plain Kotlin library |

**How devices are counted.** A monthly active device is a device that runs Comma at least once in a calendar month. The limit applies separately to each product, each platform (Android, iOS, web…) and each Hoverfly model. Once a product passes it, you have 30 days to get a commercial license. The library keeps working and never checks in with a server.

**Not allowed** under any tier (unless agreed in writing):

- selling or redistributing Comma or its model on its own, or inside another SDK or library
- extracting, modifying, fine-tuning or retraining the model weights
- using the model or its outputs to train or distill another model
- reverse engineering the model or its file format
- offering it as a hosted API for others

**Custom models.** Hoverfly also designs and trains small, fast on-device models for your needs: punctuation, moderation, classification, language detection, smart replies and more.

**Contact** for a commercial license or a custom model: [raju348636@gmail.com](mailto:raju348636@gmail.com) or **+91 63533 21951** (call or WhatsApp).

Full terms: [Hoverfly Community License](LICENSE).
