package io.github.sor2171.kaudioanalyser

import io.github.sor2171.kaudioanalyser.entity.NoteData
import io.github.sor2171.kaudioanalyser.entity.NoteNameStyle
import io.github.sor2171.kaudioanalyser.utils.Const.solfegeNamesFlat
import io.github.sor2171.kaudioanalyser.utils.NoteNameCalculator.getFrequency
import io.github.sor2171.kaudioanalyser.utils.NoteNameFormatter.getNoteData

fun main() {
    val note = getNoteData(
        frequency = 580f,
        a4 = 440,
        style = NoteNameStyle.Scientific,
        useSharp = true
    )

    println("Test Main")
    println()
    println(solfegeNamesFlat.contentToString())
    println()
    println(NoteData("A4", 0.6f))
    println(NoteData("A4", -20f))
    println()
    println(note)
    println(
        getNoteData(
            frequency = 2500f,
            a4 = 440,
            style = NoteNameStyle.Helmholtz,
            useSharp = false
        )
    )
    println(
        getNoteData(
            frequency = 2980f,
            a4 = 440,
            style = NoteNameStyle.Solfege,
            useSharp = false
        )
    )
    println()
    println(getFrequency(note.name!!, 440f))
}
