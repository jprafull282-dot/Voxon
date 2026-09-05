package com.example.engine

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

/**
 * Data payload representing a live real-time detection threat event.
 */
data class ThreatAlertData(
    val incidentId: String,
    val callerName: String,
    val callerNumber: String,
    val riskScore: Int,
    val threatType: String,
    val explanation: String,
    val aiProbability: Float = (riskScore / 100f),
    val spectralAnomaly: String = "HIGH",
    val phaseConsistency: String = "DISCONTINUOUS",
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * VoiceGuardNotificationManager
 *
 * Dedicated Android Notification Manager responsible for generating high-priority,
 * heads-up notifications with interactive custom actions ('Disconnect Call', 'Mark Safe', 'View Forensics').
 *
 * Triggered in real-time by the detection engines (Continuous Audio Monitor & Telephony Interceptors).
 */
object VoiceGuardNotificationManager {

    private const val TAG = "VoiceGuardNotifManager"

    // Notification Channel IDs
    const val CHANNEL_ID_THREAT_EMERGENCY = "voiceguard_threat_emergency_v2"
    const val CHANNEL_ID_PROTECTION_ACTIVE = "voiceguard_protection_shield_v2"
    const val CHANNEL_ID_LIVE_SCANNING = "voiceguard_live_call_scanning_v2"
    const val CHANNEL_ID_STATUS_UPDATES = "voiceguard_security_status_v2"

    // Standard Notification IDs
    const val NOTIF_ID_THREAT_EMERGENCY = 3001
    const val NOTIF_ID_FOREGROUND_SHIELD = 3002
    const val NOTIF_ID_SAFE_RESOLVED = 3003
    const val NOTIF_ID_LIVE_SCANNING = 3004
    const val NOTIF_ID_DEEPFAKE_BANNER = 3005
    const val NOTIF_ID_FRAUD_INTENT_BANNER = 3006

    // Request Codes for PendingIntents
    private const val REQ_CODE_DISCONNECT = 4001
    private const val REQ_CODE_MARK_SAFE = 4002
    private const val REQ_CODE_FORENSICS = 4003
    private const val REQ_CODE_FULL_SCREEN = 4004
    private const val REQ_CODE_CONTENT_CLICK = 4005
    private const val REQ_CODE_SIMULATE = 4006
    private const val REQ_CODE_DASHBOARD = 4007

    /**
     * Initializes all required Android notification channels.
     * Safe to call multiple times.
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        // 1. High-Priority Emergency Threat Channel (Heads-up banner, Alarm sound, Haptics, Red LED)
        val threatChannel = NotificationChannel(
            CHANNEL_ID_THREAT_EMERGENCY,
            "🚨 VoiceGuard Deepfake Emergency Alert",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "High-priority heads-up alerts with instant response actions when synthetic voice clones or scams are detected."
            enableLights(true)
            lightColor = Color.RED
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 450, 150, 450, 150, 800)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setBypassDnd(true)
            setShowBadge(true)

            val alarmSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()
            setSound(alarmSoundUri, audioAttributes)
        }

        // 2. 24/7 Protection Shield Channel (Low priority, non-intrusive ongoing foreground service)
        val protectionChannel = NotificationChannel(
            CHANNEL_ID_PROTECTION_ACTIVE,
            "VoiceGuard 24/7 Call Protection",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Continuous telecommunication shield monitoring calls against AI impersonation."
            setShowBadge(false)
            enableLights(false)
            enableVibration(false)
        }

        // 3. Security Status & Mitigation Channel (Default priority for verification and resolution badges)
        val statusChannel = NotificationChannel(
            CHANNEL_ID_STATUS_UPDATES,
            "VoiceGuard Security Status",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Status notices when threats are mitigated or callers are marked safe."
            enableLights(true)
            lightColor = Color.GREEN
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 150, 100, 150)
            setShowBadge(true)
        }

        // 4. Live Call Deepfake Audio Scanning Channel (Ongoing notification during active calls)
        val liveScanChannel = NotificationChannel(
            CHANNEL_ID_LIVE_SCANNING,
            "🎙️ VoiceGuard Live Deepfake Call Scanner",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Ongoing persistent indicator displaying real-time audio spectrum & deepfake anomaly analysis during active calls."
            enableLights(true)
            lightColor = Color.CYAN
            setShowBadge(true)
            enableVibration(false)
        }

        notificationManager.createNotificationChannel(threatChannel)
        notificationManager.createNotificationChannel(protectionChannel)
        notificationManager.createNotificationChannel(statusChannel)
        notificationManager.createNotificationChannel(liveScanChannel)
        Log.d(TAG, "Notification channels registered successfully.")
    }

    /**
     * Generates and fires a High-Priority, Heads-Up Notification with custom interactive actions:
     * - 'Disconnect Call' (Terminates call and mitigates deepfake risk)
     * - 'Mark Safe' (Whitelists caller and updates security baseline)
     * - 'View Forensics' (Opens deepfake forensic analysis dashboard)
     *
     * Triggered directly by the real-time audio detection engine when anomaly score exceeds critical thresholds.
     */
    fun showThreatHeadsUpNotification(
        context: Context,
        alertData: ThreatAlertData
    ) {
        createNotificationChannels(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted. Cannot post threat notification.")
                return
            }
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        // 1. Full-Screen Intent for locked devices to immediately present the Security HUD
        val popupIntent = Intent(context, CallThreatPopupActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_CALLER_NAME", alertData.callerName)
            putExtra("EXTRA_CALLER_NUMBER", alertData.callerNumber)
            putExtra("EXTRA_RISK_SCORE", alertData.riskScore)
            putExtra("EXTRA_THREAT_TYPE", alertData.threatType)
            putExtra("EXTRA_EXPLANATION", alertData.explanation)
            putExtra("EXTRA_INCIDENT_ID", alertData.incidentId)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            REQ_CODE_FULL_SCREEN,
            popupIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2. Main Content Intent (tapping notification body opens Forensics / Incident in MainActivity)
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_NAVIGATE_TO", "INCIDENT_DETAIL")
            putExtra("EXTRA_INCIDENT_ID", alertData.incidentId)
            putExtra("EXTRA_CALLER_NUMBER", alertData.callerNumber)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            REQ_CODE_CONTENT_CLICK,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. Action: 'Disconnect Call' (Broadcast Intent)
        val disconnectIntent = Intent(context, ThreatNotificationActionReceiver::class.java).apply {
            action = ThreatNotificationActionReceiver.ACTION_DISCONNECT_CALL
            putExtra(ThreatNotificationActionReceiver.EXTRA_CALLER_NAME, alertData.callerName)
            putExtra(ThreatNotificationActionReceiver.EXTRA_CALLER_NUMBER, alertData.callerNumber)
            putExtra(ThreatNotificationActionReceiver.EXTRA_INCIDENT_ID, alertData.incidentId)
            putExtra(ThreatNotificationActionReceiver.EXTRA_RISK_SCORE, alertData.riskScore)
        }
        val disconnectPendingIntent = PendingIntent.getBroadcast(
            context,
            REQ_CODE_DISCONNECT,
            disconnectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 4. Action: 'Mark Safe' (Broadcast Intent)
        val markSafeIntent = Intent(context, ThreatNotificationActionReceiver::class.java).apply {
            action = ThreatNotificationActionReceiver.ACTION_MARK_SAFE
            putExtra(ThreatNotificationActionReceiver.EXTRA_CALLER_NAME, alertData.callerName)
            putExtra(ThreatNotificationActionReceiver.EXTRA_CALLER_NUMBER, alertData.callerNumber)
            putExtra(ThreatNotificationActionReceiver.EXTRA_INCIDENT_ID, alertData.incidentId)
        }
        val markSafePendingIntent = PendingIntent.getBroadcast(
            context,
            REQ_CODE_MARK_SAFE,
            markSafeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 5. Action: 'View Forensics' (Activity Intent)
        val forensicsIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_NAVIGATE_TO", "INCIDENT_DETAIL")
            putExtra("EXTRA_INCIDENT_ID", alertData.incidentId)
        }
        val forensicsPendingIntent = PendingIntent.getActivity(
            context,
            REQ_CODE_FORENSICS,
            forensicsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Construct Rich BigTextStyle Content
        val bigText = StringBuilder().apply {
            append("⚠️ CRITICAL THREAT DETECTED (${alertData.riskScore}% Risk)\n")
            append("👤 Caller: ${alertData.callerName} (${alertData.callerNumber})\n")
            append("🧬 Forensic Anomaly: AI Prob ${(alertData.aiProbability * 100).toInt()}% • Spectral: ${alertData.spectralAnomaly} • Phase: ${alertData.phaseConsistency}\n")
            append("🚨 Threat: ${alertData.threatType}\n")
            append("🛡️ Recommended Action: ${alertData.explanation}")
        }.toString()

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID_THREAT_EMERGENCY)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setLargeIcon(createThreatBadgeBitmap(alertData.riskScore))
            .setContentTitle("🚨 VOICE CLONE DETECTED: ${alertData.callerName} (${alertData.riskScore}%)")
            .setContentText("Incoming call exhibits synthetic deepfake artifacts. Disconnect immediately!")
            .setSubText("AI Deepfake Defense")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(bigText)
                    .setBigContentTitle("🚨 VOICE CLONE DETECTED (${alertData.riskScore}% Risk)")
                    .setSummaryText("Threat Intercepted")
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(0xFFD32F2F.toInt()) // Crimson Red
            .setColorized(true)
            .setOngoing(true) // Keeps active until user acts
            .setAutoCancel(false)
            .setContentIntent(contentPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true) // Triggers heads-up banner & lock screen wakeup
            // Custom Action 1: Disconnect Call
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "DISCONNECT CALL",
                disconnectPendingIntent
            )
            // Custom Action 2: Mark Safe
            .addAction(
                android.R.drawable.ic_menu_agenda,
                "MARK SAFE",
                markSafePendingIntent
            )
            // Custom Action 3: View Forensics
            .addAction(
                android.R.drawable.ic_dialog_info,
                "VIEW FORENSICS",
                forensicsPendingIntent
            )

        val notification = notificationBuilder.build()
        notificationManager.notify(NOTIF_ID_THREAT_EMERGENCY, notification)
        Log.i(TAG, "Posted Heads-Up Threat Notification for ${alertData.callerName} (Risk: ${alertData.riskScore}%)")
    }

    /**
     * Generates a high-priority Heads-Up Banner alerting the user that potential DEEPFAKE AUDIO has been detected.
     */
    fun showDeepfakeAudioAlertBanner(
        context: Context,
        callerName: String,
        callerNumber: String,
        riskScore: Int,
        vocoderAnomaly: String = "Neural Vocoder Synthesis Artifacts",
        explanation: String = "Synthetic voice frequency patterns detected in live caller stream."
    ) {
        createNotificationChannels(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPerm) return
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        val dashboardIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_NAVIGATE_TO", "CALL_DASHBOARD")
            putExtra("EXTRA_CALLER_NAME", callerName)
            putExtra("EXTRA_CALLER_NUMBER", callerNumber)
            putExtra("EXTRA_RISK_SCORE", riskScore)
        }
        val dashboardPendingIntent = PendingIntent.getActivity(
            context,
            REQ_CODE_DASHBOARD,
            dashboardIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val disconnectIntent = Intent(context, ThreatNotificationActionReceiver::class.java).apply {
            action = ThreatNotificationActionReceiver.ACTION_DISCONNECT_CALL
            putExtra(ThreatNotificationActionReceiver.EXTRA_CALLER_NAME, callerName)
            putExtra(ThreatNotificationActionReceiver.EXTRA_CALLER_NUMBER, callerNumber)
            putExtra(ThreatNotificationActionReceiver.EXTRA_RISK_SCORE, riskScore)
        }
        val disconnectPendingIntent = PendingIntent.getBroadcast(
            context,
            REQ_CODE_DISCONNECT,
            disconnectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val markSafeIntent = Intent(context, ThreatNotificationActionReceiver::class.java).apply {
            action = ThreatNotificationActionReceiver.ACTION_MARK_SAFE
            putExtra(ThreatNotificationActionReceiver.EXTRA_CALLER_NAME, callerName)
            putExtra(ThreatNotificationActionReceiver.EXTRA_CALLER_NUMBER, callerNumber)
        }
        val markSafePendingIntent = PendingIntent.getBroadcast(
            context,
            REQ_CODE_MARK_SAFE,
            markSafeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val bigText = "🚨 POTENTIAL DEEPFAKE AUDIO DETECTED ($riskScore% Risk)\n" +
            "👤 Caller: $callerName ($callerNumber)\n" +
            "🧬 Anomaly: $vocoderAnomaly\n" +
            "⚡ Detail: $explanation\n" +
            "🛡️ Action: Disconnect immediately or open Call Dashboard to inspect live waveform."

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_THREAT_EMERGENCY)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setLargeIcon(createThreatBadgeBitmap(riskScore))
            .setContentTitle("🚨 DEEPFAKE AUDIO DETECTED: $callerName ($riskScore%)")
            .setContentText("Potential synthetic voice clone active! Vocoder anomalies detected.")
            .setSubText("AI Voice Shield")
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(0xFFD32F2F.toInt())
            .setColorized(true)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(dashboardPendingIntent)
            .setFullScreenIntent(dashboardPendingIntent, true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "DISCONNECT CALL", disconnectPendingIntent)
            .addAction(android.R.drawable.ic_dialog_dialer, "CALL DASHBOARD", dashboardPendingIntent)
            .addAction(android.R.drawable.ic_menu_agenda, "MARK SAFE", markSafePendingIntent)

        notificationManager.notify(NOTIF_ID_DEEPFAKE_BANNER, builder.build())
        Log.i(TAG, "Fired High-Priority Deepfake Audio Banner for $callerName")
    }

    /**
     * Generates a high-priority Heads-Up Banner alerting the user that SUSPICIOUS FRAUD INTENT has been detected.
     */
    fun showSuspiciousFraudIntentAlertBanner(
        context: Context,
        callerName: String,
        callerNumber: String,
        fraudScore: Int,
        detectedTactics: List<String>,
        summary: String
    ) {
        createNotificationChannels(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPerm) return
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        val dashboardIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_NAVIGATE_TO", "CALL_DASHBOARD")
            putExtra("EXTRA_CALLER_NAME", callerName)
            putExtra("EXTRA_CALLER_NUMBER", callerNumber)
            putExtra("EXTRA_RISK_SCORE", fraudScore)
        }
        val dashboardPendingIntent = PendingIntent.getActivity(
            context,
            REQ_CODE_DASHBOARD + 1,
            dashboardIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val disconnectIntent = Intent(context, ThreatNotificationActionReceiver::class.java).apply {
            action = ThreatNotificationActionReceiver.ACTION_DISCONNECT_CALL
            putExtra(ThreatNotificationActionReceiver.EXTRA_CALLER_NAME, callerName)
            putExtra(ThreatNotificationActionReceiver.EXTRA_CALLER_NUMBER, callerNumber)
            putExtra(ThreatNotificationActionReceiver.EXTRA_RISK_SCORE, fraudScore)
        }
        val disconnectPendingIntent = PendingIntent.getBroadcast(
            context,
            REQ_CODE_DISCONNECT,
            disconnectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val tacticsStr = if (detectedTactics.isNotEmpty()) detectedTactics.joinToString(", ") else "Coercive urgency & financial demand"
        val bigText = "⚠️ SUSPICIOUS FRAUD INTENT DETECTED ($fraudScore% Risk)\n" +
            "👤 Caller: $callerName ($callerNumber)\n" +
            "🎯 Tactics: $tacticsStr\n" +
            "🧠 Summary: $summary\n" +
            "🛡️ Guard: Do not transfer funds, share OTPs, or grant remote access."

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_THREAT_EMERGENCY)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setLargeIcon(createThreatBadgeBitmap(fraudScore))
            .setContentTitle("⚠️ FRAUD INTENT DETECTED: $callerName ($fraudScore%)")
            .setContentText("Suspicious scam tactics: $tacticsStr")
            .setSubText("Gemini Fraud Sentinel")
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(0xFFF57C00.toInt()) // Amber Orange
            .setColorized(true)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(dashboardPendingIntent)
            .setFullScreenIntent(dashboardPendingIntent, true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "DISCONNECT CALL", disconnectPendingIntent)
            .addAction(android.R.drawable.ic_dialog_dialer, "CALL DASHBOARD", dashboardPendingIntent)

        notificationManager.notify(NOTIF_ID_FRAUD_INTENT_BANNER, builder.build())
        Log.i(TAG, "Fired High-Priority Fraud Intent Banner for $callerName")
    }

    /**
     * Posts a heads-up resolution notification when a call is successfully disconnected
     * or a threat has been safely mitigated.
     */
    fun showThreatMitigatedNotification(
        context: Context,
        title: String,
        message: String,
        callerNumber: String? = null
    ) {
        createNotificationChannels(context)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        // Cancel active emergency alert
        notificationManager.cancel(NOTIF_ID_THREAT_EMERGENCY)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            REQ_CODE_CONTENT_CLICK,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_STATUS_UPDATES)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setColor(0xFF00C853.toInt()) // Emerald Green
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIF_ID_SAFE_RESOLVED, notification)
    }

    /**
     * Posts a notification when a caller is marked as safe / whitelisted by the user.
     */
    fun showSafeStatusNotification(
        context: Context,
        callerName: String,
        callerNumber: String
    ) {
        createNotificationChannels(context)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        // Cancel the active threat notification immediately
        notificationManager.cancel(NOTIF_ID_THREAT_EMERGENCY)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            REQ_CODE_CONTENT_CLICK,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_STATUS_UPDATES)
            .setSmallIcon(android.R.drawable.checkbox_on_background)
            .setContentTitle("🛡️ Caller Marked as Safe")
            .setContentText("$callerName ($callerNumber) has been verified. 24/7 protection standing by.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Caller $callerName ($callerNumber) is now verified and marked as safe. VoiceGuard real-time shield will continue safeguarding background phone activity.")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setColor(0xFF00E5FF.toInt()) // Electric Cyan
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIF_ID_SAFE_RESOLVED, notification)
    }

    /**
     * Builds the persistent ongoing Foreground Service notification for background call protection.
     */
    fun buildProtectionShieldNotification(
        context: Context,
        title: String,
        message: String
    ): Notification {
        createNotificationChannels(context)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID_PROTECTION_ACTIVE)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setColor(0xFF00E5FF.toInt())
            .build()
    }

    /**
     * Builds a persistent ongoing Notification displayed while a live call is being actively scanned
     * for AI synthetic voice deepfake patterns in real-time.
     */
    fun buildLiveScanningNotification(
        context: Context,
        callerName: String,
        callerNumber: String,
        riskScore: Int,
        durationSeconds: Int = 0,
        spectralAnomaly: String = "NORMAL",
        threatStatus: String = "ACTIVE REAL-TIME DEEPFAKE SCAN",
        isThreat: Boolean = false
    ): Notification {
        createNotificationChannels(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            REQ_CODE_CONTENT_CLICK,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 1: Disconnect Call Action
        val disconnectIntent = Intent(context, ThreatNotificationActionReceiver::class.java).apply {
            action = ThreatNotificationActionReceiver.ACTION_DISCONNECT_CALL
            putExtra(ThreatNotificationActionReceiver.EXTRA_CALLER_NAME, callerName)
            putExtra(ThreatNotificationActionReceiver.EXTRA_CALLER_NUMBER, callerNumber)
            putExtra(ThreatNotificationActionReceiver.EXTRA_RISK_SCORE, riskScore)
        }
        val disconnectPendingIntent = PendingIntent.getBroadcast(
            context,
            REQ_CODE_DISCONNECT,
            disconnectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 2: Mark Safe / Whitelist
        val markSafeIntent = Intent(context, ThreatNotificationActionReceiver::class.java).apply {
            action = ThreatNotificationActionReceiver.ACTION_MARK_SAFE
            putExtra(ThreatNotificationActionReceiver.EXTRA_CALLER_NAME, callerName)
            putExtra(ThreatNotificationActionReceiver.EXTRA_CALLER_NUMBER, callerNumber)
        }
        val markSafePendingIntent = PendingIntent.getBroadcast(
            context,
            REQ_CODE_MARK_SAFE,
            markSafeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val accentColor = ThreatLevel.getColorArgb(riskScore)

        val minutes = durationSeconds / 60
        val seconds = durationSeconds % 60
        val timeFormatted = String.format("%02d:%02d", minutes, seconds)

        val title = when {
            riskScore >= 60 -> "🚨 CRITICAL THREAT: EMERGENCY ($riskScore%)"
            riskScore >= 30 -> "⚠️ HIGH RISK / DEEPFAKE WARNING ($riskScore%)"
            riskScore >= 10 -> "⚡ SUSPICIOUS / ELEVATED ($riskScore%)"
            else -> "🛡️ CALL SECURE / LOW RISK ($riskScore%)"
        }

        val shortText = "$callerName ($callerNumber) • TFLite & Gemini DSP"
        val expandedText = "• Caller: $callerName ($callerNumber)\n" +
                "• Deepfake Risk: $riskScore% | Status: $threatStatus\n" +
                "• Spectral Anomaly: $spectralAnomaly\n" +
                "• AI Engine: TensorFlow Lite Neural Vocoder + Gemini 3.5 Flash"

        return NotificationCompat.Builder(context, CHANNEL_ID_LIVE_SCANNING)
            .setSmallIcon(if (isThreat) android.R.drawable.stat_notify_error else android.R.drawable.ic_lock_idle_lock)
            .setContentTitle(title)
            .setContentText(shortText)
            .setSubText("AI VoiceGuard Live")
            .setStyle(NotificationCompat.BigTextStyle().bigText(expandedText))
            .setPriority(if (isThreat) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_LOW)
            .setColor(accentColor)
            .setOngoing(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(android.R.drawable.ic_menu_view, "Forensics", openAppPendingIntent)
            .addAction(android.R.drawable.ic_delete, "Disconnect", disconnectPendingIntent)
            .addAction(android.R.drawable.checkbox_on_background, "Mark Safe", markSafePendingIntent)
            .build()
    }

    /**
     * Shows or updates the ongoing live scanning notification.
     */
    fun updateLiveScanningNotification(
        context: Context,
        callerName: String,
        callerNumber: String,
        riskScore: Int,
        durationSeconds: Int,
        spectralAnomaly: String = "NORMAL",
        threatStatus: String = "ACTIVE REAL-TIME DEEPFAKE SCAN",
        isThreat: Boolean = false
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return
        val notif = buildLiveScanningNotification(
            context = context,
            callerName = callerName,
            callerNumber = callerNumber,
            riskScore = riskScore,
            durationSeconds = durationSeconds,
            spectralAnomaly = spectralAnomaly,
            threatStatus = threatStatus,
            isThreat = isThreat
        )
        notificationManager.notify(NOTIF_ID_LIVE_SCANNING, notif)
    }

    /**
     * Dismisses the ongoing live scanning notification.
     */
    fun dismissLiveScanningNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(NOTIF_ID_LIVE_SCANNING)
    }

    /**
     * Dismisses active threat alerts.
     */
    fun dismissThreatAlert(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(NOTIF_ID_THREAT_EMERGENCY)
    }

    /**
     * Cancels all notifications created by VoiceGuard.
     */
    fun cancelAll(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancelAll()
    }

    /**
     * Helper to create a circular threat badge bitmap for large notification icon.
     */
    private fun createThreatBadgeBitmap(riskScore: Int): Bitmap {
        val size = 128
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ThreatLevel.getColorArgb(riskScore)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, bgPaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 42f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }

        val yPos = (canvas.height / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText("$riskScore%", size / 2f, yPos, textPaint)

        return bitmap
    }
}
