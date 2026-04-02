plugins {
    id("konfigure.kmp-library")
    id("konfigure.publishing")
}

kotlin {
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
