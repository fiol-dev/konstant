plugins {
    id("konfigure.kmp-library")
    id("konfigure.publishing")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(kotlin("stdlib"))
        }
    }
}
