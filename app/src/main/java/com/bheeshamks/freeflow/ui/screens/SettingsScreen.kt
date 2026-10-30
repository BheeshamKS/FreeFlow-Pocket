package com.bheeshamks.freeflow.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.bheeshamks.freeflow.core.data.model.BloomAnimationStyle
import com.bheeshamks.freeflow.core.data.model.BloomColorPreset
import com.bheeshamks.freeflow.core.data.model.DictationMode
import com.bheeshamks.freeflow.core.data.model.HapticStrength
import com.bheeshamks.freeflow.core.data.model.KeyBorderStyle
import com.bheeshamks.freeflow.core.data.model.KeySoundTheme
import com.bheeshamks.freeflow.core.data.model.KeyboardSettings
import com.bheeshamks.freeflow.core.data.model.KeyboardTheme
import kotlinx.coroutines.launch
import org.json.JSONObject
import com.bheeshamks.freeflow.core.data.prefs.ThemeAndGoodiesPreferences
import com.bheeshamks.freeflow.core.network.GroqWhisperClient
import com.bheeshamks.freeflow.core.security.ApiKeyStore
import com.bheeshamks.freeflow.ime.audio.KeyboardSoundPlayer
import com.bheeshamks.freeflow.service.FreeFlowAccessibilityService
import com.bheeshamks.freeflow.ui.theme.AccentGreen
import com.bheeshamks.freeflow.ui.theme.AccentRed
import com.bheeshamks.freeflow.ui.theme.DarkBackground
import com.bheeshamks.freeflow.ui.theme.DarkCard
import com.bheeshamks.freeflow.ui.theme.DarkCardBorder
import com.bheeshamks.freeflow.ui.theme.PrimaryBlue
import com.bheeshamks.freeflow.ui.theme.TextPrimary
import com.bheeshamks.freeflow.ui.theme.TextSecondary
import kotlinx.coroutines.launch

/**
 * Dedicated sub-tabs for organizing FreeFlow Settings cleanly.
 */
enum class SettingsSubTab(val title: String, val icon: ImageVector) {
    VOICE_API("API & Voice", Icons.Default.Key),
    KEYBOARD("Keyboard", Icons.Default.Keyboard),
    WISPR_BUBBLE("Wispr Bubble", Icons.Default.Widgets),
    SYSTEM_PERMS("System & Privacy", Icons.Default.Security)
}

/**
 * Fully segmented, 4-tab Settings Studio for FreeFlow.
 * Separates Groq API, Keys Cafe Keyboard Customization, Wispr Flow Floating Bubble, and System Permissions.
 */
