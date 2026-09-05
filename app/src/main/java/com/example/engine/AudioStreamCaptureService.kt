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
import com.example.engine.tflite.TfliteAudioProcessingPipeline
import com.example.engine.tflite.TfliteSpectralInferenceResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.min

/**
 * AudioStreamCaptureService
 *
 * Dedicated Android Foreground Service boilerplate that captures live incoming
 * audio streams via [AudioRecord] and executes real-time Deepfake & Voice Clone
 * inference using TensorFlow Lite.
 *
 * Capabilities:
 * - 16kHz 16-bit PCM continuous ring-buffer streaming
 * - Multi-layer TFLite spectral, phase-hop, and prosody inference
 * - Dynamic Foreground Service Notification with live risk score HUD
 * - Reactive [StateFlow] and broadcast intent emission for alert popups / UI
 */
class AudioStreamCaptureService : Service() {

    companion object {
        private const val TAG = "AudioCaptureService"
        const val NOTIFICATION_ID = 2099
        const val NOTIFICATION_CHANNEL_ID = "voxen_audio_stream_channel"

        const val ACTION_START_AUDIO_CAPTURE = "com.example.action.START_AUDIO_CAPTURE"
        const val ACTION_STOP_AUDIO_CAPTURE = "com.example.action.STOP_AUDIO_CAPTURE"

        const val EXTRA_CALLER_NAME = "extra_caller_name"
        const val EXTRA_CALLER_NUMBER = "extra_caller_number"
        const val EXTRA_THREAT_SIMULATION = "extra_threat_sim"

        const val SAMPLE_RATE = 16000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

        // Observable state for UI and Background Observers
        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

        private val _currentInference = MutableStateFlow<TfliteSpectralInferenceResult?>(null)
        val currentInference: StateFlow<TfliteSpectralInferenceResult?> = _currentInference.asStateFlow()

        /**
         * Helper method to start audio capture service
         */
        fun start(context: Context, callerName: String = "Incoming Call", callerNumber: String = "Unknown", isSimulated: Boolean = false) {
            val intent = Intent(context, AudioStreamCaptureService::class.java).apply {
                action = ACTION_START_AUDIO_CAPTURE
                putExtra(EXTRA_CALLER_NAME, callerName)
                putExtra(EXTRA_CALLER_NUMBER, callerNumber)
                putExtra(EXTRA_THREAT_SIMULATION, isSimulated)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /**
         * Helper method to stop audio capture service
         */
        fun stop(context: Context) {
            val intent = Intent(context, AudioStreamCaptureService::class.java).apply {
                action = ACTION_STOP_AUDIO_CAPTURE
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var recordingJob: Job? = null
    private var audioRecord: AudioRecord? = null
    private var isRecording = false

    private lateinit var tflitePipeline: TfliteAudioProcessingPipeline
    private var callerName: String = "Incoming Stream"
    private var callerNumber: String = "Telephony"
    private var isSimulationMode: Boolean = false

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "AudioStreamCaptureService created.")
        createNotificationChannel()
        tflitePipeline = TfliteAudioProcessingPipeline(applicationContext, serviceScope)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_AUDIO_CAPTURE -> {
                callerName = intent.getStringExtra(EXTRA_CALLER_NAME) ?: "Incoming Stream"
                callerNumber = intent.getStringExtra(EXTRA_CALLER_NUMBER) ?: "Telephony"
                isSimulationMode = intent.getBooleanExtra(EXTRA_THREAT_SIMULATION, false)
                startForegroundAudioCapture()
            }
            ACTION_STOP_AUDIO_CAPTURE -> {
                stopForegroundAudioCapture()
                stopSelf()
            }
            else -> {
                startForegroundAudioCapture()
            }
        }
        return START_STICKY
    }

    private fun startForegroundAudioCapture() {
        val initialNotification = buildNotification(
            title = "🛡️ Voxen Real-Time Audio Deepfake Shield",
            content = "Capturing $callerName ($callerNumber) • 16kHz TFLite DSP Active",
            riskScore = 5
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val hasMicPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            var serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE or ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            }
            if (hasMicPermission) {
                serviceType = serviceType or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            }

            try {
                if (serviceType != 0) {
                    startForeground(NOTIFICATION_ID, initialNotification, serviceType)
                } else {
                    startForeground(NOTIFICATION_ID, initialNotification)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed startForeground: ${e.message}, falling back")
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        startForeground(
                            NOTIFICATION_ID,
                            initialNotification,
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE or ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                        )
                    } else {
                        startForeground(NOTIFICATION_ID, initialNotification)
                    }
                } catch (ex: Exception) {
                    Log.e(TAG, "Fatal fallback startForeground: ${ex.message}", ex)
                }
            }
        } else {
            startForeground(NOTIFICATION_ID, initialNotification)
        }

        _isServiceRunning.value = true
        startAudioProcessingPipeline()
        startAudioRecordThread()
    }

    private fun startAudioProcessingPipeline() {
        tflitePipeline.startPipeline { result ->
            _currentInference.value = result

            // Update Foreground Notification if significant change
            val riskScore = (result.deepfakeProbability * 100).toInt()
            if (riskScore >= 70) {
                updateNotification(
                    title = "🚨 SYNTHETIC DEEPFAKE VOICE DETECTED",
                    content = "${result.detectedVocoderSignature} (Risk $riskScore%)",
                    riskScore = riskScore
                )
            }
        }
    }

    private fun startAudioRecordThread() {
        if (isRecording) return

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "RECORD_AUDIO permission not granted! Cannot start AudioRecord.")
            return
        }

        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        val bufferSize = (minBufferSize * 2).coerceAtLeast(4096)

        try {
            // Prioritize VOICE_COMMUNICATION or MIC
            val audioSource = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                MediaRecorder.AudioSource.VOICE_COMMUNICATION
            } else {
                MediaRecorder.AudioSource.MIC
            }

            audioRecord = AudioRecord(
                audioSource,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.w(TAG, "VOICE_COMMUNICATION AudioRecord init failed, falling back to MIC")
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize
                )
            }

            audioRecord?.startRecording()
            isRecording = true
            Log.i(TAG, "AudioRecord started successfully at $SAMPLE_RATE Hz.")

            recordingJob = serviceScope.launch(Dispatchers.IO) {
                val audioBuffer = ShortArray(1024)
                while (isActive && isRecording) {
                    val readCount = audioRecord?.read(audioBuffer, 0, audioBuffer.size) ?: -1
                    if (readCount > 0) {
                        tflitePipeline.ingestPcmSamples(audioBuffer, readCount)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error launching AudioRecord loop: ${e.message}", e)
        }
    }

    private fun stopForegroundAudioCapture() {
        isRecording = false
        _isServiceRunning.value = false

        recordingJob?.cancel()
        recordingJob = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping AudioRecord: ${e.message}")
        } finally {
            audioRecord = null
        }

        tflitePipeline.stopPipeline()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun updateNotification(title: String, content: String, riskScore: Int) {
        val notification = buildNotification(title, content, riskScore)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(title: String, content: String, riskScore: Int): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, AudioStreamCaptureService::class.java).apply {
            action = ACTION_STOP_AUDIO_CAPTURE
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isHighRisk = riskScore >= 70
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(if (isHighRisk) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_DEFAULT)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Shield", stopPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Voxen Real-Time Audio Deepfake Shield",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Continuous TFLite audio streaming and deepfake vocoder analysis"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopForegroundAudioCapture()
        tflitePipeline.release()
        serviceScope.cancel()
        super.onDestroy()
        Log.i(TAG, "AudioStreamCaptureService destroyed.")
    }
}
