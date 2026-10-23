package com.bheeshamks.freeflow.service.overlay

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.PixelFormat
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.bheeshamks.freeflow.core.data.model.DictationMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Manages the floating WindowManager overlay capsule backed by a Jetpack ComposeView.
 */
class FloatingOverlayManager(
    private val service: AccessibilityService,
    private val onStartRecording: () -> Unit,
    private val onStopRecording: () -> Unit,
    private val onDismissOverlay: () -> Unit,
    private val amplitudeFlow: StateFlow<Float>
) : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    companion object {
        private const val TAG = "FloatingOverlayManager"
    }

    private val windowManager = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val viewModelStoreInstance = ViewModelStore()

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = viewModelStoreInstance
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private var composeView: ComposeView? = null
    private var windowLayoutParams: WindowManager.LayoutParams? = null
    private var isViewAttached = false

    private val _hudState = MutableStateFlow(OverlayHudState.IDLE)
    val hudState: StateFlow<OverlayHudState> = _hudState

    private val _dictationMode = MutableStateFlow(DictationMode.HOLD_TO_TALK)

    init {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    fun setDictationMode(mode: DictationMode) {
        _dictationMode.value = mode
    }

    fun setHudState(state: OverlayHudState) {
        _hudState.value = state
    }

    fun show() {
        if (isViewAttached) return

        try {
            lifecycleRegistry.currentState = Lifecycle.State.STARTED

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                y = 200 // Default margin above IME or navigation bar
            }
            windowLayoutParams = params

            val view = ComposeView(service).apply {
                setViewTreeLifecycleOwner(this@FloatingOverlayManager)
                setViewTreeViewModelStoreOwner(this@FloatingOverlayManager)
                setViewTreeSavedStateRegistryOwner(this@FloatingOverlayManager)

                setContent {
                    val state by _hudState.collectAsState()
                    val mode by _dictationMode.collectAsState()

                    FloatingWaveformHUD(
                        hudState = state,
                        amplitudeFlow = amplitudeFlow,
                        dictationMode = mode,
                        onStartRecording = onStartRecording,
                        onStopRecording = onStopRecording,
                        onCancel = {
                            _hudState.value = OverlayHudState.IDLE
                        },
                        onDismiss = onDismissOverlay
                    )
                }

                // Drag handling
                setOnTouchListener(object : View.OnTouchListener {
                    private var initialX = 0
                    private var initialY = 0
                    private var initialTouchX = 0f
                    private var initialTouchY = 0f
                    private var isDragging = false

                    override fun onTouch(v: View, event: MotionEvent): Boolean {
                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                initialX = params.x
                                initialY = params.y
                                initialTouchX = event.rawX
                                initialTouchY = event.rawY
                                isDragging = false
                                return false
                            }
                            MotionEvent.ACTION_MOVE -> {
                                val dx = (event.rawX - initialTouchX).toInt()
                                val dy = (initialTouchY - event.rawY).toInt()
                                if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                                    isDragging = true
                                    params.x = initialX + dx
                                    params.y = initialY + dy
                                    if (isViewAttached) {
                                        windowManager.updateViewLayout(v, params)
                                    }
                                }
                                return isDragging
                            }
                        }
                        return false
                    }
                })
            }

            windowManager.addView(view, params)
            composeView = view
            isViewAttached = true
            lifecycleRegistry.currentState = Lifecycle.State.RESUMED
            Log.i(TAG, "Floating HUD overlay mounted successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mount floating HUD overlay", e)
        }
    }

    fun hide() {
        if (!isViewAttached || composeView == null) return

        try {
            lifecycleRegistry.currentState = Lifecycle.State.CREATED
            windowManager.removeView(composeView)
            Log.i(TAG, "Floating HUD overlay unmounted.")
        } catch (e: Exception) {
            Log.e(TAG, "Error removing floating HUD overlay view", e)
        } finally {
            composeView = null
            isViewAttached = false
            _hudState.value = OverlayHudState.IDLE
        }
    }

    fun destroy() {
        hide()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        viewModelStoreInstance.clear()
    }
}
