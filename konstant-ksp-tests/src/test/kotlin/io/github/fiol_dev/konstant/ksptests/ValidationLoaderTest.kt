package io.github.fiol_dev.konstant.ksptests

import io.github.fiol_dev.konstant.core.ConfigError
import io.github.fiol_dev.konstant.core.ConfigLoader
import io.github.fiol_dev.konstant.core.ConfigResult
import io.github.fiol_dev.konstant.test.MapSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class ValidationLoaderTest {

    private fun load(vararg values: Pair<String, String>) =
        ConfigLoader { sources { +MapSource.screamingSnake(*values) } }.loadValidatedConfig()

    private fun failure(vararg values: Pair<String, String>): ConfigError.ValidationFailed {
        val errors = assertIs<ConfigResult.Failure>(load(*values)).errors
        return assertIs<ConfigError.ValidationFailed>(errors.single())
    }

    @Test
    fun acceptsValidValuesAndDefaults() {
        val config = load("PORT" to "443", "UPSTREAM" to "db:5432").getOrThrow()

        assertEquals(443, config.port)
        assertEquals(HostPort("db", 5432), config.upstream)
        assertNull(config.backup)
    }

    @Test
    fun rangeRejectsOutOfBoundsNumbers() {
        val error = failure("PORT" to "70000")
        assertEquals("PORT", error.key)
        assertEquals("70000", error.rawValue)
        assertEquals("must be between 1 and 65535", error.reason)

        assertEquals("must be at least 0", failure("RATIO" to "-1").reason)
    }

    @Test
    fun stringChecks() {
        assertEquals("must not be blank", failure("NAME" to "   ").reason)
        assertEquals("size must be at most 10", failure("NAME" to "a-very-long-name").reason)
        assertEquals("must match [a-z]+-\\d+", failure("CODE" to "AB-1").reason)
    }

    @Test
    fun sizeAppliesToCollections() {
        assertEquals("size must be at least 1", failure("HOSTS" to "").reason)
    }

    @Test
    fun validationMasksSecrets() {
        val error = failure("TOKEN" to "short")
        assertEquals("***", error.rawValue)
        assertEquals("size must be at least 8", error.reason)
    }

    @Test
    fun customConverterReportsConversionErrors() {
        val errors = assertIs<ConfigResult.Failure>(load("BACKUP" to "nohost")).errors
        val error = assertIs<ConfigError.ConversionFailed>(errors.single())
        assertEquals("HostPort", error.targetType)
        assertEquals("expected host:port", error.cause)
    }

    @Test
    fun nullableCustomConverterField() {
        assertEquals(HostPort("b", 1), load("BACKUP" to "b:1").getOrThrow().backup)
    }

    @Test
    fun initBlockRequireBecomesValidationError() {
        val error = failure("MIN_WORKERS" to "5")
        assertEquals("ValidatedConfig", error.key)
        assertNull(error.rawValue)
        assertEquals("minWorkers must not exceed maxWorkers", error.reason)
    }

    @Test
    fun reportsAllFieldProblemsAtOnce() {
        val errors = assertIs<ConfigResult.Failure>(
            load("PORT" to "0", "NAME" to "", "HOSTS" to "", "BACKUP" to "x")
        ).errors

        assertEquals(listOf("PORT", "NAME", "HOSTS", "BACKUP"), errors.map { it.key })
    }

    @Test
    fun floatRangeIncludesItsExactBound() {
        assertEquals(0.1f, load("JITTER" to "0.1").getOrThrow().jitter)
        assertEquals("must be at most 0.1", failure("JITTER" to "0.11").reason)
    }

    @Test
    fun rangeRejectsNaN() {
        assertEquals("must be a number", failure("RATIO" to "NaN").reason)
    }

    @Test
    fun validationAppliesToConvertedFields() {
        assertEquals(75, load("SHARE" to "75%").getOrThrow().share)
        assertEquals("must be at most 100", failure("SHARE" to "120%").reason)
    }
}
