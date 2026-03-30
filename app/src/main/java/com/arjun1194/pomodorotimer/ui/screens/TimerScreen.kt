package com.arjun1194.pomodorotimer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arjun1194.pomodorotimer.TimerMode
import com.arjun1194.pomodorotimer.TimerStatus
import com.arjun1194.pomodorotimer.ui.PomodoroViewModel
import com.arjun1194.pomodorotimer.ui.components.WaveBackground

@Composable
fun TimerScreen(
    viewModel: PomodoroViewModel,
    onNavigateToSettings: () -> Unit
) {
    val progress by viewModel.progress.collectAsState()
    val timeRemaining by viewModel.timeRemaining.collectAsState()
    val status by viewModel.status.collectAsState()
    val mode by viewModel.mode.collectAsState()

    val focusDuration by viewModel.focusDuration.collectAsState()
    val breakDuration by viewModel.breakDuration.collectAsState()

    val waveColor = if (mode == TimerMode.FOCUS) {
        Color(0xFFFF5252) // Vibrant Coral/Red
    } else {
        Color(0xFF00BFA5) // Calming Teal/Green
    }

    val displayTime = if (status == TimerStatus.IDLE) {
        val duration = if (mode == TimerMode.FOCUS) focusDuration else breakDuration
        String.format("%02d:00", duration)
    } else {
        formatTime(timeRemaining)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E)) // Dark Theme Background
    ) {
        // Continuous wave background animation
        WaveBackground(
            progress = progress,
            waveColor = waveColor
        )

        // Main Timer Content
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (mode == TimerMode.FOCUS) "Focus" else "Break",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 24.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = displayTime,
                color = Color.White,
                fontSize = 80.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }

        // Floating Controls at the bottom
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 64.dp, start = 32.dp, end = 32.dp)
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            
            // Skip/Switch button (only visible when not running)
            if (status != TimerStatus.RUNNING) {
                IconButton(onClick = { viewModel.switchMode() }) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Skip to Next Mode",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }

            // Main Play/Pause Float Action Button
            FloatingActionButton(
                onClick = {
                    when (status) {
                        TimerStatus.RUNNING -> viewModel.pauseTimer()
                        TimerStatus.FINISHED -> viewModel.dismissAlarm()
                        else -> viewModel.startTimer()
                    }
                },
                containerColor = Color.White,
                contentColor = waveColor,
                modifier = Modifier.size(80.dp)
            ) {
                Icon(
                    imageVector = when (status) {
                        TimerStatus.RUNNING -> Icons.Default.Pause
                        TimerStatus.FINISHED -> Icons.Default.Stop
                        else -> Icons.Default.PlayArrow
                    },
                    contentDescription = when (status) {
                        TimerStatus.RUNNING -> "Pause"
                        TimerStatus.FINISHED -> "Stop Alarm"
                        else -> "Play"
                    },
                    modifier = Modifier.size(40.dp)
                )
            }

            // Settings button (lower right as in sketch)
            IconButton(onClick = onNavigateToSettings) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).toInt()
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return String.format("%02d:%02d", m, s)
}
