// End-to-end tests for the KSP processor: real @ConfigSpec classes are processed at build
// time and the generated loaders are exercised at runtime. Not published.
plugins {
    id("konstant.jvm-library")
    id("konstant.ksp")
}

dependencies {
    implementation(project(":konstant-annotations"))
    implementation(project(":konstant-core"))
    ksp(project(":konstant-ksp"))
    testImplementation(project(":konstant-sources"))
    testImplementation(project(":konstant-test"))
    testImplementation(libs.kotlin.test)
}
