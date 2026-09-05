package com.example.engine

import kotlin.math.sqrt

/**
 * Reusable audio preprocessing layer.
 * Normalizes input PCM audio streams to standard 16 kHz 16-bit mono PCM,
 * computes RMS energy, handles resampling if needed, and packages audio into
 * standardized streaming chunks for VAD and detector consumption.
 */
object AudioPreprocessor {

    const val TARGET_SAMPLE_RATE = 16000
    const val TARGET_CHANNELS = 1
    const val BYTES_PER_SAMPLE = 2 // 16-bit PCM

    /**
     * Calculates the Root Mean Square (RMS) energy of a 16-bit mono PCM byte array.
     * Normalized between 0.0f and 1.0f.
     */
    fun calculateRms(pcmBytes: ByteArray, length: Int = pcmBytes.size): Float {
        if (length < 2) return 0f
        var sumSquares = 0.0
        val sampleCount = length / 2

        for (i in 0 until length - 1 step 2) {
            val sample = (pcmBytes[i].toInt() and 0xFF) or (pcmBytes[i + 1].toInt() shl 8)
            val signedSample = sample.toShort()
            sumSquares += (signedSample * signedSample)
        }

        val meanSquare = sumSquares / sampleCount
        val rms = sqrt(meanSquare).toFloat()
        return (rms / 32768f).coerceIn(0f, 1f)
    }

    /**
     * Converts a ShortArray of 16-bit PCM samples into a ByteArray (little-endian).
     */
    fun shortArrayToByteArray(shorts: ShortArray, length: Int = shorts.size): ByteArray {
        val bytes = ByteArray(length * 2)
        for (i in 0 until length) {
            val s = shorts[i].toInt()
            bytes[i * 2] = (s and 0xFF).toByte()
            bytes[i * 2 + 1] = ((s shr 8) and 0xFF).toByte()
        }
        return bytes
    }

    /**
     * Converts a ByteArray of 16-bit PCM samples (little-endian) into a ShortArray.
     */
    fun byteArrayToShortArray(bytes: ByteArray, length: Int = bytes.size): ShortArray {
        val shorts = ShortArray(length / 2)
        for (i in shorts.indices) {
            val low = bytes[i * 2].toInt() and 0xFF
            val high = bytes[i * 2 + 1].toInt() shl 8
            shorts[i] = (low or high).toShort()
        }
        return shorts
    }

    /**
     * Resamples 16-bit mono PCM audio from [sourceRate] to [TARGET_SAMPLE_RATE] (16kHz)
     * using linear interpolation if the input sample rate differs from 16kHz.
     */
    fun resampleTo16kHz(input: ShortArray, sourceRate: Int): ShortArray {
        if (sourceRate == TARGET_SAMPLE_RATE) return input
        val ratio = TARGET_SAMPLE_RATE.toDouble() / sourceRate.toDouble()
        val targetLength = (input.size * ratio).toInt()
        val output = ShortArray(targetLength)

        for (i in 0 until targetLength) {
            val sourcePos = i / ratio
            val index = sourcePos.toInt()
            val frac = (sourcePos - index).toFloat()

            if (index + 1 < input.size) {
                val sample1 = input[index]
                val sample2 = input[index + 1]
                output[i] = (sample1 + frac * (sample2 - sample1)).toInt().toShort()
            } else if (index < input.size) {
                output[i] = input[index]
            }
        }
        return output
    }
}
