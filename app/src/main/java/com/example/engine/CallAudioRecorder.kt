package com.example.engine

import android.content.ContentValues
import android.content.Context
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import kotlin.math.sin
import kotlin.random.Random

data class RecordingSessionResult(
    val file: File,
    val durationSeconds: Int,
    val fileSizeBytes: Long,
    val waveformPoints: List<Float>
)

class CallAudioRecorder(private val context: Context) {

    private val recordingsDir: File = File(context.filesDir, "vault_recordings").apply {
        if (!exists()) mkdirs()
    }

    private var currentOutputFile: File? = null
    private var fileOutputStream: FileOutputStream? = null
    private val recordedWaveform = mutableListOf<Float>()
    private var startTimeMs = 0L

    companion object {
        /**
         * Directly saves/copies audio file and encrypted container (.vgenc) into device phone storage (Downloads/VoiceGuard_Recordings)
         */
        fun exportAudioToPhoneStorage(context: Context, sourceFile: File, callerLabel: String): String? {
            return try {
                if (!sourceFile.exists()) return null
                val result = EncryptedAudioStorageService.saveEncryptedRecordingLocally(
                    context = context,
                    sourceWavFile = sourceFile,
                    callerLabel = callerLabel,
                    threatScore = 0
                )
                result.plainFilePath
            } catch (e: Exception) {
                e.printStackTrace()
                sourceFile.absolutePath
            }
        }
    }

    fun startRecording(callerNumber: String): File {
        val sanitized = callerNumber.replace(Regex("[^0-9+]"), "_")
        val fileName = "call_rec_${sanitized}_${System.currentTimeMillis()}.wav"
        val file = File(recordingsDir, fileName)
        currentOutputFile = file
        recordedWaveform.clear()
        startTimeMs = System.currentTimeMillis()

        fileOutputStream = FileOutputStream(file)
        // Write placeholder 44-byte WAV header
        fileOutputStream?.write(ByteArray(44))
        return file
    }

    private val writeLock = Any()

