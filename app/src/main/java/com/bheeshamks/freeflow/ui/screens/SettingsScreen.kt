package com.bheeshamks.freeflow.ui.screens

import android.Manifest
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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Error
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
import androidx.compose.material.icons.filled.Widgets
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.bheeshamks.freeflow.core.data.model.BloomAnimationStyle
import com.bheeshamks.freeflow.core.data.model.BloomColorPreset
import com.bheeshamks.freeflow.core.data.model.HapticStrength
import com.bheeshamks.freeflow.core.data.model.KeyboardSettings
import com.bheeshamks.freeflow.core.data.prefs.ThemeAndGoodiesPreferences
import com.bheeshamks.freeflow.core.network.GroqWhisperClient
import com.bheeshamks.freeflow.core.security.ApiKeyStore
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
 * Unified Settings Screen:
 * 1. Groq API configuration & connection test
 * 2. Dictation Modes (Wispr Flow Floating Window, Quick Settings Tile, Keys Cafe Keyboard)
 * 3. Permissions Hub
 * 4. Onboarding Replay & Privacy Disclosures
 */
@Composable
fun SettingsScreen(
    apiKeyStore: ApiKeyStore,
    preferences: ThemeAndGoodiesPreferences,
    groqClient: GroqWhisperClient,
    onReplayOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by preferences.settingsFlow.collectAsState(initial = KeyboardSettings())

    var apiKeyInput by remember { mutableStateOf("") }
    var isKeyVisible by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var isTestingConnection by remember { mutableStateOf(false) }

    var isAccessibilityEnabled by remember { mutableStateOf(false) }

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

    fun checkAccessibility() {
        val expectedServiceName = "${context.packageName}/${FreeFlowAccessibilityService::class.java.canonicalName}"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""
        isAccessibilityEnabled = enabledServices.contains(expectedServiceName, ignoreCase = true)
    }

    LaunchedEffect(Unit) {
        checkAccessibility()
        val currentKey = apiKeyStore.getApiKey()
        if (!currentKey.isNullOrBlank()) {
            apiKeyInput = currentKey
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Title
        Text(
            text = "Settings",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        // ==========================================
        // SECTION 1: GROQ LPU API CONFIGURATION
        // ==========================================
        SettingsSectionCard(
            title = "Groq LPU Whisper API",
            icon = Icons.Default.Key
        ) {
            Text(
                text = "FreeFlow uses Groq's high-throughput LPU infrastructure to achieve ~300ms Whisper transcription.",
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = apiKeyInput,
                onValueChange = {
                    apiKeyInput = it.trim()
                    testResult = null
                },
                label = { Text("Groq API Key (gsk_...)") },
                placeholder = { Text("gsk_xxxxxxxxxxxxxxxxxxxx") },
                singleLine = true,
                visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
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

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Save Button
                Button(
                    onClick = {
                        if (apiKeyInput.isNotBlank()) {
                            apiKeyStore.saveApiKey(apiKeyInput)
                            Toast.makeText(context, "API Key saved securely ✓", Toast.LENGTH_SHORT).show()
                        } else {
                            apiKeyStore.clearApiKey()
                            Toast.makeText(context, "API Key cleared", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Save Key", color = Color.Black, fontWeight = FontWeight.SemiBold)
                }

                // Test Button
                OutlinedButton(
                    onClick = {
                        isTestingConnection = true
                        testResult = null
                        if (apiKeyInput.isNotBlank()) {
                            apiKeyStore.saveApiKey(apiKeyInput)
                        }
                        scope.launch {
                            val result = groqClient.testConnection()
                            isTestingConnection = false
                            testResult = result.fold(
                                onSuccess = { "Connected ✓ ($it models available)" },
                                onFailure = { it.message ?: "Connection failed" }
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = apiKeyInput.isNotBlank() && !isTestingConnection
                ) {
                    if (isTestingConnection) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = PrimaryBlue, strokeWidth = 2.dp)
                    } else {
                        Text("Test Connection", color = PrimaryBlue)
                    }
                }
            }

            testResult?.let { res ->
                Spacer(modifier = Modifier.height(8.dp))
                val isSuccess = res.contains("Connected")
                Text(
                    text = res,
                    color = if (isSuccess) AccentGreen else AccentRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Get Key link
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://console.groq.com/keys"))
                        context.startActivity(browserIntent)
                    }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Get a free API key at console.groq.com", color = PrimaryBlue, fontSize = 12.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(imageVector = Icons.Default.OpenInNew, contentDescription = "Open", tint = PrimaryBlue, modifier = Modifier.size(14.dp))
            }
        }

        // ==========================================
        // SECTION 2: DICTATION MODES
        // ==========================================
        SettingsSectionCard(
            title = "Dictation Modes",
            icon = Icons.Default.Layers
        ) {
            // Mode A: Floating Bubble (Wispr Flow)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Floating Wispr Bubble", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(text = "Draggable edge-snapping mic overlay", color = TextSecondary, fontSize = 12.sp)
                }
                Switch(
                    checked = settings.isFloatingBubbleEnabled,
                    onCheckedChange = { enabled ->
                        scope.launch { preferences.setFloatingBubbleEnabled(enabled) }
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = PrimaryBlue)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Accessibility Status
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1C1C22))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Accessibility Permission", fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                    Text(
                        text = if (isAccessibilityEnabled) "Service Enabled ✓" else "Service Disabled (Required)",
                        fontSize = 11.sp,
                        color = if (isAccessibilityEnabled) AccentGreen else AccentRed
                    )
                }
                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isAccessibilityEnabled) Color(0xFF282832) else PrimaryBlue),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(text = if (isAccessibilityEnabled) "Settings" else "Enable", fontSize = 12.sp, color = if (isAccessibilityEnabled) TextPrimary else Color.Black)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mode B: Quick Settings Tile
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Quick Settings Tile", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(text = "One-tap dictation in notification shade", color = TextSecondary, fontSize = 12.sp)
                }
                Text(text = "Ready ✓", color = AccentGreen, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }

            Text(
                text = "To add: Swipe down notification shade twice -> tap edit (pencil icon) -> drag 'FreeFlow Dictate' to your tiles.",
                fontSize = 11.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Mode C: Keys Cafe Custom Keyboard
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Keys Cafe Keyboard", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(text = "Samsung-style keyboard with radial glow", color = TextSecondary, fontSize = 12.sp)
                }
                Switch(
                    checked = settings.isKeysCafeEnabled,
                    onCheckedChange = { enabled ->
                        scope.launch { preferences.setKeysCafeEnabled(enabled) }
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = PrimaryBlue)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                        context.startActivity(intent)
                    },
                    modifier = Modifier.weight(1f).height(38.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF24242C))
                ) {
                    Text("Enable IME", color = Color.White, fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                        imm?.showInputMethodPicker()
                    },
                    modifier = Modifier.weight(1f).height(38.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF24242C))
                ) {
                    Text("Select IME", color = Color.White, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Keyboard Color Presets
            Text(text = "Keyboard Bloom Color Theme", fontSize = 12.sp, color = TextSecondary)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BloomColorPreset.values().forEach { preset ->
                    val selected = settings.colorPreset == preset
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(preset.primaryColor))
                            .border(if (selected) 2.dp else 0.dp, Color.White, RoundedCornerShape(8.dp))
                            .clickable {
                                scope.launch { preferences.updateColorPreset(preset) }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = "Selected", tint = Color.Black, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Haptic Feedback
            Text(text = "Haptic Feedback Intensity", fontSize = 12.sp, color = TextSecondary)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                HapticStrength.values().forEach { strength ->
                    FilterChip(
                        selected = settings.hapticStrength == strength,
                        onClick = {
                            scope.launch { preferences.updateHapticStrength(strength) }
                        },
                        label = { Text(strength.displayName, fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryBlue,
                            selectedLabelColor = Color.Black
                        )
                    )
                }
            }
        }

        // ==========================================
        // SECTION 3: PERMISSIONS & SYSTEM STATUS
        // ==========================================
        SettingsSectionCard(
            title = "Permissions Hub",
            icon = Icons.Default.Security
        ) {
            // Microphone
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.Mic, contentDescription = "Mic", tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                    Text(text = "Microphone Permission", color = TextPrimary, fontSize = 13.sp)
                }
                if (hasMicPermission) {
                    Text(text = "Granted ✓", color = AccentGreen, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                } else {
                    Button(
                        onClick = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Grant", fontSize = 11.sp, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Accessibility
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.Widgets, contentDescription = "Accessibility", tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                    Text(text = "Accessibility Service", color = TextPrimary, fontSize = 13.sp)
                }
                Text(
                    text = if (isAccessibilityEnabled) "Active ✓" else "Not Active",
                    color = if (isAccessibilityEnabled) AccentGreen else AccentRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // ==========================================
        // SECTION 4: ONBOARDING & WALKTHROUGH
        // ==========================================
        SettingsSectionCard(
            title = "Walkthrough & Guide",
            icon = Icons.Default.Refresh
        ) {
            Text(
                text = "Need to review how FreeFlow works or re-configure permissions?",
                fontSize = 13.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = {
                    onReplayOnboarding()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryBlue)
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Replay", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Replay Onboarding Guide")
            }
        }

        // ==========================================
        // SECTION 5: PRIVACY & ABOUT
        // ==========================================
        SettingsSectionCard(
            title = "Privacy & Local Storage",
            icon = Icons.Default.Info
        ) {
            Text(
                text = "• 100% In-Memory Audio: Recorded audio is streamed directly to Groq Whisper and never stored on disk.\n" +
                        "• Local SQLite History: Dictation history is stored entirely on your device in a private database.\n" +
                        "• Zero Background Telemetry: No background tracking or telemetry.\n" +
                        "• FreeFlow Version: 1.0.0",
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 18.sp
            )
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(DarkCard)
            .border(1.dp, DarkCardBorder, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }

            Spacer(modifier = Modifier.height(14.dp))

            content()
        }
    }
}
