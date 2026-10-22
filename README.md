# Volume Limiter

An Android app that stops the volume from going above a limit while Bluetooth earbuds are connected, without needing a kid profile. Built for a Fire HD 10 running Fire OS 7 (Android 9) but works on Android 8.0+.

A web app (PWA) cannot do this, because browsers cannot read or change the system volume. This is a small native app that you install from a GitHub release.

## How it works
- A foreground service watches the volume. When a Bluetooth audio device is connected and the media volume goes above the cap, the service sets it back down immediately. Voice-call volume is limited the same way over Bluetooth hands-free.
- The cap is a percentage of the maximum volume (10% to 100%, in steps of 5%), rounded down to a whole volume step. The default is 50%, and it applies even before a password is set.
- Changing the cap requires the parent password. After 5 wrong guesses the app locks for 30 seconds, doubling each time up to an hour. The password is stored only as a salted PBKDF2-SHA256 hash.
- Optional: also limit the tablet speaker and wired headphones.
- The service restarts after a reboot and after the app is updated.

## Install on the tablet
1. On the tablet, open https://github.com/reloru/Volume-Limiter/releases/latest/download/VolumeLimiter.apk in Silk or Chrome and download it.
2. When asked, allow that browser to "Install unknown apps" (Settings > Security & Privacy > Apps from Unknown Sources), then open the file and install it.
3. Open Volume Limiter, set the parent password, then unlock and pick the cap.
4. Connect the earbuds, press volume up past the cap, and confirm it snaps back. Reboot once and confirm the "Volume limit active" notification returns.

Updates install over the existing app and keep the password and cap, as long as the release is signed with the same key (see below).

## Limits
- The volume can jump one step for a fraction of a second before it snaps back.
- If the earbuds have their own volume buttons that do not report to the tablet (absolute volume off, or "Disable absolute volume" turned on in Developer options), the earbuds' own amplifier is outside the app's reach. Keep that Developer option off, and turn Developer options off after setup so it cannot be changed.
- Force stop or Clear data in Settings > Apps stops the limiter until the tablet reboots or the app is opened. Android does not let a normal app prevent this.
- Behavior on the actual Fire HD 10 has not been tested by the author; the unit tests cover the cap math, password hashing and lockout timing only.

## Build
```
ANDROID_HOME=/path/to/android-sdk ./gradlew testDebugUnitTest lintRelease assembleRelease
```
Requires JDK 17+ and Android SDK platform 34 with build-tools 34.0.0.

## Releases and signing
GitHub Actions (`.github/workflows/build.yml`) builds on every push. Pushes to `main` publish a release only when the repository secret `KEYSTORE_B64` (base64 of the signing keystore) exists, so that every release is signed with the same key and installs as an update.
