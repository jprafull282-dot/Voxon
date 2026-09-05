package com.example.engine

/**
 * Standardized status of a security analysis engine.
 */
enum class EngineStatus {
    AVAILABLE,
    INSUFFICIENT_DATA,
    UNAVAILABLE,
    ERROR
}

/**
 * Standardized categorical verdict for an individual engine.
 */
enum class EngineVerdict {
    LOW,
    ELEVATED,
    HIGH,
    UNKNOWN
}

/**
 * Common standardized result model produced by every VoiceGuard security engine.
 *
 * CRITICAL SCORING INTEGRITY RULES:
 * 1. Never convert ERROR into SAFE.
 * 2. Never convert UNKNOWN into SAFE.
 * 3. Individual engine scores represent engine-specific findings, NOT overall threat score.
 */
data class EngineResult(
    val engine: String,                         // e.g. "AURIGIN", "CONVERSATION", "REPLAY"
    val score: Float,                          // 0.0 to 1.0 (normalized engine risk indicator)
    val confidence: Float,                     // 0.0 to 1.0
    val verdict: EngineVerdict,                // LOW, ELEVATED, HIGH, UNKNOWN
    val evidenceQuality: Float,                // 0.0 to 1.0 based on duration, SNR, window consistency
    val evidence: List<String> = emptyList(),  // Structured evidence items
    val analyzedDuration: Float = 0f,          // Analyzed voice/speech duration in seconds
    val timestamp: Long = System.currentTimeMillis(),
    val status: EngineStatus = EngineStatus.AVAILABLE,
    val errorMessage: String? = null
) {
    companion object {
        fun unavailable(engine: String, reason: String): EngineResult = EngineResult(
            engine = engine,
            score = 0f,
            confidence = 0f,
            verdict = EngineVerdict.UNKNOWN,
            evidenceQuality = 0f,
            evidence = listOf(reason),
            status = EngineStatus.UNAVAILABLE,
            errorMessage = reason
        )

        fun insufficientData(engine: String, message: String): EngineResult = EngineResult(
            engine = engine,
            score = 0f,
            confidence = 0f,
            verdict = EngineVerdict.UNKNOWN,
            evidenceQuality = 0f,
            evidence = listOf(message),
            status = EngineStatus.INSUFFICIENT_DATA,
            errorMessage = message
        )

        fun error(engine: String, errorMsg: String): EngineResult = EngineResult(
            engine = engine,
            score = 0f,
            confidence = 0f,
            verdict = EngineVerdict.UNKNOWN,
            evidenceQuality = 0f,
            evidence = listOf("Error: $errorMsg"),
            status = EngineStatus.ERROR,
            errorMessage = errorMsg
        )
    }
}
