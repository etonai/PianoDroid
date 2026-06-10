# DevCycle 001: Listen Screen — Microphone Note and Chord Detection

**Status:** Planning
**Start Date:** 2026-06-10
**Target Completion:** 2026-07-05
**Focus:** Add a Listen screen that uses the microphone to identify piano notes and chords in real time and display them on screen.

---

## Goal

Implement the core listening feature described in `doc/planning/ideas/ListeningIdea.txt`. The user taps a Listen button on the main screen, the app opens a full-screen listening view, captures microphone audio, and identifies what piano note or chord is being played. When audio is detected but no note is identifiable, the screen shows a large light blue dot. When a note or chord is recognized, it is displayed prominently on screen.

## Desired Outcome

A working Listen screen that:
- Is reachable via a single "Listen" button on the main screen
- Listens to the microphone continuously while on screen
- Shows a large light blue dot when sound above the ambient threshold is heard but no note is identified
- Shows the note name (e.g., "A4") or chord name (e.g., "C major") when one is recognized
- Requests `RECORD_AUDIO` permission gracefully and handles the denied case
- Stops listening and releases audio resources when the user navigates away

---

## Tasks

### Phase 1: Main Screen Update

**Status:** Planning

- [ ] Replace the current placeholder content in `MainScreen.kt` with a single centered "Listen" button (dark background, `PrimaryColor` button, `displaySmall` label)
- [ ] Wire the button to navigate to the new Listen screen via `AppNavigation`

**Technical Notes:**
The existing `MainScreen.kt` shows a static title, tagline, version, and "Ready." placeholder. All of that is replaced by a single button. The tagline and version label can be removed entirely — this is the app's permanent main screen, not a placeholder. Navigation already uses a `NavHost`; add a `"listen"` destination alongside `"main"` in `Navigation.kt`.

---

### Phase 2: Microphone Permission

**Status:** Planning

- [ ] Add `<uses-permission android:name="android.permission.RECORD_AUDIO" />` to `AndroidManifest.xml`
- [ ] Create a `PermissionHelper.kt` composable (or utility) that requests `RECORD_AUDIO` at runtime using `rememberLauncherForActivityResult` / `ActivityResultContracts.RequestPermission`
- [ ] In the Listen screen, show a permission rationale message and a "Grant Permission" button if permission is denied; disable listening until granted

**Technical Notes:**
Use Compose's `rememberLauncherForActivityResult` with `RequestPermission`. Check current status with `ContextCompat.checkSelfPermission` on entry. Do not use the deprecated `onRequestPermissionsResult` path. Keep the permission UI simple: if denied, show a short message explaining why the mic is needed and offer a button that re-triggers the request (or opens Settings if permanently denied).

---

### Phase 3: Audio Capture

**Status:** Planning

- [ ] Create `audio/AudioCapture.kt` — wraps `AudioRecord` and emits a continuous stream of PCM float samples as a `Flow<FloatArray>`
- [ ] Use `SAMPLE_RATE = 44100`, `CHANNEL_IN_MONO`, `ENCODING_PCM_FLOAT` (fall back to `ENCODING_PCM_16BIT` if unavailable)
- [ ] Choose a buffer/chunk size based on `AudioRecord.getMinBufferSize` — target ~50 ms chunks (~2048 samples at 44100 Hz)
- [ ] Expose `start()` and `stop()` methods; emit chunks only while started
- [ ] Wire `AudioCapture` lifecycle to the Listen screen composable via a `DisposableEffect` or `LaunchedEffect`

**Technical Notes:**
`AudioRecord` requires a background thread or coroutine dispatcher; run the read loop on `Dispatchers.IO`. The `Flow` should be `callbackFlow` or a simple channel-backed flow. Buffer size: `max(AudioRecord.getMinBufferSize(...), 2048 * bytesPerSample)`. The downstream analysis (Phase 4) will receive chunks as `FloatArray`.

---

### Phase 4: Pitch Detection

**Status:** Planning

- [ ] Implement `audio/PitchDetector.kt` — accepts a `FloatArray` chunk and returns a `List<Float>` of detected frequencies (Hz) above a confidence threshold
- [ ] Use a Cooley-Tukey FFT (pure Kotlin, no external library) to compute the magnitude spectrum
- [ ] Apply a Hann window to the input chunk before FFT to reduce spectral leakage
- [ ] Find magnitude peaks above an adaptive noise floor; return the frequencies of the top peaks (up to 6)
- [ ] Map each detected frequency to the nearest semitone/note name using the equal-temperament formula: `semitone = round(12 * log2(f / 440)) + 69` (MIDI note number), then derive octave and note letter
- [ ] Define an `AudioEvent` sealed class: `Silence`, `Noise` (above threshold, no clear pitch), `Note(name: String)`, `Chord(name: String, notes: List<String>)`
- [ ] Classify each chunk into one of these four events and expose the result