@Composable
fun SettingsScreen(
    apiKeyStore: ApiKeyStore,
    preferences: ThemeAndGoodiesPreferences,
    groqClient: GroqWhisperClient,
    onReplayOnboarding: () -> Unit,
    initialSubTab: SettingsSubTab? = null,
    onSubTabChanged: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val settings by preferences.settingsFlow.collectAsState(initial = KeyboardSettings())

    var activeTab by remember { mutableStateOf(initialSubTab ?: SettingsSubTab.VOICE_API) }

    LaunchedEffect(initialSubTab) {
        initialSubTab?.let {
            activeTab = it
            onSubTabChanged?.invoke()
        }
    }

    var apiKeyInput by remember { mutableStateOf("") }
    var isKeyVisible by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var isTestingConnection by remember { mutableStateOf(false) }

    var isAccessibilityEnabled by remember { mutableStateOf(false) }
    var isOverlayEnabled by remember { mutableStateOf(false) }
    var isKeyboardEnabled by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var restoreJsonInput by remember { mutableStateOf("") }

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val micLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
    }

    fun checkAllPermissions() {
        val expectedAccessibility = "${context.packageName}/${FreeFlowAccessibilityService::class.java.canonicalName}"
        val enabledAccessibility = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""
        isAccessibilityEnabled = enabledAccessibility.contains(expectedAccessibility, ignoreCase = true)

        isOverlayEnabled = Settings.canDrawOverlays(context)

        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val enabledImes = imm.enabledInputMethodList.map { it.id }
        isKeyboardEnabled = enabledImes.any { it.contains("com.bheeshamks.freeflow") }

        hasMicPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Auto-refresh permission statuses whenever user returns from Android Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkAllPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        checkAllPermissions()
        val currentKey = apiKeyStore.getApiKey()
        if (!currentKey.isNullOrBlank()) {
            apiKeyInput = currentKey
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // Top Header
        Text(
            text = "Settings",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Horizontal Segmented Sub-Tab Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SettingsSubTab.values().forEach { tab ->
                val isSelected = activeTab == tab
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) PrimaryBlue else Color(0xFF1E1E26))
                        .clickable { activeTab = tab }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.title,
                            tint = if (isSelected) Color.White else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = tab.title,
                            color = if (isSelected) Color.White else TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Content Area for Active Tab
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            when (activeTab) {
                SettingsSubTab.VOICE_API -> {
                    VoiceApiTabContent(
                        apiKeyInput = apiKeyInput,
                        onApiKeyChange = { apiKeyInput = it; testResult = null },
                        isKeyVisible = isKeyVisible,
                        onToggleKeyVisibility = { isKeyVisible = !isKeyVisible },
                        testResult = testResult,
                        isTestingConnection = isTestingConnection,
                        onSaveKey = {
                            apiKeyStore.saveApiKey(apiKeyInput)
                            Toast.makeText(context, "Groq API key saved successfully!", Toast.LENGTH_SHORT).show()
                        },
                        onTestConnection = {
                            if (apiKeyInput.isBlank()) {
                                testResult = "Error: Please enter an API key first"
                                return@VoiceApiTabContent
                            }
                            isTestingConnection = true
                            testResult = null
                            scope.launch {
                                val startTime = System.currentTimeMillis()
                                val result = groqClient.testConnection(apiKeyInput)
                                val latency = System.currentTimeMillis() - startTime
                                isTestingConnection = false
                                result.onSuccess { modelCount ->
                                    testResult = "✓ Connected to Groq LPU ($modelCount models, ${latency}ms)"
                                }.onFailure { err ->
                                    testResult = "Connection failed: ${err.message}"
                                }
                            }
                        },
                        onOpenGroqConsole = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://console.groq.com/keys"))
                            context.startActivity(intent)
                        }
                    )
                }

                SettingsSubTab.KEYBOARD -> {
                    KeyboardStudioTabContent(
                        settings = settings,
                        preferences = preferences,
                        isKeyboardEnabled = isKeyboardEnabled,
                        onEnableKeyboard = {
                            val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                            context.startActivity(intent)
                        },
                        onSelectKeyboard = {
                            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                            imm.showInputMethodPicker()
                        }
                    )
                }

                SettingsSubTab.WISPR_BUBBLE -> {
                    WisprBubbleTabContent(
                        settings = settings,
                        preferences = preferences,
                        isAccessibilityEnabled = isAccessibilityEnabled,
                        isOverlayEnabled = isOverlayEnabled,
                        onEnableAccessibility = {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            context.startActivity(intent)
                        },
                        onEnableOverlay = {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        }
                    )
                }

                SettingsSubTab.SYSTEM_PERMS -> {
                    SystemPermissionsTabContent(
                        hasMicPermission = hasMicPermission,
                        onRequestMic = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                        isAccessibilityEnabled = isAccessibilityEnabled,
                        onEnableAccessibility = {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            context.startActivity(intent)
                        },
                        isOverlayEnabled = isOverlayEnabled,
                        onEnableOverlay = {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        },
                        onBackupSettings = {
                            val jsonStr = exportSettingsToJson(settings, apiKeyStore.getApiKey())
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("FreeFlow Settings Backup", jsonStr)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Settings backup copied to clipboard!", Toast.LENGTH_SHORT).show()
                            try {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "FreeFlow Pocket Settings Backup")
                                    putExtra(Intent.EXTRA_TEXT, jsonStr)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Save or Share Settings Backup"))
                            } catch (e: Exception) {
                                // ignore
                            }
                        },
                        onRestoreSettings = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clipText = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                            restoreJsonInput = if (clipText.contains("keyboardTheme")) clipText else ""
                            showRestoreDialog = true
                        },
                        onReplayOnboarding = onReplayOnboarding
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Restore Settings Backup", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Paste your backup JSON below to restore themes, haptics, bubble positions, and API key:",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = restoreJsonInput,
                        onValueChange = { restoreJsonInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        placeholder = { Text("{\n  \"keyboardTheme\": \"AMOLED_BLACK\",\n  ...\n}", color = TextSecondary.copy(alpha = 0.5f), fontSize = 11.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val success = importSettingsFromJson(restoreJsonInput, preferences, apiKeyStore)
                            if (success) {
                                Toast.makeText(context, "Settings restored successfully!", Toast.LENGTH_SHORT).show()
                                showRestoreDialog = false
                            } else {
                                Toast.makeText(context, "Invalid backup format. Please check JSON.", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Restore", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRestoreDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkBackground,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// =========================================================================
// TAB 1: GROQ WHISPER API CONTENT
// =========================================================================
@Composable
private fun VoiceApiTabContent(
    apiKeyInput: String,
    onApiKeyChange: (String) -> Unit,
    isKeyVisible: Boolean,
    onToggleKeyVisibility: () -> Unit,
    testResult: String?,
    isTestingConnection: Boolean,
    onSaveKey: () -> Unit,
    onTestConnection: () -> Unit,
    onOpenGroqConsole: () -> Unit
) {
    SettingsCard(
        title = "Groq LPU Whisper Cloud",
        icon = Icons.Default.Key
    ) {
        Text(
            text = "FreeFlow transcribes audio at ~300ms using Groq's high-throughput LPU infrastructure and Whisper Large V3. 100% in-memory streaming with zero temporary sound files saved to your device.",
            fontSize = 13.sp,
            color = TextSecondary,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = apiKeyInput,
            onValueChange = onApiKeyChange,
            label = { Text("Groq API Key (gsk_...)") },
            placeholder = { Text("gsk_xxxxxxxxxxxxxxxxxxxx") },
            singleLine = true,
            visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = onToggleKeyVisibility) {
                    Icon(
                        imageVector = if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle visibility",
                        tint = TextSecondary
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryBlue,
                unfocusedBorderColor = DarkCardBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onSaveKey,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                modifier = Modifier.weight(1f)
            ) {
                Text("Save Key", color = Color.White, fontWeight = FontWeight.SemiBold)
            }

            OutlinedButton(
                onClick = onTestConnection,
                enabled = !isTestingConnection,
                modifier = Modifier.weight(1f)
            ) {
                if (isTestingConnection) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = PrimaryBlue, strokeWidth = 2.dp)
                } else {
                    Text("Test Connection", color = TextPrimary)
                }
            }
        }

        testResult?.let { result ->
            Spacer(modifier = Modifier.height(10.dp))
            val isSuccess = result.startsWith("✓")
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSuccess) Color(0x2200E676) else Color(0x22FF5252))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null,
                    tint = if (isSuccess) AccentGreen else AccentRed,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = result,
                    fontSize = 12.sp,
                    color = if (isSuccess) AccentGreen else AccentRed,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .clickable { onOpenGroqConsole() }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Get a free API key at console.groq.com",
                color = PrimaryBlue,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.OpenInNew,
                contentDescription = "Open Groq Console",
                tint = PrimaryBlue,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

// =========================================================================
// TAB 2: KEYBOARD STUDIO CONTENT
// =========================================================================
@Composable
private fun KeyboardStudioTabContent(
    settings: KeyboardSettings,
    preferences: ThemeAndGoodiesPreferences,
    isKeyboardEnabled: Boolean,
    onEnableKeyboard: () -> Unit,
    onSelectKeyboard: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val soundPlayer = remember { KeyboardSoundPlayer(context) }
    DisposableEffect(Unit) {
        onDispose { soundPlayer.release() }
    }

    // 1. Live Interactive Key Preview
    SettingsCard(
        title = "Live Keycap Preview",
        icon = Icons.Default.Keyboard
    ) {
        Text(
            text = "Real-time preview of your selected theme, border style, and bloom glow.",
            fontSize = 12.sp,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(settings.keyboardTheme.bgHex))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            val borderModifier = when (settings.borderStyle) {
                KeyBorderStyle.NO_BORDER -> Modifier
                KeyBorderStyle.SUBTLE -> Modifier.border(0.8.dp, Color(0x33FFFFFF), RoundedCornerShape(settings.cornerRadiusDp.dp))
                KeyBorderStyle.GLOW_ACCENT -> Modifier.border(1.2.dp, Color(settings.colorPreset.primaryColor), RoundedCornerShape(settings.cornerRadiusDp.dp))
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (settings.showKeyPreviewPopup) {
                    Box(
                        modifier = Modifier
                            .size(46.dp, 50.dp)
                            .shadow(6.dp, RoundedCornerShape(12.dp), spotColor = Color(settings.colorPreset.outerGlowColor))
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(settings.keyboardTheme.keyHex))
                            .border(1.2.dp, Color(settings.colorPreset.primaryColor), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("A", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                Box(
                    modifier = Modifier
                        .width(64.dp)
                        .height(settings.keyHeightDp.dp)
                        .clip(RoundedCornerShape(settings.cornerRadiusDp.dp))
                        .background(Color(settings.keyboardTheme.keyHex))
                        .then(borderModifier),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "A",
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    // 2. Keyboard Themes
    SettingsCard(
        title = "Keyboard Theme (Background)",
        icon = Icons.Default.Layers
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            KeyboardTheme.values().forEach { theme ->
                val isSelected = settings.keyboardTheme == theme
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) Color(0x335AB4FF) else Color(0xFF1B1B22))
                        .border(
                            width = if (isSelected) 1.dp else 0.5.dp,
                            color = if (isSelected) PrimaryBlue else Color(0x22FFFFFF),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { scope.launch { preferences.updateKeyboardTheme(theme) } }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color(theme.bgHex))
                                .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                        )
                        Text(
                            text = theme.displayName,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }

    // 3. Key Border Style
    SettingsCard(
        title = "Letter Keycap Border Style",
        icon = Icons.Default.Widgets
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KeyBorderStyle.values().forEach { borderStyle ->
                val isSelected = settings.borderStyle == borderStyle
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) PrimaryBlue else Color(0xFF1E1E26))
                        .clickable { scope.launch { preferences.updateBorderStyle(borderStyle) } }
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = borderStyle.displayName,
                        fontSize = 11.sp,
                        color = if (isSelected) Color.White else TextSecondary,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }

    // 4. Keys Cafe Bloom Animation
    SettingsCard(
        title = "Keys Cafe Radial Bloom",
        icon = Icons.Default.GraphicEq
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Animated Keystroke Bloom", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text("Radial neon ripples on keypress", color = TextSecondary, fontSize = 12.sp)
            }
            Switch(
                checked = settings.isKeysCafeEnabled,
                onCheckedChange = { scope.launch { preferences.setKeysCafeEnabled(it) } },
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PrimaryBlue)
            )
        }

        if (settings.isKeysCafeEnabled) {
            Spacer(modifier = Modifier.height(14.dp))
            Text("Bloom Color Preset", fontSize = 12.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BloomColorPreset.values().forEach { preset ->
                    val isSelected = settings.colorPreset == preset
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) Color(preset.primaryColor).copy(alpha = 0.25f) else Color(0xFF1E1E26))
                            .border(
                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                color = if (isSelected) Color(preset.primaryColor) else Color.Transparent,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { scope.launch { preferences.updateColorPreset(preset) } }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color(preset.primaryColor)))
                            Text(preset.displayName, fontSize = 12.sp, color = if (isSelected) Color.White else TextSecondary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text("Animation Style", fontSize = 12.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                BloomAnimationStyle.values().forEach { style ->
                    val isSelected = settings.animationStyle == style
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color(0x335AB4FF) else Color(0xFF1B1B22))
                            .clickable { scope.launch { preferences.updateAnimationStyle(style) } }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(style.displayName, fontSize = 13.sp, color = TextPrimary)
                        if (isSelected) Icon(Icons.Default.Check, null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Bloom Radius", fontSize = 12.sp, color = TextSecondary)
                Text("${settings.bloomRadiusDp.toInt()}dp", fontSize = 12.sp, color = PrimaryBlue, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = settings.bloomRadiusDp,
                onValueChange = { scope.launch { preferences.updateBloomRadius(it) } },
                valueRange = 60f..180f,
                colors = SliderDefaults.colors(thumbColor = PrimaryBlue, activeTrackColor = PrimaryBlue)
            )
        }
    }

    // 5. Layout & Sizing
    SettingsCard(
        title = "Keycap Sizing & Ergonomics",
        icon = Icons.Default.Settings
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text("Show Key Preview Above Finger", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text("Enlarged floating letter bubble while typing (iOS / Samsung)", color = TextSecondary, fontSize = 12.sp)
            }
            Switch(
                checked = settings.showKeyPreviewPopup,
                onCheckedChange = { scope.launch { preferences.updateShowKeyPreview(it) } },
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PrimaryBlue)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text("Dedicated Number Row", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text("Row of numbers (1-0) above QWERTY", color = TextSecondary, fontSize = 12.sp)
            }
            Switch(
                checked = settings.showNumberRow,
                onCheckedChange = { scope.launch { preferences.updateShowNumberRow(it) } },
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PrimaryBlue)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Key Height", fontSize = 12.sp, color = TextSecondary)
            Text("${settings.keyHeightDp.toInt()}dp", fontSize = 12.sp, color = PrimaryBlue, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = settings.keyHeightDp,
            onValueChange = { scope.launch { preferences.updateKeyHeight(it) } },
            valueRange = 42f..56f,
            colors = SliderDefaults.colors(thumbColor = PrimaryBlue, activeTrackColor = PrimaryBlue)
        )

        Spacer(modifier = Modifier.height(14.dp))
        Text("Haptic Typing Feedback", fontSize = 12.sp, color = TextSecondary)
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            HapticStrength.values().forEach { strength ->
                val isSelected = settings.hapticStrength == strength
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) PrimaryBlue else Color(0xFF1E1E26))
                        .clickable { scope.launch { preferences.updateHapticStrength(strength) } }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = strength.displayName.split(" ").first(),
                        fontSize = 11.sp,
                        color = if (isSelected) Color.White else TextSecondary,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }

    // 6. Keypress Audio & Sound Themes
    SettingsCard(
        title = "Keypress Audio & Sound Themes",
        icon = Icons.Default.VolumeUp
    ) {
        Text(
            text = "Crisp acoustic feedback tailored for hyper-fast typing. Tap any theme to hear an immediate live preview.",
            fontSize = 12.sp,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            KeySoundTheme.values().forEach { soundTheme ->
                val isSelected = settings.soundTheme == soundTheme
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) Color(0x335AB4FF) else Color(0xFF1B1B22))
                        .border(
                            width = if (isSelected) 1.dp else 0.5.dp,
                            color = if (isSelected) PrimaryBlue else Color(0x22FFFFFF),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            soundPlayer.playKeySound(soundTheme, settings.soundVolume)
                            scope.launch { preferences.updateSoundTheme(soundTheme) }
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = soundTheme.displayName,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                        Text(
                            text = soundTheme.description,
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        if (settings.soundTheme != KeySoundTheme.OFF) {
            Spacer(modifier = Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Sound Volume", fontSize = 12.sp, color = TextSecondary)
                Text("${(settings.soundVolume * 100).toInt()}%", fontSize = 12.sp, color = PrimaryBlue, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = settings.soundVolume,
                onValueChange = {
                    scope.launch { preferences.updateSoundVolume(it) }
                },
                valueRange = 0.05f..1.0f,
                colors = SliderDefaults.colors(thumbColor = PrimaryBlue, activeTrackColor = PrimaryBlue)
            )
        }
    }

    // 7. System Keyboard IME Activation
    SettingsCard(
        title = "System Keyboard Activation",
        icon = Icons.Default.Keyboard
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("FreeFlow IME Status", color = TextPrimary, fontSize = 13.sp)
            Text(
                text = if (isKeyboardEnabled) "Enabled ✓" else "Not Enabled",
                color = if (isKeyboardEnabled) AccentGreen else AccentRed,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onEnableKeyboard,
                colors = ButtonDefaults.buttonColors(containerColor = if (isKeyboardEnabled) Color(0xFF282832) else PrimaryBlue),
                modifier = Modifier.weight(1f)
            ) {
                Text(if (isKeyboardEnabled) "Manage IMEs" else "Enable Keyboard", fontSize = 12.sp, color = if (isKeyboardEnabled) TextPrimary else Color.White)
            }

            Button(
                onClick = onSelectKeyboard,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                modifier = Modifier.weight(1f)
            ) {
                Text("Select Keyboard", fontSize = 12.sp, color = Color.White)
            }
        }
    }
}

// =========================================================================
// TAB 3: WISPR BUBBLE CONTENT
// =========================================================================
@Composable
private fun WisprBubbleTabContent(
    settings: KeyboardSettings,
    preferences: ThemeAndGoodiesPreferences,
    isAccessibilityEnabled: Boolean,
    isOverlayEnabled: Boolean,
    onEnableAccessibility: () -> Unit,
    onEnableOverlay: () -> Unit
) {
    val scope = rememberCoroutineScope()

    SettingsCard(
        title = "Wispr Flow Floating Bubble",
        icon = Icons.Default.Widgets
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Floating Capsule Overlay", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text("Discreet 52dp mic bubble docked to the screen edge", color = TextSecondary, fontSize = 12.sp)
            }
            Switch(
                checked = settings.isFloatingBubbleEnabled,
                onCheckedChange = { scope.launch { preferences.setFloatingBubbleEnabled(it) } },
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PrimaryBlue)
            )
        }

        if (settings.isFloatingBubbleEnabled) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Bubble Opacity: ${(settings.floatingBubbleOpacity * 100).toInt()}%",
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Slider(
                value = settings.floatingBubbleOpacity,
                onValueChange = { scope.launch { preferences.updateFloatingBubbleOpacity(it) } },
                valueRange = 0.2f..1.0f,
                colors = SliderDefaults.colors(thumbColor = PrimaryBlue, activeTrackColor = PrimaryBlue)
            )

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Hide When Text Box Selected", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text("Auto-hide floating bubble when typing so it never covers your keyboard or text", color = TextSecondary, fontSize = 12.sp)
                }
                Switch(
                    checked = settings.hideBubbleOnKeyboard,
                    onCheckedChange = { scope.launch { preferences.setHideBubbleOnKeyboard(it) } },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PrimaryBlue)
                )
            }
        }
    }

    SettingsCard(
        title = "Magnetic Edge-Snapping Behavior",
        icon = Icons.Default.Info
    ) {
        Text(
            text = "• Middle-Right Default: Placed comfortably within the natural thumb zone.\n• Magnetic Snapping: Dragging the bubble to the center or sides automatically snaps it smoothly to the closest screen border (left or right side only).\n• Tap-to-Toggle Only: Tap once to start dictating, tap the green tick to inject transcribed text into the active app, or tap the red cross to discard.",
            fontSize = 13.sp,
            color = TextSecondary,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(14.dp))
        Text("3-Bubble Dynamic Expansion Architecture:", fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(8.dp))

        // Visual diagram of 3-bubble expansion
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF16161E))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("When docked on Right Edge:", fontSize = 11.sp, color = PrimaryBlue, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BubbleMock(text = "✖ Cancel", color = Color(0xFFFF5252))
                Spacer(modifier = Modifier.width(6.dp))
                BubbleMock(text = "〰 Waveform", color = PrimaryBlue)
                Spacer(modifier = Modifier.width(6.dp))
                BubbleMock(text = "✔ Done", color = AccentGreen)
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text("When docked on Left Edge:", fontSize = 11.sp, color = PrimaryBlue, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BubbleMock(text = "✔ Done", color = AccentGreen)
                Spacer(modifier = Modifier.width(6.dp))
                BubbleMock(text = "〰 Waveform", color = PrimaryBlue)
                Spacer(modifier = Modifier.width(6.dp))
                BubbleMock(text = "✖ Cancel", color = Color(0xFFFF5252))
            }
        }
    }

    SettingsCard(
        title = "Required System Permissions",
        icon = Icons.Default.Security
    ) {
        // Accessibility Permission
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Accessibility Service", fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                Text(
                    text = if (isAccessibilityEnabled) "Service Enabled ✓" else "Service Disabled (Required)",
                    fontSize = 11.sp,
                    color = if (isAccessibilityEnabled) AccentGreen else AccentRed
                )
            }
            Button(
                onClick = onEnableAccessibility,
                colors = ButtonDefaults.buttonColors(containerColor = if (isAccessibilityEnabled) Color(0xFF282832) else PrimaryBlue),
                modifier = Modifier.height(34.dp)
            ) {
                Text(if (isAccessibilityEnabled) "Settings" else "Enable", fontSize = 12.sp, color = if (isAccessibilityEnabled) TextPrimary else Color.White)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Display Over Other Apps Permission
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Display Over Other Apps", fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                Text(
                    text = if (isOverlayEnabled) "Permission Granted ✓" else "Permission Required",
                    fontSize = 11.sp,
                    color = if (isOverlayEnabled) AccentGreen else AccentRed
                )
            }
            Button(
                onClick = onEnableOverlay,
                colors = ButtonDefaults.buttonColors(containerColor = if (isOverlayEnabled) Color(0xFF282832) else PrimaryBlue),
                modifier = Modifier.height(34.dp)
            ) {
                Text(if (isOverlayEnabled) "Settings" else "Grant", fontSize = 12.sp, color = if (isOverlayEnabled) TextPrimary else Color.White)
            }
        }
    }
}

