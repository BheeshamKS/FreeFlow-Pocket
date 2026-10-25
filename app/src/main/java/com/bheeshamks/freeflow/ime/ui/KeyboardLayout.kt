package com.bheeshamks.freeflow.ime.ui

import com.bheeshamks.freeflow.ime.model.KeyModel
import com.bheeshamks.freeflow.ime.model.KeyType
import com.bheeshamks.freeflow.ime.model.ShiftState

/**
 * Standard keyboard layout definitions and state transitions matching Samsung Keyboard geometry.
 * Enforces a strict 10.0f proportional weight grid across all rows for balanced ergonomics.
 */
object KeyboardLayout {

    fun getNumberRow(): List<KeyModel> = listOf(
        KeyModel("1"), KeyModel("2"), KeyModel("3"), KeyModel("4"), KeyModel("5"),
        KeyModel("6"), KeyModel("7"), KeyModel("8"), KeyModel("9"), KeyModel("0")
    )

    fun getTopAlphaRow(): List<KeyModel> = listOf(
        KeyModel("q"), KeyModel("w"), KeyModel("e"), KeyModel("r"), KeyModel("t"),
        KeyModel("y"), KeyModel("u"), KeyModel("i"), KeyModel("o"), KeyModel("p")
    )

    fun getHomeAlphaRow(): List<KeyModel> = listOf(
        KeyModel("a"), KeyModel("s"), KeyModel("d"), KeyModel("f"), KeyModel("g"),
        KeyModel("h"), KeyModel("j"), KeyModel("k"), KeyModel("l")
    )

    fun getBottomAlphaRow(): List<KeyModel> = listOf(
        KeyModel(primaryText = "Shift", weight = 1.3f, keyType = KeyType.SHIFT),
        KeyModel("z"), KeyModel("x"), KeyModel("c"), KeyModel("v"),
        KeyModel("b"), KeyModel("n"), KeyModel("m"),
        KeyModel(primaryText = "Backspace", weight = 1.7f, keyType = KeyType.BACKSPACE)
    )

    fun getActionRow(isSearchAction: Boolean = false): List<KeyModel> = listOf(
        KeyModel(primaryText = "!#1", weight = 1.3f, keyType = KeyType.SYMBOLS_TOGGLE),
        KeyModel(primaryText = "Emoji", weight = 1.0f, keyType = KeyType.EMOJI),
        KeyModel(primaryText = "English (UK)", weight = 5.4f, keyType = KeyType.SPACE),
        KeyModel(primaryText = ".", weight = 1.0f, keyType = KeyType.PERIOD),
        if (isSearchAction) {
            KeyModel(primaryText = "Search", subtext = "search", weight = 1.3f, keyType = KeyType.ENTER)
        } else {
            KeyModel(primaryText = "Enter", weight = 1.3f, keyType = KeyType.ENTER)
        }
    )

    fun getSearchActionRow(): List<KeyModel> = listOf(
        KeyModel(primaryText = "!#1", weight = 1.3f, keyType = KeyType.SYMBOLS_TOGGLE),
        KeyModel(primaryText = ",", weight = 1.0f, keyType = KeyType.CHARACTER),
        KeyModel(primaryText = "English (UK)", weight = 5.4f, keyType = KeyType.SPACE),
        KeyModel(primaryText = ".", weight = 1.0f, keyType = KeyType.PERIOD),
        KeyModel(primaryText = "Search", subtext = "search", weight = 1.3f, keyType = KeyType.ENTER)
    )

    fun getSymbolsRow1(): List<KeyModel> = listOf(
        KeyModel("1"), KeyModel("2"), KeyModel("3"), KeyModel("4"), KeyModel("5"),
        KeyModel("6"), KeyModel("7"), KeyModel("8"), KeyModel("9"), KeyModel("0")
    )

    fun getSymbolsRow2(): List<KeyModel> = listOf(
        KeyModel("@"), KeyModel("#"), KeyModel("$"), KeyModel("_"), KeyModel("&"),
        KeyModel("-"), KeyModel("+"), KeyModel("("), KeyModel(")"), KeyModel("/")
    )

    fun getSymbolsRow3(): List<KeyModel> = listOf(
        KeyModel("*"), KeyModel("\""), KeyModel("'"), KeyModel(":"), KeyModel(";"),
        KeyModel("!"), KeyModel("?"), KeyModel("~"), KeyModel("`")
    )

    fun getSymbolsRow4(): List<KeyModel> = listOf(
        KeyModel(primaryText = "1/2", weight = 1.3f, keyType = KeyType.SYMBOLS_TOGGLE),
        KeyModel("<"), KeyModel(">"), KeyModel("{"), KeyModel("}"),
        KeyModel("["), KeyModel("]"), KeyModel("="),
        KeyModel(primaryText = "Backspace", weight = 1.7f, keyType = KeyType.BACKSPACE)
    )

    fun getSymbolsActionRow(isSearchAction: Boolean = false): List<KeyModel> = listOf(
        KeyModel(primaryText = "ABC", weight = 1.3f, keyType = KeyType.SYMBOLS_TOGGLE),
        KeyModel(primaryText = "Emoji", weight = 1.0f, keyType = KeyType.EMOJI),
        KeyModel(primaryText = "English (UK)", weight = 5.4f, keyType = KeyType.SPACE),
        KeyModel(primaryText = ".", weight = 1.0f, keyType = KeyType.PERIOD),
        if (isSearchAction) {
            KeyModel(primaryText = "Search", subtext = "search", weight = 1.3f, keyType = KeyType.ENTER)
        } else {
            KeyModel(primaryText = "Enter", weight = 1.3f, keyType = KeyType.ENTER)
        }
    )

    fun getNextShiftState(current: ShiftState, isDoubleTap: Boolean): ShiftState {
        return if (isDoubleTap) {
            ShiftState.CAPS_LOCKED
        } else {
            when (current) {
                ShiftState.UNSHIFTED -> ShiftState.SHIFTED
                ShiftState.SHIFTED -> ShiftState.UNSHIFTED
                ShiftState.CAPS_LOCKED -> ShiftState.UNSHIFTED
            }
        }
    }
}
