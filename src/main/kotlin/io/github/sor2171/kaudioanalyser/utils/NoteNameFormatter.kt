package io.github.sor2171.kaudioanalyser.utils

import io.github.sor2171.kaudioanalyser.entity.NoteData
import io.github.sor2171.kaudioanalyser.entity.NoteNameStyle
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.roundToInt

object NoteNameFormatter {
    fun getNoteData(frequency: Float, base: Float, style: NoteNameStyle): NoteData {
        return when (style) {
            NoteNameStyle.Scientific -> convertToScientific(frequency, base)
            NoteNameStyle.Helmholtz -> convertToHelmholtz(frequency, base)
            NoteNameStyle.Solfege -> convertToSolfege(frequency, base)
        }
    }

    fun getNoteData(frequency: Float, base: Int, style: NoteNameStyle): NoteData {
        return getNoteData(frequency, base.toFloat(), style)
    }

    private fun getCalculatedData(frequency: Float, base: Float): Triple<Int, Int, Int> {
        val semitonesFromA4 = 12 * log2(frequency / base)
        val midiNoteFloat = semitonesFromA4 + 69
        val midiNote = midiNoteFloat.roundToInt()
        val cent = ((midiNoteFloat - midiNote) * 100).roundToInt()

        val noteIndex = (midiNote % 12).let { if (it < 0) it + 12 else it }
        val octave = (midiNote / 12) - 1
        return Triple(noteIndex, octave, cent)
    }

    private fun convertToScientific(frequency: Float, base: Float): NoteData {
        val (noteIndex, octave, cent) = getCalculatedData(frequency, base)
        return NoteData("${Const.noteNamesSharp[noteIndex]}$octave", cent)
    }

    private fun convertToHelmholtz(frequency: Float, base: Float): NoteData {
        val (noteIndex, octave, cent) = getCalculatedData(frequency, base)
        val noteName = Const.noteNamesSharp[noteIndex]

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
        return NoteData(name, cent)
    }

    private fun convertToSolfege(frequency: Float, base: Float): NoteData {
        val (noteIndex, _, cent) = getCalculatedData(frequency, base)
        return NoteData(Const.solfegeNames[noteIndex], cent)
    }
}