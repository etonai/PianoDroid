package com.pseddev.pianodroid.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * Captures microphone audio and emits fixed-size PCM float chunks.
 *
 * The [AudioRecord] is created when the flow is collected and released when
 * collection is cancelled, so collectors fully control the microphone lifecycle.
 * Requires the RECORD_AUDIO permission to be granted before collection.
 */
class AudioCapture(
    private val sampleRate: Int = SAMPLE_RATE,
    private val chunkSize: Int = CHUNK_SIZE,
) {

    fun stream(): Flow<FloatArray> = flow {
        val minBuffer = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_FLOAT,
        )
        val record = AudioRecord.Builder()
            .setAudioSource(MediaRecorder.AudioSource.MIC)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .build()
            )
            .setBufferSizeInBytes(maxOf(minBuffer, chunkSize * Float.SIZE_BYTES * 2))
            .build()
        try {
            check(record.state == AudioRecord.STATE_INITIALIZED) { "Microphone is unavailable" }
            record.startRecording()
            val buffer = FloatArray(chunkSize)
            while (true) {
                var filled = 0
                while (filled < chunkSize) {
                    val read = record.read(buffer, filled, chunkSize - filled, AudioRecord.READ_BLOCKING)
                    check(read > 0) { "Microphone read failed: $read" }
                    filled += read
                }
                emit(buffer.copyOf())
            }
        } finally {
            runCatching { record.stop() }
            record.release()
        }
    }.flowOn(Dispatchers.IO)

    companion object {
        const val SAMPLE_RATE = 44100
        const val CHUNK_SIZE = 4096
    }
}
