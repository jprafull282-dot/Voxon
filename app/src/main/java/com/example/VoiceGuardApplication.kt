package com.example

import android.app.Application
import android.util.Log
import com.example.engine.PhoneStateMonitor
import com.example.engine.PhoneStateMonitorForegroundService
import com.example.engine.VoiceGuardNotificationManager
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

class VoiceGuardApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.i("VoiceGuardApp", "VoiceGuardApplication initialized. Starting continuous 24/7 background telephony protection.")

        // 1. Initialize Firebase and Firebase App Check
        try {
            FirebaseApp.initializeApp(this)
            val firebaseAppCheck = FirebaseAppCheck.getInstance()
            firebaseAppCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
            Log.i("VoiceGuardApp", "Firebase App Check successfully initialized with DebugAppCheckProviderFactory.")
        } catch (e: Exception) {
            Log.w("VoiceGuardApp", "Firebase App Check initialization note: ${e.message}")
        }

        // 2. Initialize Notification channels immediately
        VoiceGuardNotificationManager.createNotificationChannels(this)

        // 3. Start phone state listener & monitoring
        PhoneStateMonitor.startListening(this)
    }
}
