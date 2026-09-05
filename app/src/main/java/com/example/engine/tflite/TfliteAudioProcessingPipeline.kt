package com.example.engine.tflite

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.min

/**
 * TfliteAudioProcessingPipeline
 *
 * High-performance Kotlin-based audio processing pipeline that uses TensorFlow Lite
 * to perform real-time spectral analysis of incoming voice streams, specifically targeting
 * phase inconsistencies and prosody anomalies for deepfake and voice clone detection.
 */
class TfliteAudioProcessingPipeline(
    context: Context,
    private val scope: CoroutineScope
) {
    private val spectralEngine = TfliteVoiceSpectralEngine(context)

    // Streaming Audio Buffer Configuration
    private val frameSize = 1024
    private val hopSize = 512
    private val ringBuffer = FloatArray(4096)
    private var bufferHead = 0
    private var bufferCount = 0

    // Processing Queue & Async Pipeline
    private val audioChannel = Channel<FloatArray>(capacity = 64)
    private var processingJob: Job? = null

    // Reactive StateFlow for UI and Background Monitors
    private val _inferenceState = MutableStateFlow(
        TfliteSpectralInferenceResult(
            deepfakeProbability = 0.05f,
            phaseInconsistencyScore = 0.05f,
            prosodyAnomalyScore = 0.05f,
            spectralArtifactScore = 0.05f,
            detectedVocoderSignature = "Acoustic Stream Authentic",
            confidenceScore = 5,
            isSyntheticDetected = false,
            forensicFindings = listOf("Ready for voice stream spectral analysis."),
            acousticFeatures = emptyMap()
        )
    )
    val inferenceState: StateFlow<TfliteSpectralInferenceResult> = _inferenceState.asStateFlow()

    private var onInferenceListener: ((TfliteSpectralInferenceResult) -> Unit)? = null

    fun startPipeline(listener: ((TfliteSpectralInferenceResult) -> Unit)? = null) {
        onInferenceListener = listener
        if (processingJob != null && processingJob?.isActive == true) return

        processingJob = scope.launch(Dispatchers.Default) {
            val analysisWindow = FloatArray(frameSize)

            for (audioChunk in audioChannel) {
                if (!isActive) break

                // Append incoming chunk into circular buffer
                for (s in audioChunk) {
                    ringBuffer[bufferHead] = s
                    bufferHead = (bufferHead + 1) % ringBuffer.size
                    bufferCount = min(ringBuffer.size, bufferCount + 1)
                }

                // If enough samples accumulated for a full window
                if (bufferCount >= frameSize) {
                    var readIdx = (bufferHead - frameSize + ringBuffer.size) % ringBuffer.size
                    for (i in 0 until frameSize) {
                        analysisWindow[i] = ringBuffer[readIdx]
                        readIdx = (readIdx + 1) % ringBuffer.size
                    }

                    // Perform TFLite Spectral, Phase, and Prosody Inference
                    val result = spectralEngine.analyzeAudioChunk(analysisWindow)

                    _inferenceState.value = result
                    onInferenceListener?.invoke(result)
                }
            }
        }
    }

    /**
     * Ingests 16-bit PCM ShortArray samples from live microphone or call stream.
     */
    fun ingestPcmSamples(samples: ShortArray, readCount: Int) {
        if (readCount <= 0) return
        val floatSamples = FloatArray(readCount)
        for (i in 0 until readCount) {
            floatSamples[i] = samples[i] / 32768.0f
        }
        audioChannel.trySend(floatSamples)
    }

    /**
     * Ingests normalized FloatArray samples.
     */
    fun ingestFloatSamples(samples: FloatArray) {
        if (samples.isEmpty()) return
        audioChannel.trySend(samples)
    }

    /**
     * Synchronous immediate chunk analysis (for pre-recorded audio files or testing).
     */
    fun analyzeStaticClip(samples: FloatArray): TfliteSpectralInferenceResult {
        return spectralEngine.analyzeAudioChunk(samples)
    }

    fun stopPipeline() {
        processingJob?.cancel()
        processingJob = null
        bufferCount = 0
        bufferHead = 0
    }

    fun release() {
        stopPipeline()
        spectralEngine.close()
    }
}
