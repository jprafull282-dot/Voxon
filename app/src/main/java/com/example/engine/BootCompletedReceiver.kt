package com.example.engine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * BootCompletedReceiver
 *
 * Restarts the 24/7 background telephony protection and deepfake monitoring engine
 * immediately whenever the phone boots up or updates, ensuring VoiceGuard is active
 * even if the user never manually opens the app.
 */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        Log.i("BootCompletedReceiver", "Boot broadcast received: $action")
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            try {
                VoiceGuardNotificationManager.createNotificationChannels(context.applicationContext)
                PhoneStateMonitor.startListening(context.applicationContext)
                PhoneStateMonitorForegroundService.startService(context.applicationContext)
                Log.i("BootCompletedReceiver", "VoiceGuard background shield successfully engaged after device restart.")
            } catch (e: Exception) {
                Log.e("BootCompletedReceiver", "Failed to launch background service after boot: ${e.message}", e)
            }
        }
    }
}
