package com.bheeshamks.freeflow.core

import com.bheeshamks.freeflow.core.audio.AudioCaptureManager
import com.bheeshamks.freeflow.core.audio.WavEncoder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AudioCaptureBufferTest {

    @Test
    fun testBufferCapacityConstantIsSafe() {
        // 8MB capacity check
        assertEquals(8 * 1024 * 1024, AudioCaptureManager.MAX_AUDIO_BUFFER_BYTES)

        // At 16kHz 16-bit mono PCM: 16,000 samples/sec * 2 bytes/sample = 32,000 bytes/sec
        val bytesPerSecond = 16000 * 2
        val maxRecordingSeconds = AudioCaptureManager.MAX_AUDIO_BUFFER_BYTES / bytesPerSecond
        // 8MB should provide > 250 seconds (over 4 minutes) of continuous speech
        assertTrue("Max recording should exceed 250 seconds", maxRecordingSeconds > 250)
    }

    @Test
    fun testSyntheticPcmWaveformEncodesCleanly() {
        val sampleRate = 16000
        val durationMs = 500 // 0.5s of 440Hz sine wave tone
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val pcmData = ByteArray(numSamples * 2)

        val buffer = ByteBuffer.wrap(pcmData).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until numSamples) {
            val angle = 2.0 * Math.PI * 440.0 * i / sampleRate
            val sample = (Math.sin(angle) * 30000).toInt().toShort()
            buffer.putShort(sample)
        }

        val wav = WavEncoder.encodePcmToWav(pcmData, sampleRate, 1, 16)
        assertNotNull(wav)
        assertEquals(pcmData.size + 44, wav.size)

        // Validate first 4 bytes are RIFF
        assertEquals('R'.code.toByte(), wav[0])
        assertEquals('I'.code.toByte(), wav[1])
        assertEquals('F'.code.toByte(), wav[2])
        assertEquals('F'.code.toByte(), wav[3])
    }
}
