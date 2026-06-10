# DevCycle 001: Listen Screen — Microphone Note and Chord Detection

**Status:** In Progress
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
- Shows the note name (e.g., "A4") or chord name (e.g., "C major") when one is recognized, without flickering between states on every analysis frame
- Requests `RECORD_AUDIO` permission gracefully and handles the denied case
- Stops listening and releases audio resources when the user navigates away or the app goes to the background
- Has unit-tested pitch detection, chord identification, and event classification (testable without a microphone)

---

## Tasks

### Phase 1: Main Screen Update

**Status:** Work Complete

- [x] Replace the current placeholder content in `MainScreen.kt` with a single centered "Listen" button (dark background, `PrimaryColor` button, `displaySmall` label)
- [x] Wire the button to navigate to the new Listen screen via `AppNavigation`

**Technical Notes:**
The existing `MainScreen.kt` shows a static title, tagline, version, and "Ready." placeholder. All of that is replaced by a single button. The tagline and version label can be removed entirely — this is the app's permanent main screen, not a placeholder. Navigation already uses a `NavHost`; add a `"listen"` destination alongside `"main"` in `Navigation.kt` and pass a navigate callback into `MainScreen`.

---

### Phase 2: Microphone Permission

**Status:** Work Complete

- [x] Add `<uses-permission android:name="android.permission.RECORD_AUDIO" />` to `AndroidManifest.xml`
- [x] In the Listen screen, request `RECORD_AUDIO` at runtime using `rememberLauncherForActivityResult` with `ActivityResultContracts.RequestPermission`
- [x] Show a rationale message and a "Grant Permission" button when permission is denied; do not start audio capture until granted
- [x] If the request can no longer be shown (permanently denied), have the button open the app's system Settings page instead

**Technical Notes:**
Check current status with `ContextCompat.checkSelfPermission` on entry; request once automatically on first entry to the screen. Keep the permission logic inside `ListenScreen.kt` for now — extract a helper only if a second screen ever needs a permission. Detecting "permanently denied" reliably requires `shouldShowRequestPermissionRationale` via the Activity; an acceptable simplification is to always offer both "Try Again" and "Open Settings" on the denied state.

---

### Phase 3: Audio Capture

**Status:** Work Complete

- [x] Create `audio/AudioCapture.kt` — wraps `AudioRecord` and emits a continuous stream of PCM samples as a `Flow<FloatArray>`
- [x] Use `SAMPLE_RATE = 44100`, `CHANNEL_IN_MONO`, `ENCODING_PCM_FLOAT`
- [x] Emit fixed 4096-sample chunks (~93 ms at 44100 Hz); buffer size at least `max(AudioRecord.getMinBufferSize(...), 4096 * 4 bytes)`
- [x] Run the read loop on `Dispatchers.IO` inside a `callbackFlow` (or channel-backed flow); stop and release the `AudioRecord` when the flow collector is cancelled

**Technical Notes:**
`ENCODING_PCM_FLOAT` is available from API 23, below our `minSdk 26`, so no 16-bit fallback path is needed. Tying the `AudioRecord` lifecycle to flow collection (start on first collect, release in `awaitClose`) means the ViewModel controls capture simply by collecting or cancelling — no separate `start()`/`stop()` API to keep in sync. Verify `AudioRecord.state == STATE_INITIALIZED` after construction and fail the flow cleanly if not (some emulators lack mic support).

*As implemented:* a plain `flow { }` with `flowOn(Dispatchers.IO)` turned out simpler than `callbackFlow` — `AudioRecord.read(..., READ_BLOCKING)` is just a blocking loop, `emit()` provides the cancellation point, and a `finally` block (rather than `awaitClose`) stops and releases the recorder. Each emission is a defensive `copyOf()` of the reusable read buffer.

---

### Phase 4: Pitch Detection

**Status:** Work Complete

- [x] Implement `audio/Fft.kt` — in-place iterative Cooley-Tukey radix-2 FFT, pure Kotlin, no external dependency
- [x] Implement `audio/PitchDetector.kt` — maintains a sliding 8192-sample analysis window (two most recent 4096-sample chunks); each new chunk produces one analysis frame
- [x] Apply a Hann window before the FFT to reduce spectral leakage
- [x] Find magnitude peaks above an adaptive noise floor (peak must be ≥ 10 dB above the local median magnitude); limit search to the C2–C7 range (~65–2093 Hz)
- [x] Refine each peak frequency with parabolic (quadratic) interpolation over the peak bin and its two neighbors
- [x] Suppress harmonics: discard any peak whose frequency is within ~3% of an integer multiple (2×–6×) of a stronger, lower-frequency retained peak
- [x] Map each retained frequency to a MIDI note number via `midi = round(12 * log2(f / 440)) + 69`; reject candidates more than ±40 cents from the nearest semitone; derive note letter and octave (sharps for display)
- [x] Return the retained notes (up to 6) ordered by peak magnitude
- [x] Unit tests: synthesized sine waves at known frequencies (single notes across C2–C7), a synthesized note with artificial harmonics (must yield one note), and a synthesized triad (must yield three notes)

