package com.pseddev.pianodroid.audio

/** What the listening pipeline heard in one analysis frame. */
sealed class AudioEvent {
    /** No sound above the ambient threshold. */
    data object Silence : AudioEvent()

    /** Sound above the threshold, but no identifiable note or chord. */
    data object Noise : AudioEvent()

    /** A single identified note, e.g. "A4". */
    data class Note(val name: String) : AudioEvent()

    /** An identified chord, e.g. "C major", with its constituent notes low-to-high. */
    data class Chord(val name: String, val notes: List<String>) : AudioEvent()
}

internal val AudioEvent.isPitched: Boolean
    get() = this is AudioEvent.Note || this is AudioEvent.Chord