// =========================================================================
// TAB 4: SYSTEM & PRIVACY CONTENT
// =========================================================================
@Composable
private fun SystemPermissionsTabContent(
    hasMicPermission: Boolean,
    onRequestMic: () -> Unit,
    isAccessibilityEnabled: Boolean,
    onEnableAccessibility: () -> Unit,
    isOverlayEnabled: Boolean,
    onEnableOverlay: () -> Unit,
    onBackupSettings: () -> Unit,
    onRestoreSettings: () -> Unit,
    onReplayOnboarding: () -> Unit
) {
    val context = LocalContext.current

    SettingsCard(
        title = "Permissions Hub",
        icon = Icons.Default.Security
    ) {
        // Mic
        PermissionStatusRow(
            title = "Microphone Access",
            subtitle = "Captures voice dictation audio",
            isGranted = hasMicPermission,
            actionLabel = "Grant",
            onAction = onRequestMic
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Accessibility
        PermissionStatusRow(
            title = "Accessibility Service",
            subtitle = "Injects text into focused apps",
            isGranted = isAccessibilityEnabled,
            actionLabel = "Enable",
            onAction = onEnableAccessibility
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Overlay
        PermissionStatusRow(
            title = "Display Over Other Apps",
            subtitle = "Renders floating Wispr capsule",
            isGranted = isOverlayEnabled,
            actionLabel = "Grant",
            onAction = onEnableOverlay
        )
    }

    SettingsCard(
        title = "Updates & Data Preservation",
        icon = Icons.Default.Info
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("FreeFlow Pocket", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("Version 1.0.2 (Build 3)", color = TextSecondary, fontSize = 12.sp)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF14241B))
                    .border(1.dp, Color(0xFF00E676).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("Updatable", color = Color(0xFF00E676), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "• In-Place Updates: Seamlessly update without losing any settings by installing new release APKs over existing installs.\n• Keep Data on Uninstall: When uninstalling FreeFlow, Android OS displays a 'Keep app data' checkbox. Leaving it checked preserves your custom themes, API keys, bubble coordinates, and dictation history upon reinstall.\n• Consistent Signing: Built with unified release keys across all versions.",
            fontSize = 12.sp,
            color = TextSecondary,
            lineHeight = 17.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onBackupSettings,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3A3A48))
            ) {
                Text("Backup Settings", fontSize = 12.sp)
            }
            OutlinedButton(
                onClick = onRestoreSettings,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3A3A48))
            ) {
                Text("Restore Settings", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/bheeshamks/FreeFlow-Pocket/releases")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            },
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Check for Updates / Releases", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }

    SettingsCard(
        title = "Quick Settings Notification Tile",
        icon = Icons.Default.Widgets
    ) {
        Text(
            text = "FreeFlow includes a Quick Settings Tile for 1-tap voice dictation directly from your notification shade.\n\nTo add the tile:\n1. Swipe down twice from the top of your phone.\n2. Tap the pencil (Edit) icon in Quick Settings.\n3. Drag 'FreeFlow Dictate' up into your active tiles.",
            fontSize = 13.sp,
            color = TextSecondary,
            lineHeight = 18.sp
        )
    }

    SettingsCard(
        title = "Privacy & Local SQLite Archive",
        icon = Icons.Default.Info
    ) {
        Text(
            text = "• 100% Local Archive: All transcribed notes are stored strictly in local on-device SQLite database.\n• Zero Disk Footprint: Audio is streamed in-memory directly to Groq Whisper LPU; zero temporary audio files are stored.\n• 0% Idle Battery Drain: Dictation engines sleep completely when idle.",
            fontSize = 13.sp,
            color = TextSecondary,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = onReplayOnboarding,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF282832)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextPrimary)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Replay First-Run Onboarding Flow", color = TextPrimary, fontSize = 13.sp)
        }
    }
}

