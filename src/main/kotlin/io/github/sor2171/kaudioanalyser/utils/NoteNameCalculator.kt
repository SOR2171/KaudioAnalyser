package io.github.sor2171.kaudioanalyser.utils

import io.github.sor2171.kaudioanalyser.entity.NoteNameStyle
import kotlin.math.pow

object NoteNameCalculator {
    /**
     * Calculates the frequency based on the provided note data and the base frequency of A4.
     *
     * This function converts various musical note naming conventions into their corresponding
     * frequencies using the equal temperament tuning system.
     *
     * @param name The note name in one of the supported formats:
     *             - Scientific notation (e.g., "C4", "A#5", "Bb3")
     *             - Helmholtz notation (e.g., "c", "C", "c'", "C''")
     *             - Solfège notation (e.g., "Do", "Re", "Mi", "Fa", "Sol", "La", "Si")
     * @param base The base frequency in Hz (typically 440Hz for A4 in standard tuning)
     * @param cent The cent offset from the note, where 100 cents = 1 semitone.
     *             Used for fine-tuning the frequency. Default is 0.
     * @return The calculated frequency in Hz corresponding to the note.
     * @throws IllegalArgumentException if the note name format is not recognized or unsupported.
     *
     * @example
     * ```
     * val freq1 = getFrequency("A4", 440f)      // Returns 440f
     * val freq2 = getFrequency("C4", 440f)      // Returns ~261.63f (Middle C)
     * val freq3 = getFrequency("A4", 440f, 50f) // Returns ~449.33f (50 cents sharper)
     * ```
     */
    fun getFrequency(
        name: String,
        base: Float,
        cent: Float = 0f
    ): Float {
        var noteIndex = 0
        var octave = 4

        val style = guessNoteNameStyle(name)

        when (style) {
            NoteNameStyle.Scientific -> {
                val splitIndex = name.indexOfFirst { it.isDigit() || it == '-' }
                if (splitIndex != -1) {
                    val notePart = name.substring(0, splitIndex)
                    octave = name.substring(splitIndex).toIntOrNull() ?: 4
                    noteIndex = findNoteIndex(
                        notePart,
                        Const.scientificNamesSharp,
                        Const.scientificNamesFlat
                    )
                }
            }

            NoteNameStyle.Helmholtz -> {
                val isLower = name.isNotEmpty() && name[0].isLowerCase()
                val notePartEnd =
                    name.indexOfFirst { it == ',' || it == '\'' }.let { if (it == -1) name.length else it }

                val notePart = name.substring(0, notePartEnd).replaceFirstChar { it.uppercaseChar() }
                val modifierPart = name.substring(notePartEnd)
                noteIndex = findNoteIndex(
                    notePart,
                    Const.helmholtzNamesSharp,
                    Const.helmholtzNamesFlat
                )

                octave = if (isLower) {
                    3 + modifierPart.length
                } else {
                    2 - modifierPart.length
                }
            }

            NoteNameStyle.Solfege -> {
                noteIndex = findNoteIndex(
                    name,
                    Const.solfegeNamesSharp,
                    Const.solfegeNamesFlat
                )
                octave = 4
            }

            else -> throw IllegalArgumentException("Unsupported style: $name")
        }

        val midiNote = (octave + 1) * 12 + noteIndex
        val midiNoteFloat = midiNote + (cent / 100f)

        val semitonesFromA4 = midiNoteFloat - 69f

        return base * 2f.pow(semitonesFromA4 / 12f)
    }

    /**
     * Determines the musical note naming style of a given note name string.
     *
     * @param name The note name string to analyze.
     * @return The matched NoteNameStyle, or null if the format is unrecognized.
     */
    fun guessNoteNameStyle(name: String): NoteNameStyle? {

        if (Const.solfegePrefixes.any { name.startsWith(it, ignoreCase = true) }) {
            return NoteNameStyle.Solfege
        }

        if (name.lastOrNull()?.isDigit() == true) {
            return NoteNameStyle.Scientific
        }

        if (Regex("^[A-Ga-g][#b]?[,']*$").matches(name)) {
            return NoteNameStyle.Helmholtz
        }

        return null
    }

    private fun findNoteIndex(note: String, sharpArray: Array<String>, flatArray: Array<String>): Int {
        val index = sharpArray.indexOf(note)
        if (index != -1) return index

        val flatIndex = flatArray.indexOf(note)
        return if (flatIndex != -1) flatIndex else 0
    }
}