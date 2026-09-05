package com.example.engine

import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Audio Quality & Liveness Verification Layer.
 *
 * Checks incoming audio frames to prevent false "voice detected" claims on:
 * - Empty silence / noise floor
 * - Pure stationary ringtone / dialing tones
 * - Severe hardware clipping or DC offset
 * - Muted microphones
 */
class AudioQualityChecker(
    private val silenceRmsThreshold: Float = 0.005f,
    private val clippingThresholdRatio: Float = 0.05f
) {

    data class QualityMetrics(
        val rms: Float,
        val decibels: Float,
        val snrDb: Float,
        val isSilent: Boolean,
        val isClipped: Boolean,
        val isStationaryTone: Boolean, // e.g. dial tone or ringtone
        val qualityScore: Float,       // 0.0 (unusable) to 1.0 (clean high fidelity)
        val qualityIssues: List<String>
    )

    private var estimatedNoiseFloorRms = 0.002f

    fun analyzeChunk(pcmBytes: ByteArray, length: Int = pcmBytes.size): QualityMetrics {
        if (length < 2) {
            return QualityMetrics(
                rms = 0f,
                decibels = -90f,
                snrDb = 0f,
                isSilent = true,
                isClipped = false,
                isStationaryTone = false,
                qualityScore = 0f,
                qualityIssues = listOf("Audio buffer empty")
            )
        }

        val sampleCount = length / 2
        var sumSquares = 0.0
        var clippedCount = 0
        var zeroCrossings = 0
        var prevSample = 0
        var positiveMax = 0
        var negativeMin = 0

        for (i in 0 until length - 1 step 2) {
            val sample = (pcmBytes[i].toInt() and 0xFF) or (pcmBytes[i + 1].toInt() shl 8)
            val signed = sample.toShort().toInt()

            sumSquares += (signed * signed)
            if (signed >= 32760 || signed <= -32760) {
                clippedCount++
            }

            if (i > 0 && ((signed >= 0 && prevSample < 0) || (signed < 0 && prevSample >= 0))) {
                zeroCrossings++
            }
            prevSample = signed
            if (signed > positiveMax) positiveMax = signed
            if (signed < negativeMin) negativeMin = signed
        }

        val meanSquare = sumSquares / sampleCount
        val rawRms = sqrt(meanSquare).toFloat()
        val normalizedRms = (rawRms / 32768f).coerceIn(0f, 1f)

        val decibels = if (normalizedRms > 1e-5f) {
            (20.0 * log10(normalizedRms.toDouble())).toFloat().coerceIn(-90f, 0f)
        } else {
            -90f
        }

        // Noise floor tracking
        if (normalizedRms < estimatedNoiseFloorRms * 1.5f && normalizedRms > 1e-4f) {
            estimatedNoiseFloorRms = (estimatedNoiseFloorRms * 0.95f + normalizedRms * 0.05f).coerceIn(1e-4f, 0.05f)
        }

        val snrDb = if (estimatedNoiseFloorRms > 0f && normalizedRms > estimatedNoiseFloorRms) {
            (20.0 * log10((normalizedRms / estimatedNoiseFloorRms).toDouble())).toFloat().coerceIn(0f, 60f)
        } else {
            0f
        }

        val clippingRatio = clippedCount.toFloat() / sampleCount
        val isClipped = clippingRatio >= clippingThresholdRatio
        val isSilent = normalizedRms < silenceRmsThreshold

        // Ringtone / Stationary Tone detection:
        // Pure tones have narrow dynamic range (crest factor ~ 1.41) and extremely uniform zero-crossing rate
        val zcr = zeroCrossings.toFloat() / sampleCount
        val peak = max(positiveMax, -negativeMin).toFloat()
        val crestFactor = if (rawRms > 0) peak / rawRms else 0f
        val isStationaryTone = (crestFactor in 1.25f..1.55f && zcr in 0.08f..0.25f && normalizedRms > 0.05f)

        val issues = mutableListOf<String>()
        if (isSilent) issues.add("Signal below speech energy threshold (Silence)")
        if (isClipped) issues.add("Microphone saturation/clipping detected")
        if (isStationaryTone) issues.add("Stationary frequency detected (Ringtone / Telephony tone)")
        if (snrDb < 6f && !isSilent) issues.add("Low signal-to-noise ratio (High ambient noise)")

        // Quality score:
        var quality = 1.0f
        if (isSilent) quality = 0.0f
        if (isStationaryTone) quality *= 0.1f
        if (isClipped) quality *= 0.5f
        if (snrDb < 10f) quality *= (snrDb / 10f).coerceIn(0.2f, 1.0f)

        return QualityMetrics(
            rms = normalizedRms,
            decibels = decibels,
            snrDb = snrDb,
            isSilent = isSilent,
            isClipped = isClipped,
            isStationaryTone = isStationaryTone,
            qualityScore = quality.coerceIn(0f, 1f),
            qualityIssues = issues
        )
    }

    fun reset() {
        estimatedNoiseFloorRms = 0.002f
    }
}
