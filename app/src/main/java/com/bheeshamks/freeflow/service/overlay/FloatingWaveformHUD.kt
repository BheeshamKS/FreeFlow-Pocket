package com.bheeshamks.freeflow.service.overlay

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bheeshamks.freeflow.core.data.model.DictationMode
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
 * Wispr Flow-style floating dark glass capsule HUD with dynamic audio-reactive waveform bars.
 */
@Composable
fun FloatingWaveformHUD(
    hudState: OverlayHudState,
    amplitudeFlow: StateFlow<Float>,
    dictationMode: DictationMode,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
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
            vibrator?.vibrate(30)
        }
    }

    Box(
        modifier = modifier
            .shadow(elevation = 16.dp, shape = RoundedCornerShape(28.dp), ambientColor = Color(0x66000000))
            .clip(RoundedCornerShape(28.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xF018181C),
                        Color(0xF00C0C0E)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = when (hudState) {
                        OverlayHudState.RECORDING -> listOf(Color(0xFF5AB4FF), Color(0x445AB4FF))
                        OverlayHudState.TRANSCRIBING -> listOf(Color(0xFF00E676), Color(0x4400E676))
                        OverlayHudState.ERROR -> listOf(Color(0xFFFF5252), Color(0x44FF5252))
                        else -> listOf(Color(0x33FFFFFF), Color(0x11FFFFFF))
                    }
                ),
                shape = RoundedCornerShape(28.dp)
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Left Action / Mic Button
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        when (hudState) {
                            OverlayHudState.RECORDING -> Color(0xFF5AB4FF)
                            OverlayHudState.TRANSCRIBING -> Color(0xFF00E676)
                            else -> Color(0xFF282830)
                        }
                    )
                    .then(
                        if (dictationMode == DictationMode.HOLD_TO_TALK) {
                            Modifier.pointerInput(hudState) {
                                detectTapGestures(
                                    onPress = {
                                        if (hudState != OverlayHudState.TRANSCRIBING) {
                                            triggerHaptic(VibrationEffect.EFFECT_CLICK)
                                            onStartRecording()
                                            tryAwaitRelease()
                                            triggerHaptic(VibrationEffect.EFFECT_TICK)
                                            onStopRecording()
                                        }
                                    }
                                )
                            }
                        } else {
                            Modifier.clickable {
                                if (hudState == OverlayHudState.RECORDING) {
                                    triggerHaptic(VibrationEffect.EFFECT_TICK)
                                    onStopRecording()
                                } else if (hudState == OverlayHudState.IDLE) {
                                    triggerHaptic(VibrationEffect.EFFECT_CLICK)
                                    onStartRecording()
                                }
                            }
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                when (hudState) {
                    OverlayHudState.RECORDING -> {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop",
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    OverlayHudState.TRANSCRIBING -> {
                        CircularProgressIndicator(
                            color = Color.Black,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    else -> {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Mic",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Center Content: Waveform or Status Message
            when (hudState) {
                OverlayHudState.RECORDING -> {
                    LiveWaveformBars(amplitude = amplitude)
                    Text(
                        text = String.format("%02d:%02d", elapsedSeconds / 60, elapsedSeconds % 60),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                OverlayHudState.TRANSCRIBING -> {
                    Text(
                        text = "Transcribing...",
                        color = Color(0xFF00E676),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                OverlayHudState.SUCCESS -> {
                    Text(
                        text = "Transcribed ✓",
                        color = Color(0xFF5AB4FF),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                OverlayHudState.ERROR -> {
                    Text(
                        text = "Failed",
                        color = Color(0xFFFF5252),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                OverlayHudState.IDLE -> {
                    Text(
                        text = if (dictationMode == DictationMode.HOLD_TO_TALK) "Hold to talk" else "Tap to talk",
                        color = Color(0xFFB0B0B8),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Close / Dismiss Icon
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color(0xFF888892),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * 6 reactive audio waveform bars scaling based on instantaneous amplitude.
 */
@Composable
private fun LiveWaveformBars(amplitude: Float) {
    val baseHeights = listOf(8.dp, 14.dp, 22.dp, 18.dp, 12.dp, 8.dp)
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(28.dp)
    ) {
        baseHeights.forEachIndexed { index, baseHeight ->
            val scaleFactor = (amplitude * (1.2f + (index % 3) * 0.4f)).coerceIn(0.15f, 1.0f)
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
