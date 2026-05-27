package io.github.sor2171.kaudioanalyser.entity

enum class NoteNameStyle(
    val a4: String
) {
    Scientific(
        a4 = "A4"
    ),

    Helmholtz(
        a4 = "a'"
    ),

    Solfege(
        a4 = "La"
    )
}