package com.bheeshamks.freeflow.ime.clipboard

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class ClipItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val uriString: String? = null,
    val isImage: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Manages clipboard history items for the FreeFlow IME Clipboard Sheet.
 * Captures both text snippets and image/screenshot URIs.
 */
class ClipboardHistoryManager(context: Context) {

    private val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager

    private val defaultSampleClips = listOf(
        ClipItem(text = "506362"),
        ClipItem(text = "177916"),
        ClipItem(text = "1267804864"),
        ClipItem(text = "so I am having little like I am building apps and ..."),
        ClipItem(text = "Hello")
    )

    private val _clipsFlow = MutableStateFlow<List<ClipItem>>(defaultSampleClips)
    val clipsFlow: StateFlow<List<ClipItem>> = _clipsFlow.asStateFlow()

    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener {
        syncPrimaryClip(context)
    }

    init {
        clipboard?.addPrimaryClipChangedListener(clipListener)
        prepareSampleImage(context)
        syncPrimaryClip(context)
    }

    private fun prepareSampleImage(context: Context) {
        try {
            val cacheDir = java.io.File(context.cacheDir, "clipboard_images").apply { if (!exists()) mkdirs() }
            val sampleFile = java.io.File(cacheDir, "sample_clip_preview.png")
            if (!sampleFile.exists()) {
                context.assets.open("logo.png").use { input ->
                    sampleFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
            if (sampleFile.exists() && sampleFile.length() > 0) {
                val current = _clipsFlow.value.toMutableList()
                if (current.none { it.uriString == sampleFile.absolutePath }) {
                    current.add(0, ClipItem(
                        text = "FreeFlow Logo",
                        uriString = sampleFile.absolutePath,
                        isImage = true
                    ))
                    _clipsFlow.value = current
                }
            }
        } catch (e: Exception) {
            // Ignore if asset missing
        }
    }

    fun syncPrimaryClip(context: Context) {
        val clip = clipboard?.primaryClip ?: return
        if (clip.itemCount > 0) {
            val item = clip.getItemAt(0) ?: return
            val desc = clip.description
            val hasImageMime = desc != null && (0 until desc.mimeTypeCount).any { i ->
                desc.getMimeType(i).startsWith("image/")
            }
            val uri = item.uri ?: item.intent?.data ?: run {
                val text = item.text?.toString()?.trim()
                if (!text.isNullOrEmpty() && (text.startsWith("content://") || text.startsWith("file://"))) {
                    try { Uri.parse(text) } catch (e: Exception) { null }
                } else null
            }

            val isImageUri = uri != null && (
                hasImageMime ||
                uri.toString().contains("image", ignoreCase = true) ||
                context.contentResolver.getType(uri)?.startsWith("image/") == true ||
                listOf(".png", ".jpg", ".jpeg", ".webp", ".gif").any { uri.toString().endsWith(it, ignoreCase = true) }
            )

            if (isImageUri && uri != null) {
                val cachedPath = cacheImageLocally(context, uri)
                val uriStr = cachedPath ?: uri.toString()
                if (uriStr.isNotEmpty()) {
                    val current = _clipsFlow.value.toMutableList()
                    if (current.none { it.uriString == uriStr }) {
                        current.add(0, ClipItem(
                            text = item.text?.toString() ?: "[Image]",
                            uriString = uriStr,
                            isImage = true
                        ))
                        if (current.size > 50) current.removeAt(current.lastIndex)
                        _clipsFlow.value = current
                    }
                }
            } else {
                val text = item.coerceToText(context)?.toString()?.trim()
                if (!text.isNullOrEmpty()) {
                    addClip(text)
                }
            }
        }
    }

    private fun cacheImageLocally(context: Context, sourceUri: Uri?): String? {
        if (sourceUri == null) return null
        return try {
            val cacheDir = java.io.File(context.cacheDir, "clipboard_images").apply { if (!exists()) mkdirs() }
            val fileName = "clip_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.png"
            val destFile = java.io.File(cacheDir, fileName)

            val stream = context.contentResolver.openInputStream(sourceUri)
            if (stream != null) {
                stream.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                if (destFile.exists() && destFile.length() > 0) {
                    destFile.absolutePath
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun addClip(text: String) {
        val current = _clipsFlow.value.toMutableList()
        current.removeAll { it.text == text && !it.isImage }
        current.add(0, ClipItem(text = text, isImage = false))
        if (current.size > 50) {
            current.removeAt(current.lastIndex)
        }
        _clipsFlow.value = current
    }

    fun clearAll() {
        _clipsFlow.value = emptyList()
    }
}
