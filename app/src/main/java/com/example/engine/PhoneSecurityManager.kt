package com.example.engine

import android.app.Activity
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.os.Build
import android.view.WindowManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DetectedRiskApp(
    val packageName: String,
    val appName: String,
    val riskType: String, // REMOTE_ACCESS_TOOL, SCREEN_RECORDER, OVERLAY_HIJACK
    val description: String,
    val isSystemApp: Boolean
)

data class PhoneSecurityAuditReport(
    val screenProtectionActive: Boolean,
    val cameraBlockerActive: Boolean,
    val micWatchdogActive: Boolean,
    val detectedRemoteAccessApps: List<DetectedRiskApp>,
    val highRiskAppsCount: Int,
    val totalAppsScanned: Int,
    val overallSecurityScore: Int,
    val lastAuditTimestamp: Long
)

class PhoneSecurityManager(private val context: Context) {

    // Known Remote Access & Screen Sharing Tools often abused in financial & impersonation scams
    private val knownRemoteAccessPackages = mapOf(
        "com.anydesk.anydeskandroid" to "AnyDesk Remote Control",
        "com.teamviewer.quicksupport.market" to "TeamViewer QuickSupport",
        "com.teamviewer.host.market" to "TeamViewer Host",
        "com.teamviewer.teamviewer.market.mobile" to "TeamViewer Remote Control",
        "com.carriez.flutter_rustdesk" to "RustDesk Remote Desktop",
        "com.splashtop.remote.pad.v2" to "Splashtop Remote",
        "com.sand.airdroid" to "AirDroid Remote Access",
        "com.vysor" to "Vysor Screen Mirroring",
        "com.microsoft.appmanager" to "Link to Windows / Phone Link",
        "com.google.android.apps.tachyon" to "Google Meet / Duo (Screen Share)",
        "us.zoom.videomeetings" to "Zoom (Screen Share)",
        "com.cisco.webex.meetings" to "Webex Meetings"
    )

    fun applyScreenProtection(activity: Activity, enable: Boolean) {
        activity.runOnUiThread {
            if (enable) {
                activity.window.setFlags(
                    WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE
                )
            } else {
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }

    suspend fun runSecurityAudit(
        screenProtected: Boolean,
        cameraBlocked: Boolean,
        micWatchdog: Boolean
    ): PhoneSecurityAuditReport = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val detected = mutableListOf<DetectedRiskApp>()

        for (app in installedApps) {
            val pkg = app.packageName
            val label = pm.getApplicationLabel(app).toString()
            val isSys = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0

            // Check against known RATs & Screen Sharing apps
            if (knownRemoteAccessPackages.containsKey(pkg)) {
                detected.add(
                    DetectedRiskApp(
                        packageName = pkg,
                        appName = label,
                        riskType = "REMOTE_ACCESS_TOOL",
                        description = "Known Screen Sharing / Remote Control tool (${knownRemoteAccessPackages[pkg]}). Scammers often use this to view OTPs and bank credentials.",
                        isSystemApp = isSys
                    )
                )
            } else if (pkg.contains("screenrecorder", ignoreCase = true) || pkg.contains("screenmirror", ignoreCase = true)) {
                detected.add(
                    DetectedRiskApp(
                        packageName = pkg,
                        appName = label,
                        riskType = "SCREEN_RECORDER",
                        description = "Third-party screen recording application detected.",
                        isSystemApp = isSys
                    )
                )
            }
        }

        // Calculate Security Score
        var baseScore = 100
        if (!screenProtected) baseScore -= 15
        if (!cameraBlocked) baseScore -= 10
        if (!micWatchdog) baseScore -= 10
        baseScore -= (detected.size * 12)
        val finalScore = baseScore.coerceIn(20, 100)

        PhoneSecurityAuditReport(
            screenProtectionActive = screenProtected,
            cameraBlockerActive = cameraBlocked,
            micWatchdogActive = micWatchdog,
            detectedRemoteAccessApps = detected,
            highRiskAppsCount = detected.size,
            totalAppsScanned = installedApps.size,
            overallSecurityScore = finalScore,
            lastAuditTimestamp = System.currentTimeMillis()
        )
    }

    fun isCameraAvailable(): Boolean {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            val cameraIds = cameraManager?.cameraIdList ?: emptyArray()
            cameraIds.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
}
