package com.bheeshamks.freeflow.ime

import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.net.Uri
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
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat
import com.bheeshamks.freeflow.core.audio.AudioCaptureManager
import com.bheeshamks.freeflow.core.data.db.DictationEntity
import com.bheeshamks.freeflow.core.data.db.HistoryDatabase
import com.bheeshamks.freeflow.core.data.model.KeyboardSettings
import com.bheeshamks.freeflow.core.data.prefs.ThemeAndGoodiesPreferences
import com.bheeshamks.freeflow.core.network.GroqWhisperClient
import com.bheeshamks.freeflow.core.security.ApiKeyStore
import com.bheeshamks.freeflow.ime.audio.KeyboardSoundPlayer
import com.bheeshamks.freeflow.ime.clipboard.ClipItem
import com.bheeshamks.freeflow.ime.clipboard.ClipboardHistoryManager
import com.bheeshamks.freeflow.ime.clipboard.ClipboardView
import com.bheeshamks.freeflow.ime.emoji.EmojiPickerView
import com.bheeshamks.freeflow.ime.emoji.EmojiSearchView
import com.bheeshamks.freeflow.ime.lifecycle.ImeLifecycleHost
import com.bheeshamks.freeflow.ime.model.KeyModel
import com.bheeshamks.freeflow.ime.model.KeyType
import com.bheeshamks.freeflow.ime.model.KeyboardLayer
import com.bheeshamks.freeflow.ime.model.KeyboardViewState
import com.bheeshamks.freeflow.ime.model.ShiftState
import com.bheeshamks.freeflow.ime.prediction.WordSuggestionEngine
import com.bheeshamks.freeflow.ime.ui.KeyboardLayout
import com.bheeshamks.freeflow.ime.ui.KeyboardScreen
import com.bheeshamks.freeflow.ime.ui.SmartbarState
import com.bheeshamks.freeflow.ime.ui.ToolsMenuView
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
        val isImeVisible = MutableStateFlow(false)
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var voiceDictationJob: Job? = null

    private lateinit var apiKeyStore: ApiKeyStore
    private lateinit var preferences: ThemeAndGoodiesPreferences
    private lateinit var audioCaptureManager: AudioCaptureManager
    private lateinit var groqClient: GroqWhisperClient
    private lateinit var historyDb: HistoryDatabase
    private lateinit var vibrator: Vibrator
    private lateinit var clipboardHistoryManager: ClipboardHistoryManager

    private val lifecycleHost = ImeLifecycleHost()
    private val keysCafeController = KeysCafeController()
    private val suggestionEngine = WordSuggestionEngine()

    private val settingsStateFlow = MutableStateFlow(KeyboardSettings())
    private var shiftState by mutableStateOf(ShiftState.UNSHIFTED)
    private var activeLayer by mutableStateOf(KeyboardLayer.ALPHA)
    private var smartbarState by mutableStateOf(SmartbarState.TOOLBAR)
    private var keyboardViewState by mutableStateOf(KeyboardViewState.KEYBOARD)
    private var isVoiceRecording by mutableStateOf(false)
    private var isVoiceTranscribing by mutableStateOf(false)
    private var dynamicSuggestions by mutableStateOf(listOf("I", "The", "Thanks"))
    private var composeInputView: ComposeView? = null
    private var soundPlayer: KeyboardSoundPlayer? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Creating FreeFlowIMEService...")

        apiKeyStore = ApiKeyStore(this)
        preferences = ThemeAndGoodiesPreferences(this)
        audioCaptureManager = AudioCaptureManager()
        groqClient = GroqWhisperClient(apiKeyProvider = { apiKeyStore.getApiKey() })
        historyDb = HistoryDatabase.getInstance(this)
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        soundPlayer = KeyboardSoundPlayer(this)
        clipboardHistoryManager = ClipboardHistoryManager(this)

        serviceScope.launch {
            preferences.settingsFlow.collectLatest { settings ->
                settingsStateFlow.value = settings
            }
        }
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onComputeInsets(outInsets: Insets) {
        super.onComputeInsets(outInsets)
        val view = composeInputView ?: return
        val loc = IntArray(2)
        view.getLocationInWindow(loc)
        outInsets.contentTopInsets = loc[1]
        outInsets.visibleTopInsets = loc[1]
        outInsets.touchableInsets = Insets.TOUCHABLE_INSETS_CONTENT
        outInsets.touchableRegion.set(loc[0], loc[1], loc[0] + view.width, loc[1] + view.height)
    }

    override fun onConfigureWindow(win: android.view.Window, isFullscreen: Boolean, isCandidatesOnly: Boolean) {
        super.onConfigureWindow(win, isFullscreen, isCandidatesOnly)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            win.isNavigationBarContrastEnforced = false
        }
        win.navigationBarColor = android.graphics.Color.TRANSPARENT
    }

    override fun onCreateInputView(): View {
        lifecycleHost.onStart()
        lifecycleHost.onResume()

        val decor = window?.window?.decorView
        return ComposeView(this).apply {
            composeInputView = this
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lifecycleHost.attachToView(this, decor)

            setContent {
                val currentSettings by settingsStateFlow.collectAsState()
                val clips by clipboardHistoryManager.clipsFlow.collectAsState()
                val imeAction = currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION)
                val isSearch = imeAction == EditorInfo.IME_ACTION_SEARCH

                when (keyboardViewState) {
                    KeyboardViewState.KEYBOARD -> {
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
                                val ic = currentInputConnection
                                if (ic != null) {
                                    val textBefore = ic.getTextBeforeCursor(40, 0)?.toString() ?: ""
                                    val prefix = suggestionEngine.extractCurrentPrefix(textBefore)
                                    if (prefix.isNotEmpty()) {
                                        ic.deleteSurroundingText(prefix.length, 0)
                                    }
                                    ic.commitText("$word ", 1)
                                    suggestionEngine.learnWord(word)
                                    updateDynamicSuggestions()
                                }
                                smartbarState = SmartbarState.TOOLBAR
                            },
                            onSettingsClick = {
                                val intent = Intent(this@FreeFlowIMEService, MainActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                    putExtra("navigate_to", "settings")
                                    putExtra("settings_tab", "keyboard")
                                }
                                startActivity(intent)
                            },
                            onToolsMenuClick = {
                                keyboardViewState = KeyboardViewState.TOOLS_MENU
                            },
                            onEmojiClick = {
                                keyboardViewState = KeyboardViewState.EMOJI_PICKER
                            },
                            onClipboardClick = {
                                clipboardHistoryManager.syncPrimaryClip(this@FreeFlowIMEService)
                                keyboardViewState = KeyboardViewState.CLIPBOARD
                            },
                            onTranslateClick = {
                                handleCaseToggle()
                            },
                            onToggleSmartbarState = {
                                smartbarState = if (smartbarState == SmartbarState.TOOLBAR) SmartbarState.SUGGESTIONS else SmartbarState.TOOLBAR
                            },
                            soundPlayer = soundPlayer,
                            activeLayer = activeLayer,
                            shiftState = shiftState,
                            smartbarState = smartbarState,
                            suggestions = dynamicSuggestions,
                            isSearchAction = isSearch
                        )
                    }
                    KeyboardViewState.TOOLS_MENU -> {
                        ToolsMenuView(
                            settings = currentSettings,
                            onOpenFreeFlow = {
                                val intent = Intent(this@FreeFlowIMEService, MainActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                }
                                startActivity(intent)
                            },
                            onBackToKeyboard = {
                                keyboardViewState = KeyboardViewState.KEYBOARD
                            },
                            onEmojiClick = {
                                keyboardViewState = KeyboardViewState.EMOJI_PICKER
                            },
                            onTranslateClick = {
                                handleCaseToggle()
                            },
                            onClipboardClick = {
                                clipboardHistoryManager.syncPrimaryClip(this@FreeFlowIMEService)
                                keyboardViewState = KeyboardViewState.CLIPBOARD
                            },
                            onSettingsClick = {
                                val intent = Intent(this@FreeFlowIMEService, MainActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                    putExtra("navigate_to", "settings")
                                    putExtra("settings_tab", "keyboard")
                                }
                                startActivity(intent)
                            },
                            onHideKeyboard = {
                                requestHideSelf(0)
                            }
                        )
                    }
                    KeyboardViewState.EMOJI_PICKER -> {
                        EmojiPickerView(
                            settings = currentSettings,
                            onEmojiSelected = { emoji ->
                                currentInputConnection?.commitText(emoji, 1)
                            },
                            onBackToKeyboard = {
                                keyboardViewState = KeyboardViewState.KEYBOARD
                            },
                            onOpenSearch = {
                                keyboardViewState = KeyboardViewState.EMOJI_SEARCH
                            },
                            onHideKeyboard = {
                                requestHideSelf(0)
                            }
                        )
                    }
                    KeyboardViewState.EMOJI_SEARCH -> {
                        EmojiSearchView(
                            settings = currentSettings,
                            keysCafeController = keysCafeController,
                            soundPlayer = soundPlayer,
                            onEmojiSelected = { emoji ->
                                currentInputConnection?.commitText(emoji, 1)
                            },
                            onCloseSearch = {
                                keyboardViewState = KeyboardViewState.EMOJI_PICKER
                            },
                            onHideKeyboard = {
                                requestHideSelf(0)
                            }
                        )
                    }
                    KeyboardViewState.CLIPBOARD -> {
                        ClipboardView(
                            settings = currentSettings,
                            clips = clips,
                            onPasteClip = { clipItem ->
                                val ic = currentInputConnection
                                if (ic != null) {
                                    if (clipItem.isImage && !clipItem.uriString.isNullOrEmpty()) {
                                        try {
                                            val uri = if (clipItem.uriString.startsWith("/")) {
                                                val file = java.io.File(clipItem.uriString)
                                                androidx.core.content.FileProvider.getUriForFile(this@FreeFlowIMEService, "${packageName}.fileprovider", file)
                                            } else {
                                                val parsed = Uri.parse(clipItem.uriString)
                                                if (parsed.scheme == "file") {
                                                    val file = java.io.File(parsed.path ?: "")
                                                    androidx.core.content.FileProvider.getUriForFile(this@FreeFlowIMEService, "${packageName}.fileprovider", file)
                                                } else {
                                                    parsed
                                                }
                                            }
                                            val description = android.content.ClipDescription("Clipboard Image", arrayOf("image/png", "image/jpeg"))
                                            val contentInfo = InputContentInfoCompat(uri, description, null)
                                            val flags = InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION
                                            val committed = InputConnectionCompat.commitContent(ic, currentInputEditorInfo ?: EditorInfo(), contentInfo, flags, null)
                                            if (!committed) {
                                                ic.commitText(clipItem.text, 1)
                                            }
                                        } catch (e: Exception) {
                                            ic.commitText(clipItem.text, 1)
                                        }
                                    } else {
                                        ic.commitText(clipItem.text, 1)
                                    }
                                }
                            },
                            onClearAll = {
                                clipboardHistoryManager.clearAll()
                            },
                            onBackToKeyboard = {
                                keyboardViewState = KeyboardViewState.KEYBOARD
                            },
                            onHideKeyboard = {
                                requestHideSelf(0)
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        isImeVisible.value = true
        lifecycleHost.onResume()
        shiftState = ShiftState.UNSHIFTED
        activeLayer = KeyboardLayer.ALPHA
        smartbarState = SmartbarState.TOOLBAR
        keyboardViewState = KeyboardViewState.KEYBOARD
        clipboardHistoryManager.syncPrimaryClip(this)
        updateDynamicSuggestions()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        isImeVisible.value = false
        lifecycleHost.onPause()
        if (isVoiceRecording) {
            audioCaptureManager.cancelRecording()
            isVoiceRecording = false
        }
    }

    override fun onWindowShown() {
        super.onWindowShown()
        isImeVisible.value = true
        lifecycleHost.onResume()
        clipboardHistoryManager.syncPrimaryClip(this)
        updateDynamicSuggestions()
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        isImeVisible.value = false
        lifecycleHost.onPause()
        if (isVoiceRecording) {
            audioCaptureManager.cancelRecording()
            isVoiceRecording = false
        }
    }

    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        updateDynamicSuggestions()
    }

    private fun updateDynamicSuggestions() {
        val ic = currentInputConnection
        val textBefore = ic?.getTextBeforeCursor(40, 0)?.toString() ?: ""
        val prefix = suggestionEngine.extractCurrentPrefix(textBefore)
        val list = suggestionEngine.getSuggestions(textBefore)
        dynamicSuggestions = list
        if (prefix.isNotEmpty()) {
            smartbarState = SmartbarState.SUGGESTIONS
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
                updateDynamicSuggestions()
            }
            KeyType.SPACE -> {
                ic.commitText(" ", 1)
                updateDynamicSuggestions()
                smartbarState = SmartbarState.TOOLBAR
            }
            KeyType.PERIOD -> {
                ic.commitText(".", 1)
                updateDynamicSuggestions()
            }
            KeyType.BACKSPACE -> {
                ic.deleteSurroundingText(1, 0)
                updateDynamicSuggestions()
            }
            KeyType.EMOJI -> {
                keyboardViewState = KeyboardViewState.EMOJI_PICKER
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
                updateDynamicSuggestions()
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

    private fun handleCaseToggle() {
        val ic = currentInputConnection ?: return
        val selectedText = ic.getSelectedText(0)?.toString()
        if (!selectedText.isNullOrEmpty()) {
            val toggled = if (selectedText == selectedText.uppercase()) {
                selectedText.lowercase()
            } else {
                selectedText.uppercase()
            }
            ic.commitText(toggled, 1)
            triggerHaptic(VibrationEffect.EFFECT_TICK)
        } else {
            shiftState = KeyboardLayout.getNextShiftState(shiftState, false)
        }
    }

    private fun startKeyboardVoiceDictation() {
        if (!apiKeyStore.hasApiKey()) {
            android.widget.Toast.makeText(
                this,
                "Groq API key required for voice dictation. Please set in Settings.",
                android.widget.Toast.LENGTH_LONG
            ).show()
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            return
        }

        voiceDictationJob?.cancel()
        val started = audioCaptureManager.startRecording()
        if (started) {
            isVoiceRecording = true
            isVoiceTranscribing = false
            triggerHaptic(VibrationEffect.EFFECT_HEAVY_CLICK)
        }
    }

    private fun stopKeyboardVoiceDictation() {
        if (!isVoiceRecording) return
        isVoiceRecording = false
        isVoiceTranscribing = true

        val startTime = System.currentTimeMillis()
        val audioWavBytes = audioCaptureManager.stopRecording()

        voiceDictationJob = serviceScope.launch {
            try {
                if (audioWavBytes != null && audioWavBytes.isNotEmpty()) {
                    val result = groqClient.transcribe(audioWavBytes)
                    val text = result.getOrNull()?.text?.trim()
                    val latencyMs = System.currentTimeMillis() - startTime

                    if (!text.isNullOrEmpty()) {
                        currentInputConnection?.commitText("$text ", 1)
                        triggerHaptic(VibrationEffect.EFFECT_CLICK)

                        serviceScope.launch(Dispatchers.IO) {
                            historyDb.dictationDao().insertDictation(
                                DictationEntity(
                                    text = text,
                                    durationMs = audioWavBytes.size.toLong() / 32,
                                    latencyMs = latencyMs
                                )
                            )
                        }
                    } else {
                        val error = result.exceptionOrNull()?.message ?: "Empty audio"
                        Log.e(TAG, "Transcription failed: $error")
                        android.widget.Toast.makeText(this@FreeFlowIMEService, "Transcription error: $error", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in voice dictation flow", e)
            } finally {
                isVoiceTranscribing = false
            }
        }
    }

    private fun triggerHaptic(effectId: Int) {
        val settings = settingsStateFlow.value
        if (settings.hapticStrength == com.bheeshamks.freeflow.core.data.model.HapticStrength.OFF) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(effectId))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(30)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "Destroying FreeFlowIMEService...")
        voiceDictationJob?.cancel()
        serviceScope.cancel()
        lifecycleHost.onDestroy()
    }
}
