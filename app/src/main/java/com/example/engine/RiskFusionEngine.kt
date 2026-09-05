package com.example.engine

import android.util.Log

/**
 * Final authoritative verdict classifications for VoiceGuard.
 */
enum class FinalRiskVerdict {
    SAFE,
    INCONCLUSIVE,
    SUSPICIOUS,
    HIGH_RISK,
    CRITICAL
}

/**
 * Authoritative Security Evaluation Output.
 *
 * Contains:
 * - 3 INDEPENDENT SCORES:
 *   1. aiVoiceScore (0.0 to 1.0, from Aurigin detector only)
 *   2. conversationFraudScore (0.0 to 1.0, from Conversation engine only)
 *   3. overallThreatScore (0 to 100, calculated solely by RiskFusionEngine)
 * - Evidence summary and high-value indicators
 * - Hysteresis-stabilized final verdict
 * - Actionable security guidance
 */
data class FinalRiskAssessment(
    val overallThreatScore: Int,                 // 0 to 100
    val aiVoiceScore: Float,                     // 0.0 to 1.0 (Voice authenticity only)
    val conversationFraudScore: Float,           // 0.0 to 1.0 (Conversation scam only)
    val finalVerdict: FinalRiskVerdict,
    val evidenceQuality: Float,                  // 0.0 to 1.0
    val shouldTriggerThreatAlert: Boolean,       // Only true when persistent threat criteria are met
    val isEngineAgreement: Boolean,              // True if both engines corroborate high risk
    val explanation: String,
    val detectedTactics: List<String>,
    val recommendedAction: String,
    val voiceResult: EngineResult?,
    val conversationResult: EngineResult?,
    val temporalState: TemporalState
)

/**
 * RiskFusionEngine
 *
 * THE SINGLE AUTHORITATIVE COMPONENT RESPONSIBLE FOR CALCULATING THE FINAL
 * VOICEGUARD RISK SCORE AND FINAL VERDICT.
 *
 * No other component, view, or service may calculate, modify, override,
 * or hard-code the final overall risk score.
 */
class RiskFusionEngine {

    companion object {
        private const val TAG = "RiskFusionEngine"
    }

    private var previousVerdict: FinalRiskVerdict = FinalRiskVerdict.SAFE
    private val temporalAggregator = TemporalRiskAggregator()

