package com.example.engine

/**
 * Voice Activity Detector (VAD).
 * Inspects incoming PCM chunks and determines whether active speech is present
 * using dynamic energy thresholding and zero-crossing analysis.
 * Prevents streaming empty silence or background hiss to Aurigin while ensuring
 * genuine speech segments are forwarded smoothly.
 */
class VoiceActivityDetector(
    private val speechEnergyThreshold: Float = 0.012f,
    private val hangoverFramesLimit: Int = 3
) {
    private var hangoverCounter = 0

    data class VadResult(
        val isSpeech: Boolean,
        val energyRms: Float,
        val zcr: Float
    )

    /**
     * Evaluates a PCM chunk for active voice content.
     * Incorporates hangover smoothing so short inter-syllable pauses are not cut off.
     */
    fun processChunk(pcmBytes: ByteArray, length: Int = pcmBytes.size): VadResult {
        if (length < 2) return VadResult(isSpeech = false, energyRms = 0f, zcr = 0f)

        val rms = AudioPreprocessor.calculateRms(pcmBytes, length)
        var zeroCrossings = 0
        val sampleCount = length / 2

        var prevSample = 0
        for (i in 0 until length - 1 step 2) {
            val sample = (pcmBytes[i].toInt() and 0xFF) or (pcmBytes[i + 1].toInt() shl 8)
            val signed = sample.toShort().toInt()
            if (i > 0 && ((signed >= 0 && prevSample < 0) || (signed < 0 && prevSample >= 0))) {
                zeroCrossings++
            }
            prevSample = signed
        }

        val zcr = if (sampleCount > 0) zeroCrossings.toFloat() / sampleCount else 0f

        val instantSpeech = rms >= speechEnergyThreshold && zcr in 0.01f..0.45f

        val isSpeechWithHangover = if (instantSpeech) {
            hangoverCounter = hangoverFramesLimit
            true
        } else if (hangoverCounter > 0) {
            hangoverCounter--
            true
        } else {
            false
        }

        return VadResult(
            isSpeech = isSpeechWithHangover,
            energyRms = rms,
            zcr = zcr
        )
    }

    fun reset() {
        hangoverCounter = 0
    }
}
