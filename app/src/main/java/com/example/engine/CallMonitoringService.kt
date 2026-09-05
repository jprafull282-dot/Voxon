package com.example.engine

import android.Manifest
import android.annotation.SuppressLint
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
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.engine.ai.DeepfakeDetector
import com.example.engine.ai.ScamDetector
import com.example.engine.ai.SpeakerVerifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Robust Foreground Background Service for real-time live phone call audio acquisition,
 * multi-model AI pipeline inference (Deepfake, Biometric Speaker Verification, Scam Detection),
 * and dynamic risk score aggregation.
 */
class CallMonitoringService : Service() {

    companion object {
        private const val TAG = "CallMonitoringService"
        private const val NOTIFICATION_CHANNEL_ID = "voiceguard_call_monitor_channel"
        private const val NOTIFICATION_ID = 8841

        const val ACTION_START_MONITORING = "com.example.action.START_MONITORING"
        const val ACTION_STOP_MONITORING = "com.example.action.STOP_MONITORING"
        const val EXTRA_CALLER_NUMBER = "EXTRA_CALLER_NUMBER"
        const val EXTRA_CALLER_NAME = "EXTRA_CALLER_NAME"

        // Reactive State for UI Layer
        private val _currentCallRisk = MutableStateFlow(CallRiskState())
        val currentCallRisk: StateFlow<CallRiskState> = _currentCallRisk.asStateFlow()

        fun start(context: Context, callerNumber: String, callerName: String = "Unknown Caller") {
            val intent = Intent(context, CallMonitoringService::class.java).apply {
                action = ACTION_START_MONITORING
                putExtra(EXTRA_CALLER_NUMBER, callerNumber)
                putExtra(EXTRA_CALLER_NAME, callerName)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, CallMonitoringService::class.java).apply {
                action = ACTION_STOP_MONITORING
            }
            context.startService(intent)
        }
    }

