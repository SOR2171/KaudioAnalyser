package io.github.sor2171.kaudioanalyser.entity

import io.github.sor2171.kaudioanalyser.utils.NoteNameCalculator.getFrequency
import io.github.sor2171.kaudioanalyser.utils.NoteNameCalculator.guessNoteNameStyle
import io.github.sor2171.kaudioanalyser.utils.NoteNameFormatter.getNoteData

/**
 * Represents the musical note data, including its name, cent deviation, frequency, reference A4 frequency, and style.
 *
 * @property name The name of the note (e.g., "A4", "C#5").
 * @property cent The cent deviation from the exact pitch frequency.
 * @property frequency The actual frequency of the note.
 * @property a4 The reference frequency of A4 (defaults to 440f).
 * @property style The naming style of the note (e.g., Scientific, Helmholtz, Solfège).
 */
data class NoteData(
    val name: String? = null,
    val cent: Float = 0f,
    val frequency: Float? = null,
    val a4: Float = 440f,
    val style: NoteNameStyle? = null,
) {
    /**
     * Fills in any missing note properties (e.g., note name from frequency, or frequency from note name)
     * based on the available fields.
     *
     * @param useSharp Whether to prefer sharp accidentals (#) over flat accidentals (b) when calculating the name.
     * @return A new [NoteData] instance with all fields populated.
     * @throws IllegalArgumentException If both [frequency] and [name] are null.
     */
    fun fillAll(useSharp: Boolean = true): NoteData {
        var copy = this

        if (frequency != null && name == null) {
            copy = getNoteData(frequency, a4, style ?: NoteNameStyle.Scientific, useSharp)
        } else if ((frequency == null || style == null) && name != null) {
            copy = copy(
                name = name,
                cent = cent,
                style = style ?: guessNoteNameStyle(name),
                frequency = frequency ?: getFrequency(name, a4),
                a4 = a4
            )
        } else if (frequency == null) {
            throw IllegalArgumentException("Frequency and note name cannot be null in the same time.")
        }

        return copy
    }
}