package com.bheeshamks.freeflow.ime.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bheeshamks.freeflow.core.data.model.KeyPreviewInfo
import com.bheeshamks.freeflow.core.data.model.KeyboardSettings
import com.bheeshamks.freeflow.ime.KeysCafeBloomCanvas
import com.bheeshamks.freeflow.ime.KeysCafeController
import com.bheeshamks.freeflow.ime.audio.KeyboardSoundPlayer
import com.bheeshamks.freeflow.ime.model.KeyModel
import com.bheeshamks.freeflow.ime.model.KeyType
import com.bheeshamks.freeflow.ime.model.KeyboardLayer
import com.bheeshamks.freeflow.ime.model.ShiftState
import kotlinx.coroutines.flow.StateFlow

/**
 * Complete Samsung Good Lock / Keys Cafe inspired Keyboard Layout.
 */
@Composable
fun KeyboardScreen(
    settings: KeyboardSettings,
    keysCafeController: KeysCafeController,
    amplitudeFlow: StateFlow<Float>,
    isVoiceRecording: Boolean,
    isVoiceTranscribing: Boolean,
    onKeyPress: (KeyModel) -> Unit,
    onShiftPress: (isDoubleTap: Boolean) -> Unit,
    onSymbolsToggle: () -> Unit,
    onCursorDrag: (Float) -> Unit,
    onHideKeyboard: () -> Unit,
    onMicClick: () -> Unit,
    onStopMicClick: () -> Unit,
    onSuggestionClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onEmojiClick: () -> Unit = {},
    onClipboardClick: () -> Unit = {},
    onTranslateClick: () -> Unit = {},
    onToggleSmartbarState: () -> Unit = {},
    soundPlayer: KeyboardSoundPlayer? = null,
    activeLayer: KeyboardLayer,
    shiftState: ShiftState,
    smartbarState: SmartbarState,
    suggestions: List<String>,
    isSearchAction: Boolean = false,
    modifier: Modifier = Modifier
) {
    val amplitude by amplitudeFlow.collectAsState()
    var lastShiftTapTime by remember { mutableStateOf(0L) }
    var activeKeyPreview by remember { mutableStateOf<KeyPreviewInfo?>(null) }
    var keyboardRootOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(Color(settings.keyboardTheme.bgHex))
            .navigationBarsPadding()
            .onGloballyPositioned { keyboardRootOffset = it.positionInRoot() }
    ) {
        // Background Bloom Canvas matched strictly to keyboard bounds
        if (settings.isKeysCafeEnabled) {
            KeysCafeBloomCanvas(
                controller = keysCafeController,
                modifier = Modifier.matchParentSize()
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(bottom = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Smartbar
            Smartbar(
                state = smartbarState,
                suggestions = suggestions,
                onSuggestionClick = onSuggestionClick,
                onCollapseClick = onToggleSmartbarState,
                onEmojiClick = onEmojiClick,
                onTranslateClick = onTranslateClick,
                onClipboardClick = onClipboardClick,
                onSettingsClick = onSettingsClick,
                onMicClick = onMicClick,
                isMicActive = isVoiceRecording || isVoiceTranscribing
            )

            Spacer(modifier = Modifier.height(2.dp))

            val keyHeight = settings.keyHeightDp.dp
            val cornerRadius = settings.cornerRadiusDp.dp

            if (activeLayer == KeyboardLayer.ALPHA) {
                // Row 1: Dedicated Number Row (1 2 3 4 5 6 7 8 9 0)
                if (settings.showNumberRow) {
                    KeyRow(
                        keys = KeyboardLayout.getNumberRow(),
                        shiftState = shiftState,
                        keysCafeController = keysCafeController,
                        settings = settings,
                        keyHeight = keyHeight * 0.9f,
                        cornerRadius = cornerRadius,
                        soundPlayer = soundPlayer,
                        onKeyPreviewChange = { activeKeyPreview = it },
                        onKeyPress = onKeyPress
                    )
                }

                // Row 2: Top Alpha Row (Q W E R T Y U I O P)
                KeyRow(
                    keys = KeyboardLayout.getTopAlphaRow(),
                    shiftState = shiftState,
                    keysCafeController = keysCafeController,
                    settings = settings,
                    keyHeight = keyHeight,
                    cornerRadius = cornerRadius,
                    soundPlayer = soundPlayer,
                    onKeyPreviewChange = { activeKeyPreview = it },
                    onKeyPress = onKeyPress
                )

                // Row 3: Home Alpha Row (A S D F G H J K L) with 0.5f spacers on each side
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.weight(0.5f))
                    KeyboardLayout.getHomeAlphaRow().forEach { key ->
                        Keycap(
                            key = key,
                            shiftState = shiftState,
                            keysCafeController = keysCafeController,
                            settings = settings,
                            keyHeight = keyHeight,
                            cornerRadius = cornerRadius,
                            soundPlayer = soundPlayer,
                            onKeyPreviewChange = { activeKeyPreview = it },
                            onKeyPress = onKeyPress,
                            modifier = Modifier.weight(key.weight)
                        )
                    }
                    Spacer(modifier = Modifier.weight(0.5f))
                }

                // Row 4: Bottom Alpha Row (Shift, Z X C V B N M, Backspace)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    KeyboardLayout.getBottomAlphaRow().forEach { key ->
                        val keyModifier = Modifier.weight(key.weight)
                        Keycap(
                            key = key,
                            shiftState = shiftState,
                            keysCafeController = keysCafeController,
                            settings = settings,
                            keyHeight = keyHeight,
                            cornerRadius = cornerRadius,
                            soundPlayer = soundPlayer,
                            onKeyPreviewChange = { activeKeyPreview = it },
                            onKeyPress = { k ->
                                if (k.keyType == KeyType.SHIFT) {
                                    val now = System.currentTimeMillis()
                                    val isDouble = (now - lastShiftTapTime) < 300
                                    lastShiftTapTime = now
                                    onShiftPress(isDouble)
                                } else {
                                    onKeyPress(k)
                                }
                            },
                            modifier = keyModifier
                        )
                    }
                }

                // Row 5: Action Row (!#1, Emoji, Spacebar, ., Enter/Search)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    KeyboardLayout.getActionRow(isSearchAction = isSearchAction).forEach { key ->
                        val keyModifier = Modifier.weight(key.weight)
                        Keycap(
                            key = key,
                            shiftState = shiftState,
                            keysCafeController = keysCafeController,
                            settings = settings,
                            keyHeight = keyHeight,
                            cornerRadius = cornerRadius,
                            soundPlayer = soundPlayer,
                            onKeyPreviewChange = { activeKeyPreview = it },
                            onKeyPress = { k ->
                                when (k.keyType) {
                                    KeyType.SYMBOLS_TOGGLE -> onSymbolsToggle()
                                    KeyType.EMOJI -> onEmojiClick()
                                    else -> onKeyPress(k)
                                }
                            },
                            onCursorDrag = onCursorDrag,
                            modifier = keyModifier
                        )
                    }
                }
            } else {
                // Symbols Layer
                KeyRow(KeyboardLayout.getSymbolsRow1(), shiftState, keysCafeController, settings, keyHeight, cornerRadius, soundPlayer, { activeKeyPreview = it }, onKeyPress)
                KeyRow(KeyboardLayout.getSymbolsRow2(), shiftState, keysCafeController, settings, keyHeight, cornerRadius, soundPlayer, { activeKeyPreview = it }, onKeyPress)

                // Symbols Row 3 (with 0.5f spacers on each side)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.weight(0.5f))
                    KeyboardLayout.getSymbolsRow3().forEach { key ->
                        Keycap(
                            key = key,
                            shiftState = shiftState,
                            keysCafeController = keysCafeController,
                            settings = settings,
                            keyHeight = keyHeight,
                            cornerRadius = cornerRadius,
                            soundPlayer = soundPlayer,
                            onKeyPreviewChange = { activeKeyPreview = it },
                            onKeyPress = onKeyPress,
                            modifier = Modifier.weight(key.weight)
                        )
                    }
                    Spacer(modifier = Modifier.weight(0.5f))
                }

                // Symbols bottom row (1/2, < > { } [ ] =, Backspace)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    KeyboardLayout.getSymbolsRow4().forEach { key ->
                        Keycap(
                            key = key,
                            shiftState = shiftState,
                            keysCafeController = keysCafeController,
                            settings = settings,
                            keyHeight = keyHeight,
                            cornerRadius = cornerRadius,
                            soundPlayer = soundPlayer,
                            onKeyPreviewChange = { activeKeyPreview = it },
                            onKeyPress = { k ->
                                if (k.keyType == KeyType.SYMBOLS_TOGGLE) {
                                    onSymbolsToggle()
                                } else {
                                    onKeyPress(k)
                                }
                            },
                            modifier = Modifier.weight(key.weight)
                        )
                    }
                }

                // Symbols action row (ABC, Emoji, English (UK), ., Enter/Search)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    KeyboardLayout.getSymbolsActionRow(isSearchAction = isSearchAction).forEach { key ->
                        Keycap(
                            key = key,
                            shiftState = shiftState,
                            keysCafeController = keysCafeController,
                            settings = settings,
                            keyHeight = keyHeight,
                            cornerRadius = cornerRadius,
                            soundPlayer = soundPlayer,
                            onKeyPreviewChange = { activeKeyPreview = it },
                            onKeyPress = { k ->
                                when (k.keyType) {
                                    KeyType.SYMBOLS_TOGGLE -> onSymbolsToggle()
                                    KeyType.EMOJI -> onEmojiClick()
                                    else -> onKeyPress(k)
                                }
                            },
                            onCursorDrag = onCursorDrag,
                            modifier = Modifier.weight(key.weight)
                        )
                    }
                }
            }

            // Bottom bar with Down Chevron to hide keyboard (clean & uncluttered matching Screenshot 1)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clickable { onHideKeyboard() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Hide Keyboard",
                        tint = Color(0xFF888894),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Key Preview Bubble (iPhone / Samsung style letter popup above finger)
        if (settings.showKeyPreviewPopup && activeKeyPreview != null) {
            val preview = activeKeyPreview!!
            val density = LocalDensity.current
            val localKeyLeft = preview.rootOffset.x - keyboardRootOffset.x
            val localKeyTop = preview.rootOffset.y - keyboardRootOffset.y
            val keyWidth = preview.keyWidthPx

            val bubbleWidthDp = 50.dp
            val bubbleHeightDp = 54.dp
            val bubbleWidthPx = with(density) { bubbleWidthDp.toPx() }
            val bubbleHeightPx = with(density) { bubbleHeightDp.toPx() }

            val targetLeftPx = localKeyLeft + (keyWidth - bubbleWidthPx) / 2f
            val targetTopPx = localKeyTop - bubbleHeightPx - with(density) { 8.dp.toPx() }

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = targetLeftPx.toInt().coerceAtLeast(0),
                            y = targetTopPx.toInt()
                        )
                    }
                    .size(bubbleWidthDp, bubbleHeightDp)
                    .shadow(
                        elevation = 10.dp,
                        shape = RoundedCornerShape(12.dp),
                        spotColor = Color(settings.colorPreset.outerGlowColor).copy(alpha = 0.6f)
                    )
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(settings.keyboardTheme.keyHex))
                    .border(
                        width = 1.2.dp,
                        color = Color(settings.colorPreset.primaryColor),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = preview.text,
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Voice Dictation Full Overlay HUD - strictly bounded to keyboard box
        AnimatedVisibility(
            visible = isVoiceRecording || isVoiceTranscribing,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.matchParentSize()
        ) {
            KeyboardVoiceHUD(
                isRecording = isVoiceRecording,
                isTranscribing = isVoiceTranscribing,
                amplitude = amplitude,
                onStopClick = onStopMicClick,
                modifier = Modifier.matchParentSize()
            )
        }
    }
}

@Composable
private fun KeyRow(
    keys: List<KeyModel>,
    shiftState: ShiftState,
    keysCafeController: KeysCafeController,
    settings: KeyboardSettings,
    keyHeight: androidx.compose.ui.unit.Dp,
    cornerRadius: androidx.compose.ui.unit.Dp,
    soundPlayer: KeyboardSoundPlayer? = null,
    onKeyPreviewChange: (KeyPreviewInfo?) -> Unit = {},
    onKeyPress: (KeyModel) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        keys.forEach { key ->
            Keycap(
                key = key,
                shiftState = shiftState,
                keysCafeController = keysCafeController,
                settings = settings,
                keyHeight = keyHeight,
                cornerRadius = cornerRadius,
                soundPlayer = soundPlayer,
                onKeyPreviewChange = onKeyPreviewChange,
                onKeyPress = onKeyPress,
                modifier = Modifier.weight(key.weight)
            )
        }
    }
}
