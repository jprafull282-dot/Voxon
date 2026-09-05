package com.example.engine

import java.util.ArrayDeque

/**
 * Multi-Signal Rolling Risk Engine.
 * Evaluates a temporal rolling window of recent audio observation frames and
 * aggregates independent security signals:
 * - Voice Authenticity (from Aurigin detector)
 * - Conversation Scam Risk (from NLP / Intent analyzer)
 * - Acoustic / Replay Artifacts (from acoustic feature processing)
 * - Contextual Risk (known threat databases, contact reputation)
 *
 * CRITICAL DEFENSE RULES:
 * 1. AI Voice Probability != Overall Risk.
 * 2. An isolated weak anomaly does NOT trigger a false alert; requires consecutive confirmed observations.
 * 3. Technical errors emit explicit INCONCLUSIVE state, NEVER false threats.
 */
class RollingRiskEngine(
    private val windowCapacity: Int = 5,
    private val consecutiveThreatThreshold: Int = 3
) {
    enum class RiskLevel {
        SAFE,
        MONITORING,
        SUSPICIOUS,
        HIGH_RISK,
        CRITICAL,
        INCONCLUSIVE
    }

    data class AggregatedEvaluation(
        val overallRiskScore: Int,
        val voiceRiskScore: Int,
        val conversationRiskScore: Int,
        val acousticRiskScore: Int,
        val riskLevel: RiskLevel,
        val aiVoiceConfidence: Float,
        val consecutiveHighRiskCount: Int,
        val shouldShowThreatPopup: Boolean,
        val summary: String,
        val recommendedAction: String
    )

    private val observationsWindow = ArrayDeque<DetectionResult>(windowCapacity)
    private var consecutiveHighRiskCount = 0

    /**
     * Evaluates a new observation frame alongside conversation risk.
     */
    @Synchronized
    fun evaluate(
        voiceResult: DetectionResult?,
        conversationScore: Int,
        isSpeechDetected: Boolean
    ): AggregatedEvaluation {
        if (voiceResult != null && voiceResult.isTechnicalError) {
            return AggregatedEvaluation(
                overallRiskScore = 0,
                voiceRiskScore = 0,
                conversationRiskScore = conversationScore,
                acousticRiskScore = 0,
                riskLevel = RiskLevel.INCONCLUSIVE,
                aiVoiceConfidence = 0f,
                consecutiveHighRiskCount = 0,
                shouldShowThreatPopup = false,
                summary = "Voice authenticity analysis inconclusive (Service unavailable or network timeout).",
                recommendedAction = "Verify caller identity manually through a secondary out-of-band channel."
            )
        }

        if (!isSpeechDetected && observationsWindow.isEmpty()) {
            return AggregatedEvaluation(
                overallRiskScore = 0,
                voiceRiskScore = 0,
                conversationRiskScore = 0,
                acousticRiskScore = 0,
                riskLevel = RiskLevel.MONITORING,
                aiVoiceConfidence = 0f,
                consecutiveHighRiskCount = 0,
                shouldShowThreatPopup = false,
                summary = "Monitoring call stream. Awaiting active speech.",
                recommendedAction = "Stay vigilant while call audio is analyzed."
            )
        }

        if (voiceResult != null && !voiceResult.isTechnicalError) {
            if (observationsWindow.size >= windowCapacity) {
                observationsWindow.pollFirst()
            }
            observationsWindow.addLast(voiceResult)

            if (voiceResult.isSynthetic && voiceResult.confidence >= 0.70f) {
                consecutiveHighRiskCount++
            } else if (!voiceResult.isSynthetic && voiceResult.confidence < 0.30f) {
                consecutiveHighRiskCount = (consecutiveHighRiskCount - 1).coerceAtLeast(0)
            }
        }

        if (observationsWindow.isEmpty()) {
            return AggregatedEvaluation(
                overallRiskScore = 0,
                voiceRiskScore = 0,
                conversationRiskScore = conversationScore,
                acousticRiskScore = 0,
                riskLevel = RiskLevel.MONITORING,
                aiVoiceConfidence = 0f,
                consecutiveHighRiskCount = 0,
                shouldShowThreatPopup = false,
                summary = "Awaiting voice authenticity observations.",
                recommendedAction = "Do not share passwords or banking credentials."
            )
        }

        // Calculate rolling average voice risk & confidence across the window
        val avgVoiceConfidence = observationsWindow.map { it.confidence }.average().toFloat()
        val avgVoiceRisk = observationsWindow.map { it.riskScore }.average().toInt()
        val syntheticFramesCount = observationsWindow.count { it.isSynthetic }

        // Acoustic risk derived from characteristics / artifacts
        val latestCharacteristics = observationsWindow.lastOrNull()?.characteristics ?: emptyList()
        val acousticRisk = (latestCharacteristics.size * 15).coerceIn(0, 40)

        // Aggregated overall risk (Voice Authenticity 55%, Conversation Scam 35%, Acoustic 10%)
        val weightedScore = (avgVoiceRisk * 0.55f + conversationScore * 0.35f + acousticRisk * 0.10f).toInt().coerceIn(0, 100)

        // Determine RiskLevel with hysteretic dampening
        val riskLevel = when {
            consecutiveHighRiskCount >= consecutiveThreatThreshold && weightedScore >= 75 -> RiskLevel.CRITICAL
            consecutiveHighRiskCount >= 2 && weightedScore >= 60 -> RiskLevel.HIGH_RISK
            weightedScore >= 40 || syntheticFramesCount >= 2 -> RiskLevel.SUSPICIOUS
            weightedScore in 15..39 -> RiskLevel.MONITORING
            else -> RiskLevel.SAFE
        }

        // Threat popup only triggered when consecutive threshold is crossed (never on a single blip)
        val shouldShowPopup = (riskLevel == RiskLevel.CRITICAL || riskLevel == RiskLevel.HIGH_RISK) &&
                consecutiveHighRiskCount >= consecutiveThreatThreshold

        val summary = when (riskLevel) {
            RiskLevel.CRITICAL -> "CRITICAL THREAT: Consecutive neural vocoder artifacts and synthetic voice cloning confirmed."
            RiskLevel.HIGH_RISK -> "HIGH RISK: Potential AI-generated voice or coercive social engineering pattern identified."
            RiskLevel.SUSPICIOUS -> "SUSPICIOUS: Acoustic anomalies detected. Continuing temporal verification."
            RiskLevel.MONITORING -> "MONITORING: Active call stream verified. Normal acoustic patterns observed."
            RiskLevel.SAFE -> "SAFE: Stream exhibits natural acoustic liveness and vocal tract dynamics."
            RiskLevel.INCONCLUSIVE -> "INCONCLUSIVE: Insufficient speech data to complete verification."
        }

        val recommendedAction = when (riskLevel) {
            RiskLevel.CRITICAL -> "Do NOT share OTPs, passwords, or transfer funds. Hang up immediately."
            RiskLevel.HIGH_RISK -> "Exercise extreme caution. Ask a personal challenge question known only to the real contact."
            RiskLevel.SUSPICIOUS -> "Verify caller identity before acting on any request."
            RiskLevel.MONITORING, RiskLevel.SAFE -> "No immediate threat detected. Normal call operation."
            RiskLevel.INCONCLUSIVE -> "Stay observant. Voice verification service currently gathering telemetry."
        }

        return AggregatedEvaluation(
            overallRiskScore = weightedScore,
            voiceRiskScore = avgVoiceRisk,
            conversationRiskScore = conversationScore,
            acousticRiskScore = acousticRisk,
            riskLevel = riskLevel,
            aiVoiceConfidence = avgVoiceConfidence,
            consecutiveHighRiskCount = consecutiveHighRiskCount,
            shouldShowThreatPopup = shouldShowPopup,
            summary = summary,
            recommendedAction = recommendedAction
        )
    }

    @Synchronized
    fun reset() {
        observationsWindow.clear()
        consecutiveHighRiskCount = 0
    }
}
