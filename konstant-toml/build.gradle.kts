// Full TOML 1.0 support backed by ktoml, as an alternative to the lightweight parser in konstant-sources
plugins {
    id("konstant.kmp-library")
    id("konstant.publishing")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":konstant-core"))
            implementation(project(":konstant-sources"))
            implementation(libs.ktoml.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
