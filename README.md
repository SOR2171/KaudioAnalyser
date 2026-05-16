# KaudioAnalyser

KaudioAnalyser is a powerful audio analysis tool designed to provide acoustic characteristics
from audio flow in real-time or from audio files.

This library can be used in KMP projects, and it collaborates well with [Kodio](https://github.com/dosier/kodio).

## Features

### real-time audio analysis

- pitch detection based on FFT or YIN algorithm

### file-based audio analysis

## Installation

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.sor2171:kaudioanalyser:1.1.0")
}
```

## How to use

### real-time audio analysis

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

### file-based audio analysis

nope