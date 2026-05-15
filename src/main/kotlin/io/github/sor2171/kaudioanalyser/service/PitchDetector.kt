package io.github.sor2171.kaudioanalyser.service

import io.github.sor2171.kaudioanalyser.entity.WindowType
import io.github.sor2171.kaudioanalyser.utils.performFFT
import kotlin.math.sqrt

class PitchDetector(
    private val sampleRate: Int = 44100,
    private val bufferSize: Int = 2048
) {
    private val realBuffer = FloatArray(bufferSize)
    private val imagBuffer = FloatArray(bufferSize)
    private val magnitudes = FloatArray(bufferSize / 2)

    fun startDetection(audioData: FloatArray): Float {
        audioData.copyInto(realBuffer)
        imagBuffer.fill(0f)

        performFFT(realBuffer, imagBuffer, WindowType.HANNING)
        return findPrimaryFrequency(realBuffer, imagBuffer)
    }

    private fun findPrimaryFrequency(real: FloatArray, imag: FloatArray): Float {
        val halfSize = bufferSize / 2
        for (i in 0 until halfSize) {
            magnitudes[i] = sqrt(real[i] * real[i] + imag[i] * imag[i])
        }

        // HPS (Harmonic Product Spectrum)
        val downsampleFactor = 3
        val hpsSize = halfSize / downsampleFactor
        var maxHps = -1f
        var maxIndex = -1

        // ignore low frequencies below 20Hz to avoid noise
        val minBin = (20 * bufferSize / sampleRate).coerceAtLeast(1)

        for (i in minBin until hpsSize) {
            var product = magnitudes[i]
            for (j in 2..downsampleFactor) {
                product *= magnitudes[i * j]
            }

            if (product > maxHps) {
                maxHps = product
                maxIndex = i
            }
        }

        if (maxIndex == -1 || maxHps < 0.0001f) return 0f

        // Parabolic Interpolation for better frequency estimation
        val alpha = magnitudes[maxIndex - 1]
        val beta = magnitudes[maxIndex]
        val gamma = magnitudes[maxIndex + 1]

        val denominator = alpha - 2f * beta + gamma
        val refinedIndex = if (denominator != 0f) {
            val p = 0.5f * (alpha - gamma) / denominator
            maxIndex.toFloat() + p
        } else {
            maxIndex.toFloat()
        }

        return refinedIndex * sampleRate / bufferSize
    }
}