plugins {
    id("konstant.kmp-library")
    id("konstant.publishing")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":konstant-annotations"))
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
