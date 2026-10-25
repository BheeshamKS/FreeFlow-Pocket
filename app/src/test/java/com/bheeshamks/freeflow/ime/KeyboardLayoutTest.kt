package com.bheeshamks.freeflow.ime

import com.bheeshamks.freeflow.ime.emoji.EmojiRepository
import com.bheeshamks.freeflow.ime.model.KeyType
import com.bheeshamks.freeflow.ime.model.KeyboardViewState
import com.bheeshamks.freeflow.ime.model.ShiftState
import com.bheeshamks.freeflow.ime.ui.KeyboardLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        assertEquals("z", bottomRow[1].primaryText.lowercase())
        assertEquals("m", bottomRow[7].primaryText.lowercase())
        assertEquals(KeyType.BACKSPACE, bottomRow.last().keyType)

        val actionRow = KeyboardLayout.getActionRow(isSearchAction = false)
        // !#1 + Emoji + Space + . + Enter = 5 keys
        assertEquals(5, actionRow.size)
        assertEquals(KeyType.SYMBOLS_TOGGLE, actionRow[0].keyType)
        assertEquals(KeyType.EMOJI, actionRow[1].keyType)
        assertEquals(KeyType.SPACE, actionRow[2].keyType)
        assertEquals(KeyType.PERIOD, actionRow[3].keyType)
        assertEquals(KeyType.ENTER, actionRow[4].keyType)
        assertEquals("Enter", actionRow[4].primaryText)
    }

    @Test
    fun testSearchActionRowStructure() {
        val searchActionRow = KeyboardLayout.getSearchActionRow()
        assertEquals(5, searchActionRow.size)
        assertEquals(KeyType.SYMBOLS_TOGGLE, searchActionRow[0].keyType)
        assertEquals(",", searchActionRow[1].primaryText)
        assertEquals(KeyType.CHARACTER, searchActionRow[1].keyType)
        assertEquals(KeyType.SPACE, searchActionRow[2].keyType)
        assertEquals(".", searchActionRow[3].primaryText)
        assertEquals("Search", searchActionRow[4].primaryText)
        assertEquals("search", searchActionRow[4].subtext)
    }

    @Test
    fun testDynamicActionKeyInActionRow() {
        val searchRow = KeyboardLayout.getActionRow(isSearchAction = true)
        assertEquals("Search", searchRow[4].primaryText)
        assertEquals("search", searchRow[4].subtext)

        val defaultRow = KeyboardLayout.getActionRow(isSearchAction = false)
        assertEquals("Enter", defaultRow[4].primaryText)
    }

    @Test
    fun testProportionalWeightGridStrict10ColumnBudget() {
        // Row 1: 10 keys x 1.0f = 10.0f
        val numWeight = KeyboardLayout.getNumberRow().sumOf { it.weight.toDouble() }
        assertEquals(10.0, numWeight, 0.001)

        // Row 2: 10 keys x 1.0f = 10.0f
        val topWeight = KeyboardLayout.getTopAlphaRow().sumOf { it.weight.toDouble() }
        assertEquals(10.0, topWeight, 0.001)

        // Row 3: Home row 9 keys x 1.0f + 0.5f start + 0.5f end = 10.0f
        val homeKeysWeight = KeyboardLayout.getHomeAlphaRow().sumOf { it.weight.toDouble() }
        assertEquals(10.0, homeKeysWeight + 1.0, 0.001)

        // Row 4: Shift (1.3f) + 7 keys (7.0f) + Backspace (1.7f) = 10.0f
        val bottomWeight = KeyboardLayout.getBottomAlphaRow().sumOf { it.weight.toDouble() }
        assertEquals(10.0, bottomWeight, 0.001)

        // Row 5: 1.3f + 1.0f + 5.4f + 1.0f + 1.3f = 10.0f
        val actionWeight = KeyboardLayout.getActionRow().sumOf { it.weight.toDouble() }
        assertEquals(10.0, actionWeight, 0.001)

        // Search Action Row weight
        val searchActionWeight = KeyboardLayout.getSearchActionRow().sumOf { it.weight.toDouble() }
        assertEquals(10.0, searchActionWeight, 0.001)

        // Symbols Row 4: 1.3f + 7.0f + 1.7f = 10.0f
        val symbolsRow4Weight = KeyboardLayout.getSymbolsRow4().sumOf { it.weight.toDouble() }
        assertEquals(10.0, symbolsRow4Weight, 0.001)

        // Symbols Action Row: 1.3f + 1.0f + 5.4f + 1.0f + 1.3f = 10.0f
        val symbolsActionWeight = KeyboardLayout.getSymbolsActionRow().sumOf { it.weight.toDouble() }
        assertEquals(10.0, symbolsActionWeight, 0.001)
    }

    @Test
    fun testSymbolsRows() {
        val s1 = KeyboardLayout.getSymbolsRow1()
        assertEquals(10, s1.size)

        val s2 = KeyboardLayout.getSymbolsRow2()
        assertEquals(10, s2.size)

        val s3 = KeyboardLayout.getSymbolsRow3()
        assertEquals(9, s3.size)

        val s4 = KeyboardLayout.getSymbolsRow4()
        assertEquals(9, s4.size)
        assertEquals(KeyType.SYMBOLS_TOGGLE, s4.first().keyType)
        assertEquals("1/2", s4.first().primaryText)
        assertEquals(KeyType.BACKSPACE, s4.last().keyType)

        val sAction = KeyboardLayout.getSymbolsActionRow()
        assertEquals(5, sAction.size)
        assertEquals("ABC", sAction.first().primaryText)
        assertEquals(KeyType.SYMBOLS_TOGGLE, sAction.first().keyType)
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

    @Test
    fun testKeyboardViewStateEnum() {
        val states = KeyboardViewState.values()
        assertTrue(states.contains(KeyboardViewState.KEYBOARD))
        assertTrue(states.contains(KeyboardViewState.EMOJI_PICKER))
        assertTrue(states.contains(KeyboardViewState.EMOJI_SEARCH))
        assertTrue(states.contains(KeyboardViewState.CLIPBOARD))
    }

    @Test
    fun testEmojiRepositoryCategoriesAndSearch() {
        // Verify all categories return non-empty lists
        EmojiRepository.Category.values().forEach { cat ->
            val list = EmojiRepository.getEmojisForCategory(cat)
            assertTrue("Category ${cat.name} should not be empty", list.isNotEmpty())
        }

        // Test search engine
        val happyResults = EmojiRepository.searchEmojis("happy")
        assertTrue("Happy search should return results", happyResults.isNotEmpty())
        assertTrue("Happy search should contain 😊", happyResults.contains("😊"))

        val loveResults = EmojiRepository.searchEmojis("love")
        assertTrue("Love search should return results", loveResults.isNotEmpty())
        assertTrue("Love search should contain ❤️", loveResults.contains("❤️"))

        val emptyResults = EmojiRepository.searchEmojis("")
        assertTrue("Empty search should return empty list", emptyResults.isEmpty())

        // Test recents list updates
        val initialRecents = EmojiRepository.getRecents()
        EmojiRepository.addRecent("🦄")
        val updatedRecents = EmojiRepository.getRecents()
        assertEquals("🦄", updatedRecents.first())
    }
}
