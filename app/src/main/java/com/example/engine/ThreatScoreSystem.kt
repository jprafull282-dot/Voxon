package com.example.engine

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.VividOrange

/**
 * Authoritative VoiceGuard Threat Levels & Scoring Framework.
 * Strictly aligns with user-mandated ranges:
 *
 * 0% – 9%:   SAFE / LOW RISK            -> Emerald Green (#10B981)
 * 10% – 29%: SUSPICIOUS / ELEVATED      -> Amber Warning (#F59E0B)
 * 30% – 59%: HIGH RISK / DEEPFAKE WARN  -> Vivid Orange (#F97316)
 * 60% – 100%: CRITICAL THREAT / EMERG    -> Crimson Alert (#EF4444)
 */
enum class ThreatLevel(
    val minScore: Int,
    val maxScore: Int,
    val title: String,
    val shortLabel: String,
    val colorHex: String,
    val colorArgb: Int,
    val colorCompose: Color,
    val systemActionDescription: String,
    val liveCallSummary: String
) {
    SAFE(
        minScore = 0,
        maxScore = 9,
        title = "SAFE / LOW RISK",
        shortLabel = "SAFE",
        colorHex = "#10B981",
        colorArgb = 0xFF10B981.toInt(),
        colorCompose = EmeraldGreen,
        systemActionDescription = "Normal call. Status shows \"Call Secure / No Synthetic Voice Detected\".",
        liveCallSummary = "Call Secure / No Synthetic Voice Detected"
    ),
    SUSPICIOUS(
        minScore = 10,
        maxScore = 29,
        title = "SUSPICIOUS / ELEVATED",
        shortLabel = "SUSPICIOUS",
        colorHex = "#F59E0B",
        colorArgb = 0xFFF59E0B.toInt(),
        colorCompose = AmberWarning,
        systemActionDescription = "Live caution banner. Warns of unusual vocal jitter, pitch anomalies, or unverified caller patterns.",
        liveCallSummary = "Live Caution: Unusual vocal jitter, pitch anomalies, or unverified caller patterns"
    ),
    HIGH_RISK(
        minScore = 30,
        maxScore = 59,
        title = "HIGH RISK / DEEPFAKE WARNING",
        shortLabel = "HIGH RISK",
        colorHex = "#F97316",
        colorArgb = 0xFFF97316.toInt(),
        colorCompose = VividOrange,
        systemActionDescription = "Persistent heads-up warning notification + audible warning pulse. Flags potential AI voice clone or high-pressure scam.",
        liveCallSummary = "Deepfake Warning: Potential AI voice clone or high-pressure scam flagged"
    ),
    CRITICAL(
        minScore = 60,
        maxScore = 100,
        title = "CRITICAL THREAT / EMERGENCY",
        shortLabel = "CRITICAL",
        colorHex = "#EF4444",
        colorArgb = 0xFFEF4444.toInt(),
        colorCompose = CrimsonAlert,
        systemActionDescription = "Urgent full-screen/overlay alert popup, continuous vibration alarm, and an instant \"Disconnect Call\" action button.",
        liveCallSummary = "EMERGENCY: Confirmed AI Voice Clone / Deepfake Scam Detected!"
    );

    companion object {
        fun fromScore(score: Int): ThreatLevel {
            val clamped = score.coerceIn(0, 100)
            return when {
                clamped <= 9 -> SAFE
                clamped <= 29 -> SUSPICIOUS
                clamped <= 59 -> HIGH_RISK
                else -> CRITICAL
            }
        }

        fun getColor(score: Int): Color = fromScore(score).colorCompose
        fun getColorArgb(score: Int): Int = fromScore(score).colorArgb
        fun getColorHex(score: Int): String = fromScore(score).colorHex
        fun getTitle(score: Int): String = fromScore(score).title
        fun getShortLabel(score: Int): String = fromScore(score).shortLabel
        fun getLiveSummary(score: Int): String = fromScore(score).liveCallSummary
        fun getSystemAction(score: Int): String = fromScore(score).systemActionDescription
    }
}