**Technical Notes:**
Resolution math: at 44100 Hz an 8192-point FFT gives ~5.4 Hz per bin. Raw bin resolution alone is insufficient below roughly A3 (semitone spacing at A2 = 110 Hz is only ~6.5 Hz), which is why parabolic interpolation is required — for isolated peaks it refines the estimate to a small fraction of a bin, comfortably covering C2 and up. Do not rely on zero-padding for resolution; it interpolates the spectrum but adds no information. The 8192-sample window (~186 ms) with a 4096-sample hop yields an analysis frame every ~93 ms, fast enough to feel live. Harmonic suppression is essential, not optional: a single piano C4 has strong partials at C5, G5, and C6 and would otherwise be misread as a chord on every keypress.

*As implemented* (defaults in `PitchDetector`): search range 60–2200 Hz (small margins around C2/C7), `noiseFloorDb = 10` above the in-range median, `relativeFloorDb = 25` below the strongest peak, harmonic tolerance 3% on 2×–6× multiples, ±40 cents semitone rejection, max 6 notes. The relative floor does the real work on clean input: with a near-silent background the median is almost zero, so the median-based threshold rejects nothing — but a Hann window's first sidelobes sit ~31.5 dB below their main lobe, and the 25 dB relative floor rejects them while keeping genuine chord notes (typically within ~20 dB of the strongest). Parabolic interpolation is done on log magnitudes with the delta clamped to ±0.5 bin. The sliding window lives in `ListenClassifier` (which concatenates the previous and current chunk), keeping `PitchDetector.detect(window)` a pure function — easier to test. Note mapping dedupes peaks that round to the same MIDI note, keeping the stronger.

---

### Phase 5: Chord Identification

**Status:** Work Complete

- [x] Create `audio/ChordIdentifier.kt` — accepts detected notes (with octaves), reduces them to a pitch-class set (0–11), and returns a chord name string or `null`
- [x] Support exactly these 8 chord types: major, minor, diminished, augmented triads; dominant 7th, major 7th, minor 7th, diminished 7th
- [x] Match by trying each detected pitch class as a candidate root and comparing the interval set against a lookup table of the 8 patterns; prefer the root that equals the lowest detected note (bass note) when multiple roots match
- [x] If no pattern matches, return `null`
- [x] Unit tests: each of the 8 types in several roots, inversions (e.g., E-G-C → C major), and non-chord sets returning `null`

**Technical Notes:**
Store interval patterns as `Set<Int>` (semitone offsets from root), e.g., major = `{0, 4, 7}`, dominant 7th = `{0, 4, 7, 10}`. Augmented and diminished-7th chords are symmetric, so multiple roots match — the bass-note preference resolves this deterministically. Display as `"<Root> <quality>"` (e.g., "G minor", "C# dim"); sharps for display, consistent with Phase 4.

*As implemented:* a stateless `object`; candidate roots are restricted to the detected pitch classes (every supported pattern contains interval 0, so the root must be one of the played notes), tried bass-first. Quality display strings: " major", " minor", " dim", " aug", "7" (dominant, rendered as "G7"), " maj7", " min7", " dim7". Two-note intervals (e.g., a bare fifth) intentionally return `null` and display as the Noise dot.

---

### Phase 6: Event Classification and Smoothing

**Status:** Work Complete

- [x] Define a sealed class `AudioEvent`: `Silence`, `Noise`, `Note(name: String)`, `Chord(name: String, notes: List<String>)`
- [x] Create `audio/ListenClassifier.kt` — combines RMS gating, `PitchDetector`, and `ChordIdentifier` into one raw event per analysis frame:
  - RMS below silence threshold (~0.005, configurable) → `Silence`
  - RMS above threshold, no notes pass confidence → `Noise`
  - exactly one note → `Note`
  - 2+ notes with a chord match → `Chord`; 2+ notes with no match → `Noise`
- [x] Add temporal smoothing on top of the raw stream: a new event is displayed only after it persists for 2 consecutive frames (~190 ms), and the current `Note`/`Chord` display is held through brief `Noise`/`Silence` gaps of up to ~3 frames before clearing
- [x] Unit tests for the smoothing logic using scripted raw-event sequences (attack transient → note settles; note decays through noise to silence; flicker between two notes)