    /**
     * Evaluates independent engine observations and produces the authoritative FinalRiskAssessment.
     *
     * @param voiceResult Standardized result from Engine 1 (Aurigin)
     * @param conversationResult Standardized result from Engine 2 (Conversation Fraud Engine)
     * @param replayResult Optional replay/acoustic artifact result
     * @param isSpeechActive True if VoiceActivityDetector has confirmed active speech
     */
    @Synchronized
    fun evaluate(
        voiceResult: EngineResult?,
        conversationResult: EngineResult?,
        replayResult: EngineResult? = null,
        isSpeechActive: Boolean = true
    ): FinalRiskAssessment {

        // RULE 11 & 26: Technical failures must NEVER become security threats.
        if (voiceResult != null && voiceResult.status == EngineStatus.ERROR) {
            val temporal = temporalAggregator.addObservation(0f)
            return buildInconclusive(
                reason = "Voice authenticity analysis inconclusive (Service error: ${voiceResult.errorMessage ?: "Technical error"}).",
                voiceResult = voiceResult,
                conversationResult = conversationResult,
                temporal = temporal
            )
        }

        // Check if both engines have insufficient data or are unavailable
        val voiceAvailable = voiceResult != null && voiceResult.status == EngineStatus.AVAILABLE
        val convAvailable = conversationResult != null && conversationResult.status == EngineStatus.AVAILABLE

        if (!voiceAvailable && !convAvailable) {
            val temporal = temporalAggregator.addObservation(0f)
            val msg = if (!isSpeechActive) {
                "Monitoring call audio stream. Awaiting active caller speech."
            } else {
                "Awaiting sensor observations from analysis engines."
            }
            return FinalRiskAssessment(
                overallThreatScore = 0,
                aiVoiceScore = 0f,
                conversationFraudScore = 0f,
                finalVerdict = if (!isSpeechActive) FinalRiskVerdict.SAFE else FinalRiskVerdict.INCONCLUSIVE,
                evidenceQuality = 0f,
                shouldTriggerThreatAlert = false,
                isEngineAgreement = false,
                explanation = msg,
                detectedTactics = emptyList(),
                recommendedAction = "Stay vigilant while call stream is actively analyzed.",
                voiceResult = voiceResult,
                conversationResult = conversationResult,
                temporalState = temporal
            )
        }

        val voiceScore = if (voiceAvailable) voiceResult!!.score else 0f
        val convScore = if (convAvailable) conversationResult!!.score else 0f
        val replayScore = if (replayResult != null && replayResult.status == EngineStatus.AVAILABLE) replayResult.score else 0f

        // Compute evidence quality across available engines
        val combinedEvidenceQuality = computeCombinedEvidenceQuality(voiceResult, conversationResult)

        // RULE 12: Minimum evidence rule
        if (combinedEvidenceQuality < RiskEngineConfig.MIN_EVIDENCE_QUALITY_FOR_DECISION &&
            voiceResult?.analyzedDuration ?: 0f < RiskEngineConfig.MIN_SPEECH_DURATION_SECONDS &&
            convScore < 0.50f
        ) {
            val temporal = temporalAggregator.addObservation(0f)
            return buildInconclusive(
                reason = "Insufficient speech duration to complete forensic evaluation.",
                voiceResult = voiceResult,
                conversationResult = conversationResult,
                temporal = temporal
            )
        }

        // Conditional weighted fusion: Renormalize weights when an engine is unavailable
        val (baseRisk, hasVoice, hasConv) = calculateNormalizedBaseRisk(
            voiceResult = voiceResult,
            convResult = conversationResult,
            replayResult = replayResult
        )

        // Temporal aggregation
        val temporal = temporalAggregator.addObservation(baseRisk)

        // High-Value Combination Checks (Section 19)
        val tactics = mutableListOf<String>()
        conversationResult?.evidence?.let { tactics.addAll(it) }
        voiceResult?.evidence?.let { tactics.addAll(it) }

        val hasOtpRequest = tactics.any { it.contains("OTP", ignoreCase = true) || it.contains("verification code", ignoreCase = true) }
        val hasFinancialTransfer = tactics.any { it.contains("UPI", ignoreCase = true) || it.contains("wire", ignoreCase = true) || it.contains("transfer", ignoreCase = true) || it.contains("payment", ignoreCase = true) }
        val hasImpersonation = tactics.any { it.contains("police", ignoreCase = true) || it.contains("CBI", ignoreCase = true) || it.contains("bank", ignoreCase = true) || it.contains("customs", ignoreCase = true) || it.contains("TRAI", ignoreCase = true) }
        val hasCoercion = tactics.any { it.contains("arrest", ignoreCase = true) || it.contains("block", ignoreCase = true) || it.contains("disconnect", ignoreCase = true) || it.contains("penalty", ignoreCase = true) }

        val isSyntheticVoiceHigh = voiceAvailable && voiceScore >= RiskEngineConfig.HIGH_VALUE_SYNTHETIC_THRESHOLD
        val isHumanVoiceConfirmed = voiceAvailable && voiceScore < RiskEngineConfig.DISAGREEMENT_LOW_VOICE_THRESHOLD

        // Engine Agreement check (Section 17)
        val isAgreement = voiceAvailable && convAvailable &&
                voiceScore >= RiskEngineConfig.AGREEMENT_VOICE_MIN &&
                convScore >= RiskEngineConfig.AGREEMENT_CONV_MIN

        var adjustedScore = (temporal.rollingScore * 100f).toInt()

        // Apply modest engine agreement boost (not multiplicative)
        if (isAgreement) {
            adjustedScore += RiskEngineConfig.AGREEMENT_BOOST_POINTS
        }

        // Apply high-value combination rules
        if (isSyntheticVoiceHigh && (hasOtpRequest || hasFinancialTransfer)) {
            adjustedScore = adjustedScore.coerceAtLeast(78)
        }
        if (isSyntheticVoiceHigh && hasImpersonation && (hasFinancialTransfer || hasCoercion)) {
            adjustedScore = adjustedScore.coerceAtLeast(88)
        }
        if (isHumanVoiceConfirmed && hasImpersonation && (hasOtpRequest || hasFinancialTransfer)) {
            // Human voice committing serious fraud is still high risk!
            adjustedScore = adjustedScore.coerceAtLeast(70)
        }

        val finalScore = adjustedScore.coerceIn(0, 100)

        // Evaluate raw categorical verdict via Section 20 Verdict Matrix & Disagreement rules
        val rawVerdict = determineVerdict(
            finalScore = finalScore,
            voiceAvailable = voiceAvailable,
            voiceScore = voiceScore,
            convAvailable = convAvailable,
            convScore = convScore,
            temporal = temporal,
            isAgreement = isAgreement,
            isSyntheticVoiceHigh = isSyntheticVoiceHigh,
            hasHighValueTactic = (hasOtpRequest || hasFinancialTransfer || hasImpersonation)
        )

        // Apply Hysteresis (Section 22)
        val stabilizedVerdict = applyHysteresis(finalScore, rawVerdict)
        previousVerdict = stabilizedVerdict

        // Determine if threat alert popup should be shown (Section 23, 24, 25)
        // CRITICAL alert requires:
        // A. Multiple consecutive strong voice detections (temporal.consecutiveElevatedCount >= 3)
        // OR B. Strong voice + strong conversation risk (agreement)
        // OR C. High-value combination
        val shouldTriggerAlert = (stabilizedVerdict == FinalRiskVerdict.CRITICAL || stabilizedVerdict == FinalRiskVerdict.HIGH_RISK) &&
                (temporal.consecutiveElevatedCount >= RiskEngineConfig.MIN_CONSECUTIVE_CRITICAL_WINDOWS ||
                        isAgreement ||
                        (isSyntheticVoiceHigh && (hasOtpRequest || hasFinancialTransfer || hasImpersonation)) ||
                        (isHumanVoiceConfirmed && hasImpersonation && hasOtpRequest))

        val explanation = buildExplanation(
            verdict = stabilizedVerdict,
            voiceScore = voiceScore,
            convScore = convScore,
            isAgreement = isAgreement,
            isSyntheticHigh = isSyntheticVoiceHigh,
            tactics = tactics,
            temporal = temporal
        )

        val recommendedAction = buildRecommendedAction(stabilizedVerdict, hasOtpRequest, hasFinancialTransfer)

        return FinalRiskAssessment(
            overallThreatScore = finalScore,
            aiVoiceScore = if (voiceAvailable) voiceScore else 0f,
            conversationFraudScore = if (convAvailable) convScore else 0f,
            finalVerdict = stabilizedVerdict,
            evidenceQuality = combinedEvidenceQuality,
            shouldTriggerThreatAlert = shouldTriggerAlert,
            isEngineAgreement = isAgreement,
            explanation = explanation,
            detectedTactics = tactics.distinct(),
            recommendedAction = recommendedAction,
            voiceResult = voiceResult,
            conversationResult = conversationResult,
            temporalState = temporal
        )
    }

