// Gradle plugin "io.github.fiol-dev.konstant". A separate included build so the main build's
// own modules (konstant-ksp-tests) can apply it.
plugins {
    `kotlin-dsl`
}

group = libs.versions.libGroup.get()
version = libs.versions.libVersion.get()

dependencies {
    testImplementation(libs.kotlin.test)
    testImplementation(gradleTestKit())
}

gradlePlugin {
    plugins {
        create("konstant") {
            id = "io.github.fiol-dev.konstant"
            displayName = "Konstant"
            description = "Bakes config files into a Kotlin Multiplatform app as Konstant sources"
            implementationClass = "io.github.fiol_dev.konstant.gradle.KonstantPlugin"
        }
    }
}
