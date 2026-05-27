package io.github.sor2171.kaudioanalyser.service

import io.github.sor2171.kaudioanalyser.entity.WindowType
import io.github.sor2171.kaudioanalyser.utils.performFFT
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Service that analyzes an audio buffer and updates a StateFlow with the computed frequency spectrum.
 * It computes the magnitude spectrum in decibels (dB) and normalizes it to a 0.0 - 1.0 range for UI visualization.
 *
 * @param bufferSize The size of the audio buffer to analyze (must be a power of 2).
 */
class SpectrumAnalyzer(private val bufferSize: Int) {
    init {
        require((bufferSize and (bufferSize - 1)) == 0) {
            "Buffer size must be a power of 2"
        }
    }

    private val spectrumSize = bufferSize / 2

    private val _spectrumState = MutableStateFlow(FloatArray(spectrumSize))
    val spectrumState: StateFlow<FloatArray> = _spectrumState.asStateFlow()

    private val realTemp = FloatArray(bufferSize)
    private val imagTemp = FloatArray(bufferSize)

    private val minDb = -80f
    private val maxDb = 0f

    /**
     * Processes a single window of audio samples, computes the magnitude spectrum in decibels (dB),
     * normalizes the values, and updates [spectrumState].
     *
     * @param floatWindow The array of audio samples (size must equal [bufferSize]).
     */
    fun processAudioWindow(floatWindow: FloatArray) {
        if (floatWindow.size != bufferSize) return

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

        _spectrumState.value = newSpectrum
    }
}