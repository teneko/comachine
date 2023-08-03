import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.0"
}

group = "de.halfbit"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        val freeCompilerArgsArray = mutableListOf<String>()

        if (name.endsWith("Test")) {
            freeCompilerArgsArray.add("-opt-in=kotlin.time.ExperimentalTime")
            freeCompilerArgsArray.add("-opt-in=kotlinx.coroutines.DelicateCoroutinesApi")
            freeCompilerArgsArray.add("-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi")
        }

        freeCompilerArgs = freeCompilerArgsArray
        jvmTarget = "17"
        allWarningsAsErrors = false // TODO
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}