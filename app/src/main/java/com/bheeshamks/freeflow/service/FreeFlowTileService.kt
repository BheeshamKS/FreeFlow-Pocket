package com.bheeshamks.freeflow.service

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import android.widget.Toast
import com.bheeshamks.freeflow.core.audio.AudioCaptureManager
import com.bheeshamks.freeflow.core.data.db.DictationEntity
import com.bheeshamks.freeflow.core.data.db.HistoryDatabase
import com.bheeshamks.freeflow.core.network.GroqWhisperClient
import com.bheeshamks.freeflow.core.security.ApiKeyStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Ninja Mode Quick Settings Tile Service.
 * Fortified with Keyguard lock-screen awareness (unlockAndRun) and clipboard fallback.
 */
class FreeFlowTileService : TileService() {

    companion object {
        private const val TAG = "FreeFlowTileService"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var apiKeyStore: ApiKeyStore
    private lateinit var audioCaptureManager: AudioCaptureManager
    private lateinit var groqClient: GroqWhisperClient
    private lateinit var historyDb: HistoryDatabase
    private lateinit var vibrator: Vibrator

    private var isRecording = false

    override fun onCreate() {
        super.onCreate()
        apiKeyStore = ApiKeyStore(this)
        audioCaptureManager = AudioCaptureManager()
        groqClient = GroqWhisperClient(apiKeyProvider = { apiKeyStore.getApiKey() })
        historyDb = HistoryDatabase.getInstance(this)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTileState(
            state = if (isRecording) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE,
            label = if (isRecording) "Listening..." else "FreeFlow Dictation"
        )
    }

    override fun onClick() {
        super.onClick()

        // Fortification: Check if the device is currently locked
        if (isLocked) {
            unlockAndRun {
                handleTileAction()
            }
        } else {
            handleTileAction()
        }
    }

    private fun handleTileAction() {
        if (!apiKeyStore.hasApiKey()) {
            Toast.makeText(this, "FreeFlow: Please configure Groq API Key first", Toast.LENGTH_LONG).show()
            return
        }

        if (!isRecording) {
            // Start recording
            val started = audioCaptureManager.startRecording()
            if (started) {
                isRecording = true
                triggerHaptic(VibrationEffect.EFFECT_CLICK)
                updateTileState(Tile.STATE_ACTIVE, "Listening...")
                Toast.makeText(this, "FreeFlow: Listening...", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "FreeFlow: Microphone unavailable", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Stop recording and transcribe
            isRecording = false
            triggerHaptic(VibrationEffect.EFFECT_TICK)
            updateTileState(Tile.STATE_UNAVAILABLE, "Transcribing...")

            serviceScope.launch(Dispatchers.IO) {
                val wavBytes = audioCaptureManager.stopRecording()
                if (wavBytes == null || wavBytes.isEmpty()) {
                    launch(Dispatchers.Main) {
                        updateTileState(Tile.STATE_INACTIVE, "FreeFlow Dictation")
                    }
                    return@launch
                }

                val result = groqClient.transcribe(wavBytes)
                launch(Dispatchers.Main) {
                    result.onSuccess { transcription ->
                        triggerHaptic(VibrationEffect.EFFECT_HEAVY_CLICK)

                        // Copy to clipboard
                        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("FreeFlow Dictation", transcription.text)
                        clipboard.setPrimaryClip(clip)

                        Toast.makeText(
                            this@FreeFlowTileService,
                            "FreeFlow: Copied to clipboard (~${transcription.latencyMs}ms)",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Log to History Room DB
                        launch(Dispatchers.IO) {
                            historyDb.dictationDao().insertDictation(
                                DictationEntity(
                                    text = transcription.text,
                                    latencyMs = transcription.latencyMs,
                                    targetPackage = "QuickSettingsTile"
                                )
                            )
                        }
                    }.onFailure { error ->
                        Log.e(TAG, "Quick Settings transcription failed", error)
                        Toast.makeText(
                            this@FreeFlowTileService,
                            "FreeFlow: Transcription error (${error.message ?: "network failure"})",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    updateTileState(Tile.STATE_INACTIVE, "FreeFlow Dictation")
                }
            }
        }
    }

    private fun updateTileState(state: Int, label: String) {
        val tile = qsTile ?: return
        tile.state = state
        tile.label = label
        tile.updateTile()
    }

    private fun triggerHaptic(effect: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(effect))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(35)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isRecording) {
            audioCaptureManager.cancelRecording()
        }
        serviceScope.cancel()
    }
}
