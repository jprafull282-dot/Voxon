package com.example.engine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.telecom.TelecomManager
import android.util.Log
import androidx.room.Room
import com.example.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver responsible for processing user interaction clicks directly from the
 * high-priority heads-up notifications:
 * - 'DISCONNECT CALL': Terminates ongoing phone call, records threat mitigation, and alerts user.
 * - 'MARK SAFE': Whitelists the caller in the database and updates status in real-time.
 */
class ThreatNotificationActionReceiver : BroadcastReceiver() {

    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "ThreatNotifReceiver"

        const val ACTION_DISCONNECT_CALL = "com.example.action.DISCONNECT_CALL"
        const val ACTION_MARK_SAFE = "com.example.action.MARK_SAFE"
        const val ACTION_BLOCK_NUMBER = "com.example.action.BLOCK_NUMBER"
        const val ACTION_VIEW_FORENSICS = "com.example.action.VIEW_FORENSICS"

        const val EXTRA_CALLER_NAME = "EXTRA_CALLER_NAME"
        const val EXTRA_CALLER_NUMBER = "EXTRA_CALLER_NUMBER"
        const val EXTRA_INCIDENT_ID = "EXTRA_INCIDENT_ID"
        const val EXTRA_RISK_SCORE = "EXTRA_RISK_SCORE"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        val callerName = intent.getStringExtra(EXTRA_CALLER_NAME) ?: "Caller"
        val callerNumber = intent.getStringExtra(EXTRA_CALLER_NUMBER) ?: "Unknown"
        val incidentId = intent.getStringExtra(EXTRA_INCIDENT_ID) ?: ""
        val riskScore = intent.getIntExtra(EXTRA_RISK_SCORE, 95)

        Log.i(TAG, "Received notification action: ${intent.action} for $callerName ($callerNumber)")

        when (intent.action) {
            ACTION_DISCONNECT_CALL -> {
                handleDisconnectCall(context, callerName, callerNumber, incidentId, riskScore)
            }
            ACTION_MARK_SAFE -> {
                handleMarkSafe(context, callerName, callerNumber, incidentId)
            }
        }
    }

    /**
     * Handles 'Disconnect Call' action:
     * 1. Attempts to terminate call via TelecomManager
     * 2. Hides floating HUD overlay
     * 3. Signals PhoneCallMonitorHub & terminates active analysis
     * 4. Updates Room DB status to THREAT_BLOCKED / DISCONNECTED
     * 5. Displays mitigation notification & emits haptic confirmation
     */
    private fun handleDisconnectCall(
        context: Context,
        callerName: String,
        callerNumber: String,
        incidentId: String,
        riskScore: Int
    ) {
        // Haptic feedback confirmation (double heavy pulse)
        triggerHapticFeedback(context, isUrgent = true)

        // 1. Terminate Call programmatically if TelecomManager supports it
        try {
            val telecomManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelecomManager
                ?: context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                @Suppress("DEPRECATION")
                try {
                    telecomManager?.endCall()
                } catch (e: SecurityException) {
                    Log.w(TAG, "Telecom endCall() requires ANSWER_PHONE_CALLS or default dialer: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error invoking telecom endCall: ${e.message}")
        }

        // 2. Hide any floating HUD overlay
        CallThreatFloatingOverlayService.hideOverlay(context)

        // 3. Update Hub State
        val currentSession = PhoneCallMonitorHub.currentSession.value
        if (currentSession != null) {
            PhoneCallMonitorHub.updateSession(
                currentSession.copy(
                    callState = "DISCONNECTED_BY_USER",
                    isThreatDetected = true
                )
            )
        }

        // 4. Update Database
        receiverScope.launch {
            try {
                val db = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "voiceguard_db"
                ).fallbackToDestructiveMigration().build()

                if (incidentId.isNotEmpty()) {
                    db.incidentDao().getIncidentById(incidentId)?.let { incident ->
                        db.incidentDao().updateIncident(
                            incident.copy(
                                status = "BLOCKED_BY_USER",
                                isResolved = true
                            )
                        )
                    }
                }

                // Also update latest call metadata
                db.callMetadataDao().getAllCallMetadataList().firstOrNull()?.let { meta ->
                    if (meta.callerNumber == callerNumber) {
                        db.callMetadataDao().updateMetadata(
                            meta.copy(
                                status = "THREAT_BLOCKED",
                                endTime = System.currentTimeMillis()
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating DB on disconnect action: ${e.message}", e)
            }
        }

        // 5. Post Threat Mitigated Heads-up Notice
        VoiceGuardNotificationManager.showThreatMitigatedNotification(
            context = context,
            title = "🔴 Call Disconnected & Threat Mitigated",
            message = "VoiceGuard neutralized the high-risk deepfake scam from $callerName ($callerNumber). Risk score was $riskScore%.",
            callerNumber = callerNumber
        )
    }

    /**
     * Handles 'Mark Safe' action:
     * 1. Hides floating HUD overlay
     * 2. Updates Room DB status to VERIFIED_SAFE / WHITELISTED
     * 3. Signals PhoneCallMonitorHub
     * 4. Displays Safe status notification & emits gentle haptic confirmation
     */
    private fun handleMarkSafe(
        context: Context,
        callerName: String,
        callerNumber: String,
        incidentId: String
    ) {
        // Gentle confirmation pulse
        triggerHapticFeedback(context, isUrgent = false)

        // 1. Hide HUD overlay
        CallThreatFloatingOverlayService.hideOverlay(context)

        // 2. Update Hub State
        val currentSession = PhoneCallMonitorHub.currentSession.value
        if (currentSession != null) {
            PhoneCallMonitorHub.updateSession(
                currentSession.copy(
                    riskScore = 5,
                    threatType = "Verified Clean (Marked Safe)",
                    isThreatDetected = false
                )
            )
        }

        // 3. Update Database
        receiverScope.launch {
            try {
                val db = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "voiceguard_db"
                ).fallbackToDestructiveMigration().build()

                if (incidentId.isNotEmpty()) {
                    db.incidentDao().getIncidentById(incidentId)?.let { incident ->
                        db.incidentDao().updateIncident(
                            incident.copy(
                                status = "VERIFIED_SAFE",
                                isResolved = true
                            )
                        )
                    }
                }

                // Update call metadata
                db.callMetadataDao().getAllCallMetadataList().firstOrNull()?.let { meta ->
                    if (meta.callerNumber == callerNumber) {
                        db.callMetadataDao().updateMetadata(
                            meta.copy(
                                status = "VERIFIED_CLEAN",
                                threatType = "Marked Safe by User"
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating DB on mark safe action: ${e.message}", e)
            }
        }

        // 4. Post Safe Status notification
        VoiceGuardNotificationManager.showSafeStatusNotification(
            context = context,
            callerName = callerName,
            callerNumber = callerNumber
        )
    }

    private fun triggerHapticFeedback(context: Context, isUrgent: Boolean) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (isUrgent) {
                    vibrator?.vibrate(
                        VibrationEffect.createWaveform(
                            longArrayOf(0, 100, 80, 150),
                            intArrayOf(0, 255, 0, 255),
                            -1
                        )
                    )
                } else {
                    vibrator?.vibrate(
                        VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE)
                    )
                }
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(if (isUrgent) 250 else 100)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
