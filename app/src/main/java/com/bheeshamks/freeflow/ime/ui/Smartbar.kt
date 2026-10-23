package com.bheeshamks.freeflow.ime.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
    onEmojiClick: () -> Unit,
    onTranslateClick: () -> Unit,
    onClipboardClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onMicClick: () -> Unit,
    isMicActive: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .background(Color(0xFF000000))
            .padding(horizontal = 10.dp),
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
                    // Emoji with orange notification dot
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { onEmojiClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_emoji),
                            contentDescription = "Emoji",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        // Orange indicator dot
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 4.dp, end = 4.dp)
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF6D00))
                        )
                    }

                    // Translate
                    Box(
                        modifier = Modifier
                            .size(36.dp)
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

                    // Clipboard
                    Box(
                        modifier = Modifier
                            .size(36.dp)
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
                            .size(36.dp)
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

                    // Overflow
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreHoriz,
                            contentDescription = "Overflow",
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
                            .size(36.dp)
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
                    val displaySuggestions = suggestions.take(3).ifEmpty { listOf("The", "What", "How") }
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

                    // Overflow with green dot
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { },
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
                                .padding(top = 4.dp, end = 4.dp)
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00E676))
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Pinned FreeFlow Mic Button
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isMicActive) Color(0xFF5AB4FF) else Color(0xFF18181C))
                .clickable { onMicClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "FreeFlow Voice Dictation",
                tint = if (isMicActive) Color.Black else Color(0xFF5AB4FF),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
