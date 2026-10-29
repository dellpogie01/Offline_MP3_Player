# MP3 Player

Open the project in Android Studio with Android SDK 36.1 and JDK 21. No account,
API key, Firebase project, or internet connection is needed to use the app.
Gradle may need internet access to download build dependencies on a new machine.

The app shows a three-second MP3 Player splash, then opens Library. Grant Music and audio access (Storage on Android 12 and
earlier), then tap a local song to play. If access is denied, use Grant Permission
to try again. Library and Settings provide a rescan action after adding files.

Playback runs in a Media3 foreground service with system media controls, audio
focus, and headphone-disconnect handling. The UI connects to that same service.
Equalizer and bass settings are saved locally and applied by a PCM audio processor
in the playback service. The five graphic bands and independent bass shelf update
during playback, including in the background. Software processing avoids silent
failures on devices without native Equalizer or BassBoost effects. Smoothed gains
and automatic headroom reduce adjustment clicks and distortion. Presets enable
the equalizer; manual band changes mark the preset Custom. Disabled effects have
disabled sliders. Device speakers and headphones still determine audible bass.

Playlists, playlist membership, favorites, and audio preferences remain in the
existing `sonicbeat_database` Room database, schema version 1. Updating this app
with the same application ID (`com.juanmediahubph.mp3player`) and signing key
preserves that database and local audio files. No database reset or migration is
required for removing accounts. No remote Firebase data is accessed or deleted.

Build and host checks on Windows:

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
```

Device checks (use a test emulator, not a personal device):

```powershell
$env:ANDROID_SERIAL = 'emulator-5554'
adb -s emulator-5554 shell cmd connectivity airplane-mode enable
.\gradlew.bat connectedDebugAndroidTest
adb -s emulator-5554 shell cmd connectivity airplane-mode disable
```

`OfflinePlaybackTest` requires airplane mode, grants media access, creates a
temporary local WAV, checks direct Library startup and local playback, backgrounds
and resumes the activity, exercises media-session pause/play/seek, and checks that
Settings has no account actions. It removes its own audio fixture afterward.
For a fresh-install check, use a fresh emulator or uninstall only the test-emulator
copy before installing; never uninstall a user's copy to validate an upgrade.
