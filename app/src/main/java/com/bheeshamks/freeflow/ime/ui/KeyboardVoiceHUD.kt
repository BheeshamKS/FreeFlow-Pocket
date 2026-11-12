package com.bheeshamks.freeflow.ime.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bheeshamks.freeflow.ui.theme.DarkBackground
import com.bheeshamks.freeflow.ui.theme.DarkCard
import com.bheeshamks.freeflow.ui.theme.DarkCardBorder
import com.bheeshamks.freeflow.ui.theme.DarkRaisedSubtle
import com.bheeshamks.freeflow.ui.theme.OnPrimaryBlue
import com.bheeshamks.freeflow.ui.theme.PrimaryBlue
import com.bheeshamks.freeflow.ui.theme.TextPrimary
import com.bheeshamks.freeflow.ui.theme.TextSecondary

/**
 * Dimmed voice dictation overlay rendered directly over the keyboard grid.
 */
@Composable
fun KeyboardVoiceHUD(
    isRecording: Boolean,
    isTranscribing: Boolean,
    amplitude: Float,
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "OrbPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "OrbPulseScale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkBackground.copy(alpha = 0.94f))
            .clickable { onStopClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Audio-reactive Voice Orb
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(88.dp)
                    .scale(if (isRecording) pulseScale * (1f + amplitude * 0.35f) else 1f)
            ) {
                // Outer subtle ring
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(DarkRaisedSubtle.copy(alpha = 0.6f))
                        .border(1.dp, DarkCardBorder, CircleShape)
                )

                // Inner core
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(if (isTranscribing) DarkCard else PrimaryBlue)
                        .border(1.dp, if (isTranscribing) DarkCardBorder else PrimaryBlue, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isTranscribing) {
                        CircularProgressIndicator(
                            color = PrimaryBlue,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Dictation",
                            tint = OnPrimaryBlue,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isTranscribing) "Transcribing speech..." else "Listening to your voice...",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Tap anywhere to finish and insert text",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}
