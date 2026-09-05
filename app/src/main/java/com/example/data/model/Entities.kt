package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "incidents")
data class IncidentEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val callerNumber: String,
    val callerLabel: String,
    val threatType: String,
    val riskScore: Int,
    val severity: String, // CRITICAL, SUSPICIOUS, TRUSTED
    val aiProbability: Float,
    val spectralAnomaly: String, // HIGH, MEDIUM, LOW
    val phaseConsistency: String, // HIGH, MEDIUM, LOW
    val prosodyNaturalness: String, // HIGH, MEDIUM, LOW
    val speakerConfidence: Float,
    val language: String,
    val attackStory: String,
    val attackChain: String, // JSON array string or pipe separated
    val evidenceHash: String,
    val status: String, // BLOCKED, CHALLENGED, FLAGGED, VERIFIED
    val isResolved: Boolean = false
)

@Entity(tableName = "policies")
data class PolicyEntity(
    @PrimaryKey val id: String,
    val name: String,
    val condition: String,
    val action: String,
    val enabled: Boolean = true,
    val triggerCount: Int = 0
)

@Entity(tableName = "campaigns")
data class CampaignEntity(
    @PrimaryKey val id: String,
    val name: String,
    val affectedUsers: Int,
    val regions: String,
    val riskLevel: String,
    val indicators: String,
    val status: String
)

@Entity(tableName = "call_recordings")
data class CallRecordingEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val callerNumber: String,
    val callerLabel: String,
    val durationSeconds: Int,
    val filePath: String,
    val fileSizeBytes: Long,
    val riskScore: Int,
    val threatType: String,
    val aiProbability: Float,
    val spectralAnomaly: String,
    val waveformPointsCsv: String, // comma-separated floats
    val transcriptSummary: String,
    val evidenceHash: String,
    val isDeepfake: Boolean
)

@Entity(tableName = "call_metadata")
data class CallMetadataEntity(
    @PrimaryKey val callId: String,
    val timestamp: Long,
    val callerNumber: String,
    val callerLabel: String,
    val callState: String, // RINGING, OFFHOOK, COMPLETED, MISSED
    val direction: String, // INCOMING, OUTGOING
    val startTime: Long,
    val answerTime: Long?,
    val endTime: Long?,
    val durationSeconds: Int,
    val riskScore: Int,
    val threatType: String,
    val aiProbability: Float,
    val spectralAnomaly: String,
    val phaseConsistency: String,
    val samplePointsRecorded: Int,
    val status: String // PROTECTED, THREAT_BLOCKED, SCAM_FLAGGED, VERIFIED_CLEAN
)

/**
 * AnalyzedCallEntity
 *
 * Local Room Database entity for persisting the comprehensive history of analyzed calls,
 * including timestamps, caller phone numbers, duration, security risk levels assigned
 * by AI models (TensorFlow Lite acoustic forensics & Gemini 3.5 Flash intent analysis),
 * and cryptographic proof evidence.
 */
@Entity(tableName = "analyzed_calls")
data class AnalyzedCallEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val phoneNumber: String,
    val callerLabel: String,
    val durationSeconds: Int = 0,
    val securityRiskLevel: String, // CRITICAL, HIGH, SUSPICIOUS, MODERATE, LOW, VERIFIED_SAFE
    val securityScore: Int,        // 0 to 100 overall security score
    val riskScore: Int,            // 0 to 100 threat/fraud score
    val aiModelNames: String = "TFLite Spectral + Gemini 3.5 Flash",
    val tfliteAiProbability: Float = 0f,
    val tfliteSpectralAnomaly: String = "LOW",
    val tfliteVocoderSignature: String = "Clean Natural",
    val geminiFraudRiskScore: Int = 0,
    val geminiIntentCategory: String = "UNKNOWN",
    val geminiSecurityVerdict: String = "SAFE",
    val transcriptSnippet: String = "",
    val aiVerdictSummary: String = "",
    val threatType: String = "Authentic Voice",
    val status: String = "PROTECTED", // BLOCKED, CHALLENGED, FLAGGED, VERIFIED_SAFE
    val evidenceHash: String = "",
    val isDeepfake: Boolean = false,
    val voiceAuthenticity: String = "Likely Human",
    val detectedPatterns: String = "Natural vocal harmonics verified",
    val securityRecommendations: String = "No action required."
)

/**
 * CallLogEntity
 *
 * Dedicated Room database schema for storing phone call logs, including timestamps,
 * caller identifiers, call duration, final risk score, threat tier classifications,
 * detected language, acoustic forensics, and deepfake verification state.
 */
@Entity(tableName = "call_logs")
data class CallLogEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val callerIdentifier: String,      // Phone number, SIP URI, or caller identifier
    val callerName: String,            // Contact display name or caller label
    val callDurationSeconds: Int = 0,  // Total call duration in seconds
    val finalRiskScore: Int,           // 0 to 100 final computed deepfake risk score
    val threatLevel: String,           // SAFE (0-9%), SUSPICIOUS (10-29%), HIGH_RISK (30-59%), CRITICAL (60-100%)
    val threatType: String = "Authentic Voice",
    val languageDetected: String = "Multilingual (Auto)",
    val aiVoiceProbability: Float = 0.02f,
    val spectralAnomalyLevel: String = "LOW",
    val transcriptSnippet: String = "",
    val status: String = "PROTECTED",  // PROTECTED, FLAGGED, BLOCKED, VERIFIED_CLEAN
    val audioRecordingPath: String? = null,
    val isDeepfake: Boolean = false
)

/**
 * BlockedCallerEntity
 *
 * Local Room Database entity for storing numbers terminated and added to the blocklist
 * by the user from the Call Dashboard or threat engine.
 */
@Entity(tableName = "blocked_callers")
data class BlockedCallerEntity(
    @PrimaryKey val phoneNumber: String,
    val callerName: String = "Unknown Caller",
    val blockedAt: Long = System.currentTimeMillis(),
    val reason: String = "Blocked from Call Dashboard (Deepfake / Fraud Threat)",
    val riskScore: Int = 85,
    val threatCategory: String = "Fraud / Acoustic Impersonation"
)


