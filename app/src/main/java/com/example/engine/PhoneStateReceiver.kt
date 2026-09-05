package com.example.engine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class IncomingCallEvent(
    val state: String, // IDLE, RINGING, OFFHOOK
    val incomingNumber: String?,
    val timestamp: Long = System.currentTimeMillis()
)

object PhoneStateMonitor {
    private const val TAG = "PhoneStateMonitor"
    private val _lastCallEvent = MutableStateFlow<IncomingCallEvent?>(null)
    val lastCallEvent: StateFlow<IncomingCallEvent?> = _lastCallEvent.asStateFlow()

    private var isListenerRegistered = false
    private var dynamicReceiver: PhoneStateReceiver? = null

    fun updateCallState(state: String, number: String?) {
        _lastCallEvent.value = IncomingCallEvent(
            state = state,
            incomingNumber = number,
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * Registers both TelephonyCallback/PhoneStateListener and dynamic BroadcastReceiver
     * to guarantee real-time phone call detection on all Android versions (API 26 to API 35+)
     */
    fun startListening(context: Context) {
        if (isListenerRegistered) return
        val appContext = context.applicationContext
        val telephonyManager = appContext.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager ?: return

        try {
            val hasPhonePermission = ContextCompat.checkSelfPermission(
                appContext,
                android.Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPhonePermission) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    telephonyManager.registerTelephonyCallback(
                        appContext.mainExecutor,
                        object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                            override fun onCallStateChanged(state: Int) {
                                handleCallState(appContext, state, null)
                            }
                        }
                    )
                } else {
                    @Suppress("DEPRECATION")
                    telephonyManager.listen(
                        object : PhoneStateListener() {
                            @Deprecated("Deprecated in Java")
                            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                                handleCallState(appContext, state, phoneNumber)
                            }
                        },
                        @Suppress("DEPRECATION")
                        PhoneStateListener.LISTEN_CALL_STATE
                    )
                }
            } else {
                Log.d(TAG, "READ_PHONE_STATE not granted, dynamic BroadcastReceiver will capture call state events.")
            }

            // Register dynamic receiver for EXTRA_INCOMING_NUMBER
            if (dynamicReceiver == null) {
                dynamicReceiver = PhoneStateReceiver()
                val filter = IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    appContext.registerReceiver(dynamicReceiver, filter, Context.RECEIVER_EXPORTED)
                } else {
                    appContext.registerReceiver(dynamicReceiver, filter)
                }
            }

            isListenerRegistered = true
            Log.d(TAG, "Real-time Phone State Interceptor active.")
        } catch (e: SecurityException) {
            Log.w(TAG, "Permission needed for phone state interceptor: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error registering phone state monitor: ${e.message}", e)
        }
    }

    private fun handleCallState(context: Context, state: Int, incomingNumber: String?) {
        val number = incomingNumber ?: "+91 (Active Call)"
        when (state) {
            TelephonyManager.CALL_STATE_RINGING -> {
                updateCallState("RINGING", number)
            }
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                updateCallState("OFFHOOK", number)
                CallMonitorForegroundService.startCallMonitor(
                    context = context,
                    callerNumber = number,
                    callerName = "Active Call ($number)",
                    simulateThreat = false
                )
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                updateCallState("IDLE", number)
                CallMonitorForegroundService.stopCallMonitor(context)
                CallThreatFloatingOverlayService.hideOverlay(context)
            }
        }
    }
}

class PhoneStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val stateStr = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) ?: "+91 (Incoming Call)"

            when (stateStr) {
                TelephonyManager.EXTRA_STATE_RINGING -> {
                    PhoneStateMonitor.updateCallState("RINGING", incomingNumber)
                }
                TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                    PhoneStateMonitor.updateCallState("OFFHOOK", incomingNumber)
                    CallMonitorForegroundService.startCallMonitor(
                        context = context.applicationContext,
                        callerNumber = incomingNumber,
                        callerName = "Caller ($incomingNumber)",
                        simulateThreat = false
                    )
                }
                TelephonyManager.EXTRA_STATE_IDLE -> {
                    PhoneStateMonitor.updateCallState("IDLE", incomingNumber)
                    CallMonitorForegroundService.stopCallMonitor(context.applicationContext)
                    CallThreatFloatingOverlayService.hideOverlay(context.applicationContext)
                }
            }
        }
    }
}