// =========================================================================
// HELPER COMPONENTS
// =========================================================================
@Composable
private fun SettingsCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkCard)
            .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp))
            .padding(18.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(14.dp))
        content()
    }
}

@Composable
private fun PermissionStatusRow(
    title: String,
    subtitle: String,
    isGranted: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(
                text = if (isGranted) "Granted ✓" else subtitle,
                color = if (isGranted) AccentGreen else TextSecondary,
                fontSize = 11.sp
            )
        }
        if (!isGranted) {
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                modifier = Modifier.height(32.dp)
            ) {
                Text(actionLabel, fontSize = 11.sp, color = Color.White)
            }
        } else {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun BubbleMock(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.2f))
            .border(1.dp, color, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 10.sp, color = color, fontWeight = FontWeight.Bold)
    }
}

private fun exportSettingsToJson(settings: KeyboardSettings, apiKey: String?): String {
    val json = JSONObject()
    json.put("keyboardTheme", settings.keyboardTheme.name)
    json.put("animationStyle", settings.animationStyle.name)
    json.put("colorPreset", settings.colorPreset.name)
    json.put("borderStyle", settings.borderStyle.name)
    json.put("showNumberRow", settings.showNumberRow)
    json.put("showKeyPreviewPopup", settings.showKeyPreviewPopup)
    json.put("soundTheme", settings.soundTheme.name)
    json.put("soundVolume", settings.soundVolume.toDouble())
    json.put("bloomRadiusDp", settings.bloomRadiusDp.toDouble())
    json.put("animationSpeedMs", settings.animationSpeedMs)
    json.put("hapticStrength", settings.hapticStrength.name)
    json.put("keyHeightDp", settings.keyHeightDp.toDouble())
    json.put("cornerRadiusDp", settings.cornerRadiusDp.toDouble())
    json.put("dictationMode", settings.dictationMode.name)
    json.put("isKeysCafeEnabled", settings.isKeysCafeEnabled)
    json.put("floatingBubbleOpacity", settings.floatingBubbleOpacity.toDouble())
    json.put("floatingBubbleDockedOnRight", settings.floatingBubbleDockedOnRight)
    json.put("floatingBubbleYRatio", settings.floatingBubbleYRatio.toDouble())
    json.put("groqModel", settings.groqModel)
    if (!apiKey.isNullOrBlank()) {
        json.put("groqApiKey", apiKey)
    }
    return json.toString(2)
}

private suspend fun importSettingsFromJson(
    jsonStr: String,
    preferences: ThemeAndGoodiesPreferences,
    apiKeyStore: ApiKeyStore
): Boolean {
    return try {
        val json = JSONObject(jsonStr.trim())
        if (json.has("keyboardTheme")) {
            preferences.updateKeyboardTheme(KeyboardTheme.valueOf(json.getString("keyboardTheme")))
        }
        if (json.has("animationStyle")) {
            preferences.updateAnimationStyle(BloomAnimationStyle.valueOf(json.getString("animationStyle")))
        }
        if (json.has("colorPreset")) {
            preferences.updateColorPreset(BloomColorPreset.valueOf(json.getString("colorPreset")))
        }
        if (json.has("borderStyle")) {
            preferences.updateBorderStyle(KeyBorderStyle.valueOf(json.getString("borderStyle")))
        }
        if (json.has("showNumberRow")) {
            preferences.updateShowNumberRow(json.getBoolean("showNumberRow"))
        }
        if (json.has("showKeyPreviewPopup")) {
            preferences.updateShowKeyPreview(json.getBoolean("showKeyPreviewPopup"))
        }
        if (json.has("soundTheme")) {
            preferences.updateSoundTheme(KeySoundTheme.valueOf(json.getString("soundTheme")))
        }
        if (json.has("soundVolume")) {
            preferences.updateSoundVolume(json.getDouble("soundVolume").toFloat())
        }
        if (json.has("bloomRadiusDp")) {
            preferences.updateBloomRadius(json.getDouble("bloomRadiusDp").toFloat())
        }
        if (json.has("animationSpeedMs")) {
            preferences.updateAnimationSpeed(json.getInt("animationSpeedMs"))
        }
        if (json.has("hapticStrength")) {
            preferences.updateHapticStrength(HapticStrength.valueOf(json.getString("hapticStrength")))
        }
        if (json.has("keyHeightDp")) {
            preferences.updateKeyHeight(json.getDouble("keyHeightDp").toFloat())
        }
        if (json.has("cornerRadiusDp")) {
            preferences.updateCornerRadius(json.getDouble("cornerRadiusDp").toFloat())
        }
        if (json.has("dictationMode")) {
            preferences.updateDictationMode(DictationMode.valueOf(json.getString("dictationMode")))
        }
        if (json.has("isKeysCafeEnabled")) {
            preferences.setKeysCafeEnabled(json.getBoolean("isKeysCafeEnabled"))
        }
        if (json.has("floatingBubbleOpacity")) {
            preferences.updateFloatingBubbleOpacity(json.getDouble("floatingBubbleOpacity").toFloat())
        }
        if (json.has("floatingBubbleDockedOnRight") && json.has("floatingBubbleYRatio")) {
            preferences.setFloatingBubblePosition(
                json.getBoolean("floatingBubbleDockedOnRight"),
                json.getDouble("floatingBubbleYRatio").toFloat()
            )
        }
        if (json.has("groqModel")) {
            preferences.updateGroqModel(json.getString("groqModel"))
        }
        if (json.has("groqApiKey")) {
            apiKeyStore.saveApiKey(json.getString("groqApiKey"))
        }
        true
    } catch (e: Exception) {
        false
    }
}
