package com.bheeshamks.freeflow.service.overlay

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.util.Log
import android.view.Gravity
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
import com.bheeshamks.freeflow.ui.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Manages the floating WindowManager overlay capsule backed by a Jetpack ComposeView.
 * Fortified with magnetic edge snapping, middle-right default placement, and smooth drag mechanics.
 */
class FloatingOverlayManager(
    private val service: AccessibilityService,
    private val onStartRecording: () -> Unit,
    private val onStopRecording: () -> Unit,
    private val onCancelRecording: () -> Unit,
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

    private val _isDockedOnRight = MutableStateFlow(true)
    val isDockedOnRight: StateFlow<Boolean> = _isDockedOnRight

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    init {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    fun setHudState(state: OverlayHudState) {
        _hudState.value = state
    }

    fun setErrorMessage(message: String?) {
        _errorMessage.value = message
    }

    private fun updatePositionForNewSize(newWidth: Int, newHeight: Int) {
        val p = windowLayoutParams ?: return
        if (newWidth <= 0 || newHeight <= 0) return

        val metrics = service.resources.displayMetrics
        val screenWidth = metrics.widthPixels
        val screenHeight = metrics.heightPixels
        val edgeMarginPx = (12 * metrics.density).toInt()

        if (_isDockedOnRight.value) {
            // Anchor to right screen edge so the entire expanded capsule stays inside
            p.x = screenWidth - newWidth - edgeMarginPx
        } else {
            p.x = edgeMarginPx
        }

        // Clamp Y so it never renders off the top or bottom edge
        val topLimit = (80 * metrics.density).toInt()
        val bottomLimit = screenHeight - newHeight - (100 * metrics.density).toInt()
        p.y = p.y.coerceIn(topLimit, bottomLimit.coerceAtLeast(topLimit))

        if (isViewAttached && composeView != null) {
            try {
                windowManager.updateViewLayout(composeView, p)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating window layout for new size", e)
            }
        }
    }

    fun show() {
        if (isViewAttached) return

        try {
            lifecycleRegistry.currentState = Lifecycle.State.STARTED

            val displayMetrics = service.resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val screenHeight = displayMetrics.heightPixels
            val edgeMarginPx = (16 * displayMetrics.density).toInt()

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = screenWidth - edgeMarginPx - (56 * displayMetrics.density).toInt()
                y = (screenHeight * 0.45f).toInt()
            }
            windowLayoutParams = params

            val view = ComposeView(service).apply {
                setViewTreeLifecycleOwner(this@FloatingOverlayManager)
                setViewTreeViewModelStoreOwner(this@FloatingOverlayManager)
                setViewTreeSavedStateRegistryOwner(this@FloatingOverlayManager)

                setContent {
                    val state by _hudState.collectAsState()
                    val dockedOnRight by _isDockedOnRight.collectAsState()
                    val err by _errorMessage.collectAsState()

                    FloatingWaveformHUD(
                        hudState = state,
                        isDockedOnRight = dockedOnRight,
                        errorMessage = err,
                        amplitudeFlow = amplitudeFlow,
                        onStartRecording = onStartRecording,
                        onStopRecording = onStopRecording,
                        onCancel = onCancelRecording,
                        onErrorClick = {
                            val intent = Intent(service, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            service.startActivity(intent)
                        },
                        onDragDelta = { dx, dy ->
                            windowLayoutParams?.let { p ->
                                p.x += dx.toInt()
                                p.y += dy.toInt()
                                if (isViewAttached && composeView != null) {
                                    windowManager.updateViewLayout(composeView, p)
                                }
                            }
                        },
                        onDragEnd = {
                            windowLayoutParams?.let { p ->
                                val metrics = service.resources.displayMetrics
                                val width = metrics.widthPixels
                                val height = metrics.heightPixels
                                val viewWidth = composeView?.width ?: (56 * metrics.density).toInt()
                                val viewHeight = composeView?.height ?: (56 * metrics.density).toInt()

                                val goesToRight = (p.x + viewWidth / 2) > (width / 2)
                                _isDockedOnRight.value = goesToRight

                                p.x = if (goesToRight) {
                                    width - viewWidth - (12 * metrics.density).toInt()
                                } else {
                                    (12 * metrics.density).toInt()
                                }

                                // Clamp Y within vertical screen limits
                                val topLimit = (80 * metrics.density).toInt()
                                val bottomLimit = height - viewHeight - (100 * metrics.density).toInt()
                                p.y = p.y.coerceIn(topLimit, bottomLimit)

                                if (isViewAttached && composeView != null) {
                                    windowManager.updateViewLayout(composeView, p)
                                }
                            }
                        },
                        onContentSizeChanged = { w, h ->
                            updatePositionForNewSize(w, h)
                        }
                    )
                }
            }

            windowManager.addView(view, params)
            composeView = view
            isViewAttached = true
            lifecycleRegistry.currentState = Lifecycle.State.RESUMED
            Log.i(TAG, "Floating HUD overlay mounted successfully at middle-right.")
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
