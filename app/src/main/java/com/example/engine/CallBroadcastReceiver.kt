package com.example.engine

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * CallBroadcastReceiver
 *
 * Dedicated BroadcastReceiver in Kotlin that registers for 'PHONE_STATE_CHANGED'
 * (android.intent.action.PHONE_STATE) to detect incoming calls in real-time,
 * trigger the initial monitoring service, and emit tactile vibration feedback patterns.
 */
class CallBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "CallBroadcastReceiver"

        // Standard Intent Action
        const val ACTION_PHONE_STATE = "android.intent.action.PHONE_STATE"
        const val ACTION_NEW_OUTGOING_CALL = "android.intent.action.NEW_OUTGOING_CALL"

        // Vibration Patterns (timings in ms)
        // 1. Incoming Call Intercept: Dual-pulse tactile cadence [delay, vibrate, pause, vibrate]
        val VIBRATION_PATTERN_INCOMING_CALL = longArrayOf(0, 180, 100, 240)
        val VIBRATION_AMPLITUDES_INCOMING_CALL = intArrayOf(0, 200, 0, 255)

        // 2. Monitoring Active: Sharp crisp confirmation pulse
        val VIBRATION_PATTERN_MONITOR_ACTIVE = longArrayOf(0, 120)
        val VIBRATION_AMPLITUDES_MONITOR_ACTIVE = intArrayOf(0, 180)

        // 3. Threat Warning: Rapid triple alert buzz
        val VIBRATION_PATTERN_THREAT_ALERT = longArrayOf(0, 300, 100, 300, 100, 500)
        val VIBRATION_AMPLITUDES_THREAT_ALERT = intArrayOf(0, 255, 0, 255, 0, 255)

        // 4. Call Terminated / Standby
        val VIBRATION_PATTERN_CALL_ENDED = longArrayOf(0, 80)

        // Track last known state to prevent redundant duplicate triggers
        @Volatile
        private var lastState: String? = null
        @Volatile
        private var lastNumber: String? = null
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        val action = intent.action
        Log.d(TAG, "onReceive invoked with action: $action")

        if (action == TelephonyManager.ACTION_PHONE_STATE_CHANGED || action == ACTION_PHONE_STATE) {
            val stateStr = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            var incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

            // Fallback for number if null
            if (incomingNumber.isNullOrBlank()) {
                incomingNumber = lastNumber ?: "Incoming Call"
            } else {
                lastNumber = incomingNumber
            }

            Log.i(TAG, "Telephony State Changed: state=$stateStr, incomingNumber=$incomingNumber")

            when (stateStr) {
                TelephonyManager.EXTRA_STATE_RINGING -> {
                    if (lastState != TelephonyManager.EXTRA_STATE_RINGING) {
                        lastState = TelephonyManager.EXTRA_STATE_RINGING
                        handleIncomingCallRinging(context.applicationContext, incomingNumber)
                    }
                }
                TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                    if (lastState != TelephonyManager.EXTRA_STATE_OFFHOOK) {
                        lastState = TelephonyManager.EXTRA_STATE_OFFHOOK
                        handleCallOffhook(context.applicationContext, incomingNumber)
                    }
                }
                TelephonyManager.EXTRA_STATE_IDLE -> {
                    if (lastState != TelephonyManager.EXTRA_STATE_IDLE) {
                        lastState = TelephonyManager.EXTRA_STATE_IDLE
                        handleCallIdle(context.applicationContext, incomingNumber)
                    }
                }
                else -> {
                    Log.d(TAG, "Unhandled state: $stateStr")
                }
            }
        }
    }

    /**
     * Triggered when an incoming call is ringing.
     * Starts the initial monitoring engine, updates global call state, and triggers tactile vibration.
     */
    private fun handleIncomingCallRinging(context: Context, incomingNumber: String) {
        Log.i(TAG, "🚨 Incoming call detected from: $incomingNumber -> Initializing real-time monitoring engine.")

        // 1. Play incoming call detection vibration pattern
        playVibrationPattern(context, VIBRATION_PATTERN_INCOMING_CALL, VIBRATION_AMPLITUDES_INCOMING_CALL)

        // 2. Update state in PhoneStateMonitor
        PhoneStateMonitor.updateCallState("RINGING", incomingNumber)

        // 3. Trigger initial call monitor foreground service
        try {
            CallMonitorForegroundService.startCallMonitor(
                context = context,
                callerNumber = incomingNumber,
                callerName = "Caller ($incomingNumber)",
                simulateThreat = false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error starting CallMonitorForegroundService: ${e.message}", e)
        }

        // 4. Ensure background phone state service is standing by
        try {
            PhoneStateMonitorForegroundService.startService(context)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting PhoneStateMonitorForegroundService: ${e.message}", e)
        }

        // 5. Initiate real-time audio stream capturing & TFLite deepfake inference scan
        try {
            AudioStreamCaptureService.start(
                context = context,
                callerName = "Caller ($incomingNumber)",
                callerNumber = incomingNumber,
                isSimulated = false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error initiating AudioStreamCaptureService scan flow: ${e.message}", e)
        }
    }

    /**
     * Triggered when call is answered (OFFHOOK).
     * Engages audio analysis and speech anomaly inspection.
     */
    private fun handleCallOffhook(context: Context, incomingNumber: String) {
        Log.i(TAG, "📞 Call answered / active (OFFHOOK): $incomingNumber -> Engaging active threat stream.")

        // 1. Play active monitoring confirmation vibration pattern
        playVibrationPattern(context, VIBRATION_PATTERN_MONITOR_ACTIVE, VIBRATION_AMPLITUDES_MONITOR_ACTIVE)

        // 2. Update state in PhoneStateMonitor
        PhoneStateMonitor.updateCallState("OFFHOOK", incomingNumber)

        // 3. Ensure monitoring service is active
        try {
            CallMonitorForegroundService.startCallMonitor(
                context = context,
                callerNumber = incomingNumber,
                callerName = "Active Call ($incomingNumber)",
                simulateThreat = false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error ensuring active CallMonitorForegroundService: ${e.message}", e)
        }

        // 4. Ensure audio capture & inference scanner is actively running
        try {
            AudioStreamCaptureService.start(
                context = context,
                callerName = "Active Call ($incomingNumber)",
                callerNumber = incomingNumber,
                isSimulated = false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error starting audio stream capture on OFFHOOK: ${e.message}", e)
        }
    }

    /**
     * Triggered when call is ended or disconnected (IDLE).
     * Cleans up monitors, removes overlay, and returns to baseline shield.
     */
    private fun handleCallIdle(context: Context, incomingNumber: String) {
        Log.i(TAG, "⏹️ Call ended / disconnected (IDLE): $incomingNumber -> Returning to standby.")

        // 1. Play call ended vibration pulse
        playVibrationPattern(context, VIBRATION_PATTERN_CALL_ENDED, null)

        // 2. Update state in PhoneStateMonitor
        PhoneStateMonitor.updateCallState("IDLE", incomingNumber)

        // 3. Stop active call monitor service
        try {
            CallMonitorForegroundService.stopCallMonitor(context)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping CallMonitorForegroundService: ${e.message}", e)
        }

        // 4. Stop audio stream capture and TFLite inference scan
        try {
            AudioStreamCaptureService.stop(context)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping AudioStreamCaptureService: ${e.message}", e)
        }

        // 5. Dismiss floating HUD overlay
        try {
            CallThreatFloatingOverlayService.hideOverlay(context)
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding floating overlay: ${e.message}", e)
        }
    }

    /**
     * Executes vibration pattern using modern VibratorManager (API 31+) or Vibrator with fallback.
     */
    private fun playVibrationPattern(
        context: Context,
        pattern: LongArray,
        amplitudes: IntArray?
    ) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (vibrator == null || !vibrator.hasVibrator()) {
                Log.d(TAG, "Device has no vibrator hardware available.")
                return
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = if (amplitudes != null && amplitudes.size == pattern.size && vibrator.hasAmplitudeControl()) {
                    VibrationEffect.createWaveform(pattern, amplitudes, -1)
                } else {
                    VibrationEffect.createWaveform(pattern, -1)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val attributes = VibrationAttributes.Builder()
                        .setUsage(VibrationAttributes.USAGE_RINGTONE)
                        .build()
                    vibrator.vibrate(effect, attributes)
                } else {
                    vibrator.vibrate(effect)
                }
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
            Log.d(TAG, "Vibration pattern triggered successfully.")
        } catch (e: SecurityException) {
            Log.w(TAG, "VIBRATE permission not granted or restricted: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error triggering vibration pattern: ${e.message}", e)
        }
    }
}
