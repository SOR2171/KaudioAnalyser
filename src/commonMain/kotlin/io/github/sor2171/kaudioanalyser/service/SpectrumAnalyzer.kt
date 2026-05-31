package io.github.sor2171.kaudioanalyser.service

import io.github.sor2171.kaudioanalyser.entity.WindowType
import io.github.sor2171.kaudioanalyser.utils.performFFT
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Computes a normalized frequency spectrum from PCM audio samples using a Fast Fourier
 * Transform (FFT).
 *
 * This analyzer accepts a single audio window of fixed size, applies a Hann window,
 * performs an FFT, converts frequency-bin magnitudes to decibels, and normalizes the
 * results into the range `[0.0, 1.0]` for visualization purposes.
 *
 * The returned spectrum contains `bufferSize / 2` bins, representing the positive
 * frequency range from DC (0 Hz) to the Nyquist frequency.
 *
 * @property bufferSize Size of the input audio window. Must be a power of two because
 * FFT algorithms require power-of-two input lengths.
 * @property minDb Minimum decibel value used for normalization. Values at or below
 * this threshold are mapped to `0.0`.
 * @property maxDb Maximum decibel value used for normalization. Values at or above
 * this threshold are mapped to `1.0`.
 *
 * @throws IllegalArgumentException if [bufferSize] is not a power of two.
 */
class SpectrumAnalyzer(
    val bufferSize: Int,
    val minDb: Float = -80f,
    val maxDb: Float = 0f
) {
    init {
        require((bufferSize and (bufferSize - 1)) == 0) {
            "Buffer size must be a power of 2"
        }
    }

    private val spectrumSize = bufferSize / 2
    private val realTemp = FloatArray(bufferSize)
    private val imagTemp = FloatArray(bufferSize)

    /**
     * Processes a single audio window and returns a normalized magnitude spectrum.
     *
     * The input samples are copied into an internal FFT buffer, transformed into the
     * frequency domain using a Hanning window, converted to magnitude values, and then
     * expressed in decibels. Each bin is normalized to the range `[0.0, 1.0]` using
     * the configured [minDb] and [maxDb] thresholds.
     *
     * @param floatWindow PCM audio samples to analyze. The array length must exactly
     * match [bufferSize].
     *
     * @return A spectrum array containing `bufferSize / 2` normalized frequency bins.
     * Each value is clamped to the range `[0.0, 1.0]`.
     *
     * @throws IllegalArgumentException if [floatWindow] size does not equal
     * [bufferSize].
     */
    fun processAudioWindow(floatWindow: FloatArray): FloatArray {
        if (floatWindow.size != bufferSize)
            throw IllegalArgumentException("Buffer size must be equal to the buffer size")

        floatWindow.copyInto(realTemp)
        imagTemp.fill(0f)

        performFFT(realTemp, imagTemp, WindowType.HANNING)

        val newSpectrum = FloatArray(spectrumSize)

        for (i in 0 until spectrumSize) {
            val r = realTemp[i]
            val im = imagTemp[i]

            val magnitude = sqrt(r * r + im * im)

            val normalizedMag = magnitude / bufferSize
            val db = 20f * log10(normalizedMag.coerceAtLeast(1e-6f))

            var uiValue = (db - minDb) / (maxDb - minDb)
            uiValue = uiValue.coerceIn(0f, 1f)

            newSpectrum[i] = uiValue
        }

        return newSpectrum
    }
}