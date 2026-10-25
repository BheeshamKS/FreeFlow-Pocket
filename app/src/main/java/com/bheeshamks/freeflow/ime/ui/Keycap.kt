package com.bheeshamks.freeflow.ime.ui

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bheeshamks.freeflow.R
import com.bheeshamks.freeflow.core.data.model.HapticStrength
import com.bheeshamks.freeflow.core.data.model.KeyBorderStyle
import com.bheeshamks.freeflow.core.data.model.KeyPreviewInfo
import com.bheeshamks.freeflow.core.data.model.KeyboardSettings
import com.bheeshamks.freeflow.ime.KeysCafeController
import com.bheeshamks.freeflow.ime.audio.KeyboardSoundPlayer
import com.bheeshamks.freeflow.ime.model.KeyModel
import com.bheeshamks.freeflow.ime.model.KeyType
import com.bheeshamks.freeflow.ime.model.ShiftState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Individual keycap matching Samsung Good Lock dark matte aesthetic (#18181C).
 * Dispatches touch coordinates to [KeysCafeController] and fires tactile haptics.
 */
@Composable
fun Keycap(
    key: KeyModel,
    shiftState: ShiftState,
    keysCafeController: KeysCafeController?,
    settings: KeyboardSettings,
    keyHeight: Dp,
    cornerRadius: Dp,
    onKeyPress: (KeyModel) -> Unit,
    onKeyLongPress: (KeyModel) -> Unit = {},
    onCursorDrag: (Float) -> Unit = {},
    soundPlayer: KeyboardSoundPlayer? = null,
    onKeyPreviewChange: (KeyPreviewInfo?) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val vibrator = remember { context.getSystemService(Vibrator::class.java) }
    val scope = rememberCoroutineScope()

    var isPressed by remember { mutableStateOf(false) }
    var rootTopLeft by remember { mutableStateOf(Offset.Zero) }
    var rootCenter by remember { mutableStateOf(Offset.Zero) }
    var rootBounds by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }

    fun playFeedback() {
        soundPlayer?.playKeySound(settings.soundTheme, settings.soundVolume)
        if (settings.hapticStrength == HapticStrength.OFF) return
        val effect = when (settings.hapticStrength) {
            HapticStrength.TICK -> VibrationEffect.EFFECT_TICK
            HapticStrength.CLICK -> VibrationEffect.EFFECT_CLICK
            HapticStrength.HEAVY_CLICK -> VibrationEffect.EFFECT_HEAVY_CLICK
            HapticStrength.OFF -> return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator?.vibrate(VibrationEffect.createPredefined(effect))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(25)
        }
    }

    val displayText = remember(key, shiftState) {
        when {
            key.keyType != KeyType.CHARACTER -> key.primaryText
            shiftState == ShiftState.SHIFTED || shiftState == ShiftState.CAPS_LOCKED -> key.shiftedText
            else -> key.primaryText.lowercase()
        }
    }

    val isSpecialKey = key.keyType in listOf(KeyType.SHIFT, KeyType.BACKSPACE, KeyType.ENTER, KeyType.SYMBOLS_TOGGLE)

    val backgroundColor = when {
        isPressed -> Color(settings.colorPreset.primaryColor).copy(alpha = 0.4f)
        isSpecialKey -> Color(settings.keyboardTheme.specialKeyHex)
        else -> Color(settings.keyboardTheme.keyHex)
    }

    val borderModifier = when (settings.borderStyle) {
        KeyBorderStyle.NO_BORDER -> Modifier
        KeyBorderStyle.SUBTLE -> Modifier.border(
            width = 0.8.dp,
            color = if (isPressed) Color(settings.colorPreset.primaryColor) else Color(0x22FFFFFF),
            shape = RoundedCornerShape(cornerRadius)
        )
        KeyBorderStyle.GLOW_ACCENT -> Modifier.border(
            width = 1.dp,
            color = if (isPressed) Color(settings.colorPreset.primaryColor) else Color(settings.colorPreset.outerGlowColor).copy(alpha = 0.55f),
            shape = RoundedCornerShape(cornerRadius)
        )
    }

    Box(
        modifier = modifier
            .height(keyHeight)
            .padding(horizontal = 2.5.dp, vertical = 3.dp)
            .onGloballyPositioned { coordinates ->
                val bounds = coordinates.boundsInRoot()
                rootTopLeft = bounds.topLeft
                rootCenter = bounds.center
                rootBounds = bounds
            }
            .clip(RoundedCornerShape(cornerRadius))
            .background(backgroundColor)
            .then(borderModifier)
            .pointerInput(key, shiftState, settings) {
                if (key.keyType == KeyType.SPACE) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        isPressed = true
                        playFeedback()
                        keysCafeController?.triggerBloom(
                            offset = rootTopLeft + down.position,
                            colorPreset = settings.colorPreset,
                            style = settings.animationStyle,
                            maxRadiusPx = settings.bloomRadiusDp * 2.5f,
                            durationMs = settings.animationSpeedMs
                        )

                        var hasDragged = false
                        var accumulatedX = 0f
                        val touchSlop = viewConfiguration.touchSlop

                        while (true) {
                            val event = awaitPointerEvent()
                            val pointerChange = event.changes.firstOrNull { it.id == down.id }
                            if (pointerChange == null || !pointerChange.pressed) {
                                break
                            }

                            val posChange = pointerChange.positionChange()
                            accumulatedX += posChange.x

                            if (!hasDragged && kotlin.math.abs(accumulatedX) > touchSlop) {
                                hasDragged = true
                            }

                            if (hasDragged) {
                                pointerChange.consume()
                                onCursorDrag(posChange.x)
                            }
                        }

                        isPressed = false
                        if (!hasDragged) {
                            onKeyPress(key)
                        }
                    }
                } else if (key.keyType == KeyType.BACKSPACE) {
                    // Backspace with coroutine-based acceleration
                    detectTapGestures(
                        onPress = { offset ->
                            isPressed = true
                            playFeedback()
                            keysCafeController?.triggerBloom(
                                offset = rootTopLeft + offset,
                                colorPreset = settings.colorPreset,
                                style = settings.animationStyle,
                                maxRadiusPx = settings.bloomRadiusDp * 2.5f,
                                durationMs = settings.animationSpeedMs
                            )
                            onKeyPress(key)

                            val repeatJob: Job = scope.launch {
                                delay(400) // Initial hold delay
                                var repeatInterval = 100L
                                while (isActive && isPressed) {
                                    playFeedback()
                                    onKeyPress(key)
                                    delay(repeatInterval)
                                    if (repeatInterval > 40L) {
                                        repeatInterval -= 10L // Accelerate
                                    }
                                }
                            }

                            tryAwaitRelease()
                            repeatJob.cancel()
                            isPressed = false
                        }
                    )
                } else {
                    detectTapGestures(
                        onPress = { offset ->
                            isPressed = true
                            playFeedback()
                            if (settings.showKeyPreviewPopup && (key.keyType == KeyType.CHARACTER || key.keyType == KeyType.PERIOD)) {
                                onKeyPreviewChange(
                                    KeyPreviewInfo(
                                        text = displayText,
                                        rootOffset = rootTopLeft,
                                        keyWidthPx = rootBounds.width,
                                        keyHeightPx = rootBounds.height
                                    )
                                )
                            }
                            keysCafeController?.triggerBloom(
                                offset = rootTopLeft + offset,
                                colorPreset = settings.colorPreset,
                                style = settings.animationStyle,
                                maxRadiusPx = settings.bloomRadiusDp * 2.5f,
                                durationMs = settings.animationSpeedMs
                            )
                            tryAwaitRelease()
                            isPressed = false
                            onKeyPreviewChange(null)
                        },
                        onTap = {
                            onKeyPress(key)
                        },
                        onLongPress = {
                            onKeyLongPress(key)
                        }
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        when (key.keyType) {
            KeyType.SHIFT -> {
                val iconRes = if (shiftState == ShiftState.CAPS_LOCKED) {
                    R.drawable.ic_capslock
                } else {
                    R.drawable.ic_shift
                }
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = "Shift",
                    tint = if (shiftState != ShiftState.UNSHIFTED) Color(0xFF5AB4FF) else Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            KeyType.BACKSPACE -> {
                Icon(
                    painter = painterResource(id = R.drawable.ic_backspace),
                    contentDescription = "Backspace",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            KeyType.ENTER -> {
                if (key.subtext == "search") {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF5AB4FF),
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_enter),
                        contentDescription = "Enter",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            KeyType.EMOJI -> {
                Icon(
                    painter = painterResource(id = R.drawable.ic_emoji),
                    contentDescription = "Emoji",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            KeyType.SPACE -> {
                Text(
                    text = key.primaryText,
                    color = Color(0xFFB0B0B8),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal
                )
            }
            else -> {
                Text(
                    text = displayText,
                    color = Color.White,
                    fontSize = if (displayText.length > 2) 13.sp else 19.sp,
                    fontWeight = if (key.keyType == KeyType.CHARACTER) FontWeight.SemiBold else FontWeight.Medium
                )
            }
        }
    }
}
