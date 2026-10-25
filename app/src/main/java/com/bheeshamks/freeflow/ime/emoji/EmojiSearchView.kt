package com.bheeshamks.freeflow.ime.emoji

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bheeshamks.freeflow.core.data.model.HapticStrength
import com.bheeshamks.freeflow.core.data.model.KeyboardSettings
import com.bheeshamks.freeflow.ime.KeysCafeController
import com.bheeshamks.freeflow.ime.audio.KeyboardSoundPlayer
import com.bheeshamks.freeflow.ime.model.KeyModel
import com.bheeshamks.freeflow.ime.model.KeyType
import com.bheeshamks.freeflow.ime.model.KeyboardLayer
import com.bheeshamks.freeflow.ime.model.ShiftState
import com.bheeshamks.freeflow.ime.ui.Keycap
import com.bheeshamks.freeflow.ime.ui.KeyboardLayout

/**
 * Live Emoji Search screen matching Reference Screenshots 3 & 4.
 * Displays interactive search query bar, matching horizontal emoji strip, and full QWERTY keyboard.
 */
@Composable
fun EmojiSearchView(
    settings: KeyboardSettings,
    keysCafeController: KeysCafeController,
    soundPlayer: KeyboardSoundPlayer?,
    onEmojiSelected: (String) -> Unit,
    onCloseSearch: () -> Unit,
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

    var searchQuery by remember { mutableStateOf("") }
    var shiftState by remember { mutableStateOf(ShiftState.UNSHIFTED) }
    var activeLayer by remember { mutableStateOf(KeyboardLayer.ALPHA) }
    var lastShiftTapTime by remember { mutableStateOf(0L) }

    val matchingEmojis = remember(searchQuery) {
        if (searchQuery.isBlank()) emptyList() else EmojiRepository.searchEmojis(searchQuery)
    }

    val keyHeight = settings.keyHeightDp.dp
    val cornerRadius = settings.cornerRadiusDp.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(Color(settings.keyboardTheme.bgHex))
            .navigationBarsPadding()
    ) {
        // Search Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(Color(0xFF141418))
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Blue cursor indicator
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(20.dp)
                        .background(Color(0xFF5AB4FF))
                )
                Spacer(modifier = Modifier.width(4.dp))

                if (searchQuery.isEmpty()) {
                    Text(
                        text = "Search",
                        color = Color(0xFF7E7E88),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Normal
                    )
                } else {
                    Text(
                        text = searchQuery,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Close / Clear 'X' button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable {
                        playFeedback()
                        if (searchQuery.isNotEmpty()) {
                            searchQuery = ""
                        } else {
                            onCloseSearch()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear / Close",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Search Results Section: Either "No recent searches" or Horizontal Emoji Strip
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(Color(0xFF101014)),
            contentAlignment = Alignment.Center
        ) {
            if (searchQuery.isEmpty()) {
                Text(
                    text = "No recent searches",
                    color = Color(0xFF888894),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal
                )
            } else {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(matchingEmojis) { emoji ->
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    playFeedback()
                                    EmojiRepository.addRecent(emoji)
                                    onEmojiSelected(emoji)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = emoji,
                                fontSize = 23.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Full QWERTY Keyboard for Typing Queries
        if (activeLayer == KeyboardLayer.ALPHA) {
            // Row 1: Numbers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                KeyboardLayout.getNumberRow().forEach { key ->
                    Keycap(
                        key = key,
                        shiftState = shiftState,
                        keysCafeController = keysCafeController,
                        settings = settings,
                        keyHeight = keyHeight * 0.9f,
                        cornerRadius = cornerRadius,
                        soundPlayer = soundPlayer,
                        onKeyPress = { searchQuery += it.primaryText },
                        modifier = Modifier.weight(key.weight)
                    )
                }
            }

            // Row 2: QWERTY
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                KeyboardLayout.getTopAlphaRow().forEach { key ->
                    val text = if (shiftState != ShiftState.UNSHIFTED) key.shiftedText else key.primaryText
                    Keycap(
                        key = key,
                        shiftState = shiftState,
                        keysCafeController = keysCafeController,
                        settings = settings,
                        keyHeight = keyHeight,
                        cornerRadius = cornerRadius,
                        soundPlayer = soundPlayer,
                        onKeyPress = {
                            searchQuery += text
                            if (shiftState == ShiftState.SHIFTED) shiftState = ShiftState.UNSHIFTED
                        },
                        modifier = Modifier.weight(key.weight)
                    )
                }
            }

            // Row 3: ASDFGHJKL (Centered with 0.5f spacers)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.weight(0.5f))
                KeyboardLayout.getHomeAlphaRow().forEach { key ->
                    val text = if (shiftState != ShiftState.UNSHIFTED) key.shiftedText else key.primaryText
                    Keycap(
                        key = key,
                        shiftState = shiftState,
                        keysCafeController = keysCafeController,
                        settings = settings,
                        keyHeight = keyHeight,
                        cornerRadius = cornerRadius,
                        soundPlayer = soundPlayer,
                        onKeyPress = {
                            searchQuery += text
                            if (shiftState == ShiftState.SHIFTED) shiftState = ShiftState.UNSHIFTED
                        },
                        modifier = Modifier.weight(key.weight)
                    )
                }
                Spacer(modifier = Modifier.weight(0.5f))
            }

            // Row 4: Shift + ZXCVBNM + Backspace
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                KeyboardLayout.getBottomAlphaRow().forEach { key ->
                    Keycap(
                        key = key,
                        shiftState = shiftState,
                        keysCafeController = keysCafeController,
                        settings = settings,
                        keyHeight = keyHeight,
                        cornerRadius = cornerRadius,
                        soundPlayer = soundPlayer,
                        onKeyPress = { k ->
                            when (k.keyType) {
                                KeyType.SHIFT -> {
                                    val now = System.currentTimeMillis()
                                    val isDouble = (now - lastShiftTapTime) < 300
                                    lastShiftTapTime = now
                                    shiftState = KeyboardLayout.getNextShiftState(shiftState, isDouble)
                                }
                                KeyType.BACKSPACE -> {
                                    if (searchQuery.isNotEmpty()) {
                                        searchQuery = searchQuery.dropLast(1)
                                    }
                                }
                                else -> {
                                    val text = if (shiftState != ShiftState.UNSHIFTED) k.shiftedText else k.primaryText
                                    searchQuery += text
                                    if (shiftState == ShiftState.SHIFTED) shiftState = ShiftState.UNSHIFTED
                                }
                            }
                        },
                        modifier = Modifier.weight(key.weight)
                    )
                }
            }

            // Row 5: Action row (!#1, comma, English (UK), period, Search)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                KeyboardLayout.getSearchActionRow().forEach { key ->
                    Keycap(
                        key = key,
                        shiftState = shiftState,
                        keysCafeController = keysCafeController,
                        settings = settings,
                        keyHeight = keyHeight,
                        cornerRadius = cornerRadius,
                        soundPlayer = soundPlayer,
                        onKeyPress = { k ->
                            when (k.keyType) {
                                KeyType.SYMBOLS_TOGGLE -> activeLayer = KeyboardLayer.SYMBOLS
                                KeyType.SPACE -> searchQuery += " "
                                KeyType.PERIOD -> searchQuery += "."
                                KeyType.CHARACTER -> searchQuery += k.primaryText
                                KeyType.ENTER -> {
                                    // Submit or close
                                }
                                else -> {}
                            }
                        },
                        modifier = Modifier.weight(key.weight)
                    )
                }
            }
        } else {
            // Symbols mode in search
            KeyRowSearch(KeyboardLayout.getSymbolsRow1(), shiftState, keysCafeController, settings, keyHeight, cornerRadius, soundPlayer) { searchQuery += it }
            KeyRowSearch(KeyboardLayout.getSymbolsRow2(), shiftState, keysCafeController, settings, keyHeight, cornerRadius, soundPlayer) { searchQuery += it }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
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
                        onKeyPress = { searchQuery += it.primaryText },
                        modifier = Modifier.weight(key.weight)
                    )
                }
                Spacer(modifier = Modifier.weight(0.5f))
            }

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
                        settings = settings,
                        keyHeight = keyHeight,
                        cornerRadius = cornerRadius,
                        soundPlayer = soundPlayer,
                        onKeyPress = { k ->
                            if (k.keyType == KeyType.BACKSPACE) {
                                if (searchQuery.isNotEmpty()) searchQuery = searchQuery.dropLast(1)
                            } else {
                                searchQuery += k.primaryText
                            }
                        },
                        modifier = Modifier.weight(key.weight)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                KeyboardLayout.getSymbolsActionRow(isSearchAction = true).forEach { key ->
                    Keycap(
                        key = key,
                        shiftState = shiftState,
                        keysCafeController = keysCafeController,
                        settings = settings,
                        keyHeight = keyHeight,
                        cornerRadius = cornerRadius,
                        soundPlayer = soundPlayer,
                        onKeyPress = { k ->
                            when (k.keyType) {
                                KeyType.SYMBOLS_TOGGLE -> activeLayer = KeyboardLayer.ALPHA
                                KeyType.SPACE -> searchQuery += " "
                                KeyType.PERIOD -> searchQuery += "."
                                else -> {}
                            }
                        },
                        modifier = Modifier.weight(key.weight)
                    )
                }
            }
        }

        // Bottom chin with Down Chevron to hide keyboard
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .background(Color(0xFF0C0C0F))
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
}

@Composable
private fun KeyRowSearch(
    keys: List<KeyModel>,
    shiftState: ShiftState,
    keysCafeController: KeysCafeController,
    settings: KeyboardSettings,
    keyHeight: androidx.compose.ui.unit.Dp,
    cornerRadius: androidx.compose.ui.unit.Dp,
    soundPlayer: KeyboardSoundPlayer?,
    onChar: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
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
                onKeyPress = { onChar(it.primaryText) },
                modifier = Modifier.weight(key.weight)
            )
        }
    }
}
