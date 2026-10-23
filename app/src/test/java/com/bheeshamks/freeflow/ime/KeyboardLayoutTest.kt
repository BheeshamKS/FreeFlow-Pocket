package com.bheeshamks.freeflow.ime

import com.bheeshamks.freeflow.ime.model.KeyType
import com.bheeshamks.freeflow.ime.model.ShiftState
import com.bheeshamks.freeflow.ime.ui.KeyboardLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardLayoutTest {

    @Test
    fun testAlphaRowsContainExpectedRowCountsAndKeys() {
        val numberRow = KeyboardLayout.getNumberRow()
        assertEquals(10, numberRow.size)
        assertEquals("1", numberRow.first().primaryText)
        assertEquals("0", numberRow.last().primaryText)

        val topRow = KeyboardLayout.getTopAlphaRow()
        assertEquals(10, topRow.size)
        assertEquals("q", topRow.first().primaryText.lowercase())
        assertEquals("p", topRow.last().primaryText.lowercase())

        val homeRow = KeyboardLayout.getHomeAlphaRow()
        assertEquals(9, homeRow.size)
        assertEquals("a", homeRow.first().primaryText.lowercase())
        assertEquals("l", homeRow.last().primaryText.lowercase())

        val bottomRow = KeyboardLayout.getBottomAlphaRow()
        // Shift + 7 letters (z x c v b n m) + Backspace = 9 keys
        assertEquals(9, bottomRow.size)
        assertEquals(KeyType.SHIFT, bottomRow.first().keyType)
        assertEquals(KeyType.BACKSPACE, bottomRow.last().keyType)

        val actionRow = KeyboardLayout.getActionRow()
        // !#1 + Emoji + Space + . + Enter = 5 keys
        assertEquals(5, actionRow.size)
        assertEquals(KeyType.SYMBOLS_TOGGLE, actionRow.first().keyType)
        assertEquals(KeyType.SPACE, actionRow[2].keyType)
        assertEquals(KeyType.ENTER, actionRow.last().keyType)
    }

    @Test
    fun testShiftStateTransitions() {
        var shiftState = ShiftState.UNSHIFTED
        shiftState = KeyboardLayout.getNextShiftState(shiftState, isDoubleTap = false)
        assertEquals(ShiftState.SHIFTED, shiftState)

        shiftState = KeyboardLayout.getNextShiftState(shiftState, isDoubleTap = false)
        assertEquals(ShiftState.UNSHIFTED, shiftState)

        // Double tap triggers CAPS_LOCKED
        shiftState = KeyboardLayout.getNextShiftState(ShiftState.UNSHIFTED, isDoubleTap = true)
        assertEquals(ShiftState.CAPS_LOCKED, shiftState)

        // Tapping once while caps locked returns to unshifted
        shiftState = KeyboardLayout.getNextShiftState(ShiftState.CAPS_LOCKED, isDoubleTap = false)
        assertEquals(ShiftState.UNSHIFTED, shiftState)
    }
}