**Technical Notes:**
Piano fundamentals range from ~27.5 Hz (A0) to ~4186 Hz (C8); focus peak detection in that range. Minimum peak prominence and inter-peak distance should be tuned to piano harmonics. Confidence filter: require a peak to be at least 10 dB above the local noise floor. Silence threshold: RMS < ~0.005 (configurable). Noise threshold: RMS ≥ silence threshold but no peaks pass confidence. FFT size: use the next power-of-two ≥ chunk size (e.g., 4096 for 2048-sample chunks) for resolution. Frequency resolution = sampleRate / fftSize ≈ 10.7 Hz per bin at 44100/4096 — adequate for note separation above A1.

---

### Phase 5: Chord Identification

**Status:** Planning

- [ ] Create `audio/ChordIdentifier.kt` — accepts a `List<String>` of note names (e.g., `["C", "E", "G"]`) and returns a chord name string or `null`
- [ ] Support identification of: major, minor, dominant 7th, major 7th, minor 7th, diminished, augmented triads and seventh chords (the 12 most common chord types)
- [ ] Match by normalizing note names to pitch classes (0–11), sorting the set, and comparing against a lookup table of known chord interval patterns
- [ ] If no known chord matches, return `null` (the caller will treat this as `Noise`)
- [ ] Integrate into `PitchDetector` classification: if multiple notes detected and chord identified → `AudioEvent.Chord`; if multiple notes but no chord match → `AudioEvent.Noise`

**Technical Notes:**
Chord lookup: store interval patterns as `Set<Int>` (semitone offsets from root). Try each of the 12 pitch classes as candidate root and check whether the detected pitch classes minus the candidate root match any known pattern. Return the first match as `"<Root> <ChordType>"` (e.g., `"G minor"`). Enharmonic spelling (C# vs Db): use sharps by default for display.

---

### Phase 6: Listen Screen UI

**Status:** Planning

- [ ] Create `ui/ListenScreen.kt` — full-screen dark `Box`, centered content
- [ ] Show a large light blue circle (e.g., 200 dp, `Color(0xFFADD8E6)`, filled) when the current `AudioEvent` is `Noise`
- [ ] Show a large centered text label with the note name (e.g., `"A4"`) in `displayLarge`, `PrimaryColor`, when the event is `Note`
- [ ] Show the chord name (e.g., `"C major"`) in `displaySmall`, `PrimaryColor`, and the individual note names in `bodyMedium`, `MutedText`, below it, when the event is `Chord`
- [ ] Show nothing (dark screen) when the event is `Silence`
- [ ] Show the permission UI (from Phase 2) when `RECORD_AUDIO` is not granted
- [ ] Add a back button (top-left, `NavigationIcon` style) to return to the main screen and stop listening

**Technical Notes:**
Drive the UI from a `ViewModel` (`ListenViewModel.kt`) that collects `AudioCapture` output, feeds it to `PitchDetector`, and exposes a `StateFlow<AudioEvent>`. Use `collectAsStateWithLifecycle()` in the composable. The `ViewModel` starts/stops `AudioCapture` in `onCleared()` and when the composable enters/leaves composition. Avoid holding `AudioCapture` state directly in the composable.

---

## Open Questions

1. **FFT library vs. hand-rolled**
   TarsosDSP is a well-tested Android pitch-detection library that would replace Phases 3–5 almost entirely. However, it adds an external dependency and may include more than we need.
   Recommendation: Implement a hand-rolled Cooley-Tukey FFT for Phase 4. It is ~80 lines of Kotlin, keeps dependencies at zero, and is sufficient for the monophonic and simple polyphonic cases the app targets. Revisit if detection quality is poor after testing.

2. **Chord detection scope for DC-001**
   Full jazz chord detection (9ths, 11ths, sus, add9, etc.) is complex. Limiting to the 12 basic types (major/minor triads and 7th chords + diminished/augmented) covers the majority of beginner piano use.
   Recommendation: Ship DC-001 with the 12 basic types; defer extended chords to a future cycle.

3. **Polyphonic detection accuracy**
   Piano is polyphonic and produces rich harmonics that can confuse peak-picking. The FFT approach may misidentify harmonics as additional notes.
   Recommendation: Accept imperfect detection for DC-001; treat accuracy tuning as a separate cycle. Focus on correctness for clean single-note input first.

---

## Notes and Risks

- `AudioRecord` with `ENCODING_PCM_FLOAT` requires API 21, which is below our `minSdk 26` — no compatibility shim needed.
- Runtime permission denial must be handled gracefully; the app must not crash or freeze if the user taps "Deny."
- The `AudioRecord` buffer loop must not run on the main thread; failure to observe this causes ANRs.
- Chord detection from FFT peaks on a real piano will be imprecise at first. The Noise fallback (light blue dot) gives the UI a safe output for uncertain input.
- No UI sound output is needed in this cycle; `AudioEngine` is not used.

---

## Completion Summary

*Fill in when the cycle closes. Move this document to `doc/planning/completed/` afterward.*

**Completion Date:**
**Phases Completed:**
**Work Deferred:**

**Accomplishments:**

**Metrics:**
- Files created:
- Files modified:

**Lessons / Notes:**
