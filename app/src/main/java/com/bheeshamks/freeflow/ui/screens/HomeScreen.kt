package com.bheeshamks.freeflow.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.bheeshamks.freeflow.R
import com.bheeshamks.freeflow.core.audio.AudioCaptureManager
import com.bheeshamks.freeflow.core.data.db.DictationEntity
import com.bheeshamks.freeflow.core.data.db.HistoryDatabase
import com.bheeshamks.freeflow.core.data.prefs.ThemeAndGoodiesPreferences
import com.bheeshamks.freeflow.core.network.GroqWhisperClient
import com.bheeshamks.freeflow.core.security.ApiKeyStore
import com.bheeshamks.freeflow.ui.theme.AccentGreen
import com.bheeshamks.freeflow.ui.theme.AccentRed
import com.bheeshamks.freeflow.ui.theme.DarkBackground
import com.bheeshamks.freeflow.ui.theme.DarkCard
import com.bheeshamks.freeflow.ui.theme.DarkCardBorder
import com.bheeshamks.freeflow.ui.theme.DarkRaised
import com.bheeshamks.freeflow.ui.theme.MonospaceTabular
import com.bheeshamks.freeflow.ui.theme.PrimaryBlue
import com.bheeshamks.freeflow.ui.theme.TextPrimary
import com.bheeshamks.freeflow.ui.theme.TextSecondary
import com.bheeshamks.freeflow.ui.theme.TextTertiary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * FreeFlow Home Screen: Focused speech-to-text recording directly on FreeFlow's primary text area.
 * Single tap-to-toggle recording with real-time waveform visualization, latency metrics, and instant actions.
 */
