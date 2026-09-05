package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Report-Centric Security Entity.
 * Replaces persistent raw audio storage in the Vault with structured forensic telemetry.
 * Captures the security verdict, AI voice probability, conversation risk,
 * detected threat indicators, latency, and cryptographic hash verification.
 */
@Entity(tableName = "security_reports")
data class SecurityReportEntity(
    @PrimaryKey
    val id: String,                         // e.g. "REP-2026-XXXX"
    val callSessionId: String,              // e.g. "sess_1725540000"
    val callerName: String,                 // e.g. "Unknown / Suspect"
    val callerNumber: String,               // e.g. "+91 98765 43210"
    val timestamp: Long,                    // Unix epoch milliseconds
    val durationSeconds: Int,               // Call duration in seconds
    val voiceVerdict: String,               // SAFE, SUSPICIOUS, SYNTHETIC, INCONCLUSIVE
    val aiVoiceConfidence: Float,           // 0.0f to 1.0f from Aurigin/authoritative detector
    val conversationRiskScore: Int,         // 0 to 100 from conversation scam analyzer
    val overallRiskScore: Int,              // 0 to 100 aggregated risk score
    val threatLevel: String,                // SAFE, MONITORING, ELEVATED, HIGH, CRITICAL
    val detectedIndicators: String,         // Comma-separated list of detected tactics/artifacts
    val evidenceSummary: String,            // Descriptive technical findings
    val recommendations: String,            // Actionable advice for the user
    val detectorName: String,               // e.g. "Aurigin.ai Voice Authenticity"
    val analysisStatus: String,             // COMPLETED, INCONCLUSIVE, PARTIAL
    val latencyMs: Long,                    // Processing latency in milliseconds
    val evidenceHashSha256: String          // SHA-256 cryptographic hash of report data
)
