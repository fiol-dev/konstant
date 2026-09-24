@file:OptIn(InternalKonstantApi::class)

package io.github.fiol_dev.konstant.test

import io.github.fiol_dev.konstant.core.*
import io.github.fiol_dev.konstant.sources.source.PropertiesSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Integration tests simulating KSP-generated schemas and loaders.
 * These manually define what the KSP processor would generate.
 */

// --- Simulated data classes (what the user would write) ---

data class DatabaseConfig(
    val url: String,
    val port: Int,
    val maxPoolSize: Int,
    val password: String,
    val apiKey: String,
)

data class ServerConfig(
    val host: String,
    val port: Int,
    val debug: Boolean,
)

data class AppConfig(
    val appName: String,
    val database: DatabaseConfig,
    val server: ServerConfig,
)

// --- Simulated generated schemas ---

object DatabaseConfigSchema {
    val url = FieldDescriptor(
        propertyName = "url",
        typeName = "String",
        secret = false,
        default = null,
        hasDefault = false,
        convert = { it },
    )
    val port = FieldDescriptor(
        propertyName = "port",
        typeName = "Int",
        secret = false,
        default = 5432,
        hasDefault = true,
        convert = String::toInt,
    )
    val maxPoolSize = FieldDescriptor(
        propertyName = "maxPoolSize",
        typeName = "Int",
        secret = false,
        default = 10,
        hasDefault = true,
        convert = String::toInt,
    )
    val password = FieldDescriptor(
        propertyName = "password",
        typeName = "String",
        secret = true,
        default = null,
        hasDefault = false,
        convert = { it },
    )
    val apiKey = FieldDescriptor(
        propertyName = "apiKey",
        typeName = "String",
        secret = false,
        default = "",
        hasDefault = true,
        customKey = "DB_PASS",
        convert = { it },
    )
}

object ServerConfigSchema {
    val host = FieldDescriptor(
        propertyName = "host",
        typeName = "String",
        secret = false,
        default = "0.0.0.0",
        hasDefault = true,
        convert = { it },
    )
    val port = FieldDescriptor(
        propertyName = "port",
        typeName = "Int",
        secret = false,
        default = 8080,
        hasDefault = true,
        convert = String::toInt,
    )
    val debug = FieldDescriptor(
        propertyName = "debug",
        typeName = "Boolean",
        secret = false,
        default = false,
        hasDefault = true,
        convert = String::toBoolean,
    )
}

object AppConfigSchema {
    val appName = FieldDescriptor(
        propertyName = "appName",
        typeName = "String",
        secret = false,
        default = "MyApp",
        hasDefault = true,
        convert = { it },
    )
}

// --- Simulated generated loaders ---

fun ConfigLoader.loadDatabaseConfig(prefix: String? = null): ConfigResult<DatabaseConfig> {
    val errors = mutableListOf<ConfigError>()

    val url: String? = when (val urlResult = resolve(DatabaseConfigSchema.url, prefix)) {
        is ResolveResult.Success -> urlResult.value
        is ResolveResult.Error -> {
            errors += urlResult.error; null
        }
    }

    val port: Int? = when (val portResult = resolve(DatabaseConfigSchema.port, prefix)) {
        is ResolveResult.Success -> portResult.value
        is ResolveResult.Error -> {
            errors += portResult.error; null
        }
    }

    val maxPoolSize: Int? = when (val maxPoolSizeResult = resolve(DatabaseConfigSchema.maxPoolSize, prefix)) {
        is ResolveResult.Success -> maxPoolSizeResult.value
        is ResolveResult.Error -> {
            errors += maxPoolSizeResult.error; null
        }
    }

    val password: String? = when (val passwordResult = resolve(DatabaseConfigSchema.password, prefix)) {
        is ResolveResult.Success -> passwordResult.value
        is ResolveResult.Error -> {
            errors += passwordResult.error; null
        }
    }

    val apiKey: String? = when (val apiKeyResult = resolve(DatabaseConfigSchema.apiKey, prefix)) {
        is ResolveResult.Success -> apiKeyResult.value
        is ResolveResult.Error -> {
            errors += apiKeyResult.error; null
        }
    }

    if (errors.isNotEmpty()) return ConfigResult.Failure(errors)

    return ConfigResult.Success(
        DatabaseConfig(
            url = url!!,
            port = port!!,
            maxPoolSize = maxPoolSize!!,
            password = password!!,
            apiKey = apiKey!!,
        )
    )
}

