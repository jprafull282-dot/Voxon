package com.example.engine

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Data class representing a slice of streamed call audio.
 */
data class AudioCaptureChunk(
    val samples: ShortArray,
    val length: Int,
    val timestampMs: Long = System.currentTimeMillis(),
    val rmsLevel: Float = 0f,
    val decibels: Float = -60f
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as AudioCaptureChunk
        return length == other.length && timestampMs == other.timestampMs
    }

    override fun hashCode(): Int {
        var result = length
        result = 31 * result + timestampMs.hashCode()
        return result
    }
}

/**
 * AudioCaptureManager
 *
 * Android Foreground Service and centralized streaming manager that utilizes
 * Android's [AudioRecord] API to capture live call audio and stream it to
 * AI deepfake detection, speech-to-text, and local vault recording engines.
 *
 * Key Capabilities:
 * - Robust multi-source AudioRecord fallback (VOICE_COMMUNICATION, VOICE_RECOGNITION, MIC, DEFAULT)
 * - 16kHz 16-bit PCM continuous real-time audio chunk dispatching
 * - Decibel & RMS telemetry emission for live visualizers
 * - Thread-safe listener registration for processing pipelines
 * - Foreground Service lifecycle management compliant with Android 14+ requirements
 */
class AudioCaptureManager : Service() {

