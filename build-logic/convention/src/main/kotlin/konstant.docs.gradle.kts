// Merged API docs for every published library module: ./gradlew dokkaGenerate writes build/dokka/html
plugins {
    id("org.jetbrains.dokka")
}

dokka {
    moduleName.set("Konstant")
}

dependencies {
    listOf(
        "konstant-annotations",
        "konstant-core",
        "konstant-sources",
        "konstant-json",
        "konstant-toml",
        "konstant-yaml",
        "konstant-koin",
        "konstant-reload",
        "konstant-test",
    ).forEach { "dokka"(project(":$it")) }
}
