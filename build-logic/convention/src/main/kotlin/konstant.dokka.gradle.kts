// API docs for one library module; the root project merges them into one site (konstant.docs)
plugins {
    id("org.jetbrains.dokka")
}

dokka {
    moduleName.set(project.name)
    dokkaSourceSets.configureEach {
        includes.from("Module.md")
        sourceLink {
            localDirectory.set(projectDir.resolve("src"))
            remoteUrl("https://github.com/fiol-dev/konstant/tree/master/${project.name}/src")
            remoteLineSuffix.set("#L")
        }
    }
}
