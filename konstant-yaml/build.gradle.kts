// Full YAML support backed by kaml, as an alternative to the lightweight parser in konstant-sources
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