    private fun calculateNormalizedBaseRisk(
        voiceResult: EngineResult?,
        convResult: EngineResult?,
        replayResult: EngineResult?
    ): Triple<Float, Boolean, Boolean> {
        var totalWeight = 0f
        var weightedSum = 0f

        val hasVoice = voiceResult != null && voiceResult.status == EngineStatus.AVAILABLE
        if (hasVoice) {
            weightedSum += voiceResult!!.score * RiskEngineConfig.DEFAULT_VOICE_WEIGHT
            totalWeight += RiskEngineConfig.DEFAULT_VOICE_WEIGHT
        }

        val hasConv = convResult != null && convResult.status == EngineStatus.AVAILABLE
        if (hasConv) {
            weightedSum += convResult!!.score * RiskEngineConfig.DEFAULT_CONVERSATION_WEIGHT
            totalWeight += RiskEngineConfig.DEFAULT_CONVERSATION_WEIGHT
        }

        val hasReplay = replayResult != null && replayResult.status == EngineStatus.AVAILABLE
        if (hasReplay) {
            weightedSum += replayResult!!.score * RiskEngineConfig.DEFAULT_REPLAY_WEIGHT
            totalWeight += RiskEngineConfig.DEFAULT_REPLAY_WEIGHT
        }

        val baseRisk = if (totalWeight > 0f) weightedSum / totalWeight else 0f
        return Triple(baseRisk.coerceIn(0f, 1f), hasVoice, hasConv)
    }

