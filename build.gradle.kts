import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.21"
    `maven-publish`
}

group = "de.halfbit"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation(kotlin("test"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        val freeCompilerArgsArray = mutableListOf("-Xcontext-receivers")

        if (name.endsWith("Test")) {
            freeCompilerArgsArray.addAll(
                arrayOf(
                    "-opt-in=kotlin.time.ExperimentalTime",
                    "-opt-in=kotlinx.coroutines.DelicateCoroutinesApi",
                    "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
                )
            )
        }

        freeCompilerArgs = freeCompilerArgsArray
        jvmTarget = "17"
        allWarningsAsErrors = false // TODO
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

publishing {
    publications {
        register("mavenJava", MavenPublication::class) {
            from(components["java"])
            artifact(sourcesJar.get())
        }
    }
}