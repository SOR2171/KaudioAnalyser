package io.github.sor2171.kaudioanalyser

import io.github.sor2171.kaudioanalyser.entity.NoteData
import io.github.sor2171.kaudioanalyser.entity.NoteNameStyle
import io.github.sor2171.kaudioanalyser.service.PitchDetector
import io.github.sor2171.kaudioanalyser.service.SpectrumAnalyzer
import io.github.sor2171.kaudioanalyser.utils.Const.solfegeNamesFlat
import io.github.sor2171.kaudioanalyser.utils.NoteNameCalculator.getFrequency
import io.github.sor2171.kaudioanalyser.utils.NoteNameFormatter.getNoteData
import io.github.sor2171.kaudioanalyser.utils.detectedBy
import io.github.sor2171.kaudioanalyser.utils.getNoteFrequency
import io.github.sor2171.kaudioanalyser.utils.toAudioWindows
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.io.files.Path
import space.kodio.core.AudioRecording
import space.kodio.core.io.files.fromFile
import kotlin.test.Test

class TestMain {
    val bufferSize = 4096
    val audioRecording = AudioRecording.fromFile(Path("src/commonTest/resources/input.wav"))
    val detector = PitchDetector(audioRecording.format.sampleRate, bufferSize)
    val analyzer = SpectrumAnalyzer(bufferSize)

    @Test
    fun main() = runTest {
        basicFunctionsTest()
        filePitchDetect()
        fileSpectrum()
    }

    private fun basicFunctionsTest() {
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
        println(getNoteData(512, 440, NoteNameStyle.Scientific))
        println(getFrequency(note.name!!, 440))
        println("E3".getNoteFrequency())
        println("E3".getNoteFrequency(432))
        println(getFrequency("c'", 440))
        println()
        println(NoteData(name = "B6", a4 = 440f).fillAll())
        println(NoteData(frequency = 1024f, a4 = 440f).fillAll())
        println(NoteData(frequency = 1024f, a4 = 440f, style = NoteNameStyle.Helmholtz).fillAll())
        println()
    }

    private suspend fun filePitchDetect() {
        val result = try {
            audioRecording.asFlow().toAudioWindows(
                windowSize = bufferSize,
                hopSize = bufferSize / 2,
                channels = audioRecording.format.channels.count,
                bytesPerSample = audioRecording.format.bytesPerSample
            ) detectedBy detector
        } catch (e: Exception) {
            println(e.message)
            emptyFlow()
        }

        result.map {
            if (it < 30) "No"
            else getNoteData(it).name
        }.toList().also {
            println(it)
        }
    }

    private suspend fun fileSpectrum() {
        val result = try {
            audioRecording.asFlow().toAudioWindows(
                windowSize = bufferSize,
                hopSize = bufferSize / 2,
                channels = audioRecording.format.channels.count,
                bytesPerSample = audioRecording.format.bytesPerSample
            ) detectedBy analyzer

        } catch (e: Exception) {
            println(e.message)
            emptyFlow()
        }

        result.toList().forEach {
            println(it.contentToString())
        }
    }
}