fun ConfigLoader.loadServerConfig(prefix: String? = null): ConfigResult<ServerConfig> {
    val errors = mutableListOf<ConfigError>()

    val host: String? = when (val hostResult = resolve(ServerConfigSchema.host, prefix)) {
        is ResolveResult.Success -> hostResult.value
        is ResolveResult.Error -> {
            errors += hostResult.error; null
        }
    }

    val port: Int? = when (val portResult = resolve(ServerConfigSchema.port, prefix)) {
        is ResolveResult.Success -> portResult.value
        is ResolveResult.Error -> {
            errors += portResult.error; null
        }
    }

    val debug: Boolean? = when (val debugResult = resolve(ServerConfigSchema.debug, prefix)) {
        is ResolveResult.Success -> debugResult.value
        is ResolveResult.Error -> {
            errors += debugResult.error; null
        }
    }

    if (errors.isNotEmpty()) return ConfigResult.Failure(errors)

    return ConfigResult.Success(
        ServerConfig(host = host!!, port = port!!, debug = debug!!)
    )
}

fun ConfigLoader.loadAppConfig(prefix: String? = null): ConfigResult<AppConfig> {
    val errors = mutableListOf<ConfigError>()

    val appName: String? = when (val appNameResult = resolve(AppConfigSchema.appName, prefix)) {
        is ResolveResult.Success -> appNameResult.value
        is ResolveResult.Error -> {
            errors += appNameResult.error; null
        }
    }

    val dbPrefix = KeyUtils.resolvePrefix("database", prefix)
    val database: DatabaseConfig? = when (val dbResult = loadDatabaseConfig(dbPrefix)) {
        is ConfigResult.Success -> dbResult.value
        is ConfigResult.Failure -> {
            errors += dbResult.errors; null
        }
    }

    val serverPrefix = KeyUtils.resolvePrefix("server", prefix)
    val server: ServerConfig? = when (val serverResult = loadServerConfig(serverPrefix)) {
        is ConfigResult.Success -> serverResult.value
        is ConfigResult.Failure -> {
            errors += serverResult.errors; null
        }
    }

    if (errors.isNotEmpty()) return ConfigResult.Failure(errors)

    return ConfigResult.Success(
        AppConfig(appName = appName!!, database = database!!, server = server!!)
    )
}

// --- Actual Tests ---

class ConfigLoaderIntegrationTest {

    @Test
    fun requiredFieldMissing_reportsError() {
        val loader = ConfigLoader {
            sources {
                +MapSource.screamingSnake(
                    "PORT" to "5432"
                )
            }
        }
        val result = loader.loadDatabaseConfig()
        assertIs<ConfigResult.Failure>(result)
        assertTrue(result.errors.any { it is ConfigError.MissingRequired && it.key == "URL" })
        assertTrue(result.errors.any { it is ConfigError.MissingRequired && it.key == "PASSWORD" })
    }

    @Test
    fun requiredFieldMissing_collectsAllErrors() {
        val loader = ConfigLoader {
            sources {
                +MapSource.screamingSnake()
            }
        }
        val result = loader.loadDatabaseConfig()
        assertIs<ConfigResult.Failure>(result)
        // url and password are required, should have at least 2 errors
        assertTrue(result.errors.size >= 2)
    }

    @Test
    fun typeConversionFailure_reportsError() {
        val loader = ConfigLoader {
            sources {
                +MapSource.screamingSnake(
                    "URL" to "jdbc:test",
                    "PORT" to "not_a_number",
                    "PASSWORD" to "secret",
                )
            }
        }
        val result = loader.loadDatabaseConfig()
        assertIs<ConfigResult.Failure>(result)
        assertTrue(result.errors.any { it is ConfigError.ConversionFailed })
    }

    @Test
    fun defaultValues_usedWhenNotProvided() {
        val loader = ConfigLoader {
            sources {
                +MapSource.screamingSnake(
                    "URL" to "jdbc:test",
                    "PASSWORD" to "secret",
                )
            }
        }
        val result = loader.loadDatabaseConfig()
        assertIs<ConfigResult.Success<DatabaseConfig>>(result)
        assertEquals(5432, result.value.port)
        assertEquals(10, result.value.maxPoolSize)
        assertEquals("", result.value.apiKey)
    }

    @Test
    fun defaultValues_overriddenBySource() {
        val loader = ConfigLoader {
            sources {
                +MapSource.screamingSnake(
                    "URL" to "jdbc:test",
                    "PORT" to "3306",
                    "PASSWORD" to "secret",
                )
            }
        }
        val result = loader.loadDatabaseConfig()
        assertIs<ConfigResult.Success<DatabaseConfig>>(result)
        assertEquals(3306, result.value.port)
    }

    @Test
    fun nestedPrefixResolution() {
        val loader = ConfigLoader {
            sources {
                +MapSource.screamingSnake(
                    "DATABASE_URL" to "jdbc:test",
                    "DATABASE_PORT" to "5432",
                    "DATABASE_PASSWORD" to "secret",
                    "SERVER_HOST" to "localhost",
                    "SERVER_PORT" to "9090",
                )
            }
        }
        val result = loader.loadAppConfig()
        assertIs<ConfigResult.Success<AppConfig>>(result)
        assertEquals("jdbc:test", result.value.database.url)
        assertEquals(5432, result.value.database.port)
        assertEquals("localhost", result.value.server.host)
        assertEquals(9090, result.value.server.port)
        assertEquals("MyApp", result.value.appName)
        assertEquals(false, result.value.server.debug)
    }

