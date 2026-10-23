package com.bheeshamks.freeflow.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bheeshamks.freeflow.core.data.model.BloomAnimationStyle
import com.bheeshamks.freeflow.core.data.model.BloomColorPreset
import com.bheeshamks.freeflow.core.data.model.HapticStrength
import com.bheeshamks.freeflow.core.data.model.KeyboardSettings
import com.bheeshamks.freeflow.core.data.prefs.ThemeAndGoodiesPreferences
import com.bheeshamks.freeflow.ime.KeysCafeBloomCanvas
import com.bheeshamks.freeflow.ime.model.KeyModel
import com.bheeshamks.freeflow.ime.model.ShiftState
import com.bheeshamks.freeflow.ime.rememberKeysCafeController
import com.bheeshamks.freeflow.ime.ui.Keycap
import com.bheeshamks.freeflow.ui.theme.DarkCard
import com.bheeshamks.freeflow.ui.theme.DarkCardBorder
import com.bheeshamks.freeflow.ui.theme.PrimaryBlue
import com.bheeshamks.freeflow.ui.theme.TextPrimary
import com.bheeshamks.freeflow.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KeysCafeStudioScreen(
    preferences: ThemeAndGoodiesPreferences,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by preferences.settingsFlow.collectAsState(initial = KeyboardSettings())
    val previewKeysCafeController = rememberKeysCafeController()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Keys Cafe Studio",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Samsung Good Lock custom touch bloom & styling",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Live Interactive Preview Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0A0A0C))
                .border(1.dp, PrimaryBlue.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            // Live Bloom Canvas behind keys
            if (settings.isKeysCafeEnabled) {
                KeysCafeBloomCanvas(controller = previewKeysCafeController)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LIVE INTERACTIVE PREVIEW",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Touch any key to test bloom",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Preview Row 1
                val previewRow1 = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p").map { KeyModel(it) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    previewRow1.forEach { key ->
                        Keycap(
                            key = key,
                            shiftState = ShiftState.UNSHIFTED,
                            keysCafeController = previewKeysCafeController,
                            hapticStrength = settings.hapticStrength,
                            keyHeight = settings.keyHeightDp.dp,
                            cornerRadius = settings.cornerRadiusDp.dp,
                            onKeyPress = { },
                            modifier = Modifier.weight(key.weight)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Preview Row 2
                val previewRow2 = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l").map { KeyModel(it) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    previewRow2.forEach { key ->
                        Keycap(
                            key = key,
                            shiftState = ShiftState.UNSHIFTED,
                            keysCafeController = previewKeysCafeController,
                            hapticStrength = settings.hapticStrength,
                            keyHeight = settings.keyHeightDp.dp,
                            cornerRadius = settings.cornerRadiusDp.dp,
                            onKeyPress = { },
                            modifier = Modifier.weight(key.weight)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Activation Card: Enable / Switch Keyboard
        SectionCard(title = "FreeFlow Keyboard Activation", icon = Icons.Default.Keyboard) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Enable in Settings", color = Color.Black, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = {
                        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.showInputMethodPicker()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Switch Keyboard", color = PrimaryBlue, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Section: Keys Cafe Master Toggle & Presets
        SectionCard(title = "Touch Shaders & Presets", icon = Icons.Default.Palette) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Keys Cafe Glow Bloom",
                    fontSize = 14.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
                Switch(
                    checked = settings.isKeysCafeEnabled,
                    onCheckedChange = { scope.launch { preferences.setKeysCafeEnabled(it) } },
                    colors = SwitchDefaults.colors(checkedThumbColor = PrimaryBlue)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("Animation Style Preset", fontSize = 12.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(6.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BloomAnimationStyle.values().forEach { style ->
                    FilterChip(
                        selected = settings.animationStyle == style,
                        onClick = { scope.launch { preferences.updateAnimationStyle(style) } },
                        label = { Text(style.displayName, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryBlue,
                            selectedLabelColor = Color.Black
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text("Concentric Bloom Color", fontSize = 12.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                BloomColorPreset.values().forEach { preset ->
                    val isSelected = settings.colorPreset == preset
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(preset.primaryColor))
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) Color.White else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable {
                                scope.launch { preferences.updateColorPreset(preset) }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Section: Physics & Dynamics Sliders
        SectionCard(title = "Bloom Physics & Sizing", icon = Icons.Default.TouchApp) {
            Text(
                text = "Bloom Radius: ${settings.bloomRadiusDp.toInt()} dp",
                fontSize = 13.sp,
                color = TextSecondary
            )
            Slider(
                value = settings.bloomRadiusDp,
                onValueChange = { scope.launch { preferences.updateBloomRadius(it) } },
                valueRange = 60f..200f,
                colors = SliderDefaults.colors(thumbColor = PrimaryBlue, activeTrackColor = PrimaryBlue)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Animation Speed: ${settings.animationSpeedMs} ms",
                fontSize = 13.sp,
                color = TextSecondary
            )
            Slider(
                value = settings.animationSpeedMs.toFloat(),
                onValueChange = { scope.launch { preferences.updateAnimationSpeed(it.toInt()) } },
                valueRange = 150f..600f,
                colors = SliderDefaults.colors(thumbColor = PrimaryBlue, activeTrackColor = PrimaryBlue)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Key Height: ${settings.keyHeightDp.toInt()} dp",
                fontSize = 13.sp,
                color = TextSecondary
            )
            Slider(
                value = settings.keyHeightDp,
                onValueChange = { scope.launch { preferences.updateKeyHeight(it) } },
                valueRange = 40f..60f,
                colors = SliderDefaults.colors(thumbColor = PrimaryBlue, activeTrackColor = PrimaryBlue)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Corner Radius: ${settings.cornerRadiusDp.toInt()} dp",
                fontSize = 13.sp,
                color = TextSecondary
            )
            Slider(
                value = settings.cornerRadiusDp,
                onValueChange = { scope.launch { preferences.updateCornerRadius(it) } },
                valueRange = 2f..16f,
                colors = SliderDefaults.colors(thumbColor = PrimaryBlue, activeTrackColor = PrimaryBlue)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Section: Haptic Feedback Engine
        SectionCard(title = "Tactile Haptics Engine", icon = Icons.Default.Vibration) {
            Text("Haptic Feedback Intensity", fontSize = 12.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HapticStrength.values().forEach { strength ->
                    FilterChip(
                        selected = settings.hapticStrength == strength,
                        onClick = { scope.launch { preferences.updateHapticStrength(strength) } },
                        label = { Text(strength.displayName, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryBlue,
                            selectedLabelColor = Color.Black
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkCard)
            .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp))
            .padding(18.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            }
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}
