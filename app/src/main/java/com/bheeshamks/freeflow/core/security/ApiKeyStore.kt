package com.bheeshamks.freeflow.core.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Hardware-backed secure storage for the Groq API key using [EncryptedSharedPreferences] and [MasterKey].
 */
class ApiKeyStore(private val context: Context) {

    companion object {
        private const val TAG = "ApiKeyStore"
        private const val PREFS_FILE = "freeflow_secure_prefs"
        private const val KEY_GROQ_API_KEY = "groq_api_key"
    }

    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "Hardware Keystore initialization failed, falling back to standard private prefs.", e)
            context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        }
    }

    /**
     * Stores the user's Groq API key securely.
     */
    fun saveApiKey(apiKey: String) {
        prefs.edit().putString(KEY_GROQ_API_KEY, apiKey.trim()).apply()
    }

    /**
     * Retrieves the stored Groq API key, or null if none is configured.
     */
    fun getApiKey(): String? {
        return prefs.getString(KEY_GROQ_API_KEY, null)?.takeIf { it.isNotBlank() }
    }

    /**
     * Checks if a valid API key exists.
     */
    fun hasApiKey(): Boolean {
        return !getApiKey().isNullOrBlank()
    }

    /**
     * Clears the stored API key.
     */
    fun clearApiKey() {
        prefs.edit().remove(KEY_GROQ_API_KEY).apply()
    }

    /**
     * Returns a masked preview of the API key for safe UI display (e.g. "gsk_••••••••••••3a9F").
     */
    fun getMaskedApiKey(): String {
        val key = getApiKey() ?: return "Not configured"
        if (key.length <= 8) return "••••••••"
        val prefix = if (key.startsWith("gsk_")) "gsk_" else key.take(3)
        val suffix = key.takeLast(4)
        return "$prefix••••••••••••$suffix"
    }
}