    private fun determineVerdict(
        finalScore: Int,
        voiceAvailable: Boolean,
        voiceScore: Float,
        convAvailable: Boolean,
        convScore: Float,
        temporal: TemporalState,
        isAgreement: Boolean,
        isSyntheticVoiceHigh: Boolean,
        hasHighValueTactic: Boolean
    ): FinalRiskVerdict {
        // Section 14: A single isolated spike must NOT trigger CRITICAL
        if (temporal.isSpikeOnly && finalScore >= RiskEngineConfig.HYSTERESIS_ENTER_CRITICAL) {
            return FinalRiskVerdict.SUSPICIOUS
        }

        // Section 18: Engine Disagreement Rules
        if (voiceAvailable && convAvailable) {
            // Aurigin high, Conversation low
            if (voiceScore >= RiskEngineConfig.DISAGREEMENT_HIGH_VOICE_THRESHOLD &&
                convScore <= RiskEngineConfig.DISAGREEMENT_LOW_CONV_THRESHOLD
            ) {
                return if (temporal.consecutiveElevatedCount >= 3) FinalRiskVerdict.SUSPICIOUS else FinalRiskVerdict.INCONCLUSIVE
            }
            // Aurigin low, Conversation high (Real humans commit scams)
            if (voiceScore <= RiskEngineConfig.DISAGREEMENT_LOW_VOICE_THRESHOLD &&
                convScore >= RiskEngineConfig.DISAGREEMENT_HIGH_CONV_THRESHOLD
            ) {
                return FinalRiskVerdict.HIGH_RISK
            }
        }

        // Section 20 Verdict Matrix
        if (!voiceAvailable && convAvailable) {
            return if (convScore >= RiskEngineConfig.DISAGREEMENT_HIGH_CONV_THRESHOLD) FinalRiskVerdict.HIGH_RISK else FinalRiskVerdict.SUSPICIOUS
        }
        if (voiceAvailable && !convAvailable) {
            return if (voiceScore >= RiskEngineConfig.HIGH_VALUE_SYNTHETIC_THRESHOLD) FinalRiskVerdict.SUSPICIOUS else FinalRiskVerdict.SAFE
        }

        // Standard Thresholds (Section 21)
        return when {
            finalScore >= RiskEngineConfig.HYSTERESIS_ENTER_CRITICAL -> {
                if (temporal.consecutiveElevatedCount >= RiskEngineConfig.MIN_CONSECUTIVE_CRITICAL_WINDOWS || isAgreement || (isSyntheticVoiceHigh && hasHighValueTactic)) {
                    FinalRiskVerdict.CRITICAL
                } else {
                    FinalRiskVerdict.HIGH_RISK
                }
            }
            finalScore >= RiskEngineConfig.HYSTERESIS_ENTER_HIGH_RISK -> FinalRiskVerdict.HIGH_RISK
            finalScore > RiskEngineConfig.THRESHOLD_SAFE_MAX -> FinalRiskVerdict.SUSPICIOUS
            else -> FinalRiskVerdict.SAFE
        }
    }

    private fun applyHysteresis(currentScore: Int, calculatedVerdict: FinalRiskVerdict): FinalRiskVerdict {
        return when (previousVerdict) {
            FinalRiskVerdict.CRITICAL -> {
                if (currentScore < RiskEngineConfig.HYSTERESIS_EXIT_CRITICAL) {
                    if (currentScore >= RiskEngineConfig.HYSTERESIS_EXIT_HIGH_RISK) FinalRiskVerdict.HIGH_RISK else FinalRiskVerdict.SUSPICIOUS
                } else {
                    FinalRiskVerdict.CRITICAL
                }
            }
            FinalRiskVerdict.HIGH_RISK -> {
                if (currentScore >= RiskEngineConfig.HYSTERESIS_ENTER_CRITICAL) {
                    calculatedVerdict
                } else if (currentScore < RiskEngineConfig.HYSTERESIS_EXIT_HIGH_RISK) {
                    if (currentScore > RiskEngineConfig.THRESHOLD_SAFE_MAX) FinalRiskVerdict.SUSPICIOUS else FinalRiskVerdict.SAFE
                } else {
                    FinalRiskVerdict.HIGH_RISK
                }
            }
            else -> calculatedVerdict
        }
    }

