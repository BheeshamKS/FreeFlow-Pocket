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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bheeshamks.freeflow.R
import com.bheeshamks.freeflow.core.data.model.HapticStrength
import com.bheeshamks.freeflow.core.data.model.KeyboardSettings

/**
 * Samsung Keyboard inspired Emoji Menu matching reference specifications.
 * Includes top mode pills, search button, horizontal category strip, and 8-column emoji grid.
 */
@Composable
fun EmojiPickerView(
    settings: KeyboardSettings,
    onEmojiSelected: (String) -> Unit,
    onBackToKeyboard: () -> Unit,
    onOpenSearch: () -> Unit,
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

    var selectedCategory by remember { mutableStateOf(EmojiRepository.Category.SMILEYS) }
    var currentEmojis by remember(selectedCategory) {
        mutableStateOf(EmojiRepository.getEmojisForCategory(selectedCategory))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(Color(settings.keyboardTheme.bgHex))
            .navigationBarsPadding()
    ) {
        // Top Toolbar: Keyboard return button + Media tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(Color(0xFF141418))
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Return to QWERTY Keyboard
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { onBackToKeyboard() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_keyboard),
                    contentDescription = "Keyboard",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Tab 1: Emoji (active highlighted circular pill)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFA8C7FA)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "😊", fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Tab 2: Stickers face
            Text(
                text = "✨",
                fontSize = 18.sp,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { }
                    .padding(4.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            // Tab 3: Emoticons :-)
            Text(
                text = ":-)",
                color = Color(0xFFB0B0B8),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { }
                    .padding(4.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            // Tab 4: Kaomoji
            Text(
                text = "ʕ•ᴥ•ʔ",
                color = Color(0xFF888894),
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { }
                    .padding(4.dp)
            )
        }

        // Sub-bar: Search button + Vertical divider + 9 Category Icons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(Color(0xFF101014))
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Search Icon (opens Emoji Search screen)
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .clickable { onOpenSearch() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search Emojis",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Thin Vertical Divider
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .width(1.dp)
                    .height(20.dp)
                    .background(Color(0xFF32323C))
            )

            // Category Icons Strip
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                EmojiRepository.Category.values().forEach { cat ->
                    val isSelected = cat == selectedCategory
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Color(0xFF26334D) else Color.Transparent)
                            .clickable {
                                selectedCategory = cat
                                currentEmojis = EmojiRepository.getEmojisForCategory(cat)
                                playFeedback()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cat.icon,
                            fontSize = 17.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Main 8-Column Emojis Grid
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .background(Color(0xFF0C0C0F))
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(8),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(currentEmojis) { emoji ->
                    Box(
                        modifier = Modifier
                            .size(44.dp)
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
                            fontSize = 25.sp,
                            textAlign = TextAlign.Center
                        )
                    }
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
