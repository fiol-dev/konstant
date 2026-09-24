package io.github.fiol_dev.konstant.ksptests

import io.github.fiol_dev.konstant.core.ConfigError
import io.github.fiol_dev.konstant.core.ConfigLoader
import io.github.fiol_dev.konstant.core.ConfigResult
import io.github.fiol_dev.konstant.core.ConfigSource
import io.github.fiol_dev.konstant.core.InternalKonstantApi
import io.github.fiol_dev.konstant.sources.PropertiesSource
import io.github.fiol_dev.konstant.test.MapSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GeneratedLoaderTest {

    private fun loader(vararg stack: ConfigSource) = ConfigLoader {
        sources { stack.forEach { +it } }
    }

    private fun <T> ConfigResult<T>.errors(): List<ConfigError> =
        assertIs<ConfigResult.Failure>(this).errors

    @Test
    fun loadsEverySupportedPrimitiveType() {
        val config = loader(
            MapSource.screamingSnake(
                "NAME" to "svc",
                "COUNT" to "3",
                "BIG_COUNT" to "9000000000",
                "RATIO" to "0.25",
                "SCALE" to "1.5",
                "ENABLED" to "true",
            )
        ).loadPrimitivesConfig().getOrThrow()

        assertEquals(PrimitivesConfig("svc", 3, 9_000_000_000L, 0.25, 1.5f, true), config)
    }

    @Test
    fun usesSourceDefaultsWhenKeysAreMissing() {
        val config = loader(MapSource.screamingSnake()).loadDefaultsConfig().getOrThrow()

        assertEquals(DefaultsConfig(), config)
    }

    @Test
    fun sourceValuesOverrideDefaults() {
        val config = loader(
            MapSource.screamingSnake("PORT" to "9090", "TIMEOUT_MS" to "5")
        ).loadDefaultsConfig().getOrThrow()

        assertEquals(9090, config.port)
        assertEquals(5L, config.timeoutMs)
        assertEquals("localhost", config.host)
    }

    @Test
    fun firstSourceWins() {
        val config = loader(
            MapSource.screamingSnake("HOST" to "first"),
            MapSource.screamingSnake("HOST" to "second", "PORT" to "1"),
        ).loadDefaultsConfig().getOrThrow()

        assertEquals("first", config.host)
        assertEquals(1, config.port)
    }

    @Test
    fun customKeyOverridesDerivedKey() {
        val config = loader(
            MapSource.screamingSnake("CUSTOM_API_KEY" to "k", "API_KEY" to "wrong", "PASSWORD" to "p")
        ).loadAnnotatedConfig().getOrThrow()

        assertEquals("k", config.apiKey)
    }

    @Test
    fun reportsAllMissingRequiredFieldsAtOnce() {
        val errors = loader(MapSource.screamingSnake()).loadPrimitivesConfig().errors()

        assertEquals(6, errors.size)
        assertTrue(errors.all { it is ConfigError.MissingRequired })
        assertEquals(
            setOf("NAME", "COUNT", "BIG_COUNT", "RATIO", "SCALE", "ENABLED"),
            errors.map { it.key }.toSet(),
        )
    }

    @Test
    fun reportsConversionFailureWithKeyAndType() {
        val errors = loader(
            MapSource.screamingSnake("PORT" to "not-a-number")
        ).loadDefaultsConfig().errors()

        val error = assertIs<ConfigError.ConversionFailed>(errors.single())
        assertEquals("PORT", error.key)
        assertEquals("not-a-number", error.rawValue)
        assertEquals("Int", error.targetType)
    }

    @Test
    fun masksSecretValuesInConversionErrors() {
        val errors = loader(
            MapSource.screamingSnake("CUSTOM_API_KEY" to "k", "PASSWORD" to "p", "PIN" to "12ab")
        ).loadAnnotatedConfig().errors()

        val error = assertIs<ConfigError.ConversionFailed>(errors.single())
        assertEquals("***", error.rawValue)
        assertFalse("12ab" in error.message)
    }

    @Test
    fun resolvesNestedConfigWithPrefix() {
        val config = loader(
            MapSource.screamingSnake("DATABASE_URL" to "jdbc:x", "DATABASE_MAX_POOL_SIZE" to "4")
        ).loadAppConfig().getOrThrow()

        assertEquals(AppConfig("MyApp", DatabaseConfig("jdbc:x", 4)), config)
    }

    @Test
    fun resolvesNestedConfigFromDotNotationSource() {
        val source = PropertiesSource.fromString(
            """
            app.name=Demo
            database.url=jdbc:y
            database.max.pool.size=7
            """.trimIndent()
        )

        val config = loader(source).loadAppConfig().getOrThrow()

        assertEquals(AppConfig("Demo", DatabaseConfig("jdbc:y", 7)), config)
    }

    @Test
    fun aggregatesErrorsFromNestedConfigs() {
        val errors = loader(MapSource.screamingSnake()).loadAppConfig().errors()

        assertEquals(listOf("DATABASE_URL"), errors.map { it.key })
    }

    @OptIn(InternalKonstantApi::class)
    @Test
    fun generatesSchemaDescriptors() {
        assertFalse(DatabaseConfigSchema.url.hasDefault)
        assertTrue(DatabaseConfigSchema.maxPoolSize.hasDefault)
        assertEquals(10, DatabaseConfigSchema.maxPoolSize.default)
        assertTrue(AnnotatedConfigSchema.password.secret)
        assertEquals("CUSTOM_API_KEY", AnnotatedConfigSchema.apiKey.customKey)
    }
}
