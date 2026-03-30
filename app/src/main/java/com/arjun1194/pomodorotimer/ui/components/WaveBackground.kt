package com.arjun1194.pomodorotimer.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@Composable
fun WaveBackground(
    progress: Float, // 0.0 to 1.0
    waveColor: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveTransition")
    
    // Animate phase shift from 0 to 2*PI for continuous waving
    val phaseShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phaseShift"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        val liquidHeight = height * progress

        // The baseline Y corresponding to the top of the liquid wave
        val waveBaselineY = height - liquidHeight

        val path = Path()

        // If progress is very close to 0, don't draw anything (or draw a flat line at the very bottom)
        if (progress <= 0.001f) {
            return@Canvas
        }
        
        // If progress is very close to 1, fill essentially the whole screen
        if (progress >= 0.999f) {
            path.moveTo(0f, 0f)
            path.lineTo(width, 0f)
            path.lineTo(width, height)
            path.lineTo(0f, height)
            path.close()
            drawPath(path = path, color = waveColor)
            return@Canvas
        }

        val waveAmplitude = 16.dp.toPx() // Height of the waves
        val frequency = 1.5f // How many waves across the screen

        path.moveTo(0f, waveBaselineY)

        for (x in 0..width.toInt() step 5) {
            // Apply sine wave math
            val normalizedX = x.toFloat() / width
            val yOffset = kotlin.math.sin(
                (normalizedX * frequency * 2.0 * Math.PI) + phaseShift
            ).toFloat() * waveAmplitude

            path.lineTo(x.toFloat(), waveBaselineY + yOffset)
        }

        // Draw down to the bottom corners
        path.lineTo(width, height)
        path.lineTo(0f, height)
        path.close()

        drawPath(path = path, color = waveColor)
    }
}
