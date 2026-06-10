# New App Blueprint

This document describes a brand-new Android app that is separate from PlayChords but is designed to share its look, feel, and audio approach. It starts with no functionality — just a shell — but is structured so that features can be layered in over time, with piano-note playback as the first milestone.

---

## Purpose

Build a new Android app from scratch. It should feel like PlayChords — same visual design language, same audio approach — but carry its own identity, name, and feature set. It must not contain any code from PlayChords.

---

## Technology Stack

Match PlayChords exactly so the two apps can grow in parallel without toolchain confusion.

| Concern | Choice |
|---|---|
| Language | Kotlin |
| UI framework | Jetpack Compose + Material 3 |
| Navigation | Navigation Compose (`NavHost`) |
| ViewModel | `lifecycle-viewmodel-compose` |
| Audio | Android `SoundPool` |
| Min SDK | 26 |
| Target / Compile SDK | 35 |
| AGP | 8.11.1 |
| Gradle | 8.13 |
| JDK | 21 |
| Kotlin JVM target | 11 |

Build features to enable: `compose = true`, `buildConfig = true`.

---

## Visual Design Language

### Color Palette

Copy these exact color values into a `Color.kt` file (rename the theme wrapper to match the new app's name):

| Token | Hex | Role |
|---|---|---|
| `DarkBackground` | `#0D0D0D` | Screen background |
| `SurfaceColor` | `#1A1A2E` | Cards, chips, surface elements |
| `PrimaryColor` | `#7B83FF` | Primary buttons, active highlights |
| `SecondaryColor` | `#03DAC6` | Secondary accents |
| `AccentColor` | `#FFBB33` | High-emphasis display text (e.g., large chord name) |
| `OnSurface` | `#DDDDDD` | Body text on surfaces |
| `OnBackground` | `#FFFFFF` | Text on dark background |
| `MutedText` | `#888888` | Labels, metadata, de-emphasized text |

Use `darkColorScheme` (Material 3). No light theme needed at this stage.

### Layout Conventions

- Full-screen dark background (`fillMaxSize` + `background(colorScheme.background)`)
- Content centered vertically and horizontally with a `Box` + `Alignment.Center` pattern
- `Column` with `spacedBy(16.dp)` for vertical button stacks
- Screen-level horizontal padding: `32.dp`
- Buttons: `fillMaxWidth()`, `height(56.dp)`
- Primary action: filled `Button` with `containerColor = colorScheme.primary`
- Secondary actions: `OutlinedButton` with `color = colorScheme.primary` text

### Typography

Use Material 3 defaults with these conventions:
- App title: `displaySmall`, `FontWeight.Bold`, primary color
- Subtitle / tagline: `bodyMedium`, `MutedText`
- Version label: `labelSmall`, `MutedText` at 50% alpha
- Section headings: `headlineSmall`, `FontWeight.Bold`, `onBackground`
- Info badges (key/bpm style): `labelSmall` label + `labelLarge` bold primary value, inside a `SurfaceColor` chip

### Main Screen Header

The top of the main screen must always display these three elements in order, centered horizontally:

1. **App title** — `displaySmall`, `FontWeight.Bold`, primary color
2. **Description** — one short tagline describing what the app does, `bodyMedium`, `MutedText`
3. **Version** — `"v${BuildConfig.VERSION_NAME}"`, `labelSmall`, `MutedText` at 50% alpha

These three are grouped together at the top of the content column, above the `Spacer` that separates the header from any buttons or content below. The `Spacer` height should be `48.dp`.

Enable `buildConfig = true` in the `buildFeatures` block so `BuildConfig.VERSION_NAME` is available.

### Edge-to-Edge

Call `enableEdgeToEdge()` in `MainActivity.onCreate` before `setContent`.

---

## Audio Engine

### Approach

Use `SoundPool` (not `MediaPlayer`, not ExoPlayer). Load a small set of base piano note samples from `assets/notes/`, then pitch-shift them at playback time to cover all 12 semitones.

### Sample Files

Four base `.ogg` samples are enough to cover the full chromatic scale with minimal pitch-shifting artifacts:

| File | Note |
|---|---|
| `C4.ogg` | C4 (covers C, Db, D with +0/+1/+2 semitone shift) |
| `Ds4.ogg` | Eb4 (covers Eb, E, F) |
| `Fs4.ogg` | Gb4 (covers Gb, G, Ab) |
| `A4.ogg` | A4 (covers A, Bb, B) |

Place them in `app/src/main/assets/notes/`.

Use the same `.ogg` files that PlayChords uses — they are not code and can be copied freely.

### Pitch-Shifting Formula

`SoundPool.play()` accepts a `rate` parameter (0.5 = one octave down, 2.0 = one octave up). To shift up by `n` semitones from the base sample:

```
rate = 2^(n / 12)  (clamped to [0.5, 2.0])
```

### SoundPool Configuration

```kotlin
SoundPool.Builder()
    .setMaxStreams(8)
    .setAudioAttributes(
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
    )
    .build()
```

### Semitone-to-Base Mapping

| Semitone | Note | Base sample | Offset (semitones) |
|---|---|---|---|
| 0 | C | C4 | 0 |
| 1 | Db | C4 | 1 |
| 2 | D | C4 | 2 |
| 3 | Eb | Ds4 | 0 |
| 4 | E | Ds4 | 1 |
| 5 | F | Ds4 | 2 |
| 6 | Gb | Fs4 | 0 |
| 7 | G | Fs4 | 1 |
| 8 | Ab | Fs4 | 2 |
| 9 | A | A4 | 0 |
| 10 | Bb | A4 | 1 |
| 11 | B | A4 | 2 |

### Loading

Load all four samples in `init`. Track load completion with `setOnLoadCompleteListener` by counting loaded samples. Before playing, suspend in a loop until all four are loaded:

```kotlin
private suspend fun awaitLoaded() {
    while (loadedCount < 4) delay(50)
}
```

### Note-to-Semitone Map

```
C→0, C#→1, Db→1, D→2, D#→3, Eb→3,
E→4, F→5, F#→6, Gb→6, G→7, G#→8,
Ab→8, A→9, A#→10, Bb→10, B→11
```

### Playing a Single Note

```kotlin
fun playNote(semitone: Int, octaveRate: Float, volume: Float) {
    val (baseIdx, offset) = semitoneToBase[semitone % 12]
    val rate = (2.0.pow(offset / 12.0) * octaveRate).toFloat().coerceIn(0.5f, 2.0f)
    soundPool.play(baseSoundIds[baseIdx], volume, volume, 1, 0, rate)
}
```

`octaveRate = 1.0f` plays in octave 4. `octaveRate = 0.5f` drops one octave (useful for bass notes).

### Cleanup

Call `soundPool.release()` when the engine is no longer needed (e.g., in `onDestroy` or a `DisposableEffect`).

---

## Project Structure (Starting Shell)

```
app/
  src/
    main/
      assets/
        notes/
          C4.ogg
          Ds4.ogg
          Fs4.ogg
          A4.ogg
      java/com/<newpackage>/
        MainActivity.kt
        Navigation.kt
        audio/
          AudioEngine.kt        ← copy the approach, not the file
        ui/
          MainScreen.kt
          theme/
            Color.kt
            Theme.kt
      res/
        values/
          strings.xml
          themes.xml
        drawable/               ← new launcher icon
```

The starting `MainScreen.kt` can be a single centered column with the app name and a placeholder message — no navigation needed until the first feature is added.

---

## What the Shell Must NOT Contain

- No chord progression data
- No song type logic (I Want, I Love, Comedy, etc.)
- No training screens
- No BPM / key selection flows
- No references to the `com.playchords` package

The shell is purely: theme + one placeholder screen + AudioEngine ready to play individual notes.

---

## First Feature Milestone (Future Work)

Once the shell exists, the natural first feature is: tap a button on screen, hear a piano note play. That milestone validates the audio pipeline end-to-end before any music-theory logic is added.
