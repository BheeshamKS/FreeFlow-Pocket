package com.bheeshamks.freeflow.ime.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bheeshamks.freeflow.core.data.model.KeyboardSettings
import com.bheeshamks.freeflow.ime.KeysCafeBloomCanvas
import com.bheeshamks.freeflow.ime.KeysCafeController
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
    activeLayer: KeyboardLayer,
    shiftState: ShiftState,
    smartbarState: SmartbarState,
    suggestions: List<String>,
    modifier: Modifier = Modifier
) {
    val amplitude by amplitudeFlow.collectAsState()
    var lastShiftTapTime by remember { mutableStateOf(0L) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF000000))
            .navigationBarsPadding()
    ) {
        // Background Bloom Canvas
        if (settings.isKeysCafeEnabled) {
            KeysCafeBloomCanvas(controller = keysCafeController)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Smartbar
            Smartbar(
                state = smartbarState,
                suggestions = suggestions,
                onSuggestionClick = onSuggestionClick,
                onCollapseClick = { /* toggle back to toolbar */ },
                onEmojiClick = { onKeyPress(KeyModel("😊", keyType = KeyType.CHARACTER)) },
                onTranslateClick = { },
                onClipboardClick = { },
                onSettingsClick = onSettingsClick,
                onMicClick = onMicClick,
                isMicActive = isVoiceRecording || isVoiceTranscribing
            )

            Spacer(modifier = Modifier.height(2.dp))

            val keyHeight = settings.keyHeightDp.dp
            val cornerRadius = settings.cornerRadiusDp.dp

            if (activeLayer == KeyboardLayer.ALPHA) {
                // Row 1: Dedicated Number Row (1 2 3 4 5 6 7 8 9 0)
                KeyRow(
                    keys = KeyboardLayout.getNumberRow(),
                    shiftState = shiftState,
                    keysCafeController = keysCafeController,
                    settings = settings,
                    keyHeight = keyHeight,
                    cornerRadius = cornerRadius,
                    onKeyPress = onKeyPress
                )

                // Row 2: Top Alpha Row (Q W E R T Y U I O P)
                KeyRow(
                    keys = KeyboardLayout.getTopAlphaRow(),
                    shiftState = shiftState,
                    keysCafeController = keysCafeController,
                    settings = settings,
                    keyHeight = keyHeight,
                    cornerRadius = cornerRadius,
                    onKeyPress = onKeyPress
                )

                // Row 3: Home Alpha Row (A S D F G H J K L)
                KeyRow(
                    keys = KeyboardLayout.getHomeAlphaRow(),
                    shiftState = shiftState,
                    keysCafeController = keysCafeController,
                    settings = settings,
                    keyHeight = keyHeight,
                    cornerRadius = cornerRadius,
                    onKeyPress = onKeyPress
                )

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
                            hapticStrength = settings.hapticStrength,
                            keyHeight = keyHeight,
                            cornerRadius = cornerRadius,
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

                // Row 5: Action Row (!#1, Emoji, Spacebar, ., Enter)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    KeyboardLayout.getActionRow().forEach { key ->
                        val keyModifier = Modifier.weight(key.weight)
                        Keycap(
                            key = key,
                            shiftState = shiftState,
                            keysCafeController = keysCafeController,
                            hapticStrength = settings.hapticStrength,
                            keyHeight = keyHeight,
                            cornerRadius = cornerRadius,
                            onKeyPress = { k ->
                                if (k.keyType == KeyType.SYMBOLS_TOGGLE) {
                                    onSymbolsToggle()
                                } else {
                                    onKeyPress(k)
                                }
                            },
                            onCursorDrag = onCursorDrag,
                            modifier = keyModifier
                        )
                    }
                }
            } else {
                // Symbols Layer
                KeyRow(KeyboardLayout.getSymbolsRow1(), shiftState, keysCafeController, settings, keyHeight, cornerRadius, onKeyPress)
                KeyRow(KeyboardLayout.getSymbolsRow2(), shiftState, keysCafeController, settings, keyHeight, cornerRadius, onKeyPress)
                KeyRow(KeyboardLayout.getSymbolsRow3(), shiftState, keysCafeController, settings, keyHeight, cornerRadius, onKeyPress)

                // Symbols bottom row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    KeyboardLayout.getSymbolsRow4().forEach { key ->
                        Keycap(
                            key = key,
                            shiftState = shiftState,
                            keysCafeController = keysCafeController,
                            hapticStrength = settings.hapticStrength,
                            keyHeight = keyHeight,
                            cornerRadius = cornerRadius,
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

                // Symbols action row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    KeyboardLayout.getActionRow().forEach { key ->
                        Keycap(
                            key = key,
                            shiftState = shiftState,
                            keysCafeController = keysCafeController,
                            hapticStrength = settings.hapticStrength,
                            keyHeight = keyHeight,
                            cornerRadius = cornerRadius,
                            onKeyPress = { k ->
                                if (k.keyType == KeyType.SYMBOLS_TOGGLE) {
                                    onSymbolsToggle()
                                } else {
                                    onKeyPress(k)
                                }
                            },
                            onCursorDrag = onCursorDrag,
                            modifier = Modifier.weight(key.weight)
                        )
                    }
                }
            }

            // Bottom bar with Down Chevron to hide keyboard
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
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

        // Voice Dictation Full Overlay HUD
        AnimatedVisibility(
            visible = isVoiceRecording || isVoiceTranscribing,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            KeyboardVoiceHUD(
                isRecording = isVoiceRecording,
                isTranscribing = isVoiceTranscribing,
                amplitude = amplitude,
                onStopClick = onStopMicClick
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
                hapticStrength = settings.hapticStrength,
                keyHeight = keyHeight,
                cornerRadius = cornerRadius,
                onKeyPress = onKeyPress,
                modifier = Modifier.weight(key.weight)
            )
        }
    }
}
