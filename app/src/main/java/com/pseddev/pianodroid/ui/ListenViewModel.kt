package com.pseddev.pianodroid.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pseddev.pianodroid.audio.AudioCapture
import com.pseddev.pianodroid.audio.AudioEvent
import com.pseddev.pianodroid.audio.ListenClassifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

class ListenViewModel : ViewModel() {

    private val classifier = ListenClassifier()

    /**
     * The current displayable event. Capture runs only while this flow has
     * subscribers (WhileSubscribed with no timeout), so the microphone is
     * released as soon as the screen stops collecting — on back navigation
     * or app backgrounding.
     */
    val event: StateFlow<AudioEvent> = AudioCapture(chunkSize = classifier.chunkSize)
        .stream()
        .onStart { classifier.reset() }
        .map { chunk -> classifier.process(chunk) }
        .flowOn(Dispatchers.Default)
        .catch { emit(AudioEvent.Silence) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(0), AudioEvent.Silence)
}
