package com.pseddev.pianodroid.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChordIdentifierTest {

    private fun notes(vararg midis: Int) =
        midis.map { PitchDetector.DetectedNote(it, 0.0, 1.0) }

    @Test
    fun `identifies major and minor triads`() {
        assertEquals("C major", ChordIdentifier.identify(notes(60, 64, 67)))
        assertEquals("A minor", ChordIdentifier.identify(notes(57, 60, 64)))
        assertEquals("F# major", ChordIdentifier.identify(notes(66, 70, 73)))
    }

    @Test
    fun `identifies diminished and augmented triads`() {
        assertEquals("B dim", ChordIdentifier.identify(notes(59, 62, 65)))
        assertEquals("C aug", ChordIdentifier.identify(notes(60, 64, 68)))
    }

    @Test
    fun `identifies seventh chords`() {
        assertEquals("G7", ChordIdentifier.identify(notes(55, 59, 62, 65)))
        assertEquals("C maj7", ChordIdentifier.identify(notes(60, 64, 67, 71)))
        assertEquals("D min7", ChordIdentifier.identify(notes(62, 65, 69, 72)))
        assertEquals("B dim7", ChordIdentifier.identify(notes(59, 62, 65, 68)))
    }

    @Test
    fun `identifies inversions by trying every detected pitch class as root`() {
        // E-G-C: first inversion of C major
        assertEquals("C major", ChordIdentifier.identify(notes(64, 67, 72)))
        // G-C-E: second inversion of C major
        assertEquals("C major", ChordIdentifier.identify(notes(55, 60, 64)))
    }

    @Test
    fun `prefers the bass note as root for symmetric chords`() {
        // The augmented triad matches under all three roots; bass wins.
        assertEquals("C aug", ChordIdentifier.identify(notes(60, 64, 68)))
        assertEquals("E aug", ChordIdentifier.identify(notes(64, 68, 72)))
    }

    @Test
    fun `returns null for non-chords`() {
        assertNull(ChordIdentifier.identify(notes(60, 61, 66))) // cluster
        assertNull(ChordIdentifier.identify(notes(60, 67)))     // bare fifth
        assertNull(ChordIdentifier.identify(notes(60)))         // single note
    }
}