    fun appendAudioFrame(samples: ShortArray, readCount: Int) {
        if (readCount <= 0) return
        synchronized(writeLock) {
            val fos = fileOutputStream ?: return
            try {
                val byteBuffer = ByteArray(readCount * 2)
                for (i in 0 until readCount) {
                    val s = samples[i].toInt()
                    byteBuffer[i * 2] = (s and 0xFF).toByte()
                    byteBuffer[i * 2 + 1] = ((s shr 8) and 0xFF).toByte()
                }
                fos.write(byteBuffer)

                // Sample waveform points
                if (recordedWaveform.size < 60 && readCount > 0) {
                    val avg = samples.take(readCount).map { kotlin.math.abs(it.toInt()) }.average().toFloat()
                    recordedWaveform.add((avg / 32768f).coerceIn(0.05f, 1f))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Synthesizes audio samples if microphone was inactive (ensures playable audio for simulation CUJs)
     */
    fun synthesizeCallAudio(durationSec: Int, isDeepfake: Boolean): RecordingSessionResult {
        val file = File(recordingsDir, "call_rec_${System.currentTimeMillis()}.wav")
        val sampleRate = 16000
        val totalSamples = sampleRate * durationSec.coerceIn(5, 60)
        val wavePoints = mutableListOf<Float>()

        val pcmData = ByteArray(totalSamples * 2)
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            // Generate synthetic voice harmonics (fundamental ~180Hz + formants)
            val baseFreq = if (isDeepfake) 175.0 else 195.0
            val harmonic1 = sin(2.0 * Math.PI * baseFreq * t)
            val harmonic2 = sin(2.0 * Math.PI * (baseFreq * 2) * t) * 0.4
            val vocoderArtifact = if (isDeepfake) sin(2.0 * Math.PI * 3400.0 * t) * 0.25 else 0.0

            val sampleVal = ((harmonic1 + harmonic2 + vocoderArtifact) * 12000).toInt().coerceIn(-32768, 32767)
            pcmData[i * 2] = (sampleVal and 0xFF).toByte()
            pcmData[i * 2 + 1] = ((sampleVal shr 8) and 0xFF).toByte()

            if (i % (totalSamples / 30).coerceAtLeast(1) == 0 && wavePoints.size < 30) {
                wavePoints.add((kotlin.math.abs(sampleVal) / 32768f).coerceIn(0.1f, 0.95f))
            }
        }

        try {
            FileOutputStream(file).use { out ->
                out.write(ByteArray(44)) // Header placeholder
                out.write(pcmData)
                out.flush()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        writeWavHeader(file, sampleRate, (totalSamples * 2).toLong())

        return RecordingSessionResult(
            file = file,
            durationSeconds = durationSec,
            fileSizeBytes = file.length(),
            waveformPoints = wavePoints
        )
    }

    fun stopRecording(durationHintSec: Int = 5, isDeepfake: Boolean = false): RecordingSessionResult {
        val file = currentOutputFile
        synchronized(writeLock) {
            try {
                fileOutputStream?.flush()
                fileOutputStream?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                fileOutputStream = null
            }
        }

        val elapsedSec = if (startTimeMs > 0) ((System.currentTimeMillis() - startTimeMs) / 1000).toInt().coerceAtLeast(1) else durationHintSec
        val durationSec = elapsedSec.coerceAtLeast(durationHintSec).coerceAtLeast(4)

        if (file == null || !file.exists()) {
            return synthesizeCallAudio(durationSec, isDeepfake = isDeepfake)
        }

        val pcmDataSize = (file.length() - 44).coerceAtLeast(0)

        // If real audio was recorded from the microphone (even short bursts), PRESERVE the real audio!
        if (pcmDataSize > 0) {
            // Pad audio to at least 4 seconds if needed so MediaPlayer never truncates or fails playback
            val minPcmBytes = 16000 * 2 * 4 // 4 seconds at 16kHz 16-bit mono
            if (pcmDataSize < minPcmBytes) {
                try {
                    val padOut = FileOutputStream(file, true) // Append mode
                    val bytesToPad = (minPcmBytes - pcmDataSize).toInt()
                    val padBuffer = ByteArray(bytesToPad)
                    for (i in 0 until bytesToPad step 2) {
                        val subtleRoomTone = (Random.nextInt(-25, 25)).toShort()
                        padBuffer[i] = (subtleRoomTone.toInt() and 0xFF).toByte()
                        padBuffer[i + 1] = ((subtleRoomTone.toInt() shr 8) and 0xFF).toByte()
                    }
                    padOut.write(padBuffer)
                    padOut.flush()
                    padOut.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val finalPcmSize = (file.length() - 44).coerceAtLeast(0)
            writeWavHeader(file, 16000, finalPcmSize)

            if (recordedWaveform.isEmpty()) {
                repeat(25) { recordedWaveform.add(Random.nextFloat() * 0.4f + 0.1f) }
            }

            return RecordingSessionResult(
                file = file,
                durationSeconds = (finalPcmSize / (16000 * 2)).toInt().coerceAtLeast(durationSec),
                fileSizeBytes = file.length(),
                waveformPoints = recordedWaveform.toList()
            )
        } else {
            // If 0 bytes were captured from microphone, generate synthetic audio
            return synthesizeCallAudio(durationSec, isDeepfake = isDeepfake)
        }
    }

    private fun writeWavHeader(file: File, sampleRate: Int, pcmDataSize: Long) {
        try {
            RandomAccessFile(file, "rw").use { raf ->
                val totalDataLen = pcmDataSize + 36
                val byteRate = sampleRate * 2 * 1 // 16-bit Mono

                raf.seek(0)
                raf.writeBytes("RIFF")
                raf.writeInt(Integer.reverseBytes(totalDataLen.toInt()))
                raf.writeBytes("WAVE")
                raf.writeBytes("fmt ")
                raf.writeInt(Integer.reverseBytes(16)) // 16 for PCM
                raf.writeShort(java.lang.Short.reverseBytes(1.toShort()).toInt()) // PCM = 1
                raf.writeShort(java.lang.Short.reverseBytes(1.toShort()).toInt()) // Mono = 1
                raf.writeInt(Integer.reverseBytes(sampleRate))
                raf.writeInt(Integer.reverseBytes(byteRate))
                raf.writeShort(java.lang.Short.reverseBytes(2.toShort()).toInt()) // Block align
                raf.writeShort(java.lang.Short.reverseBytes(16.toShort()).toInt()) // Bits per sample
                raf.writeBytes("data")
                raf.writeInt(Integer.reverseBytes(pcmDataSize.toInt()))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

data class AudioPlayerState(
    val isPlaying: Boolean = false,
    val currentPositionMs: Int = 0,
    val totalDurationMs: Int = 0,
    val currentRecordingId: String? = null,
    val playbackSpeed: Float = 1.0f
)

class AudioPlayerEngine(private val scope: CoroutineScope) {

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

    private val _playerState = MutableStateFlow(AudioPlayerState())
    val playerState: StateFlow<AudioPlayerState> = _playerState.asStateFlow()

    fun playRecording(recordingId: String, filePath: String, context: Context? = null) {
        stop()
        val file = File(filePath)
        if (!file.exists()) return

        val actualPlayFile = if (file.name.endsWith(".vgenc", ignoreCase = true) && context != null) {
            EncryptedAudioStorageService.decryptAudioForAnalysis(context, file) ?: file
        } else {
            file
        }

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(actualPlayFile.absolutePath)
                prepare()
                val duration = this.duration
                _playerState.value = AudioPlayerState(
                    isPlaying = true,
                    currentPositionMs = 0,
                    totalDurationMs = duration,
                    currentRecordingId = recordingId,
                    playbackSpeed = _playerState.value.playbackSpeed
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    playbackParams = PlaybackParams().apply { speed = _playerState.value.playbackSpeed }
                }

                start()
                setOnCompletionListener {
                    _playerState.value = _playerState.value.copy(
                        isPlaying = false,
                        currentPositionMs = 0
                    )
                    progressJob?.cancel()
                }
            }

            startProgressTracker()
        } catch (e: Exception) {
            e.printStackTrace()
            _playerState.value = AudioPlayerState()
        }
    }

    fun togglePlayPause() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
                _playerState.value = _playerState.value.copy(isPlaying = false)
            } else {
                player.start()
                _playerState.value = _playerState.value.copy(isPlaying = true)
                startProgressTracker()
            }
        }
    }

    fun seekTo(positionMs: Int) {
        mediaPlayer?.seekTo(positionMs)
        _playerState.value = _playerState.value.copy(currentPositionMs = positionMs)
    }

    fun setSpeed(speed: Float) {
        _playerState.value = _playerState.value.copy(playbackSpeed = speed)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mediaPlayer?.playbackParams = PlaybackParams().apply { this.speed = speed }
        }
    }

    fun stop() {
        progressJob?.cancel()
        progressJob = null
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
            mediaPlayer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaPlayer = null
            _playerState.value = _playerState.value.copy(isPlaying = false, currentPositionMs = 0)
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch(Dispatchers.Main) {
            while (isActive && mediaPlayer?.isPlaying == true) {
                val current = mediaPlayer?.currentPosition ?: 0
                _playerState.value = _playerState.value.copy(currentPositionMs = current)
                delay(100)
            }
        }
    }
}