**Technical Notes:**
Raw per-frame output flickers in practice: a piano attack is broadband (classified `Noise`) for a frame or two before the pitch settles, and decay drifts back through `Noise`. Without hysteresis the screen would strobe between the dot and the note name on every keypress. The smoothing layer is plain Kotlin operating on event sequences, so it is fully unit-testable without audio. Keep thresholds as constructor parameters with the defaults above so they can be tuned during Phase 8 without code restructuring.

*As implemented:* smoothing lives in its own class, `audio/EventSmoother.kt`, fed by `ListenClassifier.process()` — separating it made the scripted-sequence tests trivial. Exact semantics: a frame matching the displayed event resets all counters; a pitched display holds through up to `holdFrames = 3` unpitched frames and then clears *immediately* on the next one (no extra confirmation); every other transition requires `confirmFrames = 2` consecutive agreeing frames. One accepted quirk: a pitched candidate's confirmation count survives interleaved unpitched gap frames (Note B, Noise, Note B confirms B), which reads as correct behavior for a noisy transition between notes.

---

### Phase 7: Listen Screen UI

**Status:** Work Complete

- [x] Add a `ListenDot` color token (`Color(0xFFADD8E6)`, light blue) to `ui/theme/Color.kt`
- [x] Create `ui/ListenViewModel.kt` — collects `AudioCapture`, runs chunks through `ListenClassifier`, and exposes a `StateFlow<AudioEvent>`; capture starts when the screen is visible and stops when it is not
- [x] Create `ui/ListenScreen.kt` — full-screen dark `Box`, centered content, rendering by event:
  - `Silence` → dark screen, small `MutedText` hint (e.g., "Listening…")
  - `Noise` → 200 dp filled circle in `ListenDot`
  - `Note` → note name (e.g., "A4") in `displayLarge`, `PrimaryColor`
  - `Chord` → chord name in `displaySmall`, `PrimaryColor`, with the constituent notes below in `bodyMedium`, `MutedText`
- [x] Show the permission UI (Phase 2) when `RECORD_AUDIO` is not granted
- [x] Add a back button (top-left icon button) returning to the main screen

**Technical Notes:**
Implemented with a plain `ViewModel` rather than the planned `AndroidViewModel` — `AudioRecord` turned out to need no `Context` at all. Capture lifecycle is `stateIn(viewModelScope, SharingStarted.WhileSubscribed(0), Silence)` as planned: the cold capture flow only runs while the UI collects, so back-navigation and app backgrounding both release the microphone. Classification runs on `Dispatchers.Default` (FFT off the main thread); a `.catch` collapses mic failures to `Silence` instead of crashing. Permission re-check on `ON_RESUME` (via `LifecycleEventObserver`) picks up grants made from the system Settings page.

---

### Phase 8: On-Device Verification and Tuning

**Status:** In Progress

