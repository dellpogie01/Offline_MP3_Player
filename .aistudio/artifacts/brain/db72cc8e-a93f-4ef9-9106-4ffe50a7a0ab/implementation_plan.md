# Implementation Plan - SonicBeat - Offline Music Player

## User Review Required
> [!IMPORTANT]
> - **Firebase Auth**: We will configure Firebase Authentication for secure Email/Password registration and login.
> - **Local Music & Permissions**: The app will scan local device storage via Android MediaStore and request necessary runtime media permissions (`READ_MEDIA_AUDIO` / `READ_EXTERNAL_STORAGE`).
> - **Audio Engine**: Powered by Android Media3 ExoPlayer with a foreground playback service, lock-screen controls, and media notification.
> - **Audio Effects**: Real hardware-backed `Equalizer` and `BassBoost` audio effects attached to the active audio session.

- **Theme & Aesthetic**: Neon Blue (`#00E5FF`) accents on Midnight Black (`#121212`) dark theme with Material 3 styling.

---

## Proposed Changes

### 1. Platform Metadata & Gradle Configuration
- Update `metadata.json` with app name "SonicBeat – Offline Music Player".
- Update `app/build.gradle.kts` to ensure all required dependencies are enabled (Room KSP, Media3 ExoPlayer, Firebase Auth, Lifecycle Compose, Material3 Icons Extended).

### 2. Architecture & Data Layer
- **Room Database**: Define entities and DAOs for Playlists, PlaylistSongs, Favorites, and Equalizer/Audio Settings.
- **Repositories**:
  - `MusicRepository`: Queries MediaStore for local audio files (MP3, WAV, AAC, FLAC), extracts metadata (title, artist, album, duration, album art URI).
  - `AuthRepository`: Wraps Firebase Auth for Email/Password sign-up, login, password reset, logout, and current user session.
  - `PlaybackRepository` / `PlayerConnection`: Manages Media3 ExoPlayer state, queue, shuffle, repeat modes, playback position, and background service communication.
  - `EqualizerRepository`: Manages custom equalizer bands, presets, and BassBoost intensity persisted via DataStore or Room.

### 3. Background Playback & Media3 Service
- Implement `MusicPlaybackService` (inheriting `MediaLibraryService` or `ExoPlayer` foreground service) with:
  - Notification media controls.
  - Audio focus handling.
  - Lock-screen session integration.

### 4. UI Screens & Navigation (Jetpack Compose, Material 3)
- **Splash & Auth Flow**:
  - `SplashScreen`: Checks auth session, routes to Login or Home.
  - `LoginScreen` & `RegisterScreen`: Email/Password authentication with input validation and password visibility toggle.
  - `ForgotPasswordScreen`: Password reset functionality.
- **Main Navigation Shell (`Scaffold`)**:
  - Bottom Navigation Bar: Home, Library, Playlists, Settings.
  - Persistent Mini-Player above Bottom Bar (tap to expand to Full Player).
- **Home Screen (`HomeTab`)**:
  - Greeting, search bar ("Find your music"), Recently Played, Recently Added, Quick Categories (Playlists, Favorites, Albums, Artists, Folders).
- **Library Screen (`LibraryTab`)**:
  - Tabs or sections for All Songs, Albums, Artists, Folders, with search filtering and pull-to-refresh rescan.
- **Playlists Screen (`PlaylistsTab`)**:
  - List of user playlists, create playlist dialog, detail view of playlist songs, favorites tab.
- **Full Music Player Screen (`NowPlayingScreen`)**:
  - Large album artwork, title, artist, interactive seek bar, play/pause, prev/next, shuffle, repeat, favorite toggle, queue sheet drawer, audio effects shortcuts.
- **Graphic Equalizer & Bass Boost Screen (`EqualizerScreen`)**:
  - Master enable switch, 5-band vertical sliders (60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz) with decibel indicators, preset dropdown (Normal, Bass, Treble, Pop, Rock, Classical, Jazz, Dance, Vocal), Bass Boost intensity slider, reset button.
- **Settings Screen (`SettingsTab`)**:
  - Theme selection (Light, Dark, System), Equalizer & Bass Boost shortcuts, Audio Library Rescan, Profile & Logout, Clear Local App Data.

---

## Verification Plan

### Automated Tests
- Run unit tests and Robolectric tests via `gradle :app:testDebugUnitTest`.

### Manual Verification
- Compile applet with `compile_applet`.
- Verify user registration & login flow.
- Verify local music library scanning (handling empty state or device files).
- Verify playback controls, mini-player, and full player.
- Verify equalizer and bass boost settings persistence.
