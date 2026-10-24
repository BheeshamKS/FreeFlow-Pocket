package com.bheeshamks.freeflow.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
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
 * First-run interactive Onboarding flow.
 * Guides the user step-by-step through:
 * 1. Introduction to FreeFlow
 * 2. Groq API Key configuration and validation
 * 3. Required permissions (Microphone & Accessibility)
 * 4. Completion & transition to Home
 */
@Composable
fun OnboardingScreen(
    apiKeyStore: ApiKeyStore,
    groqClient: GroqWhisperClient,
    onCompleteOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentStep by remember { mutableStateOf(1) }

    // API Key State
    var apiKeyInput by remember { mutableStateOf(apiKeyStore.getApiKey() ?: "") }
    var isKeyVisible by remember { mutableStateOf(false) }
    var isTestingKey by remember { mutableStateOf(false) }
    var keyValidationMessage by remember { mutableStateOf<String?>(null) }
    var isKeyValid by remember { mutableStateOf(apiKeyStore.hasApiKey()) }

    // Permission States
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    var isAccessibilityEnabled by remember { mutableStateOf(false) }

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

    LaunchedEffect(currentStep) {
        checkAccessibility()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Step Indicator Dots
        Row(
            modifier = Modifier.padding(top = 10.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (step in 1..4) {
                val isActive = step == currentStep
                val isCompleted = step < currentStep
                Box(
                    modifier = Modifier
                        .size(if (isActive) 24.dp else 8.dp, 8.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isActive -> PrimaryBlue
                                isCompleted -> AccentGreen
                                else -> Color(0xFF282832)
                            }
                        )
                )
            }
        }

        AnimatedContent(
            targetState = currentStep,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "OnboardingStepTransition"
        ) { step ->
            when (step) {
                1 -> Step1Welcome(
                    onNext = { currentStep = 2 }
                )
                2 -> Step2ApiKey(
                    apiKeyInput = apiKeyInput,
                    isKeyVisible = isKeyVisible,
                    isTestingKey = isTestingKey,
                    isKeyValid = isKeyValid,
                    keyValidationMessage = keyValidationMessage,
                    onKeyChange = {
                        apiKeyInput = it.trim()
                        keyValidationMessage = null
                        isKeyValid = false
                    },
                    onToggleVisibility = { isKeyVisible = !isKeyVisible },
                    onTestAndSave = {
                        isTestingKey = true
                        keyValidationMessage = null
                        apiKeyStore.saveApiKey(apiKeyInput)
                        scope.launch {
                            val result = groqClient.testConnection()
                            isTestingKey = false
                            if (result.isSuccess) {
                                isKeyValid = true
                                keyValidationMessage = "Groq LPU verified ✓ (${result.getOrNull()} models)"
                            } else {
                                isKeyValid = false
                                keyValidationMessage = "Failed: ${result.exceptionOrNull()?.message ?: "Invalid API Key"}"
                            }
                        }
                    },
                    onOpenGroqConsole = {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://console.groq.com/keys"))
                        context.startActivity(browserIntent)
                    },
                    onNext = { currentStep = 3 }
                )
                3 -> Step3Permissions(
                    hasMicPermission = hasMicPermission,
                    isAccessibilityEnabled = isAccessibilityEnabled,
                    onRequestMic = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                    onOpenAccessibility = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        context.startActivity(intent)
                    },
                    onOpenKeyboardSettings = {
                        val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                        context.startActivity(intent)
                    },
                    onRefresh = {
                        hasMicPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                        checkAccessibility()
                    },
                    onNext = { currentStep = 4 }
                )
                4 -> Step4Ready(
                    onFinish = onCompleteOnboarding
                )
            }
        }
    }
}

@Composable
private fun Step1Welcome(onNext: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Hero Icon
        Box(
            modifier = Modifier
                .size(100.dp)
                .shadow(elevation = 20.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF5AB4FF), Color(0xFF0058B8))
                    )
                )
                .border(2.dp, Color(0x66FFFFFF), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "FreeFlow",
                tint = Color.White,
                modifier = Modifier.size(50.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Welcome to FreeFlow",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "High-speed AI voice dictation (~300ms) powered by Groq LPU and Whisper Large V3. 100% private with on-device local storage.",
            fontSize = 14.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 10.dp)
        )

        Spacer(modifier = Modifier.height(30.dp))

        // 3 Key Highlights
        FeatureHighlightItem(
            icon = Icons.Default.Widgets,
            title = "Wispr Flow Floating Bubble",
            subtitle = "A compact, draggable bubble docked to the screen edge. Tap to dictate into any app."
        )

        Spacer(modifier = Modifier.height(14.dp))

        FeatureHighlightItem(
            icon = Icons.Default.Keyboard,
            title = "Samsung Keys Cafe Keyboard",
            subtitle = "Custom QWERTY keyboard with animated neon radial ripple blooms on every keystroke."
        )

        Spacer(modifier = Modifier.height(14.dp))

        FeatureHighlightItem(
            icon = Icons.Default.Security,
            title = "Zero Disk Footprint",
            subtitle = "Audio is streamed in-memory directly to Groq. Zero temporary sound files saved to your phone."
        )

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(text = "Get Started", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun Step2ApiKey(
    apiKeyInput: String,
    isKeyVisible: Boolean,
    isTestingKey: Boolean,
    isKeyValid: Boolean,
    keyValidationMessage: String?,
    onKeyChange: (String) -> Unit,
    onToggleVisibility: () -> Unit,
    onTestAndSave: () -> Unit,
    onOpenGroqConsole: () -> Unit,
    onNext: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Step 1: Connect Groq API",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "FreeFlow uses your Groq Cloud API key to deliver ultra-fast Whisper transcription. Groq provides generous free usage.",
            fontSize = 13.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = apiKeyInput,
            onValueChange = onKeyChange,
            label = { Text("Groq API Key (gsk_...)") },
            placeholder = { Text("gsk_xxxxxxxxxxxxxxxxxxxx") },
            singleLine = true,
            visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = onToggleVisibility) {
                    Icon(
                        imageVector = if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle",
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

        // Get key link
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { onOpenGroqConsole() }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Get a free API key at console.groq.com", color = PrimaryBlue, fontSize = 13.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Icon(imageVector = Icons.Default.OpenInNew, contentDescription = "Open", tint = PrimaryBlue, modifier = Modifier.size(14.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onTestAndSave,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = apiKeyInput.isNotBlank() && !isTestingKey,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF242430)),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isTestingKey) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = PrimaryBlue, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Validating with Groq...", color = Color.White)
            } else {
                Icon(imageVector = Icons.Default.Key, contentDescription = "Test", tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Test & Save Key", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }

        keyValidationMessage?.let { msg ->
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = msg,
                color = if (isKeyValid) AccentGreen else AccentRed,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(36.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = if (isKeyValid) "Continue to Permissions" else "Continue",
                color = Color.Black,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun Step3Permissions(
    hasMicPermission: Boolean,
    isAccessibilityEnabled: Boolean,
    onRequestMic: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onOpenKeyboardSettings: () -> Unit,
    onRefresh: () -> Unit,
    onNext: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Step 2: Permissions Setup",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Grant the permissions below so FreeFlow can capture voice audio and type text into your apps.",
            fontSize = 13.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 1. Microphone
        PermissionSetupCard(
            title = "1. Microphone Access",
            description = "Required to capture speech and convert it into text.",
            isGranted = hasMicPermission,
            actionLabel = "Grant Mic",
            onAction = onRequestMic
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Accessibility
        PermissionSetupCard(
            title = "2. Accessibility Service",
            description = "Enables the floating Wispr Flow bubble and types transcribed text into any app.",
            isGranted = isAccessibilityEnabled,
            actionLabel = "Enable in Settings",
            onAction = onOpenAccessibility
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Custom Keyboard (Optional)
        PermissionSetupCard(
            title = "3. Keys Cafe Keyboard (Optional)",
            description = "Enables the Samsung Keys Cafe custom keyboard with animated glow.",
            isGranted = false,
            actionLabel = "Setup IME",
            onAction = onOpenKeyboardSettings
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(text = "Next: Final Step", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun Step4Ready(onFinish: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .size(90.dp)
                .shadow(elevation = 16.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(Color(0xFF00E676)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Success",
                tint = Color.Black,
                modifier = Modifier.size(50.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "You're All Set!",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "FreeFlow is now ready. Here is how to use it:",
            fontSize = 14.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(DarkCard)
                .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "• Home Screen: Tap the big microphone to dictate into FreeFlow's notepad and copy or share text.",
                    fontSize = 13.sp,
                    color = TextPrimary,
                    lineHeight = 18.sp
                )
                Text(
                    text = "• Floating Bubble: A discreet mic icon floats at your screen edge. Tap once to talk, tap the green tick to finish and inject text.",
                    fontSize = 13.sp,
                    color = TextPrimary,
                    lineHeight = 18.sp
                )
                Text(
                    text = "• Quick Settings: Swipe down from the top of your phone and tap the FreeFlow tile for instant voice dictation.",
                    fontSize = 13.sp,
                    color = TextPrimary,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        Button(
            onClick = onFinish,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(text = "Enter FreeFlow", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun FeatureHighlightItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkCard)
            .border(1.dp, DarkCardBorder, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color(0xFF1E2638)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = PrimaryBlue, modifier = Modifier.size(22.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(text = subtitle, fontSize = 12.sp, color = TextSecondary, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun PermissionSetupCard(
    title: String,
    description: String,
    isGranted: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkCard)
            .border(1.dp, if (isGranted) Color(0x4400E676) else DarkCardBorder, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(text = description, fontSize = 11.sp, color = TextSecondary, lineHeight = 15.sp)
        }

        Spacer(modifier = Modifier.width(10.dp))

        if (isGranted) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x3300E676))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(text = "Granted ✓", color = AccentGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text(text = actionLabel, color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
