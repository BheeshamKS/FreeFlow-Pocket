package com.bheeshamks.freeflow.core

import com.bheeshamks.freeflow.core.audio.WavEncoder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AudioWavEncoderTest {

    @Test
    fun testWavHeaderEncodingStructure() {
        val sampleRate = 16000
        val channels = 1
        val bitDepth = 16
        val dummyPcmData = ByteArray(3200) { 0 } // 100ms of silence

        val wavBytes = WavEncoder.encodePcmToWav(
            pcmData = dummyPcmData,
            sampleRate = sampleRate,
            channels = channels,
            bitDepth = bitDepth
        )

        assertNotNull(wavBytes)
        assertEquals(dummyPcmData.size + 44, wavBytes.size)

        val buffer = ByteBuffer.wrap(wavBytes).order(ByteOrder.LITTLE_ENDIAN)

        // 1. RIFF chunk descriptor
        val riff = ByteArray(4)
        buffer.get(riff)
        assertEquals("RIFF", String(riff))

        val chunkSize = buffer.int
        assertEquals(dummyPcmData.size + 36, chunkSize)

        val wave = ByteArray(4)
        buffer.get(wave)
        assertEquals("WAVE", String(wave))

        // 2. fmt subchunk
        val fmt = ByteArray(4)
        buffer.get(fmt)
        assertEquals("fmt ", String(fmt))

        val subchunk1Size = buffer.int
        assertEquals(16, subchunk1Size)

        val audioFormat = buffer.short
        assertEquals(1.toShort(), audioFormat) // PCM = 1

        val numChannels = buffer.short
        assertEquals(channels.toShort(), numChannels)

        val sampleRateRead = buffer.int
        assertEquals(sampleRate, sampleRateRead)

        val byteRate = buffer.int
        assertEquals(sampleRate * channels * (bitDepth / 8), byteRate)

        val blockAlign = buffer.short
        assertEquals((channels * (bitDepth / 8)).toShort(), blockAlign)

        val bitsPerSample = buffer.short
        assertEquals(bitDepth.toShort(), bitsPerSample)

        // 3. data subchunk
        val data = ByteArray(4)
        buffer.get(data)
        assertEquals("data", String(data))

        val subchunk2Size = buffer.int
        assertEquals(dummyPcmData.size, subchunk2Size)
    }

    @Test
    fun testEmptyPcmProducesValidHeader() {
        val emptyPcm = ByteArray(0)
        val wavBytes = WavEncoder.encodePcmToWav(emptyPcm, 16000, 1, 16)

        assertEquals(44, wavBytes.size)
        val buffer = ByteBuffer.wrap(wavBytes).order(ByteOrder.LITTLE_ENDIAN)
        val riff = ByteArray(4)
        buffer.get(riff)
        assertEquals("RIFF", String(riff))
        assertEquals(36, buffer.int) // ChunkSize for 0 bytes PCM
    }
}
