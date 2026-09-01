package com.example.wavetune.playback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SleepTimerManager(private val coroutineScope: CoroutineScope) {
    private var timerJob: Job? = null

    private val _remainingSeconds = MutableStateFlow<Long?>(null)
    val remainingSeconds = _remainingSeconds.asStateFlow()

    fun startTimer(minutes: Int, onTimerFinished: () -> Unit) {
        cancelTimer()
        val totalSeconds = minutes * 60L
        _remainingSeconds.value = totalSeconds

        timerJob = coroutineScope.launch(Dispatchers.Default) {
            var currentSec = totalSeconds
            while (isActive && currentSec > 0) {
                delay(1000L)
                currentSec -= 1
                _remainingSeconds.value = currentSec
            }
            if (currentSec <= 0) {
                _remainingSeconds.value = null
                launch(Dispatchers.Main) {
                    onTimerFinished()
                }
            }
        }
    }

    fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
        _remainingSeconds.value = null
    }

    val isRunning: Boolean
        get() = _remainingSeconds.value != null
}
