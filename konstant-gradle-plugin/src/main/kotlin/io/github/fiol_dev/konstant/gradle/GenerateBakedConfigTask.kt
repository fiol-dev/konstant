package io.github.fiol_dev.konstant.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class GenerateBakedConfigTask : DefaultTask() {
    @get:Input
    @get:Optional
    abstract val packageName: Property<String>

    @get:Input
    abstract val objectName: Property<String>

    @get:Input
    abstract val environment: Property<String>

    @get:Input
    abstract val bakedFiles: ListProperty<String>

    @get:Internal
    abstract val projectDirectory: DirectoryProperty

    /** The existing baked files, so edits to them re-run the task. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputFiles: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val outDir = outputDirectory.get().asFile
        outDir.deleteRecursively()
        val specs = bakedFiles.get().map(BakedFile::decode)
        if (specs.isEmpty()) return

        val pkg = packageName.orNull
            ?: throw GradleException("konstant.packageName must be set when files are baked")
        val env = environment.get()
        val baseDir = projectDirectory.get().asFile

        val baked = specs.mapNotNull { spec ->
            val relative = spec.path.replace("{env}", env)
            val file = baseDir.resolve(relative)
            when {
                file.isFile -> BakedSourceWriter.Input(relative, file.readText())
                spec.optional -> null
                else -> throw GradleException("Baked config file not found: $relative (environment '$env')")
            }
        }

        val source = BakedSourceWriter.render(pkg, objectName.get(), env, baked)
        val target = outDir.resolve(pkg.replace('.', '/')).resolve("${objectName.get()}.kt")
        target.parentFile.mkdirs()
        target.writeText(source)
    }
}
