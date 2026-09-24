package io.github.fiol_dev.konstant.gradle

import org.gradle.testkit.runner.GradleRunner
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertTrue

class KonstantPluginFunctionalTest {

    private val projectDir: File = createTempDirectory("konstant-plugin").toFile()

    @AfterTest
    fun cleanup() {
        projectDir.deleteRecursively()
    }

    private fun setUp(konstantBlock: String) {
        projectDir.resolve("settings.gradle.kts").writeText("rootProject.name = \"sample\"")
        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins { id("io.github.fiol-dev.konstant") }
            konstant {
            $konstantBlock
            }
            """.trimIndent()
        )
        projectDir.resolve("config").mkdirs()
        projectDir.resolve("config/app.toml").writeText("url = \"base\"")
        projectDir.resolve("config/app.prod.toml").writeText("url = \"prod\"")
    }

    private fun runner(vararg args: String) = GradleRunner.create()
        .withProjectDir(projectDir)
        .withPluginClasspath()
        .withArguments(*args)

    private fun generated(): String =
        projectDir.resolve("build/generated/konstant/kotlin/com/example/KonstantBaked.kt").readText()

    @Test
    fun bakesFilesForTheSelectedEnvironment() {
        setUp(
            """
            packageName.set("com.example")
            bake("config/app.toml")
            bake("config/app.{env}.toml", optional = true)
            """
        )

        runner("generateKonstantBaked", "-Pkonstant.env=prod").build()

        val source = generated()
        assertTrue("\"prod\"" in source, source)
        assertTrue(source.indexOf("url = \\\"prod\\\"") < source.indexOf("url = \\\"base\\\""), source)
    }

    @Test
    fun skipsMissingOptionalFileForDefaultEnvironment() {
        setUp(
            """
            packageName.set("com.example")
            bake("config/app.toml")
            bake("config/app.{env}.toml", optional = true)
            """
        )

        runner("generateKonstantBaked").build()

        val source = generated()
        assertTrue("ENVIRONMENT: String = \"dev\"" in source, source)
        assertTrue("url = \\\"base\\\"" in source && "prod" !in source, source)
    }

    @Test
    fun failsWhenARequiredFileIsMissing() {
        setUp(
            """
            packageName.set("com.example")
            bake("config/app.{env}.toml")
            """
        )

        val result = runner("generateKonstantBaked", "-Pkonstant.env=staging").buildAndFail()

        assertTrue("Baked config file not found: config/app.staging.toml" in result.output, result.output)
    }
}
