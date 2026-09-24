plugins {
    id("konstant.jvm-library")
    id("konstant.publishing")
}

dependencies {
    implementation(project(":konstant-annotations"))
    implementation(project(":konstant-core"))
    implementation(libs.ksp.api)
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinpoet.ksp)
    testImplementation(libs.kotlin.test)
}