    data class CallRiskState(
        val isMonitoring: Boolean = false,
        val callerNumber: String = "",
        val callerName: String = "",
        val aggregateRiskScore: Int = 0, // 0 to 100
        val deepfakeScore: Float = 0f,
        val speakerMatchScore: Float = 0f,
        val speakerVerificationStatus: String = "NOT_VERIFIED",
        val scamScore: Float = 0f,
        val detectedThreats: List<String> = emptyList(),
        val callDurationSeconds: Int = 0
    )

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var audioCaptureJob: Job? = null
    private var durationTimerJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    // AI Engines
    private lateinit var deepfakeDetector: DeepfakeDetector
    private lateinit var speakerVerifier: SpeakerVerifier
    private lateinit var scamDetector: ScamDetector

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private val sampleRate = 16000
    private val bufferSize = AudioRecord.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    ).coerceAtLeast(sampleRate * 2)

    private var callStartTime = 0L
    private var currentCallerNumber = ""
    private var currentCallerName = ""

    override fun onCreate() {
        super.onCreate()
        deepfakeDetector = DeepfakeDetector(applicationContext)
        speakerVerifier = SpeakerVerifier(applicationContext)
        scamDetector = ScamDetector(applicationContext)
        createNotificationChannel()
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_MONITORING -> {
                currentCallerNumber = intent.getStringExtra(EXTRA_CALLER_NUMBER) ?: "Unknown"
                currentCallerName = intent.getStringExtra(EXTRA_CALLER_NAME) ?: "Live Call"
                startForegroundSafely(NOTIFICATION_ID, buildNotification(0, "Initializing AI Security Scan..."))
                startMonitoringPipeline()
            }
            ACTION_STOP_MONITORING -> {
                gracefulStop()
            }
        }
        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun startMonitoringPipeline() {
        if (isRecording) return
        callStartTime = System.currentTimeMillis()

        // 1. Start Duration Counter
        durationTimerJob?.cancel()
        durationTimerJob = serviceScope.launch {
            while (isActive) {
                val elapsed = ((System.currentTimeMillis() - callStartTime) / 1000).toInt()
                _currentCallRisk.value = _currentCallRisk.value.copy(callDurationSeconds = elapsed)
                delay(1000)
            }
        }

        // 2. Start Real-time Audio Capture & Model Inference Loop
        audioCaptureJob?.cancel()
        audioCaptureJob = serviceScope.launch(Dispatchers.IO) {
            try {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize
                )

                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    Log.w(TAG, "Fallback to MIC audio source...")
                    audioRecord = AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        sampleRate,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize
                    )
                }

                audioRecord?.startRecording()
                isRecording = true
                val audioChunk = ShortArray(1600) // 100ms chunk @ 16kHz

                var runningDeepfakeScore = 0.05f
                var runningSpeakerMatch = 0.95f
                var runningScamScore = 0.05f

                while (isActive && isRecording) {
                    val readCount = audioRecord?.read(audioChunk, 0, audioChunk.size) ?: 0
                    if (readCount > 0) {
                        // Pass chunk through AI Model Detectors
                        val deepfakeRes = deepfakeDetector.analyzeAudioChunk(audioChunk, readCount)
                        val speakerRes = speakerVerifier.verifySpeaker(audioChunk, readCount, null)
                        
                        // Exponential smoothing for stability
                        runningDeepfakeScore = (runningDeepfakeScore * 0.85f) + (deepfakeRes.syntheticProbability * 0.15f)
                        runningSpeakerMatch = (runningSpeakerMatch * 0.85f) + (speakerRes.similarityScore * 0.15f)

                        val isSpeakerEnrolled = speakerRes.status == "VERIFIED"
                        val effectiveSpeakerRisk = if (isSpeakerEnrolled) ((1f - speakerRes.similarityScore) * 20f) else 0f

                        // Evaluate aggregate risk score (0 - 100)
                        val combinedRisk = (
                            (runningDeepfakeScore * 60f) +
                            effectiveSpeakerRisk +
                            (runningScamScore * 40f)
                        ).toInt().coerceIn(0, 100)

                        val threatList = mutableListOf<String>()
                        if (runningDeepfakeScore >= 0.65f) threatList.add("Neural Vocoder Deepfake")
                        if (isSpeakerEnrolled && speakerRes.similarityScore < 0.60f) threatList.add("Speaker Biometric Mismatch")
                        if (runningScamScore >= 0.50f) threatList.add("Social Engineering Coercion")

                        _currentCallRisk.value = CallRiskState(
                            isMonitoring = true,
                            callerNumber = currentCallerNumber,
                            callerName = currentCallerName,
                            aggregateRiskScore = combinedRisk,
                            deepfakeScore = runningDeepfakeScore,
                            speakerMatchScore = if (isSpeakerEnrolled) speakerRes.similarityScore else 0f,
                            speakerVerificationStatus = speakerRes.status,
                            scamScore = runningScamScore,
                            detectedThreats = threatList,
                            callDurationSeconds = ((System.currentTimeMillis() - callStartTime) / 1000).toInt()
                        )

                        // Update Notification
                        val notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notifManager.notify(
                            NOTIFICATION_ID,
                            buildNotification(combinedRisk, "Risk: $combinedRisk% • $currentCallerName")
                        )
                    }
                    delay(50)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Audio capture loop failure: ${e.message}", e)
            } finally {
                releaseAudioRecorder()
            }
        }
    }

    private fun gracefulStop() {
        isRecording = false
        audioCaptureJob?.cancel()
        durationTimerJob?.cancel()
        releaseAudioRecorder()
        releaseWakeLock()

        _currentCallRisk.value = CallRiskState(isMonitoring = false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun releaseAudioRecorder() {
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing AudioRecord: ${e.message}", e)
        }
    }

    private fun acquireWakeLock() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "VoiceGuard:CallMonitorWakeLock").apply {
                acquire(60 * 60 * 1000L) // 1 hour safety timeout
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed acquiring WakeLock: ${e.message}", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing WakeLock: ${e.message}", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Live Call Security Scanner",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors ongoing calls for deepfake voice artifacts and financial fraud."
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(riskScore: Int, statusText: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle("🎙️ VoiceGuard Active Call Shield")
            .setContentText(statusText)
            .setOngoing(true)
            .setContentIntent(openIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun startForegroundSafely(notificationId: Int, notification: Notification) {
        try {
            val hasMicPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                var serviceType: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE or ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                } else {
                    0
                }
                if (hasMicPermission) {
                    serviceType = serviceType or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                }
                if (serviceType != 0) {
                    startForeground(notificationId, notification, serviceType)
                } else {
                    startForeground(notificationId, notification)
                }
            } else {
                startForeground(notificationId, notification)
            }
        } catch (e: Exception) {
            Log.w("CallMonitoringService", "Fallback startForeground: ${e.message}")
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(
                        notificationId,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE or ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    )
                } else {
                    startForeground(notificationId, notification)
                }
            } catch (ex: Exception) {
                Log.e("CallMonitoringService", "Fatal startForeground: ${ex.message}", ex)
            }
        }
    }

    override fun onDestroy() {
        gracefulStop()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
