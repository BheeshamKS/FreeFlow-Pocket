package com.bheeshamks.freeflow.ime.ui

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bheeshamks.freeflow.R
import com.bheeshamks.freeflow.core.data.model.HapticStrength
import com.bheeshamks.freeflow.core.data.model.KeyboardSettings

data class ToolMenuItem(
    val id: String,
    val title: String,
    val icon: ImageVector? = null,
    val drawableRes: Int? = null,
    val isPrimary: Boolean = false
)

/**
 * Samsung Keyboard Overflow / Tools Sheet matching Screenshot media_1792950545553_7e85c897.jpg.
 * Tapping three dots (•••) displays this panel with the "Open FreeFlow" tile to launch the main app.
 */
@Composable
fun ToolsMenuView(
    settings: KeyboardSettings,
    onOpenFreeFlow: () -> Unit,
    onBackToKeyboard: () -> Unit,
    onEmojiClick: () -> Unit,
    onTranslateClick: () -> Unit,
    onClipboardClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onHideKeyboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val vibrator = remember { context.getSystemService(Vibrator::class.java) }

    fun playFeedback() {
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

    val toolItems = remember {
        listOf(
            ToolMenuItem("open_freeflow", "Open FreeFlow", drawableRes = R.drawable.ic_freeflow_logo, isPrimary = true),
            ToolMenuItem("modes", "Modes", icon = Icons.Default.Keyboard),
            ToolMenuItem("extract_text", "Extract text", icon = Icons.Default.TextFields),
            ToolMenuItem("theme", "Theme", icon = Icons.Default.Palette),
            ToolMenuItem("size", "Keyboard size", icon = Icons.Default.AspectRatio),
            ToolMenuItem("editing", "Text editing", icon = Icons.Default.OpenWith),
            ToolMenuItem("add", "", icon = Icons.Default.Add)
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(Color(settings.keyboardTheme.bgHex))
            .navigationBarsPadding()
            .padding(bottom = 6.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var accumulatedY = 0f
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
                        accumulatedY += posChange.y

                        if (accumulatedY > touchSlop * 1.5f && accumulatedY > kotlin.math.abs(accumulatedX) * 1.2f) {
                            pointerChange.consume()
                            playFeedback()
                            onBackToKeyboard()
                            break
                        }
                    }
                }
            }
    ) {
        // Top Toolbar Header (Emoji | Translate | Clipboard | Settings | ••• active)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(Color(0xFF16161A))
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Emoji with dot
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable {
                        playFeedback()
                        onEmojiClick()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_emoji),
                    contentDescription = "Emoji",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 6.dp, end = 6.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF6D00))
                )
            }

            // Translate
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable {
                        playFeedback()
                        onTranslateClick()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_translate),
                    contentDescription = "Translate",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Clipboard
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable {
                        playFeedback()
                        onClipboardClick()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_clipboard),
                    contentDescription = "Clipboard",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Settings
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable {
                        playFeedback()
                        onSettingsClick()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_settings),
                    contentDescription = "Settings",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Active Three Dots (•••) button - toggles back to keyboard
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2C2C38))
                    .clickable {
                        playFeedback()
                        onBackToKeyboard()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "•••",
                    color = Color(0xFF5AB4FF),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Tools Grid (4 Columns matching Samsung One UI reference)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(Color(0xFF0D0D11))
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(toolItems) { item ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                playFeedback()
                                if (item.id == "open_freeflow") {
                                    onOpenFreeFlow()
                                } else {
                                    android.widget.Toast.makeText(
                                        context,
                                        "${item.title.ifEmpty { "Feature" }} is available in FreeFlow",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (item.isPrimary) Color(0xFF1E2638) else Color(0xFF1E1E26)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (item.drawableRes != null) {
                                Icon(
                                    painter = painterResource(id = item.drawableRes),
                                    contentDescription = item.title,
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(26.dp)
                                )
                            } else if (item.icon != null) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = if (item.isPrimary) Color.Black else Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        if (item.title.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = item.title,
                                color = if (item.isPrimary) Color(0xFF5AB4FF) else Color.White,
                                fontSize = 12.sp,
                                fontWeight = if (item.isPrimary) FontWeight.SemiBold else FontWeight.Normal,
                                textAlign = TextAlign.Center,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}
