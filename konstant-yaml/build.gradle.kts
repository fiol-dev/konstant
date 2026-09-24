// Full YAML 1.2 support backed by kaml
plugins {
    id("konstant.kmp-library")
    id("konstant.publishing")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":konstant-core"))
            implementation(project(":konstant-sources"))
            implementation(libs.kaml)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
