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

rootProject.name = "konfigure"
include(":konfigure-annotations")
include(":konfigure-core")
include(":konfigure-ksp")
include(":konfigure-sources")
include(":konfigure-test")
