plugins {
    id("konfigure.kmp-library")
    id("konfigure.publishing")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":konfigure-annotations"))
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
