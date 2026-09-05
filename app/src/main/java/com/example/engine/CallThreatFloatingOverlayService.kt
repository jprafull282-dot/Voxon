package com.example.engine

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import com.example.MainActivity
import com.example.R

/**
 * Floating System Alert Window that overlays on top of the native phone dialer / incoming call screen
 * when a high-risk AI voice clone or scam is detected.
 */
class CallThreatFloatingOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null

    companion object {
        const val ACTION_SHOW_OVERLAY = "com.example.action.SHOW_OVERLAY"
        const val ACTION_SHOW_SCANNING_HUD = "com.example.action.SHOW_SCANNING_HUD"
        const val ACTION_UPDATE_SCANNING_DATA = "com.example.action.UPDATE_SCANNING_DATA"
        const val ACTION_HIDE_OVERLAY = "com.example.action.HIDE_OVERLAY"

        fun showScanningHud(
            context: Context,
            callerName: String,
            callerNumber: String,
            riskScore: Int = 12,
            spectralAnomaly: String = "NORMAL",
            vocoderRatio: Float = 0.05f
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                return
            }
            val intent = Intent(context, CallThreatFloatingOverlayService::class.java).apply {
                action = ACTION_SHOW_SCANNING_HUD
                putExtra("EXTRA_CALLER_NAME", callerName)
                putExtra("EXTRA_CALLER_NUMBER", callerNumber)
                putExtra("EXTRA_RISK_SCORE", riskScore)
                putExtra("EXTRA_SPECTRAL_ANOMALY", spectralAnomaly)
                putExtra("EXTRA_VOCODER_RATIO", vocoderRatio)
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun updateScanningData(
            context: Context,
            riskScore: Int,
            spectralAnomaly: String,
            vocoderRatio: Float
        ) {
            val intent = Intent(context, CallThreatFloatingOverlayService::class.java).apply {
                action = ACTION_UPDATE_SCANNING_DATA
                putExtra("EXTRA_RISK_SCORE", riskScore)
                putExtra("EXTRA_SPECTRAL_ANOMALY", spectralAnomaly)
                putExtra("EXTRA_VOCODER_RATIO", vocoderRatio)
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun showOverlay(
            context: Context,
            callerName: String,
            callerNumber: String,
            riskScore: Int,
            threatType: String,
            explanation: String
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                return
            }
            val intent = Intent(context, CallThreatFloatingOverlayService::class.java).apply {
                action = ACTION_SHOW_OVERLAY
                putExtra("EXTRA_CALLER_NAME", callerName)
                putExtra("EXTRA_CALLER_NUMBER", callerNumber)
                putExtra("EXTRA_RISK_SCORE", riskScore)
                putExtra("EXTRA_THREAT_TYPE", threatType)
                putExtra("EXTRA_EXPLANATION", explanation)
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun hideOverlay(context: Context) {
            val intent = Intent(context, CallThreatFloatingOverlayService::class.java).apply {
                action = ACTION_HIDE_OVERLAY
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    }

    private var currentTitleView: TextView? = null
    private var currentSubtitleView: TextView? = null
    private var currentMetricsView: TextView? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW_SCANNING_HUD -> {
                val callerName = intent.getStringExtra("EXTRA_CALLER_NAME") ?: "Caller"
                val callerNumber = intent.getStringExtra("EXTRA_CALLER_NUMBER") ?: "+91 (Active Call)"
                val riskScore = intent.getIntExtra("EXTRA_RISK_SCORE", 12)
                val spectral = intent.getStringExtra("EXTRA_SPECTRAL_ANOMALY") ?: "NORMAL"
                val vocoder = intent.getFloatExtra("EXTRA_VOCODER_RATIO", 0.05f)
                displayScanningHud(callerName, callerNumber, riskScore, spectral, vocoder)
            }
            ACTION_UPDATE_SCANNING_DATA -> {
                val riskScore = intent.getIntExtra("EXTRA_RISK_SCORE", 12)
                val spectral = intent.getStringExtra("EXTRA_SPECTRAL_ANOMALY") ?: "NORMAL"
                val vocoder = intent.getFloatExtra("EXTRA_VOCODER_RATIO", 0.05f)
                updateScanningHudView(riskScore, spectral, vocoder)
            }
            ACTION_SHOW_OVERLAY -> {
                val callerName = intent.getStringExtra("EXTRA_CALLER_NAME") ?: "Caller"
                val callerNumber = intent.getStringExtra("EXTRA_CALLER_NUMBER") ?: "+91 00000 00000"
                val riskScore = intent.getIntExtra("EXTRA_RISK_SCORE", 95)
                val threatType = intent.getStringExtra("EXTRA_THREAT_TYPE") ?: "AI Voice Deepfake"
                val explanation = intent.getStringExtra("EXTRA_EXPLANATION") ?: "Synthetic micro-vocoder phase artifacts detected."
                displayEmergencyOverlay(callerName, callerNumber, riskScore, threatType, explanation)
            }
            ACTION_HIDE_OVERLAY -> {
                removeOverlay()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    @SuppressLint("SetTextI18n")
    private fun displayScanningHud(
        callerName: String,
        callerNumber: String,
        riskScore: Int,
        spectralAnomaly: String,
        vocoderRatio: Float
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            return
        }

        removeOverlay()

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = 0
            y = 50
        }

        val root = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(32, 28, 32, 28)
            elevation = 24f

            val borderDrawable = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.argb(248, 12, 16, 24))
                setStroke(3, android.graphics.Color.parseColor("#00E5FF"))
                cornerRadius = 24f
            }
            background = borderDrawable
        }

        val titleView = TextView(this).apply {
            text = "🎙️ VOICEGUARD AI SCANNER • ACTIVE CALL"
            setTextColor(android.graphics.Color.parseColor("#00E5FF"))
            textSize = 14f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER_HORIZONTAL
        }
        currentTitleView = titleView
        root.addView(titleView)

        val callerInfoView = TextView(this).apply {
            text = "● Live Audio Stream: $callerName ($callerNumber)"
            setTextColor(android.graphics.Color.WHITE)
            textSize = 12f
            setPadding(0, 10, 0, 4)
            gravity = Gravity.CENTER_HORIZONTAL
        }
        currentSubtitleView = callerInfoView
        root.addView(callerInfoView)

        val metricsView = TextView(this).apply {
            text = "Acoustic Risk: $riskScore% (Normal) • Phase: $spectralAnomaly • Vocoder: ${(vocoderRatio * 100).toInt()}% • TFLite DSP"
            setTextColor(android.graphics.Color.parseColor("#00E676"))
            textSize = 11f
            typeface = android.graphics.Typeface.MONOSPACE
            setPadding(0, 4, 0, 16)
            gravity = Gravity.CENTER_HORIZONTAL
        }
        currentMetricsView = metricsView
        root.addView(metricsView)

        val btnRow = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            weightSum = 3f
        }

        val simulateBtn = Button(this).apply {
            text = "SIMULATE"
            setBackgroundColor(android.graphics.Color.parseColor("#FF9100"))
            setTextColor(android.graphics.Color.BLACK)
            textSize = 10f
            layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(0, 0, 4, 0)
            }
            setOnClickListener {
                CallMonitorForegroundService.startCallMonitor(
                    context = applicationContext,
                    callerNumber = callerNumber,
                    callerName = callerName,
                    simulateThreat = true
                )
            }
        }
        btnRow.addView(simulateBtn)

        val forensicsBtn = Button(this).apply {
            text = "FORENSICS"
            setBackgroundColor(android.graphics.Color.parseColor("#00E5FF"))
            setTextColor(android.graphics.Color.BLACK)
            textSize = 10f
            layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(4, 0, 4, 0)
            }
            setOnClickListener {
                val appIntent = Intent(applicationContext, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                startActivity(appIntent)
            }
        }
        btnRow.addView(forensicsBtn)

        val minimizeBtn = Button(this).apply {
            text = "MINIMIZE"
            setBackgroundColor(android.graphics.Color.parseColor("#263238"))
            setTextColor(android.graphics.Color.WHITE)
            textSize = 10f
            layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(4, 0, 0, 0)
            }
            setOnClickListener {
                removeOverlay()
            }
        }
        btnRow.addView(minimizeBtn)

        root.addView(btnRow)

        overlayView = root
        try {
            windowManager?.addView(overlayView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateScanningHudView(riskScore: Int, spectralAnomaly: String, vocoderRatio: Float) {
        val colorHex = ThreatLevel.getColorHex(riskScore)
        val statusLabel = ThreatLevel.getTitle(riskScore)
        currentMetricsView?.apply {
            text = "Risk Level: $riskScore% ($statusLabel)\nPhase: $spectralAnomaly • Vocoder: ${(vocoderRatio * 100).toInt()}% • Real-Time Multilingual Active"
            setTextColor(android.graphics.Color.parseColor(colorHex))
        }
    }

    @SuppressLint("InflateParams", "SetTextI18n")
    private fun displayEmergencyOverlay(
        callerName: String,
        callerNumber: String,
        riskScore: Int,
        threatType: String,
        explanation: String
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            return
        }

        removeOverlay()

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = 0
            y = 80
        }

        // Dynamically create the HUD view
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setBackgroundColor(android.graphics.Color.argb(240, 20, 10, 15))
            setPadding(36, 36, 36, 36)
            elevation = 20f

            // Red border outline
            val borderDrawable = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.argb(245, 18, 20, 28))
                setStroke(4, ThreatLevel.getColorArgb(riskScore))
                cornerRadius = 28f
            }
            background = borderDrawable
        }

        val titleView = TextView(this).apply {
            text = "🚨 VOICEGUARD: ${ThreatLevel.getTitle(riskScore)} ($riskScore%)"
            setTextColor(ThreatLevel.getColorArgb(riskScore))
            textSize = 15f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER_HORIZONTAL
        }
        layout.addView(titleView)

        val callerInfoView = TextView(this).apply {
            text = "⚠️ Incoming: $callerName ($callerNumber)\nThreat: $threatType\nAction: ${ThreatLevel.getSystemAction(riskScore)}"
            setTextColor(android.graphics.Color.WHITE)
            textSize = 12.5f
            setPadding(0, 16, 0, 20)
        }
        layout.addView(callerInfoView)

        val btnRow = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            weightSum = 3f
        }

        val hangUpBtn = Button(this).apply {
            text = "DISCONNECT"
            setBackgroundColor(ThreatLevel.getColorArgb(riskScore))
            setTextColor(android.graphics.Color.WHITE)
            textSize = 10f
            layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(0, 0, 6, 0)
            }
            setOnClickListener {
                val disconnectIntent = Intent(applicationContext, ThreatNotificationActionReceiver::class.java).apply {
                    action = ThreatNotificationActionReceiver.ACTION_DISCONNECT_CALL
                    putExtra(ThreatNotificationActionReceiver.EXTRA_CALLER_NAME, callerName)
                    putExtra(ThreatNotificationActionReceiver.EXTRA_CALLER_NUMBER, callerNumber)
                    putExtra(ThreatNotificationActionReceiver.EXTRA_RISK_SCORE, riskScore)
                }
                sendBroadcast(disconnectIntent)
                removeOverlay()
                stopSelf()
            }
        }
        btnRow.addView(hangUpBtn)

        val markSafeBtn = Button(this).apply {
            text = "MARK SAFE"
            setBackgroundColor(android.graphics.Color.parseColor("#00E676"))
            setTextColor(android.graphics.Color.BLACK)
            textSize = 10f
            layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(6, 0, 6, 0)
            }
            setOnClickListener {
                val markSafeIntent = Intent(applicationContext, ThreatNotificationActionReceiver::class.java).apply {
                    action = ThreatNotificationActionReceiver.ACTION_MARK_SAFE
                    putExtra(ThreatNotificationActionReceiver.EXTRA_CALLER_NAME, callerName)
                    putExtra(ThreatNotificationActionReceiver.EXTRA_CALLER_NUMBER, callerNumber)
                }
                sendBroadcast(markSafeIntent)
                removeOverlay()
                stopSelf()
            }
        }
        btnRow.addView(markSafeBtn)

        val openAppBtn = Button(this).apply {
            text = "FORENSICS"
            setBackgroundColor(android.graphics.Color.parseColor("#00F0FF"))
            setTextColor(android.graphics.Color.BLACK)
            textSize = 10f
            layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(6, 0, 0, 0)
            }
            setOnClickListener {
                val appIntent = Intent(applicationContext, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                startActivity(appIntent)
                removeOverlay()
                stopSelf()
            }
        }
        btnRow.addView(openAppBtn)

        layout.addView(btnRow)

        overlayView = layout
        try {
            windowManager?.addView(overlayView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun removeOverlay() {
        try {
            overlayView?.let {
                windowManager?.removeView(it)
                overlayView = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removeOverlay()
    }
}
