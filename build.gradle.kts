@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform") version "2.3.20"
    id("com.vanniktech.maven.publish") version "0.36.0"
}

group = "io.github.sor2171"
version = "1.3.3"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(17)
    jvm()

    iosX64()
    iosArm64()
    iosSimulatorArm64()
    macosArm64()

    js(IR) { browser(); nodejs() }
    wasmJs { browser(); nodejs() }

    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

mavenPublishing {
    pom {
        name.set("KaudioAnalyser")

        description.set(
            "KaudioAnalyser is a powerful audio analysis tool designed to provide acoustic characteristics " +
                    "from audio flow in real-time or from audio files. For more details, please visit the GitHub repository."
        )

        inceptionYear.set("2026")

        url.set("https://github.com/sor2171/KaudioAnalyser")

        licenses {
            license {
                name.set("MIT")
                url.set("https://opensource.org/licenses/MIT")
            }
        }

        developers {
            developer {
                id.set("sor2171")
                name.set("SOR")
                email.set("sor2171@foxmail.com")
            }
        }

        scm {
            connection.set("scm:git:github.com/sor2171/KaudioAnalyser.git")
            developerConnection.set("scm:git:ssh://github.com/sor2171/KaudioAnalyser.git")
            url.set("https://github.com/sor2171/KaudioAnalyser")
        }
    }
}