package com.bheeshamks.freeflow.core.data.model

import androidx.compose.ui.graphics.Color

/**
 * Key-press animation presets inspired by Samsung Good Lock / Keys Cafe.
 */
enum class BloomAnimationStyle(val displayName: String) {
    RADIAL_BLOOM("Good Lock Radial Bloom"),
    SONIC_RIPPLE("Sonic Ripple"),
    NEON_PULSE("Neon Keycap Pulse"),
    MINIMALIST("Minimalist Subtle")
}

/**
 * Color presets for the Keys Cafe concentric glow ripples.
 */
enum class BloomColorPreset(
    val displayName: String,
    val primaryColor: Long,
    val outerGlowColor: Long
) {
    ICY_BLUE("Icy Blue (Samsung)", 0xFF5AB4FF, 0x555AB4FF),
    EMERALD("Emerald Cyber", 0xFF00E676, 0x5500E676),
    VIOLET("Electric Violet", 0xFFD500F9, 0x55D500F9),
    SUNSET_GOLD("Sunset Gold", 0xFFFFD600, 0x55FFD600)
}

/**
 * Haptic feedback intensities for tactile typing.
 */
enum class HapticStrength(val displayName: String) {
    OFF("Off"),
    TICK("Tick (Subtle)"),
    CLICK("Click (Standard)"),
    HEAVY_CLICK("Heavy (Crisp)")
}

/**
 * Floating Accessibility Bubble recording interaction mode.
 */
enum class DictationMode(val displayName: String) {
    HOLD_TO_TALK("Hold-to-Talk"),
    TAP_TO_TOGGLE("Tap-to-Toggle")
}

/**
 * Complete user configuration for the Custom IME Keyboard & Side Goodies Studio.
 */
data class KeyboardSettings(
    val animationStyle: BloomAnimationStyle = BloomAnimationStyle.RADIAL_BLOOM,
    val colorPreset: BloomColorPreset = BloomColorPreset.ICY_BLUE,
    val bloomRadiusDp: Float = 110f,
    val animationSpeedMs: Int = 350,
    val hapticStrength: HapticStrength = HapticStrength.CLICK,
    val keyHeightDp: Float = 48f,
    val cornerRadiusDp: Float = 8f,
    val dictationMode: DictationMode = DictationMode.HOLD_TO_TALK,
    val excludedPackages: Set<String> = emptySet(),
    val isKeysCafeEnabled: Boolean = true
)