- [x] Run the unit test suite (`testDebugUnitTest`) and confirm all DSP/classification tests pass
- [ ] Install on a physical device; verify permission grant and deny flows
- [ ] Play single piano notes (real piano, keyboard, or piano app on a second device) across at least C3–C6 and confirm correct note display
- [ ] Play several basic triads and confirm chord display; note misidentifications
- [ ] Verify the light blue dot appears for non-pitched sound (talking, tapping) and the screen clears in silence
- [ ] Verify display stability (no rapid flicker during a held note's attack and decay)
- [ ] Tune RMS thresholds, peak confidence (dB), and smoothing frame counts as needed; record final values in this document
- [ ] Confirm audio capture stops when navigating back and when the app is backgrounded (no mic-in-use indicator lingering)

**Technical Notes:**
Emulator microphone passthrough from the host is unreliable; use a physical device for the audio checks. DC-000 deferred its emulator verification — this cycle should not close with verification deferred again, since detection quality is the entire feature. Expect to iterate on thresholds here; that is why they are parameterized in Phases 4 and 6.

All 21 unit tests pass (`testDebugUnitTest`: PitchDetector 5, ChordIdentifier 6, EventSmoother 6, ListenClassifier 4) and `assembleDebug` builds clean. CLI build note: this machine's `JAVA_HOME` points to JDK 24, which Gradle 8.13 cannot run on ("Type T not present" configuration failure). Build from the CLI with `$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.7.6-hotspot'` set first; Android Studio is unaffected (uses its bundled JBR). The remaining device checks require a physical device and user verification.

---

## Decisions

Recorded at planning time; revisit only if testing shows they were wrong.

1. **Hand-rolled FFT, no external DSP library.** TarsosDSP could replace Phases 4–6 but adds a dependency, targets monophonic pitch tracking in its standard detectors, and obscures tuning. An iterative radix-2 FFT is ~60–80 lines of Kotlin and fully unit-testable. Revisit if detection quality remains poor after Phase 8 tuning.
2. **Chord scope: 8 basic types.** Major/minor/diminished/augmented triads plus dominant/major/minor/diminished 7ths. Extended and suspended chords (9ths, 11ths, sus2/sus4, add9) are deferred to a future cycle.
3. **Detection range C2–C7, not the full piano.** Below C2 the partials crowd within the FFT's resolving power even with interpolation; above C7 fundamentals are weak and musically rare. The range is a named constant and can be widened later.
4. **Accuracy expectations.** Polyphonic piano detection from FFT peaks is approximate. DC-001 aims for: reliable single notes in C3–C6, reasonable detection of cleanly-voiced triads, and graceful fallback to the Noise dot otherwise. Accuracy improvement beyond that is future-cycle work.

---

## Notes and Risks

- **Risk: harmonic confusion.** Even with suppression, real piano partials are slightly inharmonic (stretched), so the ~3% multiple tolerance may need widening during Phase 8. Mitigation: tolerance is a parameter; the Noise dot is the safe fallback for uncertain frames.
- **Risk: room acoustics and mic quality** vary widely across devices; thresholds tuned on one device may need a second device check. Mitigation: adaptive noise floor in Phase 4 rather than absolute magnitude thresholds.
- The `AudioRecord` read loop must never run on the main thread (ANR risk); Phase 3 pins it to `Dispatchers.IO`.
- Runtime permission denial must not crash or soft-lock the screen; the denied state is a designed UI state, not an error.
- `AudioEngine` (playback) is not used in this cycle; no changes to it.
- Unit tests live under `app/src/test/` and require no emulator; if `testImplementation(junit)` is not already in `app/build.gradle.kts`, add it in Phase 4.

---

## Implementation Record (2026-06-10)

Implementation of Phases 1–7 plus the unit-test portion of Phase 8 is complete. On-device verification remains.

**Results:**
- 21 unit tests, all passing, covering pitch detection (synthesized sines across C2–C7, harmonic suppression, triads), chord identification (all 8 types, inversions, symmetric-chord root preference, non-chords), display smoothing (scripted event sequences), and the end-to-end classifier (silence / tone / triad / cluster fed as phase-continuous chunks)
- `assembleDebug` builds clean with no warnings
- Debug APK now builds as `PianoDroid-debug.apk` (`base.archivesName = "PianoDroid"` in `app/build.gradle.kts`) instead of the default `app-debug.apk`
- `versionName` bumped to `0.1.0`

**Files created (13):**
- `audio/`: `AudioCapture.kt`, `Fft.kt`, `PitchDetector.kt`, `ChordIdentifier.kt`, `AudioEvent.kt`, `EventSmoother.kt`, `ListenClassifier.kt`
- `ui/`: `ListenViewModel.kt`, `ListenScreen.kt`
- `app/src/test/`: `PitchDetectorTest.kt`, `ChordIdentifierTest.kt`, `EventSmootherTest.kt`, `ListenClassifierTest.kt`

**Files modified (6):** `MainScreen.kt`, `Navigation.kt`, `ui/theme/Color.kt`, `AndroidManifest.xml`, `app/build.gradle.kts`, `gradle/libs.versions.toml`

**Dependencies added:** `lifecycle-runtime-compose` (for `collectAsStateWithLifecycle` and the non-deprecated `LocalLifecycleOwner`), `junit` (test only). Still zero external DSP dependencies.

**Lessons learned:**
- Gradle 8.13 cannot run on JDK 24 — fails at configuration with "Type T not present". This machine's `JAVA_HOME` points to JDK 24, so CLI builds need `JAVA_HOME` overridden to the Temurin 21 install (see Phase 8 notes). Android Studio is unaffected.
- On clean input the adaptive *median* noise floor rejects almost nothing (the median of a quiet spectrum is ~0); the floor *relative to the strongest peak* is what actually filters spurious peaks. 25 dB was chosen because Hann sidelobes sit ~31.5 dB down while real chord notes stay within ~20 dB.
- Test signals fed as consecutive chunks must be phase-continuous (generate one long signal, then split). Independently generated chunks have a phase discontinuity at the window seam that smears the spectrum.
- `LocalLifecycleOwner` in `androidx.compose.ui.platform` is deprecated; the replacement lives in `androidx.lifecycle.compose` (lifecycle-runtime-compose artifact).
- `AudioRecord` needs no `Context`, so a plain `ViewModel` suffices — the planned `AndroidViewModel` was unnecessary.
- Keeping every pipeline stage except `AudioCapture` free of Android imports made the whole DSP/classification chain testable on the JVM with plain JUnit — no Robolectric, no emulator.

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
