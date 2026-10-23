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
import java.util.concurrent.TimeUnit

/**
 * Ultra-fast Groq LPU Whisper client for speech-to-text transcriptions.
 * Uses OkHttp3 with Coroutines to stream in-memory audio bytes to Groq's API.
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
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()
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
        val apiKey = apiKeyProvider()
        if (apiKey.isNullOrBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Groq API key is not configured. Please enter your API key in Dictation Hub.")
            )
        }

        if (wavBytes.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Recorded audio byte array is empty."))
        }

        val startTime = System.currentTimeMillis()

        try {
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

            Log.d(TAG, "Sending ${wavBytes.size} bytes audio to Groq Whisper ($model)...")

            client.newCall(request).execute().use { response ->
                val latency = System.currentTimeMillis() - startTime
                val responseBody = response.body?.string().orEmpty()

                if (!response.isSuccessful) {
                    val errorMessage = try {
                        val json = JSONObject(responseBody)
                        json.optJSONObject("error")?.optString("message") ?: responseBody
                    } catch (e: Exception) {
                        responseBody.ifBlank { "HTTP ${response.code}" }
                    }
                    Log.e(TAG, "Groq transcription failed [HTTP ${response.code}]: $errorMessage")
                    return@withContext Result.failure(
                        GroqApiException(response.code, errorMessage)
                    )
                }

                val json = JSONObject(responseBody)
                val rawText = json.optString("text", "").trim()
                Log.i(TAG, "Transcription completed in ${latency}ms: '$rawText'")

                Result.success(TranscriptionResult(text = rawText, latencyMs = latency))
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network I/O exception during Groq transcription", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during Groq transcription", e)
            Result.failure(e)
        }
    }

    /**
     * Tests connectivity and validates the API key against the Groq models endpoint.
     *
     * @return [Result] with count of available models, or exception on failure.
     */
    suspend fun testConnection(): Result<Int> = withContext(Dispatchers.IO) {
        val apiKey = apiKeyProvider()
        if (apiKey.isNullOrBlank()) {
            return@withContext Result.failure(IllegalStateException("API key is blank."))
        }

        try {
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
                        json.optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}"
                    } catch (e: Exception) {
                        "HTTP ${response.code}"
                    }
                    return@withContext Result.failure(GroqApiException(response.code, errorMessage))
                }

                val json = JSONObject(responseBody)
                val dataArray = json.optJSONArray("data")
                val count = dataArray?.length() ?: 0
                Result.success(count)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
