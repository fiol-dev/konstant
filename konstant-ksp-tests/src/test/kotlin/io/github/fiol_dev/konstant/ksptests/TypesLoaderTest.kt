package io.github.fiol_dev.konstant.ksptests

import io.github.fiol_dev.konstant.core.ConfigError
import io.github.fiol_dev.konstant.core.ConfigLoader
import io.github.fiol_dev.konstant.core.ConfigResult
import io.github.fiol_dev.konstant.core.ConfigSource
import io.github.fiol_dev.konstant.ksptests.other.CacheConfig
import io.github.fiol_dev.konstant.ksptests.other.Mode
import io.github.fiol_dev.konstant.sources.source.PropertiesSource
import io.github.fiol_dev.konstant.sources.source.TomlSource
import io.github.fiol_dev.konstant.test.MapSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class TypesLoaderTest {

    private fun loader(vararg stack: ConfigSource) = ConfigLoader {
        sources { stack.forEach { +it } }
    }

    private val required = arrayOf(
        "LEVEL" to "warn-only",
        "TIMEOUT" to "1m 30s",
        "HOSTS" to "a.example, b.example",
        "PORTS" to "80, 443, 80",
        "WEIGHTS" to "a=0.5, b=1",
    )

    @Test
    fun loadsEnumsDurationsAndCollections() {
        val config = loader(MapSource.screamingSnake(*required)).loadTypesConfig().getOrThrow()

        assertEquals(LogLevel.WARN_ONLY, config.level)
        assertEquals(1.minutes + 30.seconds, config.timeout)
        assertEquals(listOf("a.example", "b.example"), config.hosts)
        assertEquals(setOf(80, 443), config.ports)
        assertEquals(mapOf("a" to 0.5, "b" to 1.0), config.weights)
    }

    @Test
    fun appliesDefaultsAndNullsForOptionalFields() {
        val config = loader(MapSource.screamingSnake(*required)).loadTypesConfig().getOrThrow()

        assertEquals(emptyList(), config.levels)
        assertNull(config.nickname)
        assertEquals(3, config.retries)
        assertEquals(LogLevel.INFO, config.fallbackLevel)
        assertEquals(5.seconds, config.grace)
    }

    @Test
    fun readsNullableAndListOfEnumValues() {
        val config = loader(
            MapSource.screamingSnake(
                *required,
                "NICKNAME" to "nick",
                "RETRIES" to "0",
                "LEVELS" to "debug,INFO",
                "GRACE" to "500ms",
            )
        ).loadTypesConfig().getOrThrow()

        assertEquals("nick", config.nickname)
        assertEquals(0, config.retries)
        assertEquals(listOf(LogLevel.DEBUG, LogLevel.INFO), config.levels)
        assertEquals(500.milliseconds, config.grace)
    }

    @Test
    fun readsTomlInlineArrays() {
        val source = TomlSource.fromString(
            """
            level = "info"
            timeout = "PT10S"
            hosts = ["x", "y"]
            ports = [1, 2]
            weights = "k=2"
            """.trimIndent()
        )

        val config = loader(source).loadTypesConfig().getOrThrow()

        assertEquals(listOf("x", "y"), config.hosts)
        assertEquals(setOf(1, 2), config.ports)
        assertEquals(10.seconds, config.timeout)
        assertEquals(mapOf("k" to 2.0), config.weights)
    }

    @Test
    fun reportsInvalidEnumWithAllowedValues() {
        val result = loader(
            MapSource.screamingSnake(*required, "LEVEL" to "loud")
        ).loadTypesConfig()

        val error = assertIs<ConfigError.ConversionFailed>(assertIs<ConfigResult.Failure>(result).errors.single())
        assertEquals("LEVEL", error.key)
        assertEquals("LogLevel", error.targetType)
        assertTrue("DEBUG, INFO, WARN_ONLY" in error.cause)
    }

    @Test
    fun reportsEveryBadElementTypeAtOnce() {
        val result = loader(
            MapSource.screamingSnake(*required, "PORTS" to "80, http", "TIMEOUT" to "soon")
        ).loadTypesConfig()

        val errors = assertIs<ConfigResult.Failure>(result).errors
        assertEquals(setOf("PORTS", "TIMEOUT"), errors.map { it.key }.toSet())
        assertEquals("Set<Int>", (errors.first { it.key == "PORTS" } as ConfigError.ConversionFailed).targetType)
    }

    @Test
    fun loadsNestedSpecAndDefaultsFromAnotherPackage() {
        val config = loader(MapSource.screamingSnake("CACHE_SIZE" to "5")).loadServiceConfig().getOrThrow()

        assertEquals(ServiceConfig("svc", Mode.FIFO, CacheConfig(5, Mode.LRU)), config)
    }

    @Test
    fun resolvesDoublyNestedDotNotationKeys() {
        val source = PropertiesSource.fromString(
            """
            database.url=jdbc:z
            database.pool.size=5
            """.trimIndent()
        )

        val config = loader(source).loadAppConfig().getOrThrow()

        assertEquals(5, config.database.pool.size)
    }

    @Test
    fun resolvesDoublyNestedEnvKeys() {
        val config = loader(
            MapSource.screamingSnake("DATABASE_URL" to "jdbc:z", "DATABASE_POOL_SIZE" to "6")
        ).loadAppConfig().getOrThrow()

        assertEquals(6, config.database.pool.size)
    }

    @Test
    fun describesOptionalNullableFieldInSchema() {
        assertTrue(TypesConfigSchema.nickname.hasDefault)
        assertEquals(false, TypesConfigSchema.nickname.required)
        assertNull(TypesConfigSchema.nickname.default)
        assertEquals("Map<String, Double>", TypesConfigSchema.weights.typeName)
    }
}
