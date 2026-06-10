package com.pseddev.pianodroid.audio

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class ListenClassifierTest {

    private val sampleRate = 44100

    /** Phase-continuous tone split into chunks, as the mic would deliver it. */
    private fun toneChunks(count: Int, chunkSize: Int, vararg frequencies: Double): List<FloatArray> {
        val signal = FloatArray(count * chunkSize)
        for (i in signal.indices) {
            var sample = 0.0
            for (frequency in frequencies) {
                sample += 0.4 * sin(2.0 * PI * frequency * i / sampleRate)
            }
            signal[i] = sample.toFloat()
        }
        return (0 until count).map { signal.copyOfRange(it * chunkSize, (it + 1) * chunkSize) }
    }

    @Test
    fun `silence stays silent`() {
        val classifier = ListenClassifier()
        repeat(5) {
            assertEquals(AudioEvent.Silence, classifier.process(FloatArray(classifier.chunkSize)))
        }
    }

    @Test
    fun `a sustained tone is displayed as its note`() {
        val classifier = ListenClassifier()
        var event: AudioEvent = AudioEvent.Silence
        for (chunk in toneChunks(6, classifier.chunkSize, 440.0)) {
            event = classifier.process(chunk)
        }
        assertEquals(AudioEvent.Note("A4"), event)
    }

    @Test
    fun `a sustained triad is displayed as a chord`() {
        val classifier = ListenClassifier()
        var event: AudioEvent = AudioEvent.Silence
        for (chunk in toneChunks(6, classifier.chunkSize, 261.63, 329.63, 392.0)) {
            event = classifier.process(chunk)
        }
        assertEquals(AudioEvent.Chord("C major", listOf("C4", "E4", "G4")), event)
    }

    @Test
    fun `a loud unidentifiable cluster is displayed as noise`() {
        val classifier = ListenClassifier()
        var event: AudioEvent = AudioEvent.Silence
        // C4 + C#4 + F#4: no chord pattern matches this pitch-class set
        for (chunk in toneChunks(6, classifier.chunkSize, 261.63, 277.18, 369.99)) {
            event = classifier.process(chunk)
        }
        assertEquals(AudioEvent.Noise, event)
    }
}
