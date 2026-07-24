# Nothing Glyph Matrix SDK (not included)

This app needs `GlyphMatrixSDK.aar`, Nothing's closed-source Glyph Matrix
SDK. **It is intentionally not committed here** — Nothing's SDK licence
prohibits redistributing the binary, so you must download it yourself:

1. Get `glyph-matrix-sdk-2.0.aar` from Nothing's official kit:
   https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit
2. Drop it into this folder and rename it to `GlyphMatrixSDK.aar`:

       app/libs/GlyphMatrixSDK.aar

It is already wired up in `app/build.gradle.kts`
(`implementation(files("libs/GlyphMatrixSDK.aar"))`).

No API key is required; the manifest carries `NothingKey=test` like Nothing's
example project. During development, enable Glyph debug mode once (expires
after 48h, re-run as needed):

    adb shell settings put global nt_glyph_interface_debug_enable 1

`app/src/main/java/com/demetrius/f1glyph/glyph/GlyphMatrixService.kt` is
adapted from Nothing's GlyphMatrix-Example-Project wrapper.

The SDK is licensed by Nothing Technology Ltd under its own EULA (closed
source, non-commercial). See the kit repo above for the full terms.
