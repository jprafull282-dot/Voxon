package com.example.engine

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.engine.gemini.GeminiFraudAnalysisService
import com.example.engine.gemini.GeminiFraudIntentResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * CallTranscriptFraudAnalysisService
 *
 * Android Background & Foreground Service that sends transcribed snippets
 * of incoming call audio to the Gemini API (gemini-3.5-flash) to evaluate
 * fraud intent, extract behavioral tactics, and calculate the security score.
 */
class CallTranscriptFraudAnalysisService : Service() {

    companion object {
        private const val TAG = "TranscriptFraudService"
        const val NOTIFICATION_ID = 2098
        const val CHANNEL_ID = "voxen_gemini_fraud_channel"

        const val ACTION_ANALYZE_TRANSCRIPT = "com.example.action.ANALYZE_CALL_TRANSCRIPT"
        const val EXTRA_TRANSCRIPT_SNIPPET = "extra_transcript_snippet"
        const val EXTRA_CALLER_NAME = "extra_caller_name"
        const val EXTRA_CALLER_NUMBER = "extra_caller_number"
        const val EXTRA_CALL_DURATION = "extra_call_duration"
        const val EXTRA_LANGUAGE_PREFERENCE = "extra_language_preference"
        const val EXTRA_SENSITIVITY_LEVEL = "extra_sensitivity_level"

        // Observable state flow for UI components
        private val _lastAnalysisResult = MutableStateFlow<GeminiFraudIntentResult?>(null)
        val lastAnalysisResult: StateFlow<GeminiFraudIntentResult?> = _lastAnalysisResult.asStateFlow()

        private val _isAnalyzing = MutableStateFlow(false)
        val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

        /**
         * Helper method to trigger asynchronous Gemini transcript evaluation
         */
        fun analyzeTranscript(
            context: Context,
            transcript: String,
            callerName: String = "Incoming Call",
            callerNumber: String = "Unknown",
            durationSeconds: Int = 0,
            preferredLanguage: String = "Auto-Detect Multilingual",
            sensitivityLevel: Float = 0.85f
        ) {
            val intent = Intent(context, CallTranscriptFraudAnalysisService::class.java).apply {
                action = ACTION_ANALYZE_TRANSCRIPT
                putExtra(EXTRA_TRANSCRIPT_SNIPPET, transcript)
                putExtra(EXTRA_CALLER_NAME, callerName)
                putExtra(EXTRA_CALLER_NUMBER, callerNumber)
                putExtra(EXTRA_CALL_DURATION, durationSeconds)
                putExtra(EXTRA_LANGUAGE_PREFERENCE, preferredLanguage)
                putExtra(EXTRA_SENSITIVITY_LEVEL, sensitivityLevel)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var geminiService: GeminiFraudAnalysisService

    override fun onCreate() {
        super.onCreate()
        geminiService = GeminiFraudAnalysisService(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_ANALYZE_TRANSCRIPT) {
            val transcript = intent.getStringExtra(EXTRA_TRANSCRIPT_SNIPPET) ?: ""
            val callerName = intent.getStringExtra(EXTRA_CALLER_NAME) ?: "Incoming Caller"
            val callerNumber = intent.getStringExtra(EXTRA_CALLER_NUMBER) ?: "Unknown"
            val duration = intent.getIntExtra(EXTRA_CALL_DURATION, 0)
            val preferredLanguage = intent.getStringExtra(EXTRA_LANGUAGE_PREFERENCE) ?: "Auto-Detect Multilingual"
            val sensitivityLevel = intent.getFloatExtra(EXTRA_SENSITIVITY_LEVEL, 0.85f)

            val initialNotification = buildNotification(
                title = "🧠 Gemini AI Multilingual Scanner",
                content = "Scanning speech in $preferredLanguage for $callerName...",
                isHighRisk = false
            )
            startForeground(NOTIFICATION_ID, initialNotification)

            performAnalysis(transcript, callerName, callerNumber, duration, preferredLanguage, sensitivityLevel)
        }
        return START_NOT_STICKY
    }

    private fun performAnalysis(
        transcript: String,
        callerName: String,
        callerNumber: String,
        duration: Int,
        preferredLanguage: String,
        sensitivityLevel: Float
    ) {
        _isAnalyzing.value = true
        serviceScope.launch {
            try {
                val result = geminiService.analyzeCallTranscript(
                    transcriptSnippet = transcript,
                    callerName = callerName,
                    callerNumber = callerNumber,
                    callDurationSeconds = duration,
                    preferredLanguage = preferredLanguage,
                    sensitivityLevel = sensitivityLevel
                )

                _lastAnalysisResult.value = result
                Log.i(TAG, "Gemini Analysis complete. Language: ${result.detectedLanguage}, Security Score: ${result.securityScore}, Fraud Risk: ${result.fraudRiskScore}%, Verdict: ${result.verdict}")

                val isHighRisk = result.fraudRiskScore >= 65
                val notif = buildNotification(
                    title = if (isHighRisk) "🚨 ${result.vernacularThreatTag.ifEmpty { "CRITICAL FRAUD INTENT DETECTED" }}" else "🛡️ Call Intent Analyzed",
                    content = "${result.detectedLanguage} • Score: ${result.securityScore}/100 • ${result.summary}",
                    isHighRisk = isHighRisk
                )
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(NOTIFICATION_ID, notif)

                if (isHighRisk) {
                    // Trigger high-priority heads-up banner with action buttons
                    VoiceGuardNotificationManager.showSuspiciousFraudIntentAlertBanner(
                        context = applicationContext,
                        callerName = callerName,
                        callerNumber = callerNumber,
                        fraudScore = result.fraudRiskScore,
                        detectedTactics = result.detectedTactics,
                        summary = result.summary
                    )

                    // Securely log fraud intent metadata to Firestore
                    try {
                        val loggingService = com.example.data.firestore.FirestoreFraudLoggingService(applicationContext, serviceScope)
                        loggingService.logFraudIntent(
                            callerName = callerName,
                            callerNumber = callerNumber,
                            threatCategory = if (result.detectedTactics.isNotEmpty()) result.detectedTactics.first() else "Suspicious Fraud Intent",
                            fraudRiskScore = result.fraudRiskScore,
                            aiConfidence = 0.95f,
                            detectedTactics = result.detectedTactics,
                            rawTranscript = transcript,
                            acousticDeepfakeProbability = 0.88f,
                            languageDetected = result.detectedLanguage,
                            verdict = result.verdict
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to log fraud intent to Firestore: ${e.message}")
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error in background Gemini analysis: ${e.message}", e)
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    private fun buildNotification(title: String, content: String, isHighRisk: Boolean): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentIntent(pendingIntent)
            .setOngoing(false)
            .setPriority(if (isHighRisk) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Gemini AI Call Fraud Intelligence",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Real-time AI intent scanning and fraud security scoring"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
