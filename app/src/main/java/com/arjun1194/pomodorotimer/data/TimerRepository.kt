package com.arjun1194.pomodorotimer.data

import com.arjun1194.pomodorotimer.TimerMode
import com.arjun1194.pomodorotimer.TimerStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object TimerRepository {
    private val _timeRemaining = MutableStateFlow(0L) // in milliseconds
    val timeRemaining: StateFlow<Long> = _timeRemaining.asStateFlow()

    private val _progress = MutableStateFlow(0f) // 0.0 to 1.0 (empty to full)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _status = MutableStateFlow(TimerStatus.IDLE)
    val status: StateFlow<TimerStatus> = _status.asStateFlow()

    private val _mode = MutableStateFlow(TimerMode.FOCUS)
    val mode: StateFlow<TimerMode> = _mode.asStateFlow()

    fun updateTimeRemaining(timeMillis: Long) {
        _timeRemaining.value = timeMillis
    }

    fun updateProgress(progress: Float) {
        _progress.value = progress
    }

    fun updateStatus(status: TimerStatus) {
        _status.value = status
    }

    fun updateMode(mode: TimerMode) {
        _mode.value = mode
    }
}
