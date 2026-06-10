package com.pseddev.pianodroid.audio

import kotlin.math.sqrt

/**
 * Turns a stream of audio chunks into displayable [AudioEvent]s.
 *
 * Each chunk is combined with the previous one into a sliding analysis window
 * (one frame per chunk), gated by RMS, run through [PitchDetector] and
 * [ChordIdentifier], then smoothed by [EventSmoother].
 */
class ListenClassifier(
    private val pitchDetector: PitchDetector = PitchDetector(),
    private val silenceRmsThreshold: Double = 0.005,
    private val smoother: EventSmoother = EventSmoother(),
) {

    val chunkSize: Int = pitchDetector.windowSize / 2

    private var previousChunk = FloatArray(chunkSize)

    /** Feeds one chunk and returns the smoothed event to display. */
    fun process(chunk: FloatArray): AudioEvent = smoother.next(classify(chunk))

    fun reset() {
        previousChunk = FloatArray(chunkSize)
        smoother.reset()
    }

    internal fun classify(chunk: FloatArray): AudioEvent {
        require(chunk.size == chunkSize) { "expected $chunkSize samples, got ${chunk.size}" }
        val window = FloatArray(pitchDetector.windowSize)
        previousChunk.copyInto(window)
        chunk.copyInto(window, chunkSize)
        previousChunk = chunk.copyOf()

        var sumSquares = 0.0
        for (sample in chunk) sumSquares += sample.toDouble() * sample
        if (sqrt(sumSquares / chunk.size) < silenceRmsThreshold) return AudioEvent.Silence

        val notes = pitchDetector.detect(window)
        return when {
            notes.isEmpty() -> AudioEvent.Noise
            notes.size == 1 -> AudioEvent.Note(notes[0].name)
            else -> {
                val chordName = ChordIdentifier.identify(notes)
                if (chordName == null) {
                    AudioEvent.Noise
                } else {
                    AudioEvent.Chord(chordName, notes.sortedBy { it.midi }.map { it.name })
                }
            }
        }
    }
}
