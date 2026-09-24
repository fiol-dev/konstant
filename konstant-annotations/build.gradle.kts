plugins {
    id("konstant.kmp-library")
    id("konstant.publishing")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(kotlin("stdlib"))
        }
    }
}
