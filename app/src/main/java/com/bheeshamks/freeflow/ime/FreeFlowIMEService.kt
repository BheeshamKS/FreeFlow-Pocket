package com.bheeshamks.freeflow.ime

import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import com.bheeshamks.freeflow.core.audio.AudioCaptureManager
import com.bheeshamks.freeflow.core.data.db.DictationEntity
import com.bheeshamks.freeflow.core.data.db.HistoryDatabase
import com.bheeshamks.freeflow.core.data.model.KeyboardSettings
import com.bheeshamks.freeflow.core.data.prefs.ThemeAndGoodiesPreferences
import com.bheeshamks.freeflow.core.network.GroqWhisperClient
import com.bheeshamks.freeflow.core.security.ApiKeyStore
import com.bheeshamks.freeflow.ime.lifecycle.ImeLifecycleHost
import com.bheeshamks.freeflow.ime.model.KeyModel
import com.bheeshamks.freeflow.ime.model.KeyType
import com.bheeshamks.freeflow.ime.model.KeyboardLayer
import com.bheeshamks.freeflow.ime.model.ShiftState
import com.bheeshamks.freeflow.ime.ui.KeyboardLayout
import com.bheeshamks.freeflow.ime.ui.KeyboardScreen
import com.bheeshamks.freeflow.ime.ui.SmartbarState
import com.bheeshamks.freeflow.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Custom InputMethodService powering the Samsung Good Lock inspired FreeFlow Keyboard.
 */
class FreeFlowIMEService : InputMethodService() {

    companion object {
        private const val TAG = "FreeFlowIMEService"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var voiceDictationJob: Job? = null

    private lateinit var apiKeyStore: ApiKeyStore
    private lateinit var preferences: ThemeAndGoodiesPreferences
    private lateinit var audioCaptureManager: AudioCaptureManager
    private lateinit var groqClient: GroqWhisperClient
    private lateinit var historyDb: HistoryDatabase
    private lateinit var vibrator: Vibrator

    private val lifecycleHost = ImeLifecycleHost()
    private val keysCafeController = KeysCafeController()

    private val settingsStateFlow = MutableStateFlow(KeyboardSettings())
    private var shiftState by mutableStateOf(ShiftState.UNSHIFTED)
    private var activeLayer by mutableStateOf(KeyboardLayer.ALPHA)
    private var smartbarState by mutableStateOf(SmartbarState.TOOLBAR)
    private var isVoiceRecording by mutableStateOf(false)
    private var isVoiceTranscribing by mutableStateOf(false)

    private val sampleSuggestions = listOf("The", "What", "How")

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Creating FreeFlowIMEService...")

        apiKeyStore = ApiKeyStore(this)
        preferences = ThemeAndGoodiesPreferences(this)
        audioCaptureManager = AudioCaptureManager()
        groqClient = GroqWhisperClient(apiKeyProvider = { apiKeyStore.getApiKey() })
        historyDb = HistoryDatabase.getInstance(this)
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator

        serviceScope.launch {
            preferences.settingsFlow.collectLatest { settings ->
                settingsStateFlow.value = settings
            }
        }
    }

