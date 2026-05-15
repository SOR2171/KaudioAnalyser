package io.github.sor2171.kaudioanalyser.utils

import io.github.sor2171.kaudioanalyser.entity.NoteNameStyle

object Const {
    val symbolOfNoteStyle = mapOf(
        NoteNameStyle.Scientific to "A4",
        NoteNameStyle.Helmholtz to "a'",
        NoteNameStyle.Solfege to "La4"
    )

    val noteNamesSharp = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    val noteNamesFlat = arrayOf("C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B")
    val solfegeNames =
        arrayOf("Do", "Do#", "Re", "Re#", "Mi", "Fa", "Fa#", "Sol", "Sol#", "La", "La#", "Si")
}