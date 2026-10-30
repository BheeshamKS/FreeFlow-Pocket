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
    SUNSET_GOLD("Sunset Gold", 0xFFFFD600, 0x55FFD600),
    ROSE_NEON("Rose Neon", 0xFFFF1744, 0x55FF1744),
    CYAN_ELECTRIC("Electric Cyan", 0xFF00E5FF, 0x5500E5FF)
}

/**
 * Aesthetic color themes for the Keyboard background and keycaps.
 */
enum class KeyboardTheme(
    val displayName: String,
    val bgHex: Long,
    val keyHex: Long,
    val specialKeyHex: Long
) {
    AMOLED_BLACK("AMOLED Black", 0xFF0D0D11, 0xFF18181C, 0xFF141418),
    DEEP_NAVY("Deep Navy", 0xFF0A0F1D, 0xFF131D31, 0xFF0F1726),
    SLATE_CHARCOAL("Slate Charcoal", 0xFF16171D, 0xFF21232B, 0xFF1B1C22),
    CYBER_PURPLE("Cyber Purple", 0xFF0E0A1A, 0xFF1C1433, 0xFF150F26),
    PURE_MATTE("Pure Matte", 0xFF000000, 0xFF101010, 0xFF080808)
}

/**
 * Keycap border styling: seamless/no-border, subtle outline, or active glow.
 */
enum class KeyBorderStyle(val displayName: String) {
    NO_BORDER("No Border (Seamless)"),
    SUBTLE("Subtle Outline"),
    GLOW_ACCENT("Glow Accent")
}

/**
 * Height presets for keycap touch target size.
 */
enum class KeyHeightPreset(val displayName: String, val heightDp: Float) {
    COMPACT("Compact (44dp)", 44f),
    STANDARD("Standard (48dp)", 48f),
    TALL("Comfort (54dp)", 54f)
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
 * Keypress audio sound themes.
 */
enum class KeySoundTheme(val displayName: String, val description: String) {
    OFF("Mute", "Silent typing"),
    SYSTEM("System Click", "Standard Android keypress click"),
    MECHANICAL("Mechanical Thock", "Crisp physical mechanical switch feedback"),
    BUBBLE_POP("Bubble Pop", "Liquid pop droplet effect"),
    SOFT_TAP("Soft Tap", "Subtle dampened Samsung-style tap")
}

/**
 * Data holder for floating key preview callout above finger.
 */
data class KeyPreviewInfo(
    val text: String,
    val rootOffset: androidx.compose.ui.geometry.Offset,
    val keyWidthPx: Float,
    val keyHeightPx: Float
)

/**
 * Complete user configuration for the Custom IME Keyboard & Side Goodies Studio.
 */
data class KeyboardSettings(
    val animationStyle: BloomAnimationStyle = BloomAnimationStyle.RADIAL_BLOOM,
    val colorPreset: BloomColorPreset = BloomColorPreset.ICY_BLUE,
    val keyboardTheme: KeyboardTheme = KeyboardTheme.AMOLED_BLACK,
    val borderStyle: KeyBorderStyle = KeyBorderStyle.SUBTLE,
    val showNumberRow: Boolean = true,
    val showKeyPreviewPopup: Boolean = true,
    val soundTheme: KeySoundTheme = KeySoundTheme.MECHANICAL,
    val soundVolume: Float = 0.75f,
    val bloomRadiusDp: Float = 110f,
    val animationSpeedMs: Int = 350,
    val hapticStrength: HapticStrength = HapticStrength.CLICK,
    val keyHeightDp: Float = 48f,
    val cornerRadiusDp: Float = 8f,
    val dictationMode: DictationMode = DictationMode.TAP_TO_TOGGLE,
    val excludedPackages: Set<String> = emptySet(),
    val isKeysCafeEnabled: Boolean = true,
    val isOnboardingCompleted: Boolean = false,
    val isFloatingBubbleEnabled: Boolean = true,
    val floatingBubbleOpacity: Float = 0.5f,
    val hideBubbleOnKeyboard: Boolean = true,
    val floatingBubbleDockedOnRight: Boolean = true,
    val floatingBubbleYRatio: Float = 0.45f,
    val groqModel: String = "whisper-large-v3"
)
