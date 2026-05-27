package io.github.sor2171.kaudioanalyser.utils

import io.github.sor2171.kaudioanalyser.entity.WindowType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Performs a Fast Fourier Transform (FFT) on the given complex signal.
 *
 * @param real The real part of the input signal, modified in-place to contain the real part of the frequency spectrum.
 * @param imag The imaginary part of the input signal, modified in-place to contain the imaginary part of the frequency spectrum.
 * @param windowType The type of window function to apply to the input signal before FFT.
 * @throws IllegalArgumentException If the sizes of `real` and `imag` do not match, or if the size is not a power of 2.
 */
fun performFFT(
    real: FloatArray,
    imag: FloatArray,
    windowType: WindowType = WindowType.HANNING
) {
    val n = real.size
    if (n != imag.size || (n and (n - 1)) != 0) {
        throw IllegalArgumentException("Buffer size must be a power of 2")
    }

    applyWindow(real, windowType)

    var j = 0
    for (i in 0 until n) {
        if (i < j) {
            val tempR = real[i]; real[i] = real[j]; real[j] = tempR
            val tempI = imag[i]; imag[i] = imag[j]; imag[j] = tempI
        }
        var m = n shr 1
        while (m >= 1 && j >= m) { // do not trust the recommendation from IDEA
            j -= m
            m = m shr 1
        }
        j += m
    }

    var length = 2
    while (length <= n) {
        val angle = -2.0 * PI / length
        val wLenR = cos(angle).toFloat()
        val wLenI = sin(angle).toFloat()

        var i = 0
        while (i < n) {
            var wR = 1.0f
            var wI = 0.0f
            for (k in 0 until length / 2) {
                val uR = real[i + k]
                val uI = imag[i + k]
                val vR = real[i + k + length / 2] * wR - imag[i + k + length / 2] * wI
                val vI = real[i + k + length / 2] * wI + imag[i + k + length / 2] * wR

                real[i + k] = uR + vR
                imag[i + k] = uI + vI
                real[i + k + length / 2] = uR - vR
                imag[i + k + length / 2] = uI - vI

                val nextWR = wR * wLenR - wI * wLenI
                wI = wR * wLenI + wI * wLenR
                wR = nextWR
            }
            i += length
        }
        length = length shl 1
    }
}

/**
 * Applies a windowing function to the given data array to reduce spectral leakage.
 *
 * @param data The input signal array to be windowed in-place.
 * @param type The type of window function to apply (e.g., HANNING, HAMMING, RECTANGULAR).
 */
fun applyWindow(data: FloatArray, type: WindowType) {
    val n = data.size
    for (i in data.indices) {
        data[i] *= when (type) {
            WindowType.HANNING -> 0.5f * (1f - cos(2f * PI.toFloat() * i / (n - 1)))
            WindowType.HAMMING -> 0.54f - 0.46f * cos(2f * PI.toFloat() * i / (n - 1))
            WindowType.RECTANGULAR -> 1.0f
        }
    }
}

/**
 * Converts a stream of raw audio bytes into a stream of overlapping windowed float arrays.
 *
 * @param windowSize The size of each audio window (number of samples).
 * @param hopSize The number of samples to advance (hop) between consecutive windows.
 * @param channels The number of audio channels in the raw byte stream.
 * @param bytesPerSample The number of bytes per sample (e.g., 2 for 16-bit audio).
 * @return A [Flow] of [FloatArray], where each array represents one window of normalized audio samples.
 */
fun Flow<ByteArray>.toAudioWindows(
    windowSize: Int = 2048,
    hopSize: Int = 1024,
    channels: Int = 1,
    bytesPerSample: Int = 2
): Flow<FloatArray> = flow {
    val buffer = ArrayDeque<Float>()

    collect { bytes ->
        val floats = bytes.toNormalizedFloatArray(channels, bytesPerSample)
        for (f in floats) buffer.addLast(f)

        while (buffer.size >= windowSize) {
            val window = FloatArray(windowSize) { i -> buffer[i] }
            emit(window)
            repeat(hopSize) { if (buffer.isNotEmpty()) buffer.removeFirst() }
        }
    }
}

/**
 * Converts a raw byte array representing audio data into a normalized float array of samples.
 *
 * @param channels The number of audio channels in the data (e.g., 1 for mono, 2 for stereo).
 * @param bytesPerSample The number of bytes per sample (1 for 8-bit, 2 for 16-bit).
 * @return A normalized [FloatArray] with values ranging from -1.0 to 1.0.
 */
fun ByteArray.toNormalizedFloatArray(
    channels: Int = 1,
    bytesPerSample: Int = 2
): FloatArray {
    val frames = (this.size / bytesPerSample) / channels
    val result = FloatArray(frames)
    for (i in 0 until frames) {
        val baseIndex = i * channels * bytesPerSample

        var accumulator = 0L
        for (b in 0 until bytesPerSample) {
            accumulator = accumulator or ((this[baseIndex + b].toLong() and 0xFFL) shl (b * 8))
        }

        val bits = bytesPerSample * 8
        result[i] = if (bytesPerSample == 1) {
            (accumulator - 128f) / 128f
        } else {
            val mask = 1L shl (bits - 1)
            val signedValue = if ((accumulator and mask) != 0L) {
                accumulator - (1L shl bits)
            } else {
                accumulator
            }
            (signedValue.toDouble() / mask.toDouble()).toFloat()
        }
    }
    return result
}

/**
 * Calculates the frequency of a musical note represented by a string, given a base frequency and cent offset.
 *
 * @param base The base frequency for reference (e.g., 440.0 for A4).
 * @param cent The fine-tuning adjustment in cents.
 * @return The calculated frequency of the note.
 */
fun String.getNoteFrequency(base: Number = 440, cent: Number = 0) =
    NoteNameCalculator.getFrequency(this, base, cent)

/**
 * Guesses the naming style of the note name string.
 *
 * @return The guessed note naming style.
 */
fun String.guessNoteNameStyle() =
    NoteNameCalculator.guessNoteNameStyle(this)