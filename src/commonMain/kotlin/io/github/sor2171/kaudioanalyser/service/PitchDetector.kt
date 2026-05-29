package io.github.sor2171.kaudioanalyser.service

import io.github.sor2171.kaudioanalyser.entity.WindowType
import io.github.sor2171.kaudioanalyser.utils.applyWindow
import io.github.sor2171.kaudioanalyser.utils.performFFT
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Service for detecting the pitch (fundamental frequency) of an audio signal.
 * Supports FFT-based detection with Harmonic Product Spectrum (HPS) and the YIN autocorrelation algorithm.
 *
 * @property sampleRate The sampling rate of the audio data in Hz (defaults to 44100).
 * @property bufferSize The size of the audio buffer used for detection (defaults to 2048, must be a power of 2).
 * @property windowType The window function applied to the audio buffer.
 */
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


    /**
     * Detects the pitch of the given audio data using an FFT-based approach refined by Harmonic Product Spectrum (HPS).
     *
     * @param audioData The normalized input float audio samples.
     * @return The detected fundamental frequency in Hz, or 0f if no pitch is detected or the signal is too quiet.
     */
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

    /**
     * Refines and extracts the primary frequency from the FFT buffers using Harmonic Product Spectrum (HPS)
     * and parabolic interpolation.
     *
     * @return The refined fundamental frequency in Hz.
     */
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

    /**
     * Detects the pitch of the given audio data using the YIN algorithm.
     *
     * @param audio The input float audio samples.
     * @return The detected fundamental frequency in Hz, or 0f if no pitch is detected.
     */
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

    /**
     * Performs parabolic interpolation on the Cumulative Mean Normalized Difference Function (CMNDF)
     * to refine the lag estimate (tau) for more precise frequency calculation.
     *
     * @param tau The initial integer lag estimate.
     * @return The refined floating-point lag estimate.
     */
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