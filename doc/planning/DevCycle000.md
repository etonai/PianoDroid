# DevCycle 000: App Shell

**Status:** Work Complete
**Start Date:** 2026-06-08
**Target Completion:** 2026-06-15
**Focus:** Create a buildable Android app shell with the correct theme, one placeholder screen, and a ready-to-use audio engine.

---

## Goal

Stand up the PianoDroid project from scratch. The result is a clean, buildable Android app that shares the visual design language and audio approach of PlayChords but carries its own identity and contains none of PlayChords' code. No features are implemented yet — the shell exists solely to prove the stack is wired up correctly and to provide a stable foundation for layering features on top.

## Desired Outcome

A buildable Android app that:
- Launches on a device or emulator running API 26+
- Displays a single centered screen with the app title, a short tagline, and a version label
- Applies the correct dark color scheme (Material 3, `darkColorScheme`)
- Contains a working `AudioEngine` class ready to load and play piano notes via `SoundPool`
- Bundles the four base `.ogg` sample files in `assets/notes/`
- Contains no chord progression data, song-type logic, training screens, BPM/key flows, or references to `com.playchords`

---

## Tasks

### Phase 1: Gradle Project Setup

**Status:** Work Complete

- [x] Confirm `build.gradle.kts` (app) targets `minSdk = 26`, `compileSdk = 35`, `targetSdk = 35`
- [x] Set AGP to `8.11.1` and Gradle wrapper to `8.13`
- [x] Set `kotlinOptions { jvmTarget = "11" }` and JDK toolchain to 21
- [x] Enable `buildFeatures { compose = true; buildConfig = true }`
- [x] Add Compose BOM, Material 3, Navigation Compose, and `lifecycle-viewmodel-compose` dependencies
- [x] Verify the project syncs and builds without errors

**Technical Notes:**
Used `gradle/libs.versions.toml` (version catalog). Kotlin 2.0.21 with `kotlin.compose` plugin (bundled Compose compiler). BOM `2024.12.01` pins Compose artifact versions. Placeholder `MainActivity.kt` and adaptive launcher icons added to satisfy minimum compilable project requirements; both will be replaced in Phases 2–3. Gradle wrapper jar bootstrapped from cached local Gradle 8.13 distribution. `BUILD SUCCESSFUL` confirmed via `assembleDebug`.

---

### Phase 2: Theme and Color

**Status:** Work Complete

- [x] Create `ui/theme/Color.kt` with the eight color tokens from the blueprint (`DarkBackground`, `SurfaceColor`, `PrimaryColor`, `SecondaryColor`, `AccentColor`, `OnSurface`, `OnBackground`, `MutedText`)
- [x] Create `ui/theme/Theme.kt` with a `darkColorScheme` wrapping those tokens; name the composable wrapper to match the app name
- [x] Apply the theme in `MainActivity` so all screens inherit it
- [x] Confirm no light theme variant is defined

**Technical Notes:**
Color token hex values (from blueprint):
`DarkBackground=#0D0D0D`, `SurfaceColor=#1A1A2E`, `PrimaryColor=#7B83FF`, `SecondaryColor=#03DAC6`, `AccentColor=#FFBB33`, `OnSurface=#DDDDDD`, `OnBackground=#FFFFFF`, `MutedText=#888888`.

---

### Phase 3: Main Screen and Navigation

**Status:** Work Complete

- [x] Create `MainActivity.kt` — call `enableEdgeToEdge()` before `setContent`, wrap content in the app theme
- [x] Create `ui/MainScreen.kt` with a full-screen dark `Box` centered vertically and horizontally containing:
  - App title (`displaySmall`, `FontWeight.Bold`, `PrimaryColor`)
  - Short tagline (`bodyMedium`, `MutedText`)
  - Version label — `"v${BuildConfig.VERSION_NAME}"` (`labelSmall`, `MutedText` at 50% alpha)
  - `Spacer(48.dp)` below the header group
  - A placeholder message below the spacer (e.g., "Ready." or similar) to confirm the screen renders
- [x] Create `Navigation.kt` with a `NavHost` wired to `MainScreen` as the start destination (even though only one screen exists, this satisfies the structural requirement)
- [x] Set `versionName` in `build.gradle.kts` to `"0.1"` for the initial shell
- [ ] Verify the app builds and the screen renders correctly on an emulator

