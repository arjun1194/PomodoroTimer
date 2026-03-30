package com.arjun1194.pomodorotimer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.arjun1194.pomodorotimer.MainActivity
import com.arjun1194.pomodorotimer.R
import com.arjun1194.pomodorotimer.TimerStatus
import com.arjun1194.pomodorotimer.data.TimerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TimerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var timerJob: Job? = null

    private var durationMillis = 0L
    private var timeRemaining = 0L
    private var activeRingtone: Ringtone? = null

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_DISMISS_ALARM = "ACTION_DISMISS_ALARM"
        const val EXTRA_DURATION_MILLIS = "EXTRA_DURATION"

        private const val NOTIFICATION_CHANNEL_ID = "timer_channel"
        private const val NOTIFICATION_ID = 1
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                stopAlarm()
                val newDuration = intent.getLongExtra(EXTRA_DURATION_MILLIS, -1L)
                if (newDuration != -1L) {
                    durationMillis = newDuration
                    timeRemaining = newDuration
                }
                startForegroundServiceCompat()
                startTimer()
            }
            ACTION_PAUSE -> {
                stopAlarm()
                pauseTimer()
                updateNotification("Paused")
            }
            ACTION_STOP -> {
                stopAlarm()
                stopTimer()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_DISMISS_ALARM -> {
                stopAlarm()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                
                // Reset State completely
                val currentMode = TimerRepository.mode.value
                val newMode = if (currentMode == com.arjun1194.pomodorotimer.TimerMode.FOCUS) 
                    com.arjun1194.pomodorotimer.TimerMode.BREAK 
                else 
                    com.arjun1194.pomodorotimer.TimerMode.FOCUS
                    
                TimerRepository.updateMode(newMode)
                TimerRepository.updateStatus(TimerStatus.IDLE)
                TimerRepository.updateProgress(0f)
                TimerRepository.updateTimeRemaining(0L)
            }
        }
        return START_STICKY
    }

    private fun startForegroundServiceCompat() {
        val notification = buildNotification("Running...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                // We use specialUse or a fallback if not strictly required
                startForeground(
                    NOTIFICATION_ID, 
                    notification, 
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) 
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE 
                    else 0
                )
            } catch (e: Exception) {
               startForeground(NOTIFICATION_ID, notification) 
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startTimer() {
        TimerRepository.updateStatus(TimerStatus.RUNNING)
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            val targetTime = System.currentTimeMillis() + timeRemaining
            var lastUpdate = -1L

            while (timeRemaining > 0) {
                timeRemaining = targetTime - System.currentTimeMillis()
                if (timeRemaining < 0) timeRemaining = 0

                val progress = 1f - (timeRemaining.toFloat() / durationMillis.toFloat())

                TimerRepository.updateTimeRemaining(timeRemaining)
                TimerRepository.updateProgress(progress)

                val currentSec = timeRemaining / 1000
                if (currentSec != lastUpdate) {
                    lastUpdate = currentSec
                    updateNotification(formatTime(timeRemaining))
                }

                // Rapid update for smooth wave animation
                delay(50)
            }

            TimerRepository.updateStatus(TimerStatus.FINISHED)
            playAlarm()
            
            val mode = TimerRepository.mode.value
            val msg = if (mode == com.arjun1194.pomodorotimer.TimerMode.FOCUS) 
                "Focus time completed 🎯! Take a well-deserved break ☕️" 
            else 
                "Break is over ✨! Let's get back to work 🚀"
                
            val manager = getSystemService(NotificationManager::class.java)
            manager.notify(NOTIFICATION_ID, buildNotification(msg, showDismiss = true))
            // Keeps foreground service alive so notification doesn't die
        }
    }

    private fun pauseTimer() {
        TimerRepository.updateStatus(TimerStatus.PAUSED)
        timerJob?.cancel()
    }

    private fun stopTimer() {
        TimerRepository.updateStatus(TimerStatus.IDLE)
        timerJob?.cancel()
    }

    private fun formatTime(millis: Long): String {
        val totalSeconds = (millis / 1000).toInt()
        val m = totalSeconds / 60
        val s = totalSeconds % 60
        return String.format("%02d:%02d", m, s)
    }

    private fun playAlarm() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(1000)
        }

        try {
            val notificationPlay = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            activeRingtone = RingtoneManager.getRingtone(applicationContext, notificationPlay)
            activeRingtone?.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun buildNotification(text: String, showDismiss: Boolean = false): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Pomodoro Timer",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val pendingIntent = Intent(this, MainActivity::class.java).let { notificationIntent ->
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
        }

        val mode = TimerRepository.mode.value
        val title = if (showDismiss) {
            "Timer Completed ✨"
        } else if (mode == com.arjun1194.pomodorotimer.TimerMode.FOCUS) {
            "Focus.. don't get distracted 🎯"
        } else {
            "Relax and take a break ☕️"
        }

        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            
        if (!showDismiss && mode == com.arjun1194.pomodorotimer.TimerMode.BREAK) {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText("$text\nTake deep breaths, drink water, or go and look out at the nature. 🌿💧"))
        } else if (showDismiss) {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(text))
        }

        if (showDismiss) {
            val dismissIntent = Intent(this, TimerService::class.java).apply {
                action = ACTION_DISMISS_ALARM
            }
            val dismissPendingIntent = PendingIntent.getService(this, 1, dismissIntent, PendingIntent.FLAG_IMMUTABLE)
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Alarm", dismissPendingIntent)
        }

        return builder.build()
    }

    private fun updateNotification(text: String) {
            val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun stopAlarm() {
        activeRingtone?.let {
            if (it.isPlaying) {
                it.stop()
            }
        }
        activeRingtone = null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarm()
        timerJob?.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
