package com.bheeshamks.freeflow.core.audio

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Pure in-memory WAV/RIFF encoder for raw PCM audio data.
 * Prepends a standard 44-byte canonical RIFF/WAVE header to uncompressed PCM audio bytes.
 */
object WavEncoder {

    /**
     * Encodes uncompressed PCM bytes into a standard 44-byte RIFF/WAVE container.
     *
     * @param pcmData Raw PCM audio byte array.
     * @param sampleRate Sampling rate in Hz (e.g. 16000).
     * @param channels Channel count (1 for Mono, 2 for Stereo).
     * @param bitDepth Bits per sample (e.g. 16 for 16-bit PCM).
     * @return Complete .wav byte array containing the 44-byte header followed by PCM audio data.
     */
    fun encodePcmToWav(
        pcmData: ByteArray,
        sampleRate: Int = 16000,
        channels: Int = 1,
        bitDepth: Int = 16
    ): ByteArray {
        val totalAudioLen = pcmData.size
        val totalDataLen = totalAudioLen + 36
        val byteRate = sampleRate * channels * (bitDepth / 8)
        val blockAlign = channels * (bitDepth / 8)

        val header = ByteBuffer.allocate(44).apply {
            order(ByteOrder.LITTLE_ENDIAN)

            // 00-03: ChunkID "RIFF" (Big-Endian ASCII)
            put('R'.code.toByte())
            put('I'.code.toByte())
            put('F'.code.toByte())
            put('F'.code.toByte())

            // 04-07: ChunkSize = TotalDataLen (PCM size + 36)
            putInt(totalDataLen)

            // 08-11: Format "WAVE" (Big-Endian ASCII)
            put('W'.code.toByte())
            put('A'.code.toByte())
            put('V'.code.toByte())
            put('E'.code.toByte())

            // 12-15: Subchunk1ID "fmt " (Big-Endian ASCII)
            put('f'.code.toByte())
            put('m'.code.toByte())
            put('t'.code.toByte())
            put(' '.code.toByte())

            // 16-19: Subchunk1Size = 16 for PCM
            putInt(16)

            // 20-21: AudioFormat = 1 (Linear PCM)
            putShort(1.toShort())

            // 22-23: NumChannels
            putShort(channels.toShort())

            // 24-27: SampleRate
            putInt(sampleRate)

            // 28-31: ByteRate
            putInt(byteRate)

            // 32-33: BlockAlign
            putShort(blockAlign.toShort())

            // 34-35: BitsPerSample
            putShort(bitDepth.toShort())

            // 36-39: Subchunk2ID "data" (Big-Endian ASCII)
            put('d'.code.toByte())
            put('a'.code.toByte())
            put('t'.code.toByte())
            put('a'.code.toByte())

            // 40-43: Subchunk2Size (PCM audio byte length)
            putInt(totalAudioLen)
        }.array()

        val wavBytes = ByteArray(44 + totalAudioLen)
        System.arraycopy(header, 0, wavBytes, 0, 44)
        if (totalAudioLen > 0) {
            System.arraycopy(pcmData, 0, wavBytes, 44, totalAudioLen)
        }

        return wavBytes
    }
}
