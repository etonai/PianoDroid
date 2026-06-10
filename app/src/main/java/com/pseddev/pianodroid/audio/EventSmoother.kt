package com.pseddev.pianodroid.audio

/**
 * Stabilizes the raw per-frame event stream for display.
 *
 * Raw classification flickers in practice: a piano attack is broadband noise for
 * a frame or two before the pitch settles, and decay drifts back through noise.
 * Two rules suppress the flicker:
 * - a new event is displayed only after [confirmFrames] consecutive frames agree
 * - a displayed note or chord is held through up to [holdFrames] unpitched
 *   (noise/silence) frames before clearing
 */
class EventSmoother(
    private val confirmFrames: Int = 2,
    private val holdFrames: Int = 3,
) {

    private var displayed: AudioEvent = AudioEvent.Silence
    private var candidate: AudioEvent? = null
    private var candidateCount = 0
    private var gapCount = 0

    /** Feeds one raw frame event and returns the event to display. */
    fun next(raw: AudioEvent): AudioEvent {
        if (raw == displayed) {
            candidate = null
            candidateCount = 0
            gapCount = 0
            return displayed
        }
        if (displayed.isPitched && !raw.isPitched) {
            gapCount++
            if (gapCount <= holdFrames) return displayed
            switchTo(raw)
            return displayed
        }
        gapCount = 0
        if (raw == candidate) {
            candidateCount++
        } else {
            candidate = raw
            candidateCount = 1
        }
        if (candidateCount >= confirmFrames) switchTo(raw)
        return displayed
    }

    fun reset() {
        switchTo(AudioEvent.Silence)
    }

    private fun switchTo(event: AudioEvent) {
        displayed = event
        candidate = null
        candidateCount = 0
        gapCount = 0
    }
}
