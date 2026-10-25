package com.bheeshamks.freeflow.ime.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import com.bheeshamks.freeflow.core.data.model.KeySoundTheme
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Ultra-low-latency sound synthesizer and player for keyboard clicks.
 * Pre-computes crisp PCM waveforms for mechanical thocks, bubble pops, and soft taps,
 * and delegates to system sound effects for standard Android click.
 */
class KeyboardSoundPlayer(private val context: Context) {

    companion object {
        private const val TAG = "KeyboardSoundPlayer"
        private const val SAMPLE_RATE = 44100
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private var mechanicalTrack: AudioTrack? = null
    private var bubbleTrack: AudioTrack? = null
    private var softTapTrack: AudioTrack? = null

    init {
        try {
            mechanicalTrack = createMechanicalClickTrack()
            bubbleTrack = createBubblePopTrack()
            softTapTrack = createSoftTapTrack()
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing keyboard sound tracks", e)
        }
    }

    /**
     * Plays the selected sound theme at the requested volume.
     */
    fun playKeySound(theme: KeySoundTheme, volume: Float = 0.75f) {
        if (theme == KeySoundTheme.OFF || volume <= 0.01f) return

        val clampedVol = volume.coerceIn(0f, 1f)
        try {
            when (theme) {
                KeySoundTheme.OFF -> return
                KeySoundTheme.SYSTEM -> {
                    audioManager?.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, clampedVol)
                }
                KeySoundTheme.MECHANICAL -> {
                    playStatic(mechanicalTrack, clampedVol)
                }
                KeySoundTheme.BUBBLE_POP -> {
                    playStatic(bubbleTrack, clampedVol)
                }
                KeySoundTheme.SOFT_TAP -> {
                    playStatic(softTapTrack, clampedVol)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play key sound: ${e.message}")
        }
    }

    private fun playStatic(track: AudioTrack?, volume: Float) {
        track?.let {
            try {
                it.setVolume(volume)
                it.stop()
                it.reloadStaticData()
                it.play()
            } catch (e: Exception) {
                Log.e(TAG, "Error triggering static AudioTrack", e)
            }
        }
    }

    /**
     * Synthesizes a sharp, satisfying mechanical switch click (tactile thock).
     * Dual-frequency 1900Hz and 3100Hz with fast exponential decay (20ms).
     */
    private fun createMechanicalClickTrack(): AudioTrack {
        val durationMs = 22
        val numSamples = (SAMPLE_RATE * durationMs) / 1000
        val pcm = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            // Fast attack and steep exponential decay
            val envelope = exp(-t / 0.0035)
            // Resonance mix
            val s1 = sin(2.0 * PI * 1950.0 * t)
            val s2 = sin(2.0 * PI * 3200.0 * t) * 0.45
            val s3 = sin(2.0 * PI * 650.0 * t) * 0.35 // Lower tactile body
            val sample = ((s1 + s2 + s3) * envelope * Short.MAX_VALUE * 0.85).toInt()
            pcm[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        return buildTrack(pcm)
    }

    /**
     * Synthesizes a playful, modern water bubble pop sound.
     * Ascending frequency sweep from 360Hz to 950Hz (28ms).
     */
    private fun createBubblePopTrack(): AudioTrack {
        val durationMs = 28
        val numSamples = (SAMPLE_RATE * durationMs) / 1000
        val pcm = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val t = i.toDouble() / SAMPLE_RATE
            // Ascending pitch modulation
            val freq = 360.0 + 580.0 * progress
            val envelope = exp(-t / 0.0075) * (1.0 - progress * 0.2)
            val sample = (sin(2.0 * PI * freq * t) * envelope * Short.MAX_VALUE * 0.9).toInt()
            pcm[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        return buildTrack(pcm)
    }

    /**
     * Synthesizes a warm, discreet, dampened soft tap (Samsung Good Lock style).
     * 260Hz rounded pulse with soft decay (16ms).
     */
    private fun createSoftTapTrack(): AudioTrack {
        val durationMs = 18
        val numSamples = (SAMPLE_RATE * durationMs) / 1000
        val pcm = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val envelope = exp(-t / 0.004)
            val sample = (sin(2.0 * PI * 270.0 * t) * envelope * Short.MAX_VALUE * 0.8).toInt()
            pcm[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        return buildTrack(pcm)
    }

    private fun buildTrack(pcmData: ShortArray): AudioTrack {
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(pcmData.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        track.write(pcmData, 0, pcmData.size)
        return track
    }

    /**
     * Releases audio hardware resources.
     */
    fun release() {
        try {
            mechanicalTrack?.release()
            mechanicalTrack = null
            bubbleTrack?.release()
            bubbleTrack = null
            softTapTrack?.release()
            softTapTrack = null
        } catch (_: Exception) {}
    }
}
