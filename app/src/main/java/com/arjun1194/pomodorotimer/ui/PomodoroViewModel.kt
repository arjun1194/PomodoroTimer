package com.arjun1194.pomodorotimer.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arjun1194.pomodorotimer.TimerMode
import com.arjun1194.pomodorotimer.TimerStatus
import com.arjun1194.pomodorotimer.data.SettingsRepository
import com.arjun1194.pomodorotimer.data.TimerRepository
import com.arjun1194.pomodorotimer.service.TimerService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PomodoroViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    
    val timeRemaining: StateFlow<Long> = TimerRepository.timeRemaining
    val progress: StateFlow<Float> = TimerRepository.progress
    val status: StateFlow<TimerStatus> = TimerRepository.status
    val mode: StateFlow<TimerMode> = TimerRepository.mode

    val focusDuration = settingsRepository.focusDuration.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsRepository.DEFAULT_FOCUS_DURATION
    )
    val breakDuration = settingsRepository.breakDuration.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsRepository.DEFAULT_BREAK_DURATION
    )

    fun startTimer() {
        viewModelScope.launch {
            val currentMode = mode.value
            val durationMinutes = if (currentMode == TimerMode.FOCUS) focusDuration.value else breakDuration.value
            val durationMillis = durationMinutes * 60 * 1000L

            val intent = Intent(getApplication(), TimerService::class.java).apply {
                action = TimerService.ACTION_START
                // If paused, we resume (don't send new duration to service)
                if (status.value == TimerStatus.PAUSED && timeRemaining.value > 0) {
                    // Resume existing timer
                } else {
                    putExtra(TimerService.EXTRA_DURATION_MILLIS, durationMillis)
                }
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                getApplication<Application>().startForegroundService(intent)
            } else {
                getApplication<Application>().startService(intent)
            }
        }
    }

    fun pauseTimer() {
        val intent = Intent(getApplication(), TimerService::class.java).apply {
            action = TimerService.ACTION_PAUSE
        }
        getApplication<Application>().startService(intent)
    }

    fun stopTimer() {
        val intent = Intent(getApplication(), TimerService::class.java).apply {
            action = TimerService.ACTION_STOP
        }
        getApplication<Application>().startService(intent)
    }

    fun dismissAlarm() {
        val intent = Intent(getApplication(), TimerService::class.java).apply {
            action = TimerService.ACTION_DISMISS_ALARM
        }
        getApplication<Application>().startService(intent)
    }

    fun switchMode() {
        stopTimer()
        val newMode = if (mode.value == TimerMode.FOCUS) TimerMode.BREAK else TimerMode.FOCUS
        TimerRepository.updateMode(newMode)
        TimerRepository.updateProgress(0f)
        TimerRepository.updateTimeRemaining(0L)
    }

    fun updateFocusDuration(minutes: Int) {
        viewModelScope.launch {
            settingsRepository.saveFocusDuration(minutes)
        }
    }

    fun updateBreakDuration(minutes: Int) {
        viewModelScope.launch {
            settingsRepository.saveBreakDuration(minutes)
        }
    }
}
