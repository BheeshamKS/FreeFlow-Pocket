package com.bheeshamks.freeflow.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.bheeshamks.freeflow.core.data.model.DictationMode
import com.bheeshamks.freeflow.core.data.prefs.ThemeAndGoodiesPreferences
import com.bheeshamks.freeflow.core.network.GroqWhisperClient
import com.bheeshamks.freeflow.core.security.ApiKeyStore
import com.bheeshamks.freeflow.service.FreeFlowAccessibilityService
import com.bheeshamks.freeflow.ui.theme.AccentGreen
import com.bheeshamks.freeflow.ui.theme.AccentRed
import com.bheeshamks.freeflow.ui.theme.DarkCard
import com.bheeshamks.freeflow.ui.theme.DarkCardBorder
import com.bheeshamks.freeflow.ui.theme.PrimaryBlue
import com.bheeshamks.freeflow.ui.theme.TextPrimary
import com.bheeshamks.freeflow.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun DictationHubScreen(
    apiKeyStore: ApiKeyStore,
    preferences: ThemeAndGoodiesPreferences,
    groqClient: GroqWhisperClient,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by preferences.settingsFlow.collectAsState(initial = com.bheeshamks.freeflow.core.data.model.KeyboardSettings())

    var apiKeyInput by remember { mutableStateOf("") }
    var isKeyVisible by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var isTestingConnection by remember { mutableStateOf(false) }

    var showDisclosureDialog by remember { mutableStateOf(false) }
    var isAccessibilityEnabled by remember { mutableStateOf(false) }

    fun checkAccessibility() {
        val expectedServiceName = "${context.packageName}/${FreeFlowAccessibilityService::class.java.canonicalName}"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""
        isAccessibilityEnabled = enabledServices.contains(expectedServiceName)
    }

    LaunchedEffect(Unit) {
        checkAccessibility()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Text(
            text = "FreeFlow",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "Wispr Flow voice dictation powered by Groq LPU",
            fontSize = 14.sp,
            color = TextSecondary,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )

        // Card 1: Groq API Key Setup
        SetupCard(
            stepNumber = "1",
            title = "Groq LPU API Key",
            subtitle = "Required for ultra-fast (~300ms) Whisper speech transcription",
            isComplete = apiKeyStore.hasApiKey()
        ) {
            Text(
                text = "Current Key: ${apiKeyStore.getMaskedApiKey()}",
                fontSize = 13.sp,
                color = if (apiKeyStore.hasApiKey()) AccentGreen else Color(0xFFFFB300),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            OutlinedTextField(
                value = apiKeyInput,
                onValueChange = { apiKeyInput = it },
                label = { Text("Enter gsk_... key") },
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
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = DarkCardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (apiKeyInput.isNotBlank()) {
                            apiKeyStore.saveApiKey(apiKeyInput)
                            apiKeyInput = ""
                            testResult = "Key saved securely in Android Keystore."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save Key", color = Color.Black, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = {
                        isTestingConnection = true
                        testResult = null
                        scope.launch {
                            val res = groqClient.testConnection()
                            isTestingConnection = false
                            testResult = res.fold(
                                onSuccess = { "✓ Connected! ($it models available)" },
                                onFailure = { "✕ ${it.message ?: "Connection failed"}" }
                            )
                        }
                    },
                    enabled = apiKeyStore.hasApiKey() && !isTestingConnection,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isTestingConnection) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Test API", color = PrimaryBlue)
                    }
                }
            }

            testResult?.let {
                Text(
                    text = it,
                    fontSize = 12.sp,
                    color = if (it.startsWith("✓")) AccentGreen else AccentRed,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Card 2: Accessibility Service Configuration
        SetupCard(
            stepNumber = "2",
            title = "Floating Voice Bubble",
            subtitle = "Mounts an interactive voice HUD over any focused text field",
            isComplete = isAccessibilityEnabled
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isAccessibilityEnabled) "Service is ACTIVE" else "Service is DISABLED",
                    fontSize = 13.sp,
                    color = if (isAccessibilityEnabled) AccentGreen else Color(0xFFFF5252),
                    fontWeight = FontWeight.SemiBold
                )

                Button(
                    onClick = {
                        if (!isAccessibilityEnabled) {
                            showDisclosureDialog = true
                        } else {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            context.startActivity(intent)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAccessibilityEnabled) Color(0xFF24242C) else PrimaryBlue
                    )
                ) {
                    Text(
                        text = if (isAccessibilityEnabled) "Settings" else "Enable",
                        color = if (isAccessibilityEnabled) TextPrimary else Color.Black
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Card 3: Dictation Interaction Mode
        SetupCard(
            stepNumber = "3",
            title = "Interaction Mode",
            subtitle = "Choose how the floating bubble starts and stops recording",
            isComplete = true
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = settings.dictationMode == DictationMode.HOLD_TO_TALK,
                    onClick = { scope.launch { preferences.updateDictationMode(DictationMode.HOLD_TO_TALK) } },
                    label = { Text("Hold-to-Talk") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryBlue,
                        selectedLabelColor = Color.Black
                    ),
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    selected = settings.dictationMode == DictationMode.TAP_TO_TOGGLE,
                    onClick = { scope.launch { preferences.updateDictationMode(DictationMode.TAP_TO_TOGGLE) } },
                    label = { Text("Tap-to-Toggle") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryBlue,
                        selectedLabelColor = Color.Black
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Card 4: Security & Banking App Protection
        SetupCard(
            stepNumber = "4",
            title = "Automatic Security Bypass",
            subtitle = "Password fields and FLAG_SECURE screens are automatically suppressed",
            isComplete = true
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Security",
                    tint = AccentGreen,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Zero-keylogger guarantee. Banking & password fields auto-bypass.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }
    }

    // Google Play Compliant Prominent Disclosure Dialog
    if (showDisclosureDialog) {
        AlertDialog(
            onDismissRequest = { showDisclosureDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text("Accessibility Service Disclosure", fontWeight = FontWeight.Bold, color = TextPrimary)
            },
            text = {
                Text(
                    text = "FreeFlow uses the Android AccessibilityService API exclusively to detect when an editable text box gains focus so it can mount the floating voice recording HUD and inject your transcribed text directly at your cursor.\n\n" +
                            "• FreeFlow NEVER captures passwords, PINs, or sensitive financial data.\n" +
                            "• FreeFlow NEVER stores or transmits your typed keystrokes.\n" +
                            "• Password fields and banking applications are automatically bypassed.\n" +
                            "• Audio is converted in-memory to WAV and wiped immediately after transcription.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDisclosureDialog = false
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Agree & Open Settings", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisclosureDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = Color(0xFF18181C)
        )
    }
}

@Composable
private fun SetupCard(
    stepNumber: String,
    title: String,
    subtitle: String,
    isComplete: Boolean,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkCard)
            .border(
                width = 1.dp,
                color = if (isComplete) Color(0x335AB4FF) else DarkCardBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(18.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(if (isComplete) PrimaryBlue else Color(0xFF282832)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isComplete) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Done",
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Text(
                                text = stepNumber,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }
            }

            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
            )

            content()
        }
    }
}
