plugins {
    kotlin("jvm") version "2.3.20"
    id("com.vanniktech.maven.publish") version "0.36.0"
}

group = "io.github.sor2171"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}

mavenPublishing {
    pom {
        name.set("KaudioAnalyser")

        description.set(
            "KaudioAnalyser is a powerful audio analysis tool designed to provide acoustic characteristics " +
                    "from audio flow in real-time or from audio files."
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