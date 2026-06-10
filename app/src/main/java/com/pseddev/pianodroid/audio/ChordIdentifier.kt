package com.pseddev.pianodroid.audio

/**
 * Names a chord from a set of detected notes by matching pitch-class interval
 * patterns. Returns null when the notes do not form a recognized chord.
 */
object ChordIdentifier {

    // Quality suffix to semitone offsets from the root.
    private val patterns = listOf(
        " major" to setOf(0, 4, 7),
        " minor" to setOf(0, 3, 7),
        " dim" to setOf(0, 3, 6),
        " aug" to setOf(0, 4, 8),
        "7" to setOf(0, 4, 7, 10),
        " maj7" to setOf(0, 4, 7, 11),
        " min7" to setOf(0, 3, 7, 10),
        " dim7" to setOf(0, 3, 6, 9),
    )

    fun identify(notes: List<PitchDetector.DetectedNote>): String? {
        if (notes.size < 2) return null
        val pitchClasses = notes.map { it.pitchClass }.toSet()
        // Symmetric chords (aug, dim7) match under several roots; trying the
        // bass note first resolves them deterministically.
        val bass = notes.minBy { it.midi }.pitchClass
        val candidateRoots = listOf(bass) + pitchClasses.filter { it != bass }
        for (root in candidateRoots) {
            val intervals = pitchClasses.map { (it - root + 12) % 12 }.toSet()
            for ((suffix, pattern) in patterns) {
                if (intervals == pattern) return NOTE_NAMES[root] + suffix
            }
        }
        return null
    }
}
