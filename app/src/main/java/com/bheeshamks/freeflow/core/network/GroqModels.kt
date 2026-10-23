package com.bheeshamks.freeflow.core.network

/**
 * Result data class for speech-to-text transcriptions.
 */
data class TranscriptionResult(
    val text: String,
    val latencyMs: Long
)

/**
 * Custom exception representing Groq API errors.
 */
class GroqApiException(
    val statusCode: Int,
    message: String,
    cause: Throwable? = null
) : Exception("Groq API error [$statusCode]: $message", cause)
