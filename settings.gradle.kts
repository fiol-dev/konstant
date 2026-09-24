@file:Suppress("UnstableApiUsage")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "konstant"
include(":konstant-annotations")
include(":konstant-core")
include(":konstant-ksp")
include(":konstant-sources")
include(":konstant-test")
