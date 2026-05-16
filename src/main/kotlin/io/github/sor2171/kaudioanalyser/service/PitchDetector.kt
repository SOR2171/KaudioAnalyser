package io.github.sor2171.kaudioanalyser.service

import io.github.sor2171.kaudioanalyser.entity.WindowType
import io.github.sor2171.kaudioanalyser.utils.applyWindow
import io.github.sor2171.kaudioanalyser.utils.performFFT
import kotlin.math.abs
import kotlin.math.sqrt

class PitchDetector(
    private val sampleRate: Int = 44100,
    private val bufferSize: Int = 2048,
    private val windowType: WindowType = WindowType.HANNING
) {
    private val realBuffer = FloatArray(bufferSize)
    private val imagBuffer = FloatArray(bufferSize)
    private val magnitudes = FloatArray(bufferSize / 2)


    private val threshold = 0.1f
    private val difference = FloatArray(bufferSize / 2)
    private val cmndf = FloatArray(bufferSize / 2)


    // FFT-based pitch detection with HPS
    fun detectPitch(audioData: FloatArray): Float {
        // RMS
        var sumSquares = 0f
        for (sample in audioData) sumSquares += sample * sample
        val rms = sqrt(sumSquares / audioData.size)

        if (rms < 0.001f) return 0f

        audioData.copyInto(realBuffer)
        imagBuffer.fill(0f)

        performFFT(realBuffer, imagBuffer, windowType)
        return findPrimaryFrequency()
    }

    private fun findPrimaryFrequency(): Float {
        val halfSize = bufferSize / 2
        var maxMagnitude = 0f
        for (i in 0 until halfSize) {
            magnitudes[i] = sqrt(realBuffer[i] * realBuffer[i] + imagBuffer[i] * imagBuffer[i])
            if (magnitudes[i] > maxMagnitude) maxMagnitude = magnitudes[i]
        }

        if (maxMagnitude < 0.005f) return 0f

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

        if (maxIndex < minBin || maxIndex >= hpsSize - 1) return 0f

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

    // YIN Algorithm for pitch detection
    fun detectPitchYIN(audio: FloatArray): Float {
        applyWindow(audio, windowType)

        val halfBuffer = bufferSize / 2
        for (tau in 0 until halfBuffer) {
            var sum = 0f
            for (j in 0 until halfBuffer) {

                val delta = audio[j] - audio[j + tau]
                sum += delta * delta
            }

            difference[tau] = sum
        }

        cmndf[0] = 1f
        var runningSum = 0f

        for (tau in 1 until halfBuffer) {

            runningSum += difference[tau]

            cmndf[tau] =
                difference[tau] * tau / runningSum
        }

        var tauEstimate = -1

        for (tau in 2 until halfBuffer) {
            if (cmndf[tau] < threshold) {

                while (
                    tau + 1 < halfBuffer &&
                    cmndf[tau + 1] < cmndf[tau]
                ) {
                    tauEstimate = tau + 1
                    break
                }

                if (tauEstimate == -1) {
                    tauEstimate = tau
                }
                break
            }
        }

        if (tauEstimate == -1) {
            return 0f
        }

        val betterTau = parabolicInterpolation(tauEstimate)
        return sampleRate / betterTau
    }

    private fun parabolicInterpolation(tau: Int): Float {

        if (tau <= 0 || tau >= cmndf.size - 1) {
            return tau.toFloat()
        }

        val x0 = cmndf[tau - 1]
        val x1 = cmndf[tau]
        val x2 = cmndf[tau + 1]

        val denominator = 2f * (2f * x1 - x2 - x0)

        if (abs(denominator) < 1e-6f) {
            return tau.toFloat()
        }

        return tau + (x2 - x0) / denominator
    }
}