@Composable
fun HomeScreen(
    apiKeyStore: ApiKeyStore,
    preferences: ThemeAndGoodiesPreferences,
    groqClient: GroqWhisperClient,
    audioCaptureManager: AudioCaptureManager,
    historyDb: HistoryDatabase,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val vibrator = remember { context.getSystemService(Vibrator::class.java) }

    var transcribedText by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    var isTranscribing by remember { mutableStateOf(false) }
    var lastLatencyMs by remember { mutableStateOf<Long?>(null) }
    var elapsedSeconds by remember { mutableStateOf(0) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val amplitude by audioCaptureManager.amplitude.collectAsState()
    val hasApiKey = apiKeyStore.hasApiKey()

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (!granted) {
            Toast.makeText(context, "Microphone permission is required to dictate", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            elapsedSeconds = 0
            while (true) {
                delay(1000)
                elapsedSeconds++
            }
        } else {
            elapsedSeconds = 0
        }
    }

    fun triggerHaptic(effect: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator?.vibrate(VibrationEffect.createPredefined(effect))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(25)
        }
    }

    fun startRecording() {
        if (!hasMicPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        if (!hasApiKey) {
            Toast.makeText(context, "Please enter your Groq API Key in Settings first", Toast.LENGTH_LONG).show()
            onNavigateToSettings()
            return
        }

        val started = audioCaptureManager.startRecording()
        if (started) {
            isRecording = true
            isTranscribing = false
            statusMessage = null
            triggerHaptic(VibrationEffect.EFFECT_CLICK)
        } else {
            statusMessage = "Could not open microphone"
        }
    }

    fun stopRecording() {
        if (!isRecording) return
        isRecording = false
        isTranscribing = true
        triggerHaptic(VibrationEffect.EFFECT_TICK)

        scope.launch(Dispatchers.IO) {
            val wavBytes = audioCaptureManager.stopRecording()
            if (wavBytes == null || wavBytes.isEmpty()) {
                launch(Dispatchers.Main) {
                    isTranscribing = false
                }
                return@launch
            }

            val result = groqClient.transcribe(wavBytes)
            launch(Dispatchers.Main) {
                isTranscribing = false
                result.onSuccess { transcription ->
                    triggerHaptic(VibrationEffect.EFFECT_HEAVY_CLICK)
                    lastLatencyMs = transcription.latencyMs
                    transcribedText = if (transcribedText.isBlank()) {
                        transcription.text
                    } else {
                        "$transcribedText ${transcription.text}"
                    }

                    // Save to Room SQLite database
                    launch(Dispatchers.IO) {
                        historyDb.dictationDao().insertDictation(
                            DictationEntity(
                                text = transcription.text,
                                latencyMs = transcription.latencyMs,
                                targetPackage = "com.bheeshamks.freeflow.home"
                            )
                        )
                    }
                }.onFailure { err ->
                    statusMessage = err.message ?: "Transcription failed"
                    Toast.makeText(context, "Error: ${err.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_freeflow_logo),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(36.dp)
                )
                Column {
                    Text(
                        text = "FreeFlow",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Whisper Large V3 via Groq",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            if (!hasApiKey) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkRaised)
                        .border(1.dp, AccentRed.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .clickable { onNavigateToSettings() }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = AccentRed,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "Set key",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = AccentRed
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkRaised)
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(AccentGreen)
                    )
                    Text(
                        text = "Ready",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Main Text Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(DarkCard)
                .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                OutlinedTextField(
                    value = transcribedText,
                    onValueChange = { transcribedText = it },
                    placeholder = {
                        Text(
                            text = "Tap microphone below to record speech",
                            color = TextTertiary,
                            fontSize = 15.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = PrimaryBlue
                    )
                )

                // Info & Metrics Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val wordCount = if (transcribedText.isBlank()) 0 else transcribedText.trim().split("\\s+".toRegex()).size
                    val charCount = transcribedText.length

                    Text(
                        text = "$wordCount words · $charCount chars",
                        style = MonospaceTabular
                    )

                    lastLatencyMs?.let { latency ->
                        Text(
                            text = "${latency}ms",
                            style = MonospaceTabular.copy(color = TextSecondary)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action Buttons Row: Copy, Share, Clear
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Copy
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkRaised)
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(10.dp))
                    .clickable {
                        if (transcribedText.isNotBlank()) {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("FreeFlow Transcription", transcribedText))
                            triggerHaptic(VibrationEffect.EFFECT_CLICK)
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy",
                    tint = TextPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Copy", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }

            // Share
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkRaised)
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(10.dp))
                    .clickable {
                        if (transcribedText.isNotBlank()) {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, transcribedText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Transcription"))
                        }
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    tint = TextPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Share", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }

            // Clear
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkRaised)
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(10.dp))
                    .clickable {
                        transcribedText = ""
                        lastLatencyMs = null
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Clear",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Clear", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Hero Recording Control Section
        Box(
            modifier = Modifier.size(64.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        if (isRecording) AccentRed else DarkRaised
                    )
                    .border(
                        1.dp,
                        if (isRecording) AccentRed else DarkCardBorder,
                        CircleShape
                    )
                    .clickable {
                        if (isRecording) {
                            stopRecording()
                        } else if (!isTranscribing) {
                            startRecording()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isTranscribing) {
                    CircularProgressIndicator(
                        color = PrimaryBlue,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(28.dp)
                    )
                } else if (isRecording) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop Recording",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Start Recording",
                        tint = PrimaryBlue,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Status or Timer Label
        if (isRecording) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(AccentRed)
                )
                Text(
                    text = String.format("%02d:%02d", elapsedSeconds / 60, elapsedSeconds % 60),
                    style = MonospaceTabular.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold)
                )
            }
        } else if (isTranscribing) {
            Text(
                text = "Transcribing",
                fontSize = 12.sp,
                color = TextSecondary
            )
        } else {
            Text(
                text = "Tap to record",
                fontSize = 12.sp,
                color = TextTertiary
            )
        }

        statusMessage?.let { msg ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = msg,
                fontSize = 12.sp,
                color = AccentRed
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}
