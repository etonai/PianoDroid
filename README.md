# PianoDroid

A lightweight Android starting point for apps that play piano sounds.

PianoDroid provides a clean, buildable app shell with a dark Material 3 theme, a single placeholder screen, and a ready-to-use audio engine built on `SoundPool`. The intent is to eliminate setup friction when starting a new Android project that needs piano playback — clone it, rename the package, and start building features.

## What's Included

- **Dark theme** — Material 3 `darkColorScheme` with a defined set of color tokens; no light variant
- **App shell** — `MainActivity`, a `NavHost`-based navigation structure, and a centered placeholder screen
- **AudioEngine** — `SoundPool`-based piano playback, four base `.ogg` samples (`C4`, `Ds4`, `Fs4`, `A4`), semitone-to-pitch mapping, and a `suspend fun awaitLoaded()` for coroutine-friendly initialization
- **Modern stack** — Kotlin 2.0, Compose BOM 2024.12, Navigation Compose, AGP 8.11, Gradle 8.13, `minSdk 26`

## Getting Started

1. Clone the repo and open it in Android Studio.
2. Run `assembleDebug` to confirm the project builds.
3. Rename the package from `com.pseddev.pianodroid` to your own identifier.
4. Replace the app title, tagline, and color tokens in `ui/theme/` and `ui/MainScreen.kt`.
5. Start adding screens and wiring `AudioEngine` into your features.

## Project Structure

```
app/src/main/
├── assets/notes/          # C4.ogg, Ds4.ogg, Fs4.ogg, A4.ogg
├── java/com/pseddev/pianodroid/
│   ├── audio/AudioEngine.kt
│   ├── ui/
│   │   ├── theme/Color.kt
│   │   ├── theme/Theme.kt
│   │   └── MainScreen.kt
│   ├── MainActivity.kt
│   └── Navigation.kt
```

## Audio Engine

`AudioEngine` loads four base samples and uses pitch-shifting to cover all 12 semitones:

```kotlin
val engine = AudioEngine(context)
engine.awaitLoaded()              // suspend until all samples are ready
engine.playNote(semitone = 4)     // plays E4
engine.playNote(semitone = 7, octaveRate = 2f)  // plays G5
```

Semitone `0` = C, `1` = C#/Db, ... `11` = B. `octaveRate` shifts by octave (default `1f`). Rate is clamped to `[0.5f, 2.0f]`.

## Planning

This project uses a lightweight DevCycle planning workflow. See `doc/planning/DevelopmentProcess.md` for how work is organized.
