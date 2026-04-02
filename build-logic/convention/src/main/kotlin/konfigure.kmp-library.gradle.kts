import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.kotlin.multiplatform")
}

group = libs.version("libGroup")
version = libs.version("libVersion")

kotlin {
    android {
        namespace = "$group.${project.name.removePrefix("konfigure-")}"
        compileSdk = libs.version("android-compileSdk").toInt()
        minSdk = libs.version("android-minSdk").toInt()
    }
    jvm()
    js { nodejs() }
    linuxX64()
    macosArm64()
    iosArm64()
    iosSimulatorArm64()
    iosX64()
}
