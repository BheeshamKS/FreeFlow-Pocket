package com.bheeshamks.freeflow.ime.ui

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import com.bheeshamks.freeflow.ime.KeysCafeController
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
    hapticStrength: HapticStrength,
    keyHeight: Dp,
    cornerRadius: Dp,
    onKeyPress: (KeyModel) -> Unit,
    onKeyLongPress: (KeyModel) -> Unit = {},
    onCursorDrag: (Float) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val vibrator = remember { context.getSystemService(Vibrator::class.java) }
    val scope = rememberCoroutineScope()

    var isPressed by remember { mutableStateOf(false) }
    var rootPosition by remember { mutableStateOf(Offset.Zero) }

    fun playHaptic() {
        if (hapticStrength == HapticStrength.OFF) return
        val effect = when (hapticStrength) {
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
        isPressed -> Color(0xFF282832)
        isSpecialKey -> Color(0xFF141418)
        else -> Color(0xFF18181C)
    }

    Box(
        modifier = modifier
            .height(keyHeight)
            .padding(horizontal = 2.5.dp, vertical = 3.dp)
            .onGloballyPositioned { coordinates ->
                val bounds = coordinates.boundsInRoot()
                rootPosition = bounds.center
            }
            .clip(RoundedCornerShape(cornerRadius))
            .background(backgroundColor)
            .border(
                width = 0.8.dp,
                color = if (isPressed) Color(0x665AB4FF) else Color(0x22FFFFFF),
                shape = RoundedCornerShape(cornerRadius)
            )
            .pointerInput(key, shiftState, hapticStrength) {
                if (key.keyType == KeyType.SPACE) {
                    // Spacebar supports horizontal swipe to move cursor
                    detectDragGestures(
                        onDragStart = { offset ->
                            isPressed = true
                            playHaptic()
                            keysCafeController?.triggerBloom(
                                offset = rootPosition + (offset - Offset(size.width / 2f, size.height / 2f)),
                                colorPreset = com.bheeshamks.freeflow.core.data.model.BloomColorPreset.ICY_BLUE,
                                style = com.bheeshamks.freeflow.core.data.model.BloomAnimationStyle.RADIAL_BLOOM,
                                maxRadiusPx = 250f,
                                durationMs = 350
                            )
                        },
                        onDragEnd = { isPressed = false },
                        onDragCancel = { isPressed = false },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            onCursorDrag(dragAmount.x)
                        }
                    )
                } else if (key.keyType == KeyType.BACKSPACE) {
                    // Backspace with coroutine-based acceleration
                    detectTapGestures(
                        onPress = { offset ->
                            isPressed = true
                            playHaptic()
                            keysCafeController?.triggerBloom(
                                offset = rootPosition,
                                colorPreset = com.bheeshamks.freeflow.core.data.model.BloomColorPreset.ICY_BLUE,
                                style = com.bheeshamks.freeflow.core.data.model.BloomAnimationStyle.RADIAL_BLOOM,
                                maxRadiusPx = 250f,
                                durationMs = 350
                            )
                            onKeyPress(key)

                            val repeatJob: Job = scope.launch {
                                delay(400) // Initial hold delay
                                var repeatInterval = 100L
                                while (isActive && isPressed) {
                                    playHaptic()
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
                            playHaptic()
                            keysCafeController?.triggerBloom(
                                offset = rootPosition,
                                colorPreset = com.bheeshamks.freeflow.core.data.model.BloomColorPreset.ICY_BLUE,
                                style = com.bheeshamks.freeflow.core.data.model.BloomAnimationStyle.RADIAL_BLOOM,
                                maxRadiusPx = 250f,
                                durationMs = 350
                            )
                            tryAwaitRelease()
                            isPressed = false
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
                Icon(
                    painter = painterResource(id = R.drawable.ic_enter),
                    contentDescription = "Enter",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
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
