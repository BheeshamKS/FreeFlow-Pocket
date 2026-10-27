package com.bheeshamks.freeflow.ime.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bheeshamks.freeflow.R

enum class SmartbarState {
    TOOLBAR,
    SUGGESTIONS
}

/**
 * Dual-State Samsung Smartbar Header Row.
 * State A: Utility Toolbar (Emoji, Translate, Clipboard, Settings, Overflow, FreeFlow Mic).
 * State B: Active Suggestions (Collapse, 3 word suggestions, Overflow, FreeFlow Mic).
 */
@Composable
fun Smartbar(
    state: SmartbarState,
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit,
    onCollapseClick: () -> Unit,
    onToolsMenuClick: () -> Unit = {},
    onEmojiClick: () -> Unit,
    onTranslateClick: () -> Unit,
    onClipboardClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onMicClick: () -> Unit,
    isMicActive: Boolean,
    onSwipeDown: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(Color(0xFF16161A))
            .padding(horizontal = 8.dp)
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
                            onSwipeDown()
                            break
                        }
                    }
                }
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedContent(
            targetState = state,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier.weight(1f),
            label = "SmartbarTransition"
        ) { targetState ->
            if (targetState == SmartbarState.TOOLBAR) {
                // State A: Toolbar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Emoji with indicator dot
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable { onEmojiClick() },
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

                    // Translate / Quick Case
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable { onTranslateClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_translate),
                            contentDescription = "Translate",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Clipboard Paste
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable { onClipboardClick() },
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
                            .clickable { onSettingsClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_settings),
                            contentDescription = "Settings",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Overflow / Tools Menu Toggle
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable { onToolsMenuClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreHoriz,
                            contentDescription = "Suggestions Toggle",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            } else {
                // State B: Suggestions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Collapse '<' button
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable { onCollapseClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_collapse),
                            contentDescription = "Collapse",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // 3 Word Suggestions
                    val displaySuggestions = suggestions.take(3)
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        displaySuggestions.forEach { suggestion ->
                            Text(
                                text = suggestion,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Normal,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { onSuggestionClick(suggestion) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // Overflow with green dot - opens Tools Menu
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable { onToolsMenuClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreHoriz,
                            contentDescription = "More",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 6.dp, end = 6.dp)
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00E676))
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Pinned FreeFlow Mic Button
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (isMicActive) Color(0xFF5AB4FF) else Color(0xFF22222A))
                .clickable { onMicClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "FreeFlow Voice Dictation",
                tint = if (isMicActive) Color.Black else Color(0xFF5AB4FF),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
