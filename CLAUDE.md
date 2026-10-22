# Volume Limiter

Sideloaded Android app (Kotlin, no AndroidX) that caps the Bluetooth media volume on a Fire HD 10 (Fire OS 7.3.3.1, Android 9). A foreground service clamps `STREAM_MUSIC` (and `STREAM_VOICE_CALL` over Bluetooth SCO) to a percentage set in the app; changing the cap requires a parent password.

## Checks
- `ANDROID_HOME=/opt/android-sdk ./gradlew testDebugUnitTest lintRelease assembleRelease`
- No deploy target besides the GitHub release; there is no Cloudflare component.

## Build and release
- `.github/workflows/build.yml` runs the checks on every push and PR. On pushes to `main` it also publishes a GitHub release `v1.0.<run_number>` with `VolumeLimiter.apk`, but only when the `KEYSTORE_B64` Actions secret is present.
- `KEYSTORE_B64` is the base64 of a PKCS12 keystore with alias `volumelimiter` and store/key password `volumelimiter` (the password is not the secret; the file is). It must stay stable or updates will fail to install over the old version. Never commit it.
- Without `KEYSTORE_PATH` set, release builds are signed with the debug key (local use only).

## Conventions
- Testable logic lives in `VolumeMath`, `PinHasher`, `Lockout` (plain JVM unit tests). Android-facing code is in `LimiterService`, `MainActivity`, `Prefs`.
- A missing preference must mean "capped" (default 50%), never "uncapped".
- Sandbox note: if Maven Central returns 429 in the Claude Code sandbox, mirror it with a local Gradle init script pointing at `https://maven-central.storage-download.googleapis.com/maven2/`; do not change the repo's `settings.gradle.kts`.
