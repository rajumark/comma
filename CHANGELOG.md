# Changelog

## 2.0.0

- Kotlin Multiplatform: Android, JVM desktop, iOS (arm64 device + simulator), macOS arm64,
  JavaScript and WebAssembly, published to Maven Central as `io.github.rajumark:comma`.
- New constructor `Comma()`: the model ships inside the library on every platform, so no
  `Context` is needed. `Comma(context)` still compiles on Android (deprecated).
- Same model and same results as 1.x; parity with the reference (94 vectors) is tested on every target.
- The sample is now a Compose Multiplatform app (Android, desktop, iOS) plus a web page (JS and Wasm).

## 1.0.0

- First version: `Comma(context).restore(text)` adds commas, full stops, question marks, exclamation marks and
  capitals to unpunctuated or voice-typed text.
- English, Hinglish / Tanglish, Hindi, Bengali, Marathi, Gujarati, Punjabi, Tamil, Telugu, Kannada, Malayalam, Urdu,
  plus Spanish, French, German, Portuguese, Italian, Russian and Indonesian. Script-aware marks (। ۔ ؟ ،).
- Long text in overlapping 128-token windows.
- Pure Kotlin inference with no dependencies, int8 weights (7.5 MB). minSdk 21.
- Hoverfly Community License: free up to 10,000 monthly active devices per product.