**Technical Notes:**
Screen-level horizontal padding: `32.dp`. Content column uses `spacedBy(16.dp)`. The header group (title + tagline + version) should be a nested `Column` at the top of the main column, separated from the rest by the `48.dp` spacer.

---

### Phase 4: Audio Engine

**Status:** Work Complete

- [x] Create `audio/AudioEngine.kt` implementing `SoundPool`-based playback as described in the blueprint
- [x] Configure `SoundPool` with `maxStreams = 8`, `USAGE_MEDIA`, `CONTENT_TYPE_MUSIC`
- [x] Load the four base samples (`C4.ogg`, `Ds4.ogg`, `Fs4.ogg`, `A4.ogg`) from `assets/notes/` in `init`
- [x] Track load completion with `setOnLoadCompleteListener`; expose `awaitLoaded()` suspend function
- [x] Implement `playNote(semitone: Int, octaveRate: Float, volume: Float)` using the semitone-to-base mapping and pitch-shift formula `rate = 2^(offset/12) * octaveRate`, clamped to `[0.5f, 2.0f]`
- [x] Copy the four `.ogg` files from PlayChords into `app/src/main/assets/notes/`
- [x] Expose `release()` for cleanup; wire it into a `DisposableEffect` or `onDestroy` (TBD at integration time)
- [x] Confirm the class compiles; playback is not tested until the first feature milestone (DC-1)

**Technical Notes:**
Semitone-to-base mapping (semitone → base index, offset):
`C(0)→C4+0`, `Db(1)→C4+1`, `D(2)→C4+2`, `Eb(3)→Ds4+0`, `E(4)→Ds4+1`, `F(5)→Ds4+2`, `Gb(6)→Fs4+0`, `G(7)→Fs4+1`, `Ab(8)→Fs4+2`, `A(9)→A4+0`, `Bb(10)→A4+1`, `B(11)→A4+2`.

Note-to-semitone map: `C→0, C#→1, Db→1, D→2, D#→3, Eb→3, E→4, F→5, F#→6, Gb→6, G→7, G#→8, Ab→8, A→9, A#→10, Bb→10, B→11`.

---

## Open Questions

1. **App name and package identifier**
   The blueprint intentionally leaves the app name and package TBD. A name and package (e.g., `com.example.pianodroid`) must be decided before Phase 1 is started.
   Recommendation: Decide before starting Phase 1 so the package name is correct from the first file.
PianoDroid is the name
the package should be com.pseddev.pianodroid

2. **Where are the PlayChords `.ogg` files located?**
   The blueprint says to copy the four base samples from PlayChords, but the PlayChords project path is not recorded here.
   Recommendation: Confirm the path before starting Phase 4.

temp_assets/notes/


---

## Notes and Risks

- The shell intentionally contains no feature logic. Any feature work (e.g., the piano-note tap milestone) belongs in DC-1 or later.
- `AudioEngine` compilation will be verified in this cycle, but end-to-end audio playback is deferred to the first feature cycle.
- No git operations should be performed without explicit user approval (per project rules).

---

## Completion Summary

*Fill in when the cycle closes. Move this document to `doc/planning/completed/` afterward.*

**Completion Date:** 2026-06-08
**Phases Completed:** 1, 2, 3, 4
**Work Deferred:** Emulator visual verification of Phase 3 (screen rendering); audio playback end-to-end test (deferred to DC-1 per plan)

**Accomplishments:**
- Full dark Material 3 theme with all eight color tokens (`PianoDroidTheme`)
- `MainScreen` composable: title, tagline, version label, "Ready." placeholder
- `AppNavigation` / `NavHost` wired up with `MainScreen` as start destination
- `AudioEngine` compiles: SoundPool, four .ogg samples loaded from assets, pitch-shift formula, `awaitLoaded()` suspend fn
- `BUILD SUCCESSFUL` confirmed via `assembleDebug`

**Metrics:**
- Files created: 5 (`Color.kt`, `Theme.kt`, `MainScreen.kt`, `Navigation.kt`, `AudioEngine.kt`)
- Files modified: 2 (`MainActivity.kt`, `app/build.gradle.kts`)
- Assets copied: 4 (`.ogg` files to `app/src/main/assets/notes/`)

**Lessons / Notes:**
`kotlinx.coroutines.CompletableDeferred` used for `awaitLoaded()` — coroutines available transitively via `lifecycle-viewmodel-compose`.
