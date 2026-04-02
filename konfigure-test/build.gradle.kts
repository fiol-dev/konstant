import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.vanniktech.mavenPublish)
}

group = libs.versions.libGroup.get()
version = libs.versions.libVersion.get()

kotlin {
    android {
        namespace = "$group.test"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilations.configureEach {
            compileTaskProvider.configure {
                compilerOptions.jvmTarget = JvmTarget.JVM_11
            }
        }
    }
    jvm()
    js { nodejs() }
    linuxX64()
    macosArm64()
    iosArm64()
    iosSimulatorArm64()
    iosX64()

    sourceSets {
        commonMain.dependencies {
            api(project(":konfigure-core"))
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(project(":konfigure-sources"))
        }
    }
}

mavenPublishing {
    publishToMavenCentral()

    signAllPublications()

    coordinates(
        groupId = group.toString(),
        artifactId = "konfigure-test",
        version = version.toString()
    )

    pom {
        name = "Konfigure Library"
        description = "Pydantic Settings but for KMP"
        inceptionYear = "2026"
        url = "https://github.com/fiol-dev/kotlin-multiplatform-settings"
        licenses {
            license {
                name = "MIT"
                url = "https://opensource.org/licenses/MIT"
            }
        }
        developers {
            developer {
                id = "fiol-dev"
                name = "fiol-dev"
                url = "https://github.com/fiol-dev"
                email = "fiolmailosu@gmail.com"
            }
        }
        scm {
            url = "https://github.com/fiol-dev/kotlin-multiplatform-settings"
            connection = "scm:git:https://github.com/fiol-dev/kotlin-multiplatform-settings.git"
            developerConnection = "scm:git:ssh://git@github.com/fiol-dev/kotlin-multiplatform-settings.git"
        }
    }
}
