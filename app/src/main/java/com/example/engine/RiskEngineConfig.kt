package com.example.engine

/**
 * Centrally defined operational configuration for VoiceGuard Risk Engine.
 *
 * MANDATORY SCORING INTEGRITY RULE 6:
 * All thresholds and weights must exist in ONE configuration object/file.
 * Do not scatter numeric thresholds throughout the application.
 *
 * RULE 7:
 * The initial weights and thresholds are development defaults only.
 * They must be calibrated using the project's validation dataset.
 */
object RiskEngineConfig {

    // --- Conditional Fusion Weights ---
    const val DEFAULT_VOICE_WEIGHT = 0.55f
    const val DEFAULT_CONVERSATION_WEIGHT = 0.35f
    const val DEFAULT_REPLAY_WEIGHT = 0.10f

    // --- Agreement Boost Parameters ---
    const val AGREEMENT_VOICE_MIN = 0.85f
    const val AGREEMENT_CONV_MIN = 0.70f
    const val AGREEMENT_BOOST_POINTS = 8 // Modest additive boost, not multiplicative

    // --- Disagreement Bounds ---
    const val DISAGREEMENT_HIGH_VOICE_THRESHOLD = 0.80f
    const val DISAGREEMENT_LOW_CONV_THRESHOLD = 0.25f
    const val DISAGREEMENT_LOW_VOICE_THRESHOLD = 0.25f
    const val DISAGREEMENT_HIGH_CONV_THRESHOLD = 0.70f

    // --- High-Value Security Combination Thresholds ---
    const val HIGH_VALUE_SYNTHETIC_THRESHOLD = 0.75f
    const val HIGH_VALUE_CONV_CRITICAL_THRESHOLD = 0.80f

    // --- Severity Thresholds (0–100 Scale) ---
    const val THRESHOLD_SAFE_MAX = 19
    const val THRESHOLD_SUSPICIOUS_MAX = 49
    const val THRESHOLD_HIGH_RISK_MAX = 74
    // 75–100 is CRITICAL

    // --- Hysteresis Bands ---
    const val HYSTERESIS_ENTER_HIGH_RISK = 50
    const val HYSTERESIS_EXIT_HIGH_RISK = 40
    const val HYSTERESIS_ENTER_CRITICAL = 75
    const val HYSTERESIS_EXIT_CRITICAL = 65

    // --- Temporal Aggregator Parameters ---
    const val TEMPORAL_WINDOW_CAPACITY = 5
    const val TEMPORAL_MEAN_COEFF = 0.50f
    const val TEMPORAL_MEDIAN_COEFF = 0.30f
    const val TEMPORAL_PERSISTENCE_COEFF = 0.20f
    const val PERSISTENCE_ELEVATED_THRESHOLD = 0.60f

    // --- Minimum Evidence Rules ---
    const val MIN_SPEECH_DURATION_SECONDS = 1.5f
    const val MIN_EVIDENCE_QUALITY_FOR_DECISION = 0.25f
    const val MIN_CONSECUTIVE_CRITICAL_WINDOWS = 3
}
