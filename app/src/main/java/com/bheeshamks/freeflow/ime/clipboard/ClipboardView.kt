package com.bheeshamks.freeflow.ime.clipboard

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bheeshamks.freeflow.R
import com.bheeshamks.freeflow.core.data.model.HapticStrength
import com.bheeshamks.freeflow.core.data.model.KeyboardSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Samsung Keyboard inspired Clipboard Menu matching Reference Screenshot 5.
 * Features top navigation header with Pin/Trash actions and 3-column card grid of copied clips.
 * Supports image thumbnails and graceful previews for copied images/screenshots.
 */
@Composable
fun ClipboardView(
    settings: KeyboardSettings,
    clips: List<ClipItem>,
    onPasteClip: (ClipItem) -> Unit,
    onClearAll: () -> Unit,
    onBackToKeyboard: () -> Unit,
    onHideKeyboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val vibrator = remember { context.getSystemService(Vibrator::class.java) }

    fun playFeedback() {
        if (settings.hapticStrength == HapticStrength.OFF) return
        val effect = when (settings.hapticStrength) {
            HapticStrength.TICK -> VibrationEffect.EFFECT_TICK
            HapticStrength.CLICK -> VibrationEffect.EFFECT_CLICK
            HapticStrength.HEAVY_CLICK -> VibrationEffect.EFFECT_HEAVY_CLICK
            HapticStrength.OFF -> return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator?.vibrate(VibrationEffect.createPredefined(effect))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(25)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(Color(settings.keyboardTheme.bgHex))
            .navigationBarsPadding()
    ) {
        // Top Header: Keyboard Button | Clipboard title ... [Pin] [Trash]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(Color(0xFF141418))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Return to QWERTY Keyboard
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable {
                        playFeedback()
                        onBackToKeyboard()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_keyboard),
                    contentDescription = "Keyboard",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Thin Vertical Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(20.dp)
                    .background(Color(0xFF383844))
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Title "Clipboard"
            Text(
                text = "Clipboard",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.weight(1f))

            // Pin Icon Action
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .clickable { playFeedback() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_pin),
                    contentDescription = "Pin Clips",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Trash / Clear Action
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .clickable {
                        playFeedback()
                        onClearAll()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_trash),
                    contentDescription = "Clear Clipboard",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // 3-Column Card Grid of Clipboard Clips
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(Color(0xFF0C0C0F))
                .padding(horizontal = 6.dp, vertical = 6.dp)
        ) {
            if (clips.isEmpty()) {
                Box(
                    modifier = Modifier.matchParentSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Clipboard is empty",
                        color = Color(0xFF888894),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(clips) { clip ->
                        ClipboardCard(
                            clip = clip,
                            context = context,
                            onCardClick = {
                                playFeedback()
                                onPasteClip(clip)
                            }
                        )
                    }
                }
            }
        }

        // Bottom chin with Down Chevron to hide keyboard
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .background(Color(0xFF0C0C0F))
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clickable { onHideKeyboard() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Hide Keyboard",
                    tint = Color(0xFF888894),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun ClipboardCard(
    clip: ClipItem,
    context: Context,
    onCardClick: () -> Unit
) {
    var loadedBitmap by remember(clip.uriString) { mutableStateOf<ImageBitmap?>(null) }
    var loadAttempted by remember(clip.uriString) { mutableStateOf(false) }

    LaunchedEffect(clip.uriString) {
        val uriStr = clip.uriString
        if (clip.isImage && !uriStr.isNullOrEmpty()) {
            withContext(Dispatchers.IO) {
                try {
                    val bmp: android.graphics.Bitmap? = if (uriStr.startsWith("/")) {
                        val file = java.io.File(uriStr)
                        if (file.exists()) {
                            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                            BitmapFactory.decodeFile(file.absolutePath, opts)
                            val sampleSize = calculateInSampleSize(opts, 240, 240)
                            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                            BitmapFactory.decodeFile(file.absolutePath, decodeOpts) ?: BitmapFactory.decodeFile(file.absolutePath)
                        } else null
                    } else {
                        val uri = Uri.parse(uriStr)
                        if (uri.scheme == "file") {
                            val path = uri.path ?: uriStr
                            val file = java.io.File(path)
                            if (file.exists()) {
                                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                                BitmapFactory.decodeFile(file.absolutePath, opts)
                                val sampleSize = calculateInSampleSize(opts, 240, 240)
                                val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                                BitmapFactory.decodeFile(file.absolutePath, decodeOpts) ?: BitmapFactory.decodeFile(file.absolutePath)
                            } else null
                        } else {
                            context.contentResolver.openInputStream(uri)?.use { stream ->
                                val bytes = stream.readBytes()
                                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
                                val sampleSize = calculateInSampleSize(opts, 240, 240)
                                val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts) ?: BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            }
                        }
                    }
                    if (bmp != null) {
                        loadedBitmap = bmp.asImageBitmap()
                    }
                } catch (e: Exception) {
                    // Safe fallback if stream is closed or permission expired
                } finally {
                    loadAttempted = true
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF16161B))
            .border(
                width = 1.dp,
                color = Color(0xFF2C2C38),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onCardClick() },
        contentAlignment = Alignment.TopStart
    ) {
        if (clip.isImage) {
            if (loadedBitmap != null) {
                Image(
                    bitmap = loadedBitmap!!,
                    contentDescription = "Image Clip",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Polished Image Card Placeholder
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1B1B22))
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "Image",
                        tint = Color(0xFF5AB4FF),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Image",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        } else {
            Box(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = clip.text,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    val height = options.outHeight
    val width = options.outWidth
    var inSampleSize = 1
    if (height > reqHeight || width > reqWidth) {
        val halfHeight = height / 2
        val halfWidth = width / 2
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize.coerceAtLeast(1)
}
