plugins {
    id("konfigure.jvm-library")
    id("konfigure.publishing")
}

dependencies {
    implementation(project(":konfigure-annotations"))
    implementation(project(":konfigure-core"))
    implementation(libs.ksp.api)
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinpoet.ksp)
    testImplementation(libs.kotlin.test)
}