    override fun onCreateInputView(): View {
        lifecycleHost.onStart()
        lifecycleHost.onResume()

        return ComposeView(this).apply {
            lifecycleHost.attachToView(this)

            setContent {
                val currentSettings by settingsStateFlow.collectAsState()

                KeyboardScreen(
                    settings = currentSettings,
                    keysCafeController = keysCafeController,
                    amplitudeFlow = audioCaptureManager.amplitude,
                    isVoiceRecording = isVoiceRecording,
                    isVoiceTranscribing = isVoiceTranscribing,
                    onKeyPress = { key -> handleKeyInput(key) },
                    onShiftPress = { isDoubleTap ->
                        shiftState = KeyboardLayout.getNextShiftState(shiftState, isDoubleTap)
                    },
                    onSymbolsToggle = {
                        activeLayer = if (activeLayer == KeyboardLayer.ALPHA) KeyboardLayer.SYMBOLS else KeyboardLayer.ALPHA
                    },
                    onCursorDrag = { deltaX ->
                        handleCursorDrag(deltaX)
                    },
                    onHideKeyboard = {
                        requestHideSelf(0)
                    },
                    onMicClick = {
                        if (!isVoiceRecording && !isVoiceTranscribing) {
                            startKeyboardVoiceDictation()
                        } else {
                            stopKeyboardVoiceDictation()
                        }
                    },
                    onStopMicClick = {
                        stopKeyboardVoiceDictation()
                    },
                    onSuggestionClick = { word ->
                        currentInputConnection?.commitText("$word ", 1)
                        smartbarState = SmartbarState.TOOLBAR
                    },
                    onSettingsClick = {
                        val intent = Intent(this@FreeFlowIMEService, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(intent)
                    },
                    activeLayer = activeLayer,
                    shiftState = shiftState,
                    smartbarState = smartbarState,
                    suggestions = sampleSuggestions
                )
            }
        }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        lifecycleHost.onResume()
        shiftState = ShiftState.UNSHIFTED
        activeLayer = KeyboardLayer.ALPHA
        smartbarState = SmartbarState.TOOLBAR
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        lifecycleHost.onPause()
        if (isVoiceRecording) {
            audioCaptureManager.cancelRecording()
            isVoiceRecording = false
        }
    }

    private fun handleKeyInput(key: KeyModel) {
        val ic = currentInputConnection ?: return

        when (key.keyType) {
            KeyType.CHARACTER -> {
                val textToCommit = when (shiftState) {
                    ShiftState.SHIFTED, ShiftState.CAPS_LOCKED -> key.shiftedText
                    ShiftState.UNSHIFTED -> key.primaryText.lowercase()
                }
                ic.commitText(textToCommit, 1)

                // Return to lowercase if single shifted
                if (shiftState == ShiftState.SHIFTED) {
                    shiftState = ShiftState.UNSHIFTED
                }
                smartbarState = SmartbarState.SUGGESTIONS
            }
            KeyType.SPACE -> {
                ic.commitText(" ", 1)
                smartbarState = SmartbarState.TOOLBAR
            }
            KeyType.PERIOD -> {
                ic.commitText(".", 1)
            }
            KeyType.BACKSPACE -> {
                ic.deleteSurroundingText(1, 0)
            }
            KeyType.ENTER -> {
                val editorInfo = currentInputEditorInfo
                val imeAction = editorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION)
                if (imeAction != null && imeAction != EditorInfo.IME_ACTION_NONE && imeAction != EditorInfo.IME_ACTION_UNSPECIFIED) {
                    ic.performEditorAction(imeAction)
                } else {
                    sendKeyChar('\n')
                }
                smartbarState = SmartbarState.TOOLBAR
            }
            else -> {
                ic.commitText(key.primaryText, 1)
            }
        }
    }

    private var accumulatedDrag = 0f
    private fun handleCursorDrag(deltaX: Float) {
        accumulatedDrag += deltaX
        val threshold = 28f // dp threshold per character jump
        if (accumulatedDrag > threshold) {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_RIGHT)
            accumulatedDrag = 0f
        } else if (accumulatedDrag < -threshold) {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_LEFT)
            accumulatedDrag = 0f
        }
    }

    private fun startKeyboardVoiceDictation() {
        if (!apiKeyStore.hasApiKey()) {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            return
        }

        val started = audioCaptureManager.startRecording()
        if (started) {
            isVoiceRecording = true
            isVoiceTranscribing = false
            triggerHaptic(VibrationEffect.EFFECT_CLICK)
        }
    }

    private fun stopKeyboardVoiceDictation() {
        if (!isVoiceRecording && !isVoiceTranscribing) return

        isVoiceRecording = false
        isVoiceTranscribing = true
        triggerHaptic(VibrationEffect.EFFECT_TICK)

        voiceDictationJob?.cancel()
        voiceDictationJob = serviceScope.launch(Dispatchers.IO) {
            val wavBytes = audioCaptureManager.stopRecording()
            if (wavBytes == null || wavBytes.isEmpty()) {
                launch(Dispatchers.Main) {
                    isVoiceTranscribing = false
                }
                return@launch
            }

            val result = groqClient.transcribe(wavBytes)
            launch(Dispatchers.Main) {
                isVoiceTranscribing = false
                result.onSuccess { transcription ->
                    triggerHaptic(VibrationEffect.EFFECT_HEAVY_CLICK)
                    currentInputConnection?.commitText(transcription.text, 1)

                    launch(Dispatchers.IO) {
                        historyDb.dictationDao().insertDictation(
                            DictationEntity(
                                text = transcription.text,
                                latencyMs = transcription.latencyMs,
                                targetPackage = currentInputEditorInfo?.packageName
                            )
                        )
                    }
                }.onFailure { err ->
                    Log.e(TAG, "Keyboard voice dictation failed", err)
                }
            }
        }
    }

    private fun triggerHaptic(effect: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(effect))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(25)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleHost.onDestroy()
        serviceScope.cancel()
    }
}