    @Test
    fun customKeyAnnotation_overridesResolution() {
        val loader = ConfigLoader {
            sources {
                +MapSource.screamingSnake(
                    "URL" to "jdbc:test",
                    "PASSWORD" to "secret",
                    "DB_PASS" to "custom_api_key",
                )
            }
        }
        val result = loader.loadDatabaseConfig()
        assertIs<ConfigResult.Success<DatabaseConfig>>(result)
        assertEquals("custom_api_key", result.value.apiKey)
    }

    @Test
    fun secretField_maskedInConversionError() {
        val secretField = FieldDescriptor(
            propertyName = "password",
            typeName = "Int",
            secret = true,
            default = null,
            hasDefault = false,
            convert = String::toInt,
        )
        val loader = ConfigLoader {
            sources {
                +MapSource.screamingSnake("PASSWORD" to "not_a_number")
            }
        }
        val result = loader.resolve(secretField, null)
        assertIs<ResolveResult.Error>(result)
        val error = result.error
        assertIs<ConfigError.ConversionFailed>(error)
        assertEquals("***", error.rawValue)
    }

    @Test
    fun propertiesDotNotation_resolvedCorrectly() {
        val loader = ConfigLoader {
            sources {
                +MapSource.dotNotation(
                    "database.url" to "jdbc:test",
                    "database.port" to "3306",
                    "database.password" to "secret",
                )
            }
        }
        val result = loader.loadDatabaseConfig("DATABASE")
        assertIs<ConfigResult.Success<DatabaseConfig>>(result)
        assertEquals("jdbc:test", result.value.url)
        assertEquals(3306, result.value.port)
    }

    @Test
    fun sourcePriorityOrder_firstSourceWins() {
        val loader = ConfigLoader {
            sources {
                +MapSource.screamingSnake(
                    "URL" to "from_env",
                    "PASSWORD" to "secret",
                )
                +MapSource.screamingSnake(
                    "URL" to "from_dotenv",
                    "PASSWORD" to "secret2",
                )
            }
        }
        val result = loader.loadDatabaseConfig()
        assertIs<ConfigResult.Success<DatabaseConfig>>(result)
        assertEquals("from_env", result.value.url)
        // Password from first source
        assertEquals("secret", result.value.password)
    }

    @Test
    fun sourcePriorityOrder_fallsBackToLowerPriority() {
        val loader = ConfigLoader {
            sources {
                +MapSource.screamingSnake(
                    "URL" to "from_env",
                    "PASSWORD" to "secret",
                )
                +MapSource.screamingSnake(
                    "URL" to "from_dotenv",
                    "PORT" to "9999",
                    "PASSWORD" to "secret2",
                )
            }
        }
        val result = loader.loadDatabaseConfig()
        assertIs<ConfigResult.Success<DatabaseConfig>>(result)
        assertEquals("from_env", result.value.url)
        // Port only in second source — should still be found (default=5432, but 9999 from source 2)
        assertEquals(9999, result.value.port)
    }

    @Test
    fun propertiesScreamingSnakeFallback() {
        // PropertiesSource tries dot.notation first, then case-insensitive match
        val propsContent = "DATABASE_URL=jdbc:test\nDATABASE_PORT=5432\nDATABASE_PASSWORD=secret"
        val props = PropertiesSource.fromString(propsContent)

        val loader = ConfigLoader {
            sources {
                +props
            }
        }

        // PropertiesSource uses DOT_NOTATION format, so keys resolve as dot.notation
        // but the fallback case-insensitive match finds SCREAMING_SNAKE keys
        val result = loader.loadDatabaseConfig("DATABASE")
        assertIs<ConfigResult.Success<DatabaseConfig>>(result)
        assertEquals("jdbc:test", result.value.url)
    }

    @Test
    fun rawKeyFormat_forTesting() {
        val loader = ConfigLoader {
            sources {
                +MapSource.of(
                    "url" to "jdbc:test",
                    "password" to "secret",
                )
            }
        }
        val result = loader.loadDatabaseConfig()
        assertIs<ConfigResult.Success<DatabaseConfig>>(result)
        assertEquals("jdbc:test", result.value.url)
    }

    @Test
    fun nestedConfig_missingRequired_collectsAllNestedErrors() {
        val loader = ConfigLoader {
            sources {
                +MapSource.screamingSnake(
                    // Missing DATABASE_URL, DATABASE_PASSWORD, SERVER_PORT not required (has default)
                )
            }
        }
        val result = loader.loadAppConfig()
        assertIs<ConfigResult.Failure>(result)
        // Should report errors for DATABASE_URL and DATABASE_PASSWORD at minimum
        assertTrue(result.errors.size >= 2)
    }
}
