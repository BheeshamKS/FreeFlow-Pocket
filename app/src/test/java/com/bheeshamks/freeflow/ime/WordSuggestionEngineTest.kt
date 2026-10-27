package com.bheeshamks.freeflow.ime

import com.bheeshamks.freeflow.ime.prediction.WordSuggestionEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WordSuggestionEngineTest {

    private lateinit var engine: WordSuggestionEngine

    @Before
    fun setUp() {
        engine = WordSuggestionEngine()
    }

    @Test
    fun testExtractPrefix() {
        assertEquals("hel", engine.extractCurrentPrefix("hello world hel"))
        assertEquals("test", engine.extractCurrentPrefix("test"))
        assertEquals("", engine.extractCurrentPrefix("hello "))
        assertEquals("", engine.extractCurrentPrefix("hello."))
        assertEquals("", engine.extractCurrentPrefix(""))
    }

    @Test
    fun testStarterWordsWhenEmpty() {
        val suggestions = engine.getSuggestions("")
        assertTrue(suggestions.isNotEmpty())
        assertEquals("I", suggestions[0])
    }

    @Test
    fun testNextWordPrediction() {
        val suggestions = engine.getSuggestions("how ")
        assertTrue(suggestions.contains("are"))
    }

    @Test
    fun testPrefixCompletion() {
        val suggestions = engine.getSuggestions("th")
        assertTrue(suggestions.isNotEmpty())
        assertTrue(suggestions.any { it.startsWith("th", ignoreCase = true) })
        assertTrue(suggestions.contains("the") || suggestions.contains("that") || suggestions.contains("this"))
    }

    @Test
    fun testExactWordDoesNotDisappear() {
        // Typing full word "how" should still offer completions or the word itself
        val suggestions = engine.getSuggestions("how")
        assertTrue(suggestions.isNotEmpty())
        assertTrue(suggestions.contains("how"))
    }

    @Test
    fun testCasingPreservation() {
        val capitalized = engine.getSuggestions("Hel")
        assertTrue(capitalized.all { it.first().isUpperCase() })

        val uppercase = engine.getSuggestions("TH")
        assertTrue(uppercase.all { it.all { ch -> ch.isUpperCase() } })
    }

    @Test
    fun testUserLearnedWordPriority() {
        engine.learnWord("supercalifragilistic")
        val suggestions = engine.getSuggestions("superc")
        assertTrue(suggestions.contains("supercalifragilistic"))
    }
}
