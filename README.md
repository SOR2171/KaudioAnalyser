# KaudioAnalyser

KaudioAnalyser is a powerful audio analysis tool designed to provide acoustic characteristics
from audio flow in real-time or from audio files.

## Features

### real-time audio analysis

- pitch detection

### file-based audio analysis

## Installation

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.sor2171:kaudioanalyser:1.0.0")
}
```

## Example Usage

### real-time audio analysis

#### pitch detection

```kotlin
// if used in KMP with Kodio
var audioQuality by rememberSaveable { mutableStateOf(AudioQuality.Standard) }
var bufferSize by rememberSaveable { mutableStateOf(4096) }
var detectedFrequency by remember { mutableStateOf(0f) }

val detector = remember(audioQuality, bufferSize) {
    PitchDetector(audioQuality.format.sampleRate, bufferSize)
}

LaunchedEffect(bufferSize) {
    withContext(Dispatchers.Default) {
        val recorder = Kodio.recorder(quality = audioQuality)
        try {
            recorder.start()
            recorder.liveAudioFlow
                ?.toAudioWindows(
                    windowSize = bufferSize,
                    hopSize = bufferSize / 2,
                    channels = audioQuality.format.channels.count
                )
                ?.collect { floatWindow ->
                    val frequency = detector.startDetection(floatWindow)
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
```

### file-based audio analysis

nope