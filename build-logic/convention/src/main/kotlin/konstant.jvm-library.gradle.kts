import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.jvm")
}

group = libs.version("libGroup")
version = libs.version("libVersion")

// Pinned so a build on a newer JDK still produces jars that load on older runtimes
val jvmTargetVersion = libs.version("jvmTarget")

java {
    sourceCompatibility = JavaVersion.toVersion(jvmTargetVersion)
    targetCompatibility = JavaVersion.toVersion(jvmTargetVersion)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(jvmTargetVersion))
        freeCompilerArgs.add("-Xjdk-release=$jvmTargetVersion")
    }
}
