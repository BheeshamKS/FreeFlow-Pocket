package com.bheeshamks.freeflow.core

import com.bheeshamks.freeflow.core.network.GroqWhisperClient
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GroqWhisperClientTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun testSuccessfulTranscriptionParsesJsonAndLatency() = runBlocking {
        val jsonPayload = """{"text": "Hello FreeFlow for Android"}"""
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(jsonPayload)
                .setHeader("Content-Type", "application/json")
        )

        val client = GroqWhisperClient(
            apiKeyProvider = { "gsk_test_mock_key_12345" },
            baseUrl = server.url("/openai/v1/").toString()
        )

        val dummyWav = ByteArray(44 + 3200)
        val result = client.transcribe(dummyWav)

        assertTrue("Expected successful transcription: ${result.exceptionOrNull()?.message}", result.isSuccess)
        val transcription = result.getOrThrow()
        assertEquals("Hello FreeFlow for Android", transcription.text)
        assertTrue("Latency should be non-negative", transcription.latencyMs >= 0)

        val recordedRequest = server.takeRequest()
        assertEquals("POST", recordedRequest.method)
        assertEquals("/openai/v1/audio/transcriptions", recordedRequest.path)
        assertEquals("Bearer gsk_test_mock_key_12345", recordedRequest.getHeader("Authorization"))
        val contentType = recordedRequest.getHeader("Content-Type") ?: ""
        assertTrue("Content-Type must be multipart/form-data", contentType.startsWith("multipart/form-data"))
    }

    @Test
    fun testAuthenticationFailureReturnsDescriptiveError() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("""{"error":{"message":"Invalid API Key","type":"invalid_request_error"}}""")
                .setHeader("Content-Type", "application/json")
        )

        val client = GroqWhisperClient(
            apiKeyProvider = { "invalid_key" },
            baseUrl = server.url("/openai/v1/").toString()
        )

        val result = client.transcribe(ByteArray(44))
        assertTrue("Expected failure on 401", result.isFailure)
        val exception = result.exceptionOrNull()
        val msg = exception?.message ?: ""
        assertTrue("Error should mention 401 or Invalid API Key: $msg", msg.contains("401") || msg.contains("Invalid API Key"))
    }

    @Test
    fun testRateLimit429ReturnsDescriptiveError() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(429)
                .setBody("""{"error":{"message":"Rate limit reached","type":"rate_limit_exceeded"}}""")
                .setHeader("Content-Type", "application/json")
        )

        val client = GroqWhisperClient(
            apiKeyProvider = { "gsk_valid_key" },
            baseUrl = server.url("/openai/v1/").toString()
        )

        val result = client.transcribe(ByteArray(44))
        assertTrue("Expected failure on 429", result.isFailure)
        val exception = result.exceptionOrNull()
        val msg = exception?.message ?: ""
        assertTrue("Error should mention Rate limit or 429: $msg", msg.contains("Rate limit") || msg.contains("429"))
    }

    @Test
    fun testBlankApiKeyFailsImmediatelyWithoutNetworkCall() = runBlocking {
        val client = GroqWhisperClient(
            apiKeyProvider = { "" },
            baseUrl = server.url("/openai/v1/").toString()
        )

        val result = client.transcribe(ByteArray(44))
        assertTrue("Expected failure for blank API key", result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is IllegalStateException)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun testEmptyAudioFailsImmediatelyWithoutNetworkCall() = runBlocking {
        val client = GroqWhisperClient(
            apiKeyProvider = { "gsk_key" },
            baseUrl = server.url("/openai/v1/").toString()
        )

        val result = client.transcribe(ByteArray(0))
        assertTrue("Expected failure for empty audio", result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is IllegalArgumentException)
        assertEquals(0, server.requestCount)
    }
}
