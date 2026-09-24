package io.github.fiol_dev.konstant.gradle

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet

class KonstantPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("konstant", KonstantExtension::class.java)
        extension.objectName.convention("KonstantBaked")
        extension.environment.convention(
            project.providers.gradleProperty("konstant.env").orElse("dev")
        )

        val generate = project.tasks.register("generateKonstantBaked", GenerateBakedConfigTask::class.java) {
            group = "konstant"
            description = "Generates the Konstant object holding baked config files."
            packageName.set(extension.packageName)
            objectName.set(extension.objectName)
            environment.set(extension.environment)
            bakedFiles.set(extension.bakedFiles)
            projectDirectory.set(project.layout.projectDirectory)
            val projectDir = project.layout.projectDirectory
            inputFiles.from(
                extension.bakedFiles.zip(extension.environment) { files, env ->
                    files.map { projectDir.file(BakedFile.decode(it).path.replace("{env}", env)).asFile }
                        .filter { it.exists() }
                }
            )
            outputDirectory.set(project.layout.buildDirectory.dir("generated/konstant/kotlin"))
        }
        val outputDir = generate.flatMap { it.outputDirectory }
        // KSP reads every Kotlin source, including the generated object
        project.tasks.configureEach {
            if (name.startsWith("ksp") && name != generate.name) dependsOn(generate)
        }

        project.pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            addKotlinSourceDir(project, "commonMain", outputDir)
        }
        project.pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
            addKotlinSourceDir(project, "main", outputDir)
        }
        project.pluginManager.withPlugin("org.jetbrains.kotlin.android") {
            addKotlinSourceDir(project, "main", outputDir)
        }
    }

    /**
     * Adds [dir] to a Kotlin source set. Uses reflection on the `kotlin` extension so the
     * plugin doesn't depend on a particular Kotlin Gradle plugin version or classloader.
     */
    private fun addKotlinSourceDir(project: Project, sourceSetName: String, dir: Any) {
        val kotlin = project.extensions.getByName("kotlin")
        val sourceSets = kotlin.javaClass.getMethod("getSourceSets").invoke(kotlin) as NamedDomainObjectContainer<*>
        val sourceSet = sourceSets.getByName(sourceSetName)
        val sources = sourceSet.javaClass.getMethod("getKotlin").invoke(sourceSet) as SourceDirectorySet
        sources.srcDir(dir)
    }
}
