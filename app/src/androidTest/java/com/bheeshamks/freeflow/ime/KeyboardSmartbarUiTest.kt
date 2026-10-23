package com.bheeshamks.freeflow.ime

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.bheeshamks.freeflow.ime.ui.Smartbar
import com.bheeshamks.freeflow.ime.ui.SmartbarState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class KeyboardSmartbarUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testToolbarStateDisplaysUtilityIcons() {
        composeTestRule.setContent {
            Smartbar(
                state = SmartbarState.TOOLBAR,
                suggestions = emptyList(),
                onSuggestionClick = {},
                onCollapseClick = {},
                onEmojiClick = {},
                onTranslateClick = {},
                onClipboardClick = {},
                onSettingsClick = {},
                onMicClick = {},
                isMicActive = false
            )
        }

        composeTestRule.onNodeWithContentDescription("Emoji").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Translate").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Clipboard").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("FreeFlow Voice Dictation").assertIsDisplayed()
    }

    @Test
    fun testSuggestionsStateDisplaysLiveWordsAndHandlesClick() {
        var clickedSuggestion = ""
        val suggestions = listOf("The", "What", "How")

        composeTestRule.setContent {
            Smartbar(
                state = SmartbarState.SUGGESTIONS,
                suggestions = suggestions,
                onSuggestionClick = { clickedSuggestion = it },
                onCollapseClick = {},
                onEmojiClick = {},
                onTranslateClick = {},
                onClipboardClick = {},
                onSettingsClick = {},
                onMicClick = {},
                isMicActive = false
            )
        }

        composeTestRule.onNodeWithContentDescription("Collapse").assertIsDisplayed()
        composeTestRule.onNodeWithText("The").assertIsDisplayed()
        composeTestRule.onNodeWithText("What").assertIsDisplayed()
        composeTestRule.onNodeWithText("How").assertIsDisplayed()

        composeTestRule.onNodeWithText("What").performClick()
        assertEquals("What", clickedSuggestion)
    }
}
