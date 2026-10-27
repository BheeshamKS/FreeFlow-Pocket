package com.bheeshamks.freeflow.service.overlay

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow

enum class OverlayHudState {
    IDLE,
    RECORDING,
    TRANSCRIBING,
    SUCCESS,
    ERROR
}

/**
 * Wispr Flow-style floating dark glass UI with 3-bubble expansion and magnetic edge-flipping.
 * - Idle: Compact circular floating button (52dp).
 * - Recording (Docked Right): [Cancel Cross (Red)] [Live Waves + Timer] [Done Tick (Green)]
 * - Recording (Docked Left):  [Done Tick (Green)] [Live Waves + Timer] [Cancel Cross (Red)]
 * - Single tap-to-toggle dictation.
 */
@Composable
fun FloatingWaveformHUD(
    hudState: OverlayHudState,
    isDockedOnRight: Boolean,
    errorMessage: String?,
    amplitudeFlow: StateFlow<Float>,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onCancel: () -> Unit,
    onErrorClick: () -> Unit,
    onDragDelta: (Float, Float) -> Unit,
    onDragEnd: () -> Unit,
    opacity: Float = 0.5f,
    onContentSizeChanged: (Int, Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val vibrator = remember { context.getSystemService(Vibrator::class.java) }
    val amplitude by amplitudeFlow.collectAsState()

    var elapsedSeconds by remember { mutableStateOf(0) }

    LaunchedEffect(hudState) {
        if (hudState == OverlayHudState.RECORDING) {
            elapsedSeconds = 0
            while (true) {
                delay(1000)
                elapsedSeconds++
            }
        } else {
            elapsedSeconds = 0
        }
    }

    fun triggerHaptic(effect: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator?.vibrate(VibrationEffect.createPredefined(effect))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(25)
        }
    }

    Box(
        modifier = modifier
            .onSizeChanged { onContentSizeChanged(it.width, it.height) }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDragDelta(dragAmount.x, dragAmount.y)
                    },
                    onDragEnd = {
                        onDragEnd()
                    }
                )
            }
    ) {
        when (hudState) {
            OverlayHudState.IDLE -> {
                // Compact Floating Mic Bubble (Semi-transparent per user setting)
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .alpha(opacity)
                        .shadow(elevation = 12.dp, shape = CircleShape, ambientColor = Color(0x66000000))
                        .clip(CircleShape)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xF01C1C22), Color(0xF0101014))
                            )
                        )
                        .border(
                            width = 1.5.dp,
                            color = Color(0x445AB4FF),
                            shape = CircleShape
                        )
                        .clickable {
                            triggerHaptic(VibrationEffect.EFFECT_CLICK)
                            onStartRecording()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Tap to Dictate",
                        tint = Color(0xFF5AB4FF),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            OverlayHudState.RECORDING -> {
                // 3 Expanded Bubbles
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isDockedOnRight) {
                        // Order: [Cancel Cross] [Waveform Pill] [Done Tick]
                        CancelBubble(
                            onCancel = {
                                triggerHaptic(VibrationEffect.EFFECT_TICK)
                                onCancel()
                            }
                        )
                        WaveformPill(
                            amplitude = amplitude,
                            elapsedSeconds = elapsedSeconds
                        )
                        DoneBubble(
                            onDone = {
                                triggerHaptic(VibrationEffect.EFFECT_CLICK)
                                onStopRecording()
                            }
                        )
                    } else {
                        // Order: [Done Tick] [Waveform Pill] [Cancel Cross]
                        DoneBubble(
                            onDone = {
                                triggerHaptic(VibrationEffect.EFFECT_CLICK)
                                onStopRecording()
                            }
                        )
                        WaveformPill(
                            amplitude = amplitude,
                            elapsedSeconds = elapsedSeconds
                        )
                        CancelBubble(
                            onCancel = {
                                triggerHaptic(VibrationEffect.EFFECT_TICK)
                                onCancel()
                            }
                        )
                    }
                }
            }

            OverlayHudState.TRANSCRIBING -> {
                // Transcribing Pill
                Row(
                    modifier = Modifier
                        .height(48.dp)
                        .shadow(elevation = 12.dp, shape = RoundedCornerShape(24.dp))
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xF014221A), Color(0xF00A140E))
                            )
                        )
                        .border(1.dp, Color(0x5500E676), RoundedCornerShape(24.dp))
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF00E676),
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Transcribing...",
                        color = Color(0xFF00E676),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            OverlayHudState.SUCCESS -> {
                // Success Pill
                Row(
                    modifier = Modifier
                        .height(48.dp)
                        .shadow(elevation = 12.dp, shape = RoundedCornerShape(24.dp))
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xF0102030), Color(0xF008121A))
                            )
                        )
                        .border(1.dp, Color(0x665AB4FF), RoundedCornerShape(24.dp))
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Success",
                        tint = Color(0xFF5AB4FF),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Transcribed ✓",
                        color = Color(0xFF5AB4FF),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            OverlayHudState.ERROR -> {
                // Error Pill with tap to configure
                Row(
                    modifier = Modifier
                        .height(48.dp)
                        .shadow(elevation = 12.dp, shape = RoundedCornerShape(24.dp))
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xF02A1416), Color(0xF0180A0C))
                            )
                        )
                        .border(1.dp, Color(0x66FF5252), RoundedCornerShape(24.dp))
                        .clickable { onErrorClick() }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Error",
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = errorMessage?.ifBlank { "Setup FreeFlow" } ?: "Setup FreeFlow",
                        color = Color(0xFFFF5252),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun CancelBubble(onCancel: () -> Unit) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .shadow(elevation = 10.dp, shape = CircleShape)
            .clip(CircleShape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xF0281418), Color(0xF0180A0E))
                )
            )
            .border(1.5.dp, Color(0x66FF5252), CircleShape)
            .clickable { onCancel() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Cancel Transcription",
            tint = Color(0xFFFF5252),
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun DoneBubble(onDone: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .shadow(elevation = 12.dp, shape = CircleShape)
            .clip(CircleShape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF00E676), Color(0xFF00B248))
                )
            )
            .border(1.5.dp, Color(0x88FFFFFF), CircleShape)
            .clickable { onDone() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "Finish and Transcribe",
            tint = Color.Black,
            modifier = Modifier.size(26.dp)
        )
    }
}

@Composable
private fun WaveformPill(
    amplitude: Float,
    elapsedSeconds: Int
) {
    Row(
        modifier = Modifier
            .height(48.dp)
            .shadow(elevation = 10.dp, shape = RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xF0181820), Color(0xF00E0E12))
                )
            )
            .border(1.dp, Color(0x445AB4FF), RoundedCornerShape(24.dp))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        LiveWaveformBars(amplitude = amplitude)
        Text(
            text = String.format("%02d:%02d", elapsedSeconds / 60, elapsedSeconds % 60),
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun LiveWaveformBars(amplitude: Float) {
    val baseHeights = listOf(8.dp, 16.dp, 26.dp, 20.dp, 14.dp, 8.dp)
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.5.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(28.dp)
    ) {
        baseHeights.forEachIndexed { index, baseHeight ->
            val scaleFactor = (amplitude * (1.1f + (index % 3) * 0.4f)).coerceIn(0.20f, 1.0f)
            val barHeight = (baseHeight * scaleFactor).coerceAtLeast(4.dp)

            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height(barHeight)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF5AB4FF),
                                Color(0xFF00E676)
                            )
                        )
                    )
            )
        }
    }
}
