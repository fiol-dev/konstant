package io.github.fiol_dev.konstant.gradle

import org.gradle.api.GradleException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BakedSourceWriterTest {

    @Test
    fun listsLaterFilesFirstWithTheRightSourceType() {
        val source = BakedSourceWriter.render(
            "com.example", "Baked", "prod",
            listOf(
                BakedSourceWriter.Input("config/app.toml", "a = 1"),
                BakedSourceWriter.Input("config/app.prod.yaml", "a: 2"),
                BakedSourceWriter.Input(".env", "A=3"),
            ),
        )

        assertTrue("package com.example" in source)
        assertTrue("public const val ENVIRONMENT: String = \"prod\"" in source)
        val env = source.indexOf("DotEnvSource.fromString")
        val yaml = source.indexOf("YamlSource.fromString")
        val toml = source.indexOf("TomlSource.fromString")
        assertTrue(env in 0 until yaml && yaml < toml, source)
        assertTrue("import io.github.fiol_dev.konstant.sources.source.PropertiesSource" !in source)
    }

    @Test
    fun escapesContentIntoValidKotlinStrings() {
        assertEquals(
            "\"a \\\"quoted\\\" \\$HOME \\\\n\\nnext\"",
            BakedSourceWriter.kotlinString("a \"quoted\" \$HOME \\n\nnext"),
        )
    }

    @Test
    fun splitsLargeFilesIntoSeveralLiterals() {
        val big = "x".repeat(20_000)
        val source = BakedSourceWriter.render("p", "B", "dev", listOf(BakedSourceWriter.Input("big.properties", big)))
        assertEquals(3, Regex("\"x+\"").findAll(source).count())
    }

    @Test
    fun rejectsUnknownFileTypes() {
        assertFailsWith<GradleException> {
            BakedSourceWriter.render("p", "B", "dev", listOf(BakedSourceWriter.Input("config.json", "{}")))
        }
    }
}