    companion object {
        private const val TAG = "AudioCaptureManager"
        const val NOTIFICATION_ID = 2105
        const val CHANNEL_ID = "voiceguard_audio_capture_channel"

        const val ACTION_START_CAPTURE = "com.example.action.START_AUDIO_CAPTURE"
        const val ACTION_STOP_CAPTURE = "com.example.action.STOP_AUDIO_CAPTURE"

        const val EXTRA_CALLER_NAME = "extra_caller_name"
        const val EXTRA_CALLER_NUMBER = "extra_caller_number"

        const val SAMPLE_RATE = 16000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        const val CHUNK_SIZE = 1024

        // Reactive Singleton Telemetry & Streaming Bus
        private val _isCapturing = MutableStateFlow(false)
        val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

        private val _audioRms = MutableStateFlow(0f)
        val audioRms: StateFlow<Float> = _audioRms.asStateFlow()

        private val _audioDecibels = MutableStateFlow(-60f)
        val audioDecibels: StateFlow<Float> = _audioDecibels.asStateFlow()

        private val _activeCallerName = MutableStateFlow("Live Call")
        val activeCallerName: StateFlow<String> = _activeCallerName.asStateFlow()

        private val _activeCallerNumber = MutableStateFlow("")
        val activeCallerNumber: StateFlow<String> = _activeCallerNumber.asStateFlow()

        private val _audioChunkStream = MutableSharedFlow<AudioCaptureChunk>(extraBufferCapacity = 64)
        val audioChunkStream: SharedFlow<AudioCaptureChunk> = _audioChunkStream.asSharedFlow()

        // Thread-safe listener registry for processing engines
        private val audioConsumers = CopyOnWriteArrayList<(ShortArray, Int) -> Unit>()

        fun addAudioConsumer(consumer: (ShortArray, Int) -> Unit) {
            if (!audioConsumers.contains(consumer)) {
                audioConsumers.add(consumer)
            }
        }

        fun removeAudioConsumer(consumer: (ShortArray, Int) -> Unit) {
            audioConsumers.remove(consumer)
        }

        /**
         * Convenience helper to launch AudioCaptureManager as a Foreground Service.
         */
        fun startCaptureService(
            context: Context,
            callerName: String = "Incoming Call",
            callerNumber: String = ""
        ) {
            val intent = Intent(context, AudioCaptureManager::class.java).apply {
                action = ACTION_START_CAPTURE
                putExtra(EXTRA_CALLER_NAME, callerName)
                putExtra(EXTRA_CALLER_NUMBER, callerNumber)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start AudioCaptureManager service: ${e.message}", e)
            }
        }

        /**
         * Convenience helper to stop the AudioCaptureManager Foreground Service.
         */
        fun stopCaptureService(context: Context) {
            val intent = Intent(context, AudioCaptureManager::class.java).apply {
                action = ACTION_STOP_CAPTURE
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop AudioCaptureManager service: ${e.message}", e)
            }
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var captureJob: Job? = null
    private var audioRecord: AudioRecord? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_CAPTURE -> {
                val callerName = intent.getStringExtra(EXTRA_CALLER_NAME) ?: "Live Call"
                val callerNumber = intent.getStringExtra(EXTRA_CALLER_NUMBER) ?: ""
                _activeCallerName.value = callerName
                _activeCallerNumber.value = callerNumber
                startForegroundCapture(callerName, callerNumber)
            }
            ACTION_STOP_CAPTURE -> {
                stopCapture()
                stopSelf()
            }
            else -> {
                if (!_isCapturing.value) {
                    startForegroundCapture(_activeCallerName.value, _activeCallerNumber.value)
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundCapture(callerName: String, callerNumber: String) {
        val notification = buildForegroundNotification(callerName, callerNumber)

        try {
            val hasMicPermission = ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                var types = ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE or
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                if (hasMicPermission) {
                    types = types or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                }
                startForeground(NOTIFICATION_ID, notification, types)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val types = if (hasMicPermission) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                } else {
                    0
                }
                if (types != 0) {
                    startForeground(NOTIFICATION_ID, notification, types)
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting foreground service: ${e.message}", e)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE or ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            } catch (e2: Exception) {
                Log.e(TAG, "Fallback startForeground failed: ${e2.message}", e2)
            }
        }

        launchAudioRecordStream()
    }

    private fun launchAudioRecordStream() {
        if (_isCapturing.value) return

        val hasMicPermission = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasMicPermission) {
            Log.w(TAG, "RECORD_AUDIO permission missing; cannot initialize AudioRecord.")
            return
        }

        val minBufSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        val bufferSize = maxOf(minBufSize * 2, 4096)

        // Iterate candidate audio sources for maximum device OEM compatibility during live telephony calls
        val candidateSources = intArrayOf(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            MediaRecorder.AudioSource.MIC,
            MediaRecorder.AudioSource.DEFAULT
        )

        for (source in candidateSources) {
            try {
                val record = AudioRecord(
                    source,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize
                )
                if (record.state == AudioRecord.STATE_INITIALIZED) {
                    record.startRecording()
                    if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                        audioRecord = record
                        _isCapturing.value = true
                        Log.i(TAG, "AudioRecord initialized successfully with audio source $source at ${SAMPLE_RATE}Hz.")
                        break
                    } else {
                        record.release()
                    }
                } else {
                    record.release()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed initializing AudioRecord with source $source: ${e.message}")
            }
        }

        if (audioRecord == null) {
            Log.e(TAG, "All candidate audio sources failed to initialize AudioRecord.")
            return
        }

        captureJob = serviceScope.launch {
            val audioBuffer = ShortArray(CHUNK_SIZE)
            while (isActive && _isCapturing.value) {
                val readCount = audioRecord?.read(audioBuffer, 0, audioBuffer.size) ?: -1
                if (readCount > 0) {
                    // Compute acoustic telemetry (RMS and dB)
                    var sumSquare = 0.0
                    for (i in 0 until readCount) {
                        val s = audioBuffer[i].toInt()
                        sumSquare += s * s
                    }
                    val rms = sqrt(sumSquare / readCount).toFloat()
                    val normalizedRms = (rms / 32768f).coerceIn(0f, 1f)
                    val db = (20 * log10(normalizedRms.coerceAtLeast(0.0001f).toDouble())).toFloat()

                    _audioRms.value = normalizedRms
                    _audioDecibels.value = db

                    // Dispatch to reactive shared flow for UI / analysis subscribers
                    val chunk = AudioCaptureChunk(
                        samples = audioBuffer.copyOf(readCount),
                        length = readCount,
                        rmsLevel = normalizedRms,
                        decibels = db
                    )
                    _audioChunkStream.tryEmit(chunk)

                    // Dispatch to registered stream consumers (e.g. CallAudioRecorder, TflitePipeline)
                    for (consumer in audioConsumers) {
                        try {
                            consumer.invoke(audioBuffer, readCount)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error invoking audio consumer: ${e.message}", e)
                        }
                    }
                } else {
                    delay(15)
                }
            }
        }
    }

    private fun stopCapture() {
        _isCapturing.value = false
        _audioRms.value = 0f
        _audioDecibels.value = -60f

        captureJob?.cancel()
        captureJob = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping AudioRecord: ${e.message}")
        } finally {
            audioRecord = null
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopCapture()
        serviceScope.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "VoiceGuard Live Call Audio Capture",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active background audio capture for real-time AI deepfake detection"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(callerName: String, callerNumber: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, AudioCaptureManager::class.java).apply {
            action = ACTION_STOP_CAPTURE
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val callerSubtitle = if (callerNumber.isNotEmpty()) "$callerName ($callerNumber)" else callerName

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🛡️ VoiceGuard Call Audio Monitor Active")
            .setContentText("Capturing & analyzing audio for: $callerSubtitle")
            .setSubText("AI Deepfake Sentinel")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(contentPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop Audio Capture",
                stopPendingIntent
            )
            .build()
    }
}
