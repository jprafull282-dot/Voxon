package com.example.engine

import java.util.ArrayDeque
import kotlin.math.max

/**
 * Temporal state capturing rolling observations and persistence over time.
 */
data class TemporalState(
    val rollingScore: Float,                    // 0.0 to 1.0 combined temporal score
    val recentMean: Float,                      // Arithmetic mean of recent observations
    val recentMedian: Float,                    // Median of recent observations
    val persistence: Float,                     // Ratio of frames above elevated threshold (0.0 to 1.0)
    val consecutiveElevatedCount: Int,          // Contiguous count of elevated frames
    val windowCount: Int,                       // Number of frames currently in window
    val isSpikeOnly: Boolean                    // True if current window is an isolated spike following low baseline
)

/**
 * TemporalRiskAggregator
 *
 * Prevents jitter, spurious spikes, and single-frame false alarms by smoothing
 * observations across a configurable rolling window.
 */
class TemporalRiskAggregator(
    private val capacity: Int = RiskEngineConfig.TEMPORAL_WINDOW_CAPACITY,
    private val elevatedThreshold: Float = RiskEngineConfig.PERSISTENCE_ELEVATED_THRESHOLD
) {

    private val window = ArrayDeque<Float>(capacity)
    private var consecutiveElevatedCounter = 0

    @Synchronized
    fun addObservation(score: Float): TemporalState {
        val clamped = score.coerceIn(0f, 1f)

        if (window.size >= capacity) {
            window.pollFirst()
        }
        window.addLast(clamped)

        if (clamped >= elevatedThreshold) {
            consecutiveElevatedCounter++
        } else {
            consecutiveElevatedCounter = max(0, consecutiveElevatedCounter - 1)
        }

        val list = window.toList()
        val mean = list.average().toFloat()

        val sorted = list.sorted()
        val median = if (sorted.size % 2 == 1) {
            sorted[sorted.size / 2]
        } else {
            (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2f
        }

        val elevatedFramesCount = list.count { it >= elevatedThreshold }
        val persistence = if (list.isNotEmpty()) elevatedFramesCount.toFloat() / list.size else 0f

        // Check if this is an isolated spike (e.g. earlier frames were <= 0.30 and only 1 frame jumped)
        val isSpikeOnly = list.size >= 3 && clamped >= 0.70f && (list.take(list.size - 1).average() < 0.30)

        // Rolling temporal formula:
        // rollingScore = 0.50 * recentMean + 0.30 * recentMedian + 0.20 * persistence
        val rolling = (RiskEngineConfig.TEMPORAL_MEAN_COEFF * mean) +
                (RiskEngineConfig.TEMPORAL_MEDIAN_COEFF * median) +
                (RiskEngineConfig.TEMPORAL_PERSISTENCE_COEFF * persistence)

        return TemporalState(
            rollingScore = rolling.coerceIn(0f, 1f),
            recentMean = mean,
            recentMedian = median,
            persistence = persistence,
            consecutiveElevatedCount = consecutiveElevatedCounter,
            windowCount = list.size,
            isSpikeOnly = isSpikeOnly
        )
    }

    @Synchronized
    fun reset() {
        window.clear()
        consecutiveElevatedCounter = 0
    }
}
