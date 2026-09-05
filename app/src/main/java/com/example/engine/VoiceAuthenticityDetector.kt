package com.example.engine

import kotlinx.coroutines.flow.Flow

/**
 * Top-level voice detection result representing an authenticity determination
 * from an authoritative deepfake/voice-cloning detector.
 */
data class DetectionResult(
    val isSynthetic: Boolean,
    val confidence: Float,
    val riskScore: Int,
    val verdict: String,
    val latencyMs: Long = 0L,
    val characteristics: List<String> = emptyList(),
    val isTechnicalError: Boolean = false,
    val errorMessage: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Supporting acoustic indicators derived from audio signal analysis.
 * Kept strictly separate from the authoritative deepfake classifier probability.
 */
data class AcousticDetails(
    val pitchHz: Float = 0f,
    val spectralCentroid: Float = 0f,
    val jitterPercent: Float = 0f,
    val shimmerPercent: Float = 0f,
    val harmonicToNoiseRatio: Float = 0f
)

/**
 * Reusable detector abstraction for streaming voice authenticity verification.
 * Any underlying detector (Aurigin, on-device neural model, cloud service) can
 * implement this interface without requiring changes to the risk engine.
 */
interface VoiceAuthenticityDetector {
    val detectorName: String
    val detectionResults: Flow<DetectionResult>

    suspend fun connect(sessionId: String): Boolean
    suspend fun sendAudioChunk(pcmChunk: ByteArray): Boolean
    suspend fun disconnect()
    fun isConnected(): Boolean
}
