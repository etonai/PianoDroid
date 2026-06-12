package com.pseddev.pianodroid.audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.log2
import kotlin.math.roundToInt

internal val NOTE_NAMES = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

/**
 * Detects the musical notes present in a window of PCM samples via FFT peak analysis.
 *
 * Pipeline: Hann window -> FFT -> magnitude peaks above an adaptive floor ->
 * parabolic frequency interpolation -> harmonic suppression -> semitone mapping.
 */
class PitchDetector(
    private val sampleRate: Int = 44100,
    val windowSize: Int = 8192,
    private val minFrequency: Double = 60.0,    // just below C2
    private val maxFrequency: Double = 2200.0,  // just above C7
    private val noiseFloorDb: Double = 10.0,    // peak must exceed median magnitude by this much
    private val relativeFloorDb: Double = 25.0, // and be within this range of the strongest peak
    private val harmonicTolerance: Double = 0.03,
    private val maxCentsOff: Double = 40.0,
    private val maxNotes: Int = 6,
) {

    data class DetectedNote(val midi: Int, val frequency: Double, val magnitude: Double) {
        val pitchClass: Int get() = midi % 12
        val name: String get() = NOTE_NAMES[midi % 12] + (midi / 12 - 1)
    }

    private data class Peak(val frequency: Double, val magnitude: Double)

    /** Returns detected notes ordered by peak magnitude, strongest first. */
    fun detect(window: FloatArray): List<DetectedNote> {
        val notes = mutableMapOf<Int, DetectedNote>()
        for (peak in retainedPeaks(window)) {
            val midiExact = 69.0 + 12.0 * log2(peak.frequency / 440.0)
            val midi = midiExact.roundToInt()
            if (abs(midiExact - midi) * 100.0 > maxCentsOff) continue
            val existing = notes[midi]
            if (existing == null || peak.magnitude > existing.magnitude) {
                notes[midi] = DetectedNote(midi, peak.frequency, peak.magnitude)
            }
        }
        return notes.values.sortedByDescending { it.magnitude }.take(maxNotes)
    }

    /**
     * Returns the strongest peak snapped to its nearest semitone, with no tolerance check.
     * Used when [detect] finds no in-tolerance notes but a clear pitch peak exists.
     */
    fun findClosest(window: FloatArray): DetectedNote? {
        val strongest = retainedPeaks(window).maxByOrNull { it.magnitude } ?: return null
        val midiExact = 69.0 + 12.0 * log2(strongest.frequency / 440.0)
        val midi = midiExact.roundToInt()
        return DetectedNote(midi, strongest.frequency, strongest.magnitude)
    }

    private fun retainedPeaks(window: FloatArray): List<Peak> {
        require(window.size == windowSize) { "expected $windowSize samples, got ${window.size}" }

        val re = DoubleArray(windowSize)
        val im = DoubleArray(windowSize)
        for (i in 0 until windowSize) {
            val hann = 0.5 * (1.0 - cos(2.0 * PI * i / (windowSize - 1)))
            re[i] = window[i] * hann
        }
        Fft.transform(re, im)

        val minBin = maxOf(1, (minFrequency * windowSize / sampleRate).toInt())
        val maxBin = minOf(windowSize / 2 - 2, (maxFrequency * windowSize / sampleRate).toInt())
        val mag = DoubleArray(maxBin + 2)
        for (i in 0 until mag.size) {
            mag[i] = hypot(re[i], im[i])
        }

        val inRange = mag.copyOfRange(minBin, maxBin + 1).sorted()
        val median = inRange[inRange.size / 2]
        val maxMagnitude = inRange.last()
        if (maxMagnitude <= 0.0) return emptyList()
        val threshold = maxOf(
            median * dbToRatio(noiseFloorDb),
            maxMagnitude * dbToRatio(-relativeFloorDb),
        )

        val peaks = mutableListOf<Peak>()
        for (i in minBin..maxBin) {
            if (mag[i] < threshold || mag[i] <= mag[i - 1] || mag[i] < mag[i + 1]) continue
            // Parabolic interpolation over log magnitudes refines the peak position
            // well below the ~5.4 Hz bin width.
            val a = ln(mag[i - 1].coerceAtLeast(MIN_MAGNITUDE))
            val b = ln(mag[i].coerceAtLeast(MIN_MAGNITUDE))
            val c = ln(mag[i + 1].coerceAtLeast(MIN_MAGNITUDE))
            val denominator = a - 2.0 * b + c
            val delta = if (denominator == 0.0) 0.0 else (0.5 * (a - c) / denominator).coerceIn(-0.5, 0.5)
            peaks += Peak((i + delta) * sampleRate.toDouble() / windowSize, mag[i])
        }

        // Suppress harmonics: drop any peak sitting at an integer multiple (2x-6x)
        // of a stronger, lower-frequency peak. A single piano note otherwise reads
        // as a chord because of its partials.
        val retained = mutableListOf<Peak>()
        for (peak in peaks.sortedBy { it.frequency }) {
            val isHarmonic = retained.any { lower ->
                val ratio = peak.frequency / lower.frequency
                val multiple = ratio.roundToInt()
                multiple in 2..6 && abs(ratio / multiple - 1.0) <= harmonicTolerance
            }
            if (!isHarmonic) retained += peak
        }
        return retained
    }

    private fun dbToRatio(db: Double): Double = Math.pow(10.0, db / 20.0)

    private companion object {
        const val MIN_MAGNITUDE = 1e-30
    }
}
