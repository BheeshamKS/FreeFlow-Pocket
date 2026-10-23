package com.bheeshamks.freeflow.ime.model

/**
 * Types of keys rendered on the FreeFlow keyboard.
 */
enum class KeyType {
    CHARACTER,
    SHIFT,
    BACKSPACE,
    ENTER,
    SPACE,
    SYMBOLS_TOGGLE,
    EMOJI,
    PERIOD
}

/**
 * Shift toggle states for the keyboard.
 */
enum class ShiftState {
    UNSHIFTED,
    SHIFTED,
    CAPS_LOCKED
}

/**
 * Active visual layout page.
 */
enum class KeyboardLayer {
    ALPHA,
    SYMBOLS,
    MORE_SYMBOLS
}

/**
 * Representation of an individual keycap.
 */
data class KeyModel(
    val primaryText: String,
    val shiftedText: String = primaryText.uppercase(),
    val weight: Float = 1.0f,
    val keyType: KeyType = KeyType.CHARACTER,
    val subtext: String? = null
)
