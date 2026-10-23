package com.bheeshamks.freeflow.service.text

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast

/**
 * Smart text injection engine for accessibility nodes.
 * Slices transcribed speech into existing text at current cursor selection via ACTION_SET_TEXT,
 * gracefully falling back to ClipboardManager + ACTION_PASTE for WebViews and rich text fields.
 * If all automated injection methods fail, guarantees the text remains safely in the system clipboard
 * and alerts the user so dictation is never lost.
 */
object TextInjector {

    private const val TAG = "TextInjector"

    /**
     * Injects text into the target [AccessibilityNodeInfo].
     *
     * @param targetNode The currently focused editable node.
     * @param textToInject The transcribed text to splice in.
     * @param context Context for clipboard fallback access.
     * @return true if text was successfully injected, false if manually saved to clipboard.
     */
    fun injectText(
        targetNode: AccessibilityNodeInfo?,
        textToInject: String,
        context: Context
    ): Boolean {
        if (textToInject.isBlank()) {
            Log.w(TAG, "Cannot inject: text is blank.")
            return false
        }

        // Always prime clipboard as a safety net
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText("FreeFlow Dictation", textToInject)
        clipboard?.setPrimaryClip(clip)

        if (targetNode == null) {
            notifyManualPasteNeeded(context)
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
                if (start > 0 && !existingText[start - 1].isWhitespace() && !textToInject.startsWith(" ")) {
                    append(" ")
                }
                append(textToInject)
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

        // Strategy 2: Fallback to ACTION_PASTE
        return try {
            val pasteSuccess = targetNode.performAction(AccessibilityNodeInfo.ACTION_PASTE)
            if (pasteSuccess) {
                Log.i(TAG, "ACTION_PASTE fallback succeeded.")
                true
            } else {
                Log.w(TAG, "ACTION_PASTE returned false. Text remains safely in clipboard.")
                notifyManualPasteNeeded(context)
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "ACTION_PASTE fallback exception", e)
            notifyManualPasteNeeded(context)
            false
        }
    }

    private fun notifyManualPasteNeeded(context: Context) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(
                context,
                "FreeFlow: Text saved to clipboard (tap Paste to insert)",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
