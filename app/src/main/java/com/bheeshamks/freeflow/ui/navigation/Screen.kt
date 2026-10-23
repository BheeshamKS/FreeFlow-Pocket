package com.bheeshamks.freeflow.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object DictationHub : Screen("dictation_hub", "Dictation Hub", Icons.Default.Mic)
    object History : Screen("history", "History", Icons.Default.History)
    object KeysCafeStudio : Screen("keys_cafe", "Keys Cafe", Icons.Default.Keyboard)

    companion object {
        val items = listOf(DictationHub, History, KeysCafeStudio)
    }
}
