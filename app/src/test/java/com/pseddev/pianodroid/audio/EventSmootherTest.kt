package com.pseddev.pianodroid.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class EventSmootherTest {

    private val noteA = AudioEvent.Note("A4")
    private val noteB = AudioEvent.Note("B4")

    @Test
    fun `a new event displays only after two consecutive frames`() {
        val smoother = EventSmoother()
        assertEquals(AudioEvent.Silence, smoother.next(noteA))
        assertEquals(noteA, smoother.next(noteA))
    }

    @Test
    fun `an attack transient settles into the note`() {
        val smoother = EventSmoother()
        assertEquals(AudioEvent.Silence, smoother.next(AudioEvent.Noise))
        assertEquals(AudioEvent.Silence, smoother.next(noteA))
        assertEquals(noteA, smoother.next(noteA))
    }

    @Test
    fun `a note holds through brief unpitched gaps`() {
        val smoother = EventSmoother()
        smoother.next(noteA)
        smoother.next(noteA)
        assertEquals(noteA, smoother.next(AudioEvent.Noise))
        assertEquals(noteA, smoother.next(AudioEvent.Silence))
        assertEquals(noteA, smoother.next(AudioEvent.Noise))
        // The fourth unpitched frame exceeds the hold and clears the display.
        assertEquals(AudioEvent.Silence, smoother.next(AudioEvent.Silence))
    }

    @Test
    fun `decay through noise reaches silence`() {
        val smoother = EventSmoother()
        smoother.next(noteA)
        smoother.next(noteA)
        repeat(3) { assertEquals(noteA, smoother.next(AudioEvent.Noise)) }
        assertEquals(AudioEvent.Noise, smoother.next(AudioEvent.Noise))
        assertEquals(AudioEvent.Noise, smoother.next(AudioEvent.Silence))
        assertEquals(AudioEvent.Silence, smoother.next(AudioEvent.Silence))
    }

    @Test
    fun `flicker between two notes does not switch the display`() {
        val smoother = EventSmoother()
        smoother.next(noteA)
        smoother.next(noteA)
        repeat(4) {
            assertEquals(noteA, smoother.next(noteB))
            assertEquals(noteA, smoother.next(noteA))
        }
        // A sustained new note does switch.
        assertEquals(noteA, smoother.next(noteB))
        assertEquals(noteB, smoother.next(noteB))
    }

    @Test
    fun `reset returns the display to silence`() {
        val smoother = EventSmoother()
        smoother.next(noteA)
        smoother.next(noteA)
        smoother.reset()
        assertEquals(AudioEvent.Silence, smoother.next(AudioEvent.Silence))
    }
}
