// End-to-end tests for the KSP processor: real @ConfigSpec classes are processed at build
// time and the generated loaders are exercised at runtime. Not published.
plugins {
    id("konstant.jvm-library")
    id("konstant.ksp")
    id("io.github.fiol-dev.konstant")
}

konstant {
    packageName.set("io.github.fiol_dev.konstant.ksptests.baked")
    bake("baked/app.toml")
    bake("baked/app.{env}.toml", optional = true)
}

dependencies {
    implementation(project(":konstant-annotations"))
    implementation(project(":konstant-core"))
    implementation(project(":konstant-sources"))
    ksp(project(":konstant-ksp"))
    testImplementation(project(":konstant-test"))
    testImplementation(libs.kotlin.test)
}
