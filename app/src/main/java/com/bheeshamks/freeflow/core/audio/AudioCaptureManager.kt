package com.bheeshamks.freeflow.core.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import kotlin.math.sqrt

/**
 * Thread-safe real-time audio capture engine.
 * Records 16kHz, 16-bit mono PCM via Android [AudioRecord] directly into memory,
 * emitting real-time normalized audio amplitude (0.0f..1.0f) at 60fps to drive UI waveforms.
 * Fortified with buffer-overflow circuit breakers and memory safety guards.
 */
class AudioCaptureManager(
    private val sampleRate: Int = 16000,
    private val channelConfig: Int = AudioFormat.CHANNEL_IN_MONO,
    private val audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT
) {
    companion object {
        private const val TAG = "AudioCaptureManager"
        private const val MAX_PCM_AMPLITUDE = 32767f
        // Defensive circuit breaker: cap audio buffer at 8MB (~4.1 minutes of continuous 16kHz speech)
        const val MAX_AUDIO_BUFFER_BYTES = 8 * 1024 * 1024
    }

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val audioOutputStream = ByteArrayOutputStream()
    private val recordingScope = CoroutineScope(Dispatchers.IO)

    private val minBufferSize: Int by lazy {
        val calculated = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        if (calculated <= 0) 2048 else calculated
    }

    /**
     * Starts continuous audio capture. Requires [android.Manifest.permission.RECORD_AUDIO].
     * @return true if AudioRecord initialized and started successfully, false otherwise.
     */
    @SuppressLint("MissingPermission")
    @Synchronized
    fun startRecording(): Boolean {
        if (_isRecording.value) {
            Log.w(TAG, "Recording already in progress.")
            return true
        }

        synchronized(audioOutputStream) {
            audioOutputStream.reset()
        }
        _amplitude.value = 0f

        try {
            val bufferSize = (minBufferSize * 2).coerceAtLeast(4096)
            var record: AudioRecord? = null
            val audioSources = intArrayOf(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                MediaRecorder.AudioSource.MIC
            )

            for (source in audioSources) {
                try {
                    val candidate = AudioRecord(
                        source,
                        sampleRate,
                        channelConfig,
                        audioFormat,
                        bufferSize
                    )
                    if (candidate.state == AudioRecord.STATE_INITIALIZED) {
                        record = candidate
                        Log.i(TAG, "AudioRecord initialized successfully with audioSource=$source")
                        break
                    } else {
                        candidate.release()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to initialize AudioRecord with source $source: ${e.message}")
                }
            }

            if (record == null || record.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord initialization failed across all audio sources.")
                record?.release()
                return false
            }

            record.startRecording()
            audioRecord = record
            _isRecording.value = true

            recordingJob = recordingScope.launch {
                val buffer = ByteArray(minBufferSize)
                var lastAmplitudeUpdate = System.currentTimeMillis()

                try {
                    while (isActive && _isRecording.value) {
                        val bytesRead = record.read(buffer, 0, buffer.size)
                        if (bytesRead > 0) {
                            synchronized(audioOutputStream) {
                                // Circuit breaker: prevent OutOfMemoryError if user forgets to stop
                                if (audioOutputStream.size() + bytesRead <= MAX_AUDIO_BUFFER_BYTES) {
                                    audioOutputStream.write(buffer, 0, bytesRead)
                                } else {
                                    Log.w(TAG, "Maximum audio buffer limit reached ($MAX_AUDIO_BUFFER_BYTES bytes). Capping capture.")
                                    return@launch
                                }
                            }

                            // Calculate RMS amplitude at ~60fps (every ~16ms)
                            val now = System.currentTimeMillis()
                            if (now - lastAmplitudeUpdate >= 16) {
                                lastAmplitudeUpdate = now
                                val rms = calculateRms(buffer, bytesRead)
                                val normalized = (rms / MAX_PCM_AMPLITUDE).coerceIn(0f, 1f)
                                _amplitude.value = normalized
                            }
                        } else if (bytesRead < 0) {
                            Log.e(TAG, "AudioRecord read error code: $bytesRead")
                            break
                        }
                    }
                } finally {
                    _amplitude.value = 0f
                }
            }

            Log.i(TAG, "Audio capture started at $sampleRate Hz mono PCM.")
            return true
        } catch (e: SecurityException) {
            Log.e(TAG, "RECORD_AUDIO permission missing or revoked", e)
            cleanup()
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Exception starting audio recording", e)
            cleanup()
            return false
        }
    }

    /**
     * Stops audio capture and returns a 44-byte RIFF/WAVE byte array containing all recorded PCM audio.
     * Zero temporary files are written to disk.
     */
    @Synchronized
    fun stopRecording(): ByteArray? {
        if (!_isRecording.value) {
            return null
        }

        _isRecording.value = false
        recordingJob?.cancel()
        recordingJob = null

        cleanup()

        val rawPcm = synchronized(audioOutputStream) {
            val bytes = audioOutputStream.toByteArray()
            audioOutputStream.reset()
            bytes
        }

        _amplitude.value = 0f

        if (rawPcm.isEmpty()) {
            Log.w(TAG, "Recorded audio buffer was empty.")
            return null
        }

        Log.i(TAG, "Captured ${rawPcm.size} bytes of raw PCM. Packaging into in-memory WAV container.")
        return WavEncoder.encodePcmToWav(
            pcmData = rawPcm,
            sampleRate = sampleRate,
            channels = 1,
            bitDepth = 16
        )
    }

    /**
     * Cancels recording immediately and discards any audio data.
     */
    @Synchronized
    fun cancelRecording() {
        _isRecording.value = false
        recordingJob?.cancel()
        recordingJob = null
        cleanup()
        synchronized(audioOutputStream) {
            audioOutputStream.reset()
        }
        _amplitude.value = 0f
    }

    private fun cleanup() {
        try {
            audioRecord?.let {
                if (it.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up AudioRecord", e)
        } finally {
            audioRecord = null
        }
    }

    /**
     * Computes the Root Mean Square (RMS) amplitude from 16-bit little-endian PCM byte buffer.
     */
    private fun calculateRms(buffer: ByteArray, bytesRead: Int): Float {
        var sum = 0.0
        val numSamples = bytesRead / 2
        if (numSamples == 0) return 0f

        for (i in 0 until bytesRead step 2) {
            if (i + 1 < bytesRead) {
                val sample = ((buffer[i + 1].toInt() shl 8) or (buffer[i].toInt() and 0xFF)).toShort()
                sum += sample * sample
            }
        }

        val mean = sum / numSamples
        return sqrt(mean).toFloat()
    }
}
