package com.example.engine

/**
 * Calculates evidence quality (0.0 to 1.0) for security decisions.
 *
 * Prevents decisions based on insufficient data, audio glitches, or low-confidence single frames.
 */
object EvidenceQualityCalculator {

    /**
     * Computes evidence quality for a voice authenticity detection frame.
     *
     * @param speechDurationSec Total valid speech duration in seconds analyzed
     * @param audioQualityScore 0.0 to 1.0 from AudioQualityChecker
     * @param detectorConfidence 0.0 to 1.0 confidence reported by Aurigin
     * @param consistentWindowCount Number of recent contiguous consistent windows
     */
    fun computeVoiceEvidenceQuality(
        speechDurationSec: Float,
        audioQualityScore: Float,
        detectorConfidence: Float,
        consistentWindowCount: Int
    ): Float {
        // Speech duration factor (requires at least 1.5s for baseline validity; optimal at >= 3.0s)
        val durationFactor = (speechDurationSec / 3.0f).coerceIn(0f, 1f)
        if (speechDurationSec < RiskEngineConfig.MIN_SPEECH_DURATION_SECONDS) {
            // Low duration severely caps evidence quality
            return (durationFactor * 0.4f * detectorConfidence).coerceIn(0f, 0.35f)
        }

        // Window consistency factor (1 window = 0.4, 3 windows = 0.8, 5 windows = 1.0)
        val windowFactor = (consistentWindowCount / 4.0f).coerceIn(0.4f, 1.0f)

        // Combined evidence quality
        val quality = (durationFactor * 0.30f) +
                (audioQualityScore * 0.25f) +
                (detectorConfidence * 0.25f) +
                (windowFactor * 0.20f)

        return quality.coerceIn(0f, 1f)
    }

    /**
     * Computes evidence quality for conversation scam intent analysis.
     *
     * @param transcriptWordCount Number of words in the transcribed speech
     * @param detectedTacticsCount Number of verified scam tactics or keywords matched
     * @param llmConfidence Confidence reported by NLP/LLM engine
     */
    fun computeConversationEvidenceQuality(
        transcriptWordCount: Int,
        detectedTacticsCount: Int,
        llmConfidence: Float
    ): Float {
        // Less than 5 words is insufficient for linguistic intent analysis
        if (transcriptWordCount < 5) return 0f

        val wordCountFactor = (transcriptWordCount / 40f).coerceIn(0.3f, 1.0f)
        val tacticFactor = if (detectedTacticsCount > 0) 1.0f else 0.5f

        val quality = (wordCountFactor * 0.40f) + (tacticFactor * 0.35f) + (llmConfidence * 0.25f)
        return quality.coerceIn(0f, 1f)
    }
}
