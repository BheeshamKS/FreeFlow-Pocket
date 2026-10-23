package com.bheeshamks.freeflow.service.text

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Smart text injection engine for accessibility nodes.
 * Slices transcribed speech into existing text at current cursor selection via ACTION_SET_TEXT,
 * gracefully falling back to ClipboardManager + ACTION_PASTE for WebViews and rich text fields.
 */
object TextInjector {

    private const val TAG = "TextInjector"

    /**
     * Injects text into the target [AccessibilityNodeInfo].
     *
     * @param targetNode The currently focused editable node.
     * @param textToInject The transcribed text to splice in.
     * @param context Context for clipboard fallback access.
     * @return true if text was successfully injected, false otherwise.
     */
    fun injectText(
        targetNode: AccessibilityNodeInfo?,
        textToInject: String,
        context: Context
    ): Boolean {
        if (targetNode == null || textToInject.isBlank()) {
            Log.w(TAG, "Cannot inject: target node is null or text is blank.")
            return false
        }

        // Strategy 1: Smart cursor-spliced ACTION_SET_TEXT
        try {
            val existingText = targetNode.text?.toString() ?: ""
            val selectionStart = targetNode.textSelectionStart.takeIf { it >= 0 } ?: existingText.length
            val selectionEnd = targetNode.textSelectionEnd.takeIf { it >= 0 } ?: existingText.length

            val start = minOf(selectionStart, selectionEnd).coerceIn(0, existingText.length)
            val end = maxOf(selectionStart, selectionEnd).coerceIn(0, existingText.length)

            val splicedText = buildString {
                append(existingText.substring(0, start))
                // Add leading space if needed
                if (start > 0 && !existingText[start - 1].isWhitespace() && !textToInject.startsWith(" ")) {
                    append(" ")
                }
                append(textToInject)
                // Add trailing space if needed
                if (end < existingText.length && !existingText[end].isWhitespace() && !textToInject.endsWith(" ")) {
                    append(" ")
                }
                append(existingText.substring(end))
            }

            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, splicedText)
            }

            val setTextSuccess = targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            if (setTextSuccess) {
                Log.i(TAG, "ACTION_SET_TEXT succeeded. Injected ${textToInject.length} characters.")
                return true
            } else {
                Log.w(TAG, "ACTION_SET_TEXT returned false. Attempting clipboard paste fallback...")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing ACTION_SET_TEXT, attempting fallback", e)
        }

        // Strategy 2: Fallback to ClipboardManager + ACTION_PASTE
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val originalClip = clipboard.primaryClip

            val clip = ClipData.newPlainText("FreeFlow Dictation", textToInject)
            clipboard.setPrimaryClip(clip)

            val pasteSuccess = targetNode.performAction(AccessibilityNodeInfo.ACTION_PASTE)
            Log.i(TAG, "ACTION_PASTE result: $pasteSuccess")

            pasteSuccess
        } catch (e: Exception) {
            Log.e(TAG, "ACTION_PASTE fallback failed", e)
            false
        }
    }
}
