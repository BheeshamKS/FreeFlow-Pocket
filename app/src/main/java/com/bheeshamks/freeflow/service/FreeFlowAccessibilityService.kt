package com.bheeshamks.freeflow.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.bheeshamks.freeflow.core.audio.AudioCaptureManager
import com.bheeshamks.freeflow.core.data.db.DictationEntity
import com.bheeshamks.freeflow.core.data.db.HistoryDatabase
import com.bheeshamks.freeflow.core.data.prefs.ThemeAndGoodiesPreferences
import com.bheeshamks.freeflow.core.network.GroqWhisperClient
import com.bheeshamks.freeflow.core.security.ApiKeyStore
import com.bheeshamks.freeflow.ime.FreeFlowIMEService
import com.bheeshamks.freeflow.service.overlay.FloatingOverlayManager
import com.bheeshamks.freeflow.service.overlay.OverlayHudState
import com.bheeshamks.freeflow.service.text.TextInjector
import com.bheeshamks.freeflow.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Primary Wispr Flow-style Accessibility Service.
 * Detects focused editable text fields, mounts a floating draggable waveform capsule,
 * suppresses automatically on password/banking apps, and performs smart text injection.
 */
class FreeFlowAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "FreeFlowAccessibility"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var recordingScopeJob: Job? = null

    private lateinit var apiKeyStore: ApiKeyStore
    private lateinit var preferences: ThemeAndGoodiesPreferences
    private lateinit var audioCaptureManager: AudioCaptureManager
    private lateinit var groqClient: GroqWhisperClient
    private lateinit var historyDb: HistoryDatabase
    private lateinit var vibrator: Vibrator

    private var overlayManager: FloatingOverlayManager? = null
    private var currentFocusedNode: AccessibilityNodeInfo? = null
    private var currentPackageName: String? = null
    private var excludedPackages = setOf<String>()
    private var isBubbleEnabled: Boolean = true
    private var hideOnKeyboard: Boolean = true

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Initializing FreeFlowAccessibilityService...")

        apiKeyStore = ApiKeyStore(this)
        preferences = ThemeAndGoodiesPreferences(this)
        audioCaptureManager = AudioCaptureManager()
        groqClient = GroqWhisperClient(apiKeyProvider = { apiKeyStore.getApiKey() })
        historyDb = HistoryDatabase.getInstance(this)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        overlayManager = FloatingOverlayManager(
            service = this,
            onStartRecording = { startVoiceRecording() },
            onStopRecording = { stopVoiceRecordingAndInject() },
            onCancelRecording = { cancelVoiceRecording() },
            onDismissOverlay = { overlayManager?.hide() },
            amplitudeFlow = audioCaptureManager.amplitude,
            preferences = preferences
        )

        // Observe preferences and active keyboard visibility
        serviceScope.launch {
            combine(
                preferences.settingsFlow,
                FreeFlowIMEService.isImeVisible
            ) { settings, imeVisible ->
                Pair(settings, imeVisible)
            }.collectLatest { (settings, imeVisible) ->
                excludedPackages = settings.excludedPackages
                isBubbleEnabled = settings.isFloatingBubbleEnabled
                hideOnKeyboard = settings.hideBubbleOnKeyboard

                val isEditableFocused = currentFocusedNode?.let { isNodeEditable(it) } ?: false

                if (!isBubbleEnabled || !android.provider.Settings.canDrawOverlays(this@FreeFlowAccessibilityService)) {
                    overlayManager?.hide()
                } else if (hideOnKeyboard && (imeVisible || isEditableFocused)) {
                    overlayManager?.hide()
                } else {
                    overlayManager?.show()
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "FreeFlowAccessibilityService connected successfully.")
        if (isBubbleEnabled && android.provider.Settings.canDrawOverlays(this)) {
            overlayManager?.show()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_FOCUSED,
            AccessibilityEvent.TYPE_VIEW_CLICKED,
            AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED -> {
                val source = event.source ?: return
                currentPackageName = event.packageName?.toString()
                handleNodeFocused(source)
            }
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val packageName = event.packageName?.toString()
                currentPackageName = packageName

                // Check if the current window/app is in excluded packages
                if (packageName != null && excludedPackages.contains(packageName)) {
                    Log.d(TAG, "Excluded package active: $packageName. Suppressing overlay.")
                    overlayManager?.hide()
                    return
                }

                val rootNode = rootInActiveWindow
                if (rootNode != null) {
                    val focused = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                    if (focused != null) {
                        handleNodeFocused(focused)
                        return
                    }
                }

                // Keep overlay steadily visible if floating bubble is enabled and permission granted
                if (isBubbleEnabled && android.provider.Settings.canDrawOverlays(this)) {
                    overlayManager?.show()
                }
            }
        }
    }

    private fun handleNodeFocused(node: AccessibilityNodeInfo) {
        currentFocusedNode = node

        // 1. Automatic Password and Banking App Bypass
        if (node.isPassword) {
            Log.d(TAG, "Password field detected. Suppressing floating HUD.")
            overlayManager?.hide()
            return
        }

        // Check if package is in user-excluded apps
        val pkg = currentPackageName ?: node.packageName?.toString()
        if (pkg != null && excludedPackages.contains(pkg)) {
            Log.d(TAG, "Excluded app focused ($pkg). Suppressing floating HUD.")
            overlayManager?.hide()
            return
        }

        // Suppress floating HUD when textbox is selected if hideOnKeyboard is enabled
        if (hideOnKeyboard && (isNodeEditable(node) || FreeFlowIMEService.isImeVisible.value)) {
            Log.d(TAG, "Textbox focused or IME active. Suppressing floating HUD.")
            overlayManager?.hide()
            return
        }

        // Keep overlay active for user dictation
        if (isBubbleEnabled && android.provider.Settings.canDrawOverlays(this)) {
            overlayManager?.show()
        }
    }

    private fun isNodeEditable(node: AccessibilityNodeInfo): Boolean {
        if (node.isEditable) return true
        val className = node.className?.toString() ?: ""
        return className.contains("EditText", ignoreCase = true) ||
                className.contains("AutoCompleteTextView", ignoreCase = true) ||
                className.contains("SearchAutoComplete", ignoreCase = true)
    }

    private fun startVoiceRecording() {
        if (!apiKeyStore.hasApiKey()) {
            overlayManager?.setErrorMessage("No API Key")
            overlayManager?.setHudState(OverlayHudState.ERROR)
            android.widget.Toast.makeText(
                this,
                "Please configure your Groq API key in FreeFlow",
                android.widget.Toast.LENGTH_LONG
            ).show()
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            serviceScope.launch {
                delay(3000)
                overlayManager?.setHudState(OverlayHudState.IDLE)
            }
            return
        }

        val started = audioCaptureManager.startRecording()
        if (started) {
            triggerHaptic(VibrationEffect.EFFECT_CLICK)
            overlayManager?.setHudState(OverlayHudState.RECORDING)
        } else {
            overlayManager?.setErrorMessage("Mic Needed")
            overlayManager?.setHudState(OverlayHudState.ERROR)
            android.widget.Toast.makeText(
                this,
                "Microphone permission required for FreeFlow",
                android.widget.Toast.LENGTH_LONG
            ).show()
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            serviceScope.launch {
                delay(3000)
                overlayManager?.setHudState(OverlayHudState.IDLE)
            }
        }
    }

    private fun cancelVoiceRecording() {
        audioCaptureManager.cancelRecording()
        recordingScopeJob?.cancel()
        triggerHaptic(VibrationEffect.EFFECT_TICK)
        overlayManager?.setHudState(OverlayHudState.IDLE)
    }

    private fun stopVoiceRecordingAndInject() {
        overlayManager?.setHudState(OverlayHudState.TRANSCRIBING)

        recordingScopeJob?.cancel()
        recordingScopeJob = serviceScope.launch(Dispatchers.IO) {
            val wavBytes = audioCaptureManager.stopRecording()
            if (wavBytes == null || wavBytes.isEmpty()) {
                launch(Dispatchers.Main) {
                    overlayManager?.setHudState(OverlayHudState.IDLE)
                }
                return@launch
            }

            val result = groqClient.transcribe(wavBytes)
            launch(Dispatchers.Main) {
                result.onSuccess { transcription ->
                    triggerHaptic(VibrationEffect.EFFECT_HEAVY_CLICK)
                    overlayManager?.setHudState(OverlayHudState.SUCCESS)

                    // Inject text into the active node
                    TextInjector.injectText(
                        targetNode = currentFocusedNode,
                        textToInject = transcription.text,
                        context = this@FreeFlowAccessibilityService
                    )

                    // Store in local history Room DB
                    launch(Dispatchers.IO) {
                        historyDb.dictationDao().insertDictation(
                            DictationEntity(
                                text = transcription.text,
                                latencyMs = transcription.latencyMs,
                                targetPackage = currentPackageName
                            )
                        )
                    }

                    delay(1200)
                    overlayManager?.setHudState(OverlayHudState.IDLE)
                }.onFailure { err ->
                    Log.e(TAG, "Transcription failed in AccessibilityService", err)
                    overlayManager?.setErrorMessage(err.message ?: "Failed")
                    overlayManager?.setHudState(OverlayHudState.ERROR)
                    delay(2500)
                    overlayManager?.setHudState(OverlayHudState.IDLE)
                }
            }
        }
    }

    private fun triggerHaptic(effect: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(effect))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(30)
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "FreeFlowAccessibilityService interrupted.")
        audioCaptureManager.cancelRecording()
        overlayManager?.hide()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "FreeFlowAccessibilityService destroyed.")
        audioCaptureManager.cancelRecording()
        overlayManager?.destroy()
        overlayManager = null
        serviceScope.cancel()
    }
}
