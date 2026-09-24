@file:Suppress("UnstableApiUsage")

pluginManagement {
    includeBuild("build-logic")
    includeBuild("konstant-gradle-plugin")
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
include(":konstant-ksp-tests")
include(":konstant-sources")
include(":konstant-koin")
include(":konstant-test")
include(":konstant-toml")
include(":konstant-yaml")
include(":konstant-json")
