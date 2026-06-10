package com.pseddev.pianodroid.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class PitchDetectorTest {

    private val detector = PitchDetector()
    private val sampleRate = 44100

    private fun synthesize(vararg components: Pair<Double, Double>): FloatArray {
        val window = FloatArray(detector.windowSize)
        for (i in window.indices) {
            var sample = 0.0
            for ((frequency, amplitude) in components) {
                sample += amplitude * sin(2.0 * PI * frequency * i / sampleRate)
            }
            window[i] = sample.toFloat()
        }
        return window
    }

    @Test
    fun `detects single notes across the piano range`() {
        val cases = mapOf(
            65.41 to 36,    // C2
            110.00 to 45,   // A2
            261.63 to 60,   // C4
            440.00 to 69,   // A4
            1046.50 to 84,  // C6
            2093.00 to 96,  // C7
        )
        for ((frequency, expectedMidi) in cases) {
            val notes = detector.detect(synthesize(frequency to 1.0))
            assertEquals("at $frequency Hz", listOf(expectedMidi), notes.map { it.midi })
        }
    }

    @Test
    fun `suppresses harmonics of a single note`() {
        // A3 with partials at 2x, 3x, 4x — like a real piano note
        val window = synthesize(220.0 to 1.0, 440.0 to 0.5, 660.0 to 0.33, 880.0 to 0.25)
        val notes = detector.detect(window)
        assertEquals(listOf(57), notes.map { it.midi })
        assertEquals("A3", notes[0].name)
    }

    @Test
    fun `detects the three notes of a triad`() {
        val window = synthesize(261.63 to 1.0, 329.63 to 0.9, 392.0 to 0.8)
        val midis = detector.detect(window).map { it.midi }.sorted()
        assertEquals(listOf(60, 64, 67), midis)
    }

    @Test
    fun `note names include the octave`() {
        val notes = detector.detect(synthesize(261.63 to 1.0))
        assertEquals("C4", notes.single().name)
    }

    @Test
    fun `silent window yields no notes`() {
        assertTrue(detector.detect(FloatArray(detector.windowSize)).isEmpty())
    }
}
