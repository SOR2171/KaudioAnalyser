package io.github.sor2171.kaudioanalyser.utils

import io.github.sor2171.kaudioanalyser.entity.NoteData
import io.github.sor2171.kaudioanalyser.entity.NoteNameStyle
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Formatter object to convert audio frequencies into specific musical note representations
 * based on standard musical naming styles.
 */
object NoteNameFormatter {
    /**
     * Converts a given frequency into formatted note data based on the specified musical naming style.
     * 
     * @param frequency The frequency to be converted.
     * @param a4 The base reference frequency of A4 (e.g., 440.0).
     * @param style The musical note naming style to apply (Scientific, Helmholtz, or Solfège).
     * @param useSharp Indicates whether to prefer sharp (#) accidentals over flats (b). Defaults to true.
     * @return A NoteData object containing the formatted note name, cent deviation as a Float, and the applied style.
     */
    fun getNoteData(
        frequency: Number,
        a4: Number,
        style: NoteNameStyle,
        useSharp: Boolean = true
    ): NoteData {
        val frequencyFloat = frequency.toFloat()
        val baseFloat = a4.toFloat()

        return when (style) {
            NoteNameStyle.Scientific -> convertToScientific(frequencyFloat, baseFloat, useSharp)
            NoteNameStyle.Helmholtz -> convertToHelmholtz(frequencyFloat, baseFloat, useSharp)
            NoteNameStyle.Solfege -> convertToSolfege(frequencyFloat, baseFloat, useSharp)
        }
    }

    /**
     * Calculates the intermediate mathematical details for a frequency relative to a base frequency.
     *
     * @param frequency The frequency to calculate for.
     * @param base The reference base frequency.
     * @return A [Triple] containing:
     * - The note index (0 to 11, representing C to B).
     * - The octave index.
     * - The cent deviation from the nearest exact pitch.
     */
    private fun getCalculatedData(frequency: Float, base: Float): Triple<Int, Int, Float> {
        val semitonesFromA4 = 12 * log2(frequency / base)
        val midiNoteFloat = semitonesFromA4 + 69
        val midiNote = midiNoteFloat.roundToInt()
        val cent = (midiNoteFloat - midiNote) * 100

        val noteIndex = (midiNote % 12).let { if (it < 0) it + 12 else it }
        val octave = (midiNote / 12) - 1
        return Triple(noteIndex, octave, cent)
    }

    /**
     * Converts the frequency to Scientific Pitch Notation (e.g., C4, F#5).
     *
     * @param frequency The frequency to convert.
     * @param base The reference base frequency.
     * @param useSharp Whether to prefer sharp (#) or flat (b) accidentals.
     * @return A [NoteData] populated with scientific notation details.
     */
    private fun convertToScientific(frequency: Float, base: Float, useSharp: Boolean): NoteData {
        val (noteIndex, octave, cent) = getCalculatedData(frequency, base)
        val name = if (useSharp) Const.scientificNamesSharp[noteIndex] else Const.scientificNamesFlat[noteIndex]
        return NoteData("$name$octave", cent, frequency, base, NoteNameStyle.Scientific)
    }

    /**
     * Converts the frequency to Helmholtz Pitch Notation (e.g., C, C c c' c'').
     *
     * @param frequency The frequency to convert.
     * @param base The reference base frequency.
     * @param useSharp Whether to prefer sharp (#) or flat (b) accidentals.
     * @return A [NoteData] populated with Helmholtz notation details.
     */
    private fun convertToHelmholtz(frequency: Float, base: Float, useSharp: Boolean): NoteData {
        val (noteIndex, octave, cent) = getCalculatedData(frequency, base)
        val noteName = if (useSharp) Const.helmholtzNamesSharp[noteIndex] else Const.helmholtzNamesFlat[noteIndex]

        val name = when {
            octave < 3 -> {
                val commas = ",".repeat(max(0, 2 - octave))
                "${noteName.uppercase()}$commas"
            }

            octave == 3 -> noteName.lowercase()
            else -> {
                val primes = "'".repeat(octave - 3)
                "${noteName.lowercase()}$primes"
            }
        }
        return NoteData(name, cent, frequency, base, NoteNameStyle.Helmholtz)
    }

    /**
     * Converts the frequency to Solfège Notation (e.g., Do, Ré, Mi).
     *
     * @param frequency The frequency to convert.
     * @param base The reference base frequency.
     * @param useSharp Whether to prefer sharp (#) or flat (b) accidentals.
     * @return A [NoteData] populated with Solfège notation details.
     */
    private fun convertToSolfege(frequency: Float, base: Float, useSharp: Boolean): NoteData {
        val (noteIndex, _, cent) = getCalculatedData(frequency, base)
        val name = if (useSharp) Const.solfegeNamesSharp[noteIndex] else Const.solfegeNamesFlat[noteIndex]
        return NoteData(name, cent, frequency, base, NoteNameStyle.Solfege)
    }
}