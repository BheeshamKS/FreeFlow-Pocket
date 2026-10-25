package com.bheeshamks.freeflow.core.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/**
 * Ultra-fast Groq LPU Whisper client for speech-to-text transcriptions.
 * Uses OkHttp3 with Coroutines to stream in-memory audio bytes to Groq's API,
 * fortified with explicit network timeouts, retry policies, and defensive runCatching blocks.
 */
class GroqWhisperClient(
    private val apiKeyProvider: () -> String?,
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val client: OkHttpClient = defaultHttpClient
) {
    companion object {
        private const val TAG = "GroqWhisperClient"
        const val DEFAULT_BASE_URL = "https://api.groq.com/openai/v1/"
        const val DEFAULT_MODEL = "whisper-large-v3"

        val defaultHttpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(25, TimeUnit.SECONDS)
                .writeTimeout(25, TimeUnit.SECONDS)
                .callTimeout(35, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()
        }

        private fun logDebug(tag: String, message: String) {
            try { Log.d(tag, message) } catch (_: Throwable) { println("[$tag] $message") }
        }

        private fun logInfo(tag: String, message: String) {
            try { Log.i(tag, message) } catch (_: Throwable) { println("[$tag] $message") }
        }

        private fun logError(tag: String, message: String, throwable: Throwable? = null) {
            try { Log.e(tag, message, throwable) } catch (_: Throwable) { System.err.println("[$tag] $message: $throwable") }
        }

        /**
         * Safely extracts the transcription text from Groq Whisper JSON response,
         * with defensive fallback to manual JSON key parsing for JVM unit tests or stub environments.
         */
        internal fun parseJsonText(responseBody: String): String {
            if (responseBody.isBlank()) return ""
            val parsedText = try {
                val json = JSONObject(responseBody)
                json.optString("text", null)
            } catch (_: Throwable) {
                null
            }
            if (!parsedText.isNullOrBlank()) {
                return parsedText.trim()
            }
            return extractJsonField(responseBody, "text").trim()
        }

        /**
         * Safely extracts string value for a given JSON key using regex matching.
         */
        internal fun extractJsonField(jsonString: String, key: String): String {
            val regex = Regex("\"$key\"\\s*:\\s*\"((?:\\\\\"|[^\"])*)\"")
            val match = regex.find(jsonString)
            return match?.groups?.get(1)?.value
                ?.replace("\\\"", "\"")
                ?.replace("\\\\", "\\")
                ?.replace("\\n", "\n")
                ?.replace("\\r", "\r")
                ?.replace("\\t", "\t")
                ?: ""
        }
    }

    /**
     * Transcribes an in-memory WAV byte array using Groq Whisper.
     *
     * @param wavBytes Complete RIFF/WAVE byte array with 44-byte header.
     * @param model Whisper model identifier (default: "whisper-large-v3").
     * @param prompt Optional context prompt to guide spelling or technical terminology.
     * @return [Result] containing [TranscriptionResult] with transcribed text and latency in milliseconds.
     */
    suspend fun transcribe(
        wavBytes: ByteArray,
        model: String = DEFAULT_MODEL,
        prompt: String? = null
    ): Result<TranscriptionResult> = withContext(Dispatchers.IO) {
        runCatching {
            val apiKey = apiKeyProvider()
            if (apiKey.isNullOrBlank()) {
                throw IllegalStateException("Groq API key is not configured. Please enter your API key in Dictation Hub.")
            }

            if (wavBytes.isEmpty()) {
                throw IllegalArgumentException("Recorded audio byte array is empty.")
            }

            val startTime = System.currentTimeMillis()

            val audioMediaType = "audio/wav".toMediaType()
            val audioRequestBody = wavBytes.toRequestBody(audioMediaType, 0, wavBytes.size)

            val multipartBuilder = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", "audio.wav", audioRequestBody)
                .addFormDataPart("model", model)
                .addFormDataPart("temperature", "0")
                .addFormDataPart("response_format", "json")

            if (!prompt.isNullOrBlank()) {
                multipartBuilder.addFormDataPart("prompt", prompt)
            }

            val requestBody = multipartBuilder.build()
            val requestUrl = baseUrl.trimEnd('/') + "/audio/transcriptions"

            val request = Request.Builder()
                .url(requestUrl)
                .addHeader("Authorization", "Bearer $apiKey")
                .post(requestBody)
                .build()

            logDebug(TAG, "Streaming ${wavBytes.size} bytes audio to Groq Whisper ($model)...")

            client.newCall(request).execute().use { response ->
                val latency = System.currentTimeMillis() - startTime
                val responseBody = response.body?.string().orEmpty()

                if (!response.isSuccessful) {
                    val errorMessage = try {
                        val json = JSONObject(responseBody)
                        val errObj = json.optJSONObject("error")
                        errObj?.optString("message", null) ?: extractJsonField(responseBody, "message")
                    } catch (_: Throwable) {
                        extractJsonField(responseBody, "message")
                    }.ifBlank { responseBody.ifBlank { "HTTP ${response.code}" } }

                    logError(TAG, "Groq transcription failed [HTTP ${response.code}]: $errorMessage")
                    throw GroqApiException(response.code, errorMessage)
                }

                val rawText = parseJsonText(responseBody)
                logInfo(TAG, "Transcription completed in ${latency}ms: '$rawText'")

                TranscriptionResult(text = rawText, latencyMs = latency)
            }
        }.recoverCatching { throwable ->
            when (throwable) {
                is SocketTimeoutException -> {
                    logError(TAG, "Groq Whisper API request timed out", throwable)
                    throw IOException("Connection to Groq timed out. Please check your network.", throwable)
                }
                is UnknownHostException -> {
                    logError(TAG, "Unable to resolve Groq host", throwable)
                    throw IOException("No internet connection available.", throwable)
                }
                else -> throw throwable
            }
        }
    }

    /**
     * Tests connectivity and validates the API key against the Groq models endpoint.
     *
     * @param explicitApiKey Optional API key to validate, defaults to key from [apiKeyProvider].
     * @return [Result] with count of available models, or exception on failure.
     */
    suspend fun testConnection(explicitApiKey: String? = null): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val apiKey = explicitApiKey?.ifBlank { null } ?: apiKeyProvider()
            if (apiKey.isNullOrBlank()) {
                throw IllegalStateException("API key is blank.")
            }

            val requestUrl = baseUrl.trimEnd('/') + "/models"
            val request = Request.Builder()
                .url(requestUrl)
                .addHeader("Authorization", "Bearer $apiKey")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val errorMessage = try {
                        val json = JSONObject(responseBody)
                        val errObj = json.optJSONObject("error")
                        errObj?.optString("message", null) ?: extractJsonField(responseBody, "message")
                    } catch (_: Throwable) {
                        extractJsonField(responseBody, "message")
                    }.ifBlank { "HTTP ${response.code}" }
                    throw GroqApiException(response.code, errorMessage)
                }

                val modelCount = try {
                    val json = JSONObject(responseBody)
                    json.optJSONArray("data")?.length() ?: 0
                } catch (_: Throwable) {
                    Regex("\"id\"\\s*:").findAll(responseBody).count()
                }
                modelCount
            }
        }.recoverCatching { throwable ->
            when (throwable) {
                is SocketTimeoutException -> throw IOException("Connection test timed out.", throwable)
                is UnknownHostException -> throw IOException("Unable to reach Groq servers. Check internet.", throwable)
                else -> throw throwable
            }
        }
    }
}
