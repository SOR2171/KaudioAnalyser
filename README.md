# KaudioAnalyser

## Overview

KaudioAnalyser is a powerful audio analysis tool designed to provide acoustic characteristics
from audio flow in real-time or from audio files.

This library can be used in KMP projects, and it works well with
[Kodio](https://github.com/dosier/kodio).
This lib support to get `Flow<Byte>` from microphone and WAV file.

If you want to analyze an audio file,
I recommend you use [FFmpegKit](https://github.com/arthenica/ffmpeg-kit),
even though it has been retired,
because it is still the easiest way to use FFmpeg in KMP.

If you want to contribute or have any questions, please feel free to open an issue or a pull request.

If you want to view a demo project using KaudioAnalyser, you can check out the
[Audio-Detection-Tool](https://github.com/SOR2171/Audio-Detection-Tool)

## Features

### utils

- format note name with note frequency and that of A4.
- Conversion between sound frequency and note name.

### audio analysis

- pitch detection based on FFT or YIN algorithm
- spectrum analysis

## Installation

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.sor2171:kaudioanalyser:1.4.2")
}
```

## How to use

### utils

#### frequency to NoteData

```kotlin
println(getNoteData(512, 440, NoteNameStyle.Scientific))
// NoteData(name=C5, cent=-37.63199, frequency=512.0, a4=440.0, style=Scientific)
```

#### note name to frequency

```kotlin
val frequency1 = getFrequency("D5", 440f) // 587.3295f
val frequency2 = "E3".getNoteFrequency() // 164.81378f
val frequency3 = "E3".getNoteFrequency(432) // 161.81717f
```

#### note name or frequency to NoteData

```kotlin
println(NoteData(name = "B6", a4 = 440f).fillAll())
// NoteData(name=B6, cent=0.0, frequency=1975.5334, a4=440.0, style=Scientific)

println(NoteData(frequency = 1024f, a4 = 440f).fillAll())
// NoteData(name=C6, cent=-37.63199, frequency=1024.0, a4=440.0, style=Scientific)

println(NoteData(frequency = 1024f, a4 = 440f, style = NoteNameStyle.Helmholtz).fillAll())
// NoteData(name=c''', cent=-37.63199, frequency=1024.0, a4=440.0, style=Helmholtz)
```

### Audio Analyze

#### pitch detection

```kotlin
// if used in KMP with Kodio
var audioQuality by rememberSaveable { mutableStateOf(AudioQuality.Standard) }
var bufferSize by rememberSaveable { mutableStateOf(2048) }
// for YIN algorithm, this buffer size could be enough
var detectedFrequency by remember { mutableStateOf(0f) }

val detector = remember(audioQuality, bufferSize) {
    PitchDetector(audioQuality.format.sampleRate, bufferSize)
}
LaunchedEffect(
    isRecording,
    isPausing,
    audioQuality,
    bufferSize
) {
    if (isRecording && !isPausing) {
        withContext(Dispatchers.Default) {
            val recorder = Kodio.recorder(quality = audioQuality)
            try {
                delay(500) // wait for the recorder to be released
                recorder.start()
                recorder.liveAudioFlow?.toAudioWindows(
                    windowSize = bufferSize,
                    hopSize = bufferSize / 2,
                    channels = audioQuality.format.channels.count,
                    bytesPerSample = audioQuality.format.bytesPerSample
                )?.collect { floatWindow ->

                    val frequency = detector.detectPitchYIN(floatWindow)
                    // or you can use FFT-based pitch detection, like this:
                    // val frequency = detector.detectPitch(floatWindow)
                    // it needs larger buffer size to be accurate

                    withContext(Dispatchers.Main) {
                        // stable the frequency by averaging with previous value
                        detectedFrequency = if (frequency > 0f) {
                            if (detectedFrequency == 0f) frequency
                            else detectedFrequency * 0.5f + frequency * 0.5f
                        } else {
                            0f
                        }
                    }
                }
            } finally {
                recorder.release()
            }
        }
    }
}
```

#### spectrum analysis

```kotlin
// also, used in KMP with Kodio
var audioQuality by rememberSaveable { mutableStateOf(AudioQuality.Standard) }
var bufferSize by rememberSaveable { mutableStateOf(8192) }
val analyzer = remember(bufferSize) { SpectrumAnalyzer(bufferSize) }
// spectrumData is a list of pairs of frequency and magnitude,
// you can use it to visualize the frequency spectrum.
var spectrumData by remember { mutableStateOf(FloatArray(0)) }

LaunchedEffect(
    isRecording,
    isPausing,
    audioQuality,
    bufferSize
) {
    if (isRecording && !isPausing) {
        withContext(Dispatchers.Default) {
            val recorder = Kodio.recorder(quality = audioQuality)
            try {
                delay(500) // wait for the recorder to be released
                recorder.start()
                recorder.liveAudioFlow?.toAudioWindows(
                    windowSize = bufferSize,
                    hopSize = bufferSize / 2,
                    channels = audioQuality.format.channels.count,
                    bytesPerSample = audioQuality.format.bytesPerSample
                )?.collect { floatWindow ->
                    spectrumData = analyzer
                        .processAudioWindow(floatWindow)
                }
            } finally {
                recorder.release()
            }
        }
    }
}
```

### Audio File Analyze

#### pitch detection

#### spectrum analysis