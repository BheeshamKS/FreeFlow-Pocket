package com.bheeshamks.freeflow.ime.clipboard

import android.content.ClipboardManager
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages clipboard history items for the FreeFlow IME Clipboard Sheet.
 */
class ClipboardHistoryManager(context: Context) {

    private val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager

    private val defaultSampleClips = listOf(
        "506362",
        "177916",
        "1267804864",
        "so I am having little like I am building apps and ...",
        "Hello"
    )

    private val _clipsFlow = MutableStateFlow<List<String>>(defaultSampleClips)
    val clipsFlow: StateFlow<List<String>> = _clipsFlow.asStateFlow()

    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener {
        syncPrimaryClip(context)
    }

    init {
        clipboard?.addPrimaryClipChangedListener(clipListener)
        syncPrimaryClip(context)
    }

    fun syncPrimaryClip(context: Context) {
        val clip = clipboard?.primaryClip ?: return
        if (clip.itemCount > 0) {
            val text = clip.getItemAt(0)?.coerceToText(context)?.toString()?.trim()
            if (!text.isNullOrEmpty()) {
                addClip(text)
            }
        }
    }

    fun addClip(text: String) {
        val current = _clipsFlow.value.toMutableList()
        current.remove(text)
        current.add(0, text)
        if (current.size > 50) {
            current.removeAt(current.lastIndex)
        }
        _clipsFlow.value = current
    }

    fun clearAll() {
        _clipsFlow.value = emptyList()
    }
}