    private fun computeCombinedEvidenceQuality(voiceResult: EngineResult?, convResult: EngineResult?): Float {
        val q1 = voiceResult?.evidenceQuality ?: 0f
        val q2 = convResult?.evidenceQuality ?: 0f
        return if (voiceResult != null && convResult != null) {
            (q1 * 0.6f + q2 * 0.4f)
        } else {
            max(q1, q2)
        }
    }

    private fun buildInconclusive(
        reason: String,
        voiceResult: EngineResult?,
        conversationResult: EngineResult?,
        temporal: TemporalState
    ): FinalRiskAssessment {
        return FinalRiskAssessment(
            overallThreatScore = 0,
            aiVoiceScore = voiceResult?.score ?: 0f,
            conversationFraudScore = conversationResult?.score ?: 0f,
            finalVerdict = FinalRiskVerdict.INCONCLUSIVE,
            evidenceQuality = 0f,
            shouldTriggerThreatAlert = false,
            isEngineAgreement = false,
            explanation = reason,
            detectedTactics = emptyList(),
            recommendedAction = "Verify caller identity manually through a secondary out-of-band channel.",
            voiceResult = voiceResult,
            conversationResult = conversationResult,
            temporalState = temporal
        )
    }

    private fun buildExplanation(
        verdict: FinalRiskVerdict,
        voiceScore: Float,
        convScore: Float,
        isAgreement: Boolean,
        isSyntheticHigh: Boolean,
        tactics: List<String>,
        temporal: TemporalState
    ): String {
        return when (verdict) {
            FinalRiskVerdict.CRITICAL -> {
                if (isAgreement) {
                    "CRITICAL THREAT: Both engines corroborate high threat. Strong synthetic voice artifacts combined with high-risk conversational fraud indicators."
                } else if (isSyntheticHigh) {
                    "CRITICAL THREAT: High-confidence synthetic voice clone combined with urgent financial or credential coercion."
                } else {
                    "CRITICAL THREAT: Consecutive high-risk threat indicators confirmed across temporal observation windows."
                }
            }
            FinalRiskVerdict.HIGH_RISK -> {
                if (convScore >= 0.70f && voiceScore < 0.30f) {
                    "HIGH RISK: Caller voice appears acoustic/human, but conversation exhibits severe fraud and social engineering tactics."
                } else {
                    "HIGH RISK: Significant threat indicators observed. Potential deepfake clone or coercive extortion pattern."
                }
            }
            FinalRiskVerdict.SUSPICIOUS -> {
                if (voiceScore >= 0.75f && convScore < 0.25f) {
                    "SUSPICIOUS: Strong synthetic-voice signal observed, but weak conversation-fraud evidence. Continuing temporal monitoring."
                } else {
                    "SUSPICIOUS: Acoustic anomalies or unverified patterns detected. Gathering additional speech windows."
                }
            }
            FinalRiskVerdict.SAFE -> "SAFE: Stream exhibits natural acoustic characteristics and normal conversational patterns."
            FinalRiskVerdict.INCONCLUSIVE -> "INCONCLUSIVE: Insufficient speech or acoustic data to complete verification."
        }
    }

    private fun buildRecommendedAction(
        verdict: FinalRiskVerdict,
        hasOtp: Boolean,
        hasFinancial: Boolean
    ): String {
        return when (verdict) {
            FinalRiskVerdict.CRITICAL -> {
                if (hasOtp) "Do NOT disclose OTPs or security codes! Hang up immediately."
                else if (hasFinancial) "Do NOT transfer funds or execute payments. Hang up immediately."
                else "Hang up immediately. Contact the alleged entity through official phone numbers."
            }
            FinalRiskVerdict.HIGH_RISK -> "Exercise extreme caution. Ask a personal challenge question known only to the real contact."
            FinalRiskVerdict.SUSPICIOUS -> "Verify caller identity before acting on any request or sharing personal information."
            FinalRiskVerdict.SAFE -> "No threat detected. Standard protection active."
            FinalRiskVerdict.INCONCLUSIVE -> "Stay vigilant while additional call telemetry is gathered."
        }
    }

    fun reset() {
        previousVerdict = FinalRiskVerdict.SAFE
        temporalAggregator.reset()
    }
}
