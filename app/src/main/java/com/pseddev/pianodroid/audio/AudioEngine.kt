package com.pseddev.pianodroid.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.pow

class AudioEngine(context: Context) {

    private val soundPool = SoundPool.Builder()
        .setMaxStreams(8)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )
        .build()

    private val soundIds = IntArray(4)
    private val loadedCount = AtomicInteger(0)
    private val allLoaded = CompletableDeferred<Unit>()

    // semitone (0–11) -> (baseIndex into soundIds, semitone offset from base)
    private val semitoneMap = arrayOf(
        0 to 0, // C
        0 to 1, // Db
        0 to 2, // D
        1 to 0, // Eb
        1 to 1, // E
        1 to 2, // F
        2 to 0, // Gb
        2 to 1, // G
        2 to 2, // Ab
        3 to 0, // A
        3 to 1, // Bb
        3 to 2, // B
    )

    init {
        val assets = context.assets
        soundIds[0] = soundPool.load(assets.openFd("notes/C4.ogg"), 1)
        soundIds[1] = soundPool.load(assets.openFd("notes/Ds4.ogg"), 1)
        soundIds[2] = soundPool.load(assets.openFd("notes/Fs4.ogg"), 1)
        soundIds[3] = soundPool.load(assets.openFd("notes/A4.ogg"), 1)

        soundPool.setOnLoadCompleteListener { _, _, status ->
            if (status == 0 && loadedCount.incrementAndGet() == 4) {
                allLoaded.complete(Unit)
            }
        }
    }

    suspend fun awaitLoaded() = allLoaded.await()

    fun playNote(semitone: Int, octaveRate: Float = 1f, volume: Float = 1f) {
        val (baseIndex, offset) = semitoneMap[semitone % 12]
        val rate = (2.0.pow(offset / 12.0) * octaveRate).toFloat().coerceIn(0.5f, 2.0f)
        soundPool.play(soundIds[baseIndex], volume, volume, 1, 0, rate)
    }

    fun release() {
        soundPool.release()
    }
}
