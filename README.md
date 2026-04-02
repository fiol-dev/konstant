# Konfigure

A compile-time, type-safe configuration library for Kotlin Multiplatform inspired by [pydantic-settings](https://docs.pydantic.dev/latest/concepts/pydantic_settings/).

Define your config as annotated data classes. A KSP processor generates all schema and loader code at build time -- **no reflection at runtime**.

## Features

- **Type-safe**: Config fields are fully typed with compile-time validation
- **Multiplatform**: JVM, JS (Node.js), Linux, macOS, iOS
- **Multiple sources**: Environment variables, `.env` files, `.properties`, TOML, YAML, maps (for testing)
- **Source priority**: Stack multiple sources; first non-null value wins
- **Nested configs**: Compose config classes with automatic prefix resolution
- **Error aggregation**: All errors collected before throwing -- never fail-fast on the first missing field
- **Secret masking**: `@Secret` fields are masked as `***` in error messages and logs
- **Zero runtime reflection**: KSP generates everything at compile time

## Quick Start

### 1. Define your config

```kotlin
import io.github.fioldev.konfigure.annotations.ConfigSpec
import io.github.fioldev.konfigure.annotations.Key
import io.github.fioldev.konfigure.annotations.Secret

@ConfigSpec
data class DatabaseConfig(
    val url: String,
    val port: Int = 5432,
    val maxPoolSize: Int = 10,
    @Secret val password: String,
    @Key("DB_API_KEY") val apiKey: String = ""
)

@ConfigSpec
data class ServerConfig(
    val host: String = "0.0.0.0",
    val port: Int = 8080,
    val debug: Boolean = false
)

@ConfigSpec
data class AppConfig(
    val appName: String = "MyApp",
    val database: DatabaseConfig,
    val server: ServerConfig
)
```

### 2. Load your config

```kotlin
import io.github.fioldev.konfigure.core.ConfigLoader
import io.github.fioldev.konfigure.sources.*

val loader = ConfigLoader {
    sources {
        +EnvSource()                             // highest priority
        +DotEnvSource(".env")
        +loadTomlFile("config.toml")             // TOML support
        +loadYamlFile("config.yaml")             // YAML support
        +loadPropertiesFile("config.properties") // lowest
    }
}

val config = loader.loadAppConfig().getOrThrow()

println(config.database.url)    // jdbc:postgresql://localhost:5432/mydb
println(config.server.port)     // 8080
```

### 3. Access config fields anywhere in your codebase

**Option A: Global registry** -- initialize once at startup, access anywhere.

```kotlin
import io.github.fioldev.konfigure.core.Konfigure

// At application startup (main, DI init, etc.)
fun main() {
    Konfigure.init {
        sources {
            +EnvSource()
            +loadTomlFile("config.toml")
        }
    }

    val config = Konfigure.loader.loadAppConfig().getOrThrow()
    Konfigure.register(config)            // register the root config
    Konfigure.register(config.database)   // also register nested configs individually
    Konfigure.register(config.server)

    startApp()
}

// Anywhere else in your codebase
class UserRepository {
    private val dbUrl = Konfigure.get<DatabaseConfig>().url
    private val maxPool = Konfigure.get<DatabaseConfig>().maxPoolSize
}

class HttpServer {
    private val port = Konfigure.get<ServerConfig>().port
    private val debug = Konfigure.get<ServerConfig>().debug
}
```

**Option B: Property delegates** -- lazy, cached field extraction.

```kotlin
import io.github.fioldev.konfigure.core.configField

// From the global registry (requires Konfigure.register() at startup)
class UserRepository {
    private val dbUrl: String by configField<AppConfig, String> { it.database.url }
    private val maxPool: Int by configField<AppConfig, Int> { it.database.maxPoolSize }
}

// From a specific config instance (no global registry needed)
import io.github.fioldev.konfigure.core.field

val appConfig: AppConfig = loader.loadAppConfig().getOrThrow()

class HttpServer {
    private val port: Int by appConfig.field { it.server.port }
    private val debug: Boolean by appConfig.field { it.server.debug }
}
```

**Option C: Direct access** -- just use the loaded config object.

```kotlin
val config = loader.loadAppConfig().getOrThrow()

// Pass what you need
fun connectToDatabase(url: String, port: Int, password: String) { /* ... */ }

connectToDatabase(
    url = config.database.url,
    port = config.database.port,
    password = config.database.password,
)
```

### 4. Gradle setup

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

// build.gradle.kts
plugins {
    kotlin("multiplatform")
    id("com.google.devtools.ksp")
}

kotlin {
    jvm()
    // add other targets as needed

    sourceSets {
        commonMain.dependencies {
            implementation("io.github.fioldev.konfigure:konfigure-annotations:1.0.0")
            implementation("io.github.fioldev.konfigure:konfigure-core:1.0.0")
            implementation("io.github.fioldev.konfigure:konfigure-sources:1.0.0")
        }
        commonTest.dependencies {
            implementation("io.github.fioldev.konfigure:konfigure-test:1.0.0")
        }
    }
}

dependencies {
    add("kspCommonMainMetadata", "io.github.fioldev.konfigure:konfigure-ksp:1.0.0")
}
```

## Key Resolution

Property names are the source of truth for keys. No explicit key annotation needed by default.

### Naming Convention Mapping

| Property name   | Env / `.env` key       | `.properties` key     |
|-----------------|------------------------|-----------------------|
| `url`           | `URL`                  | `url`                 |
| `maxPoolSize`   | `MAX_POOL_SIZE`        | `max.pool.size`       |
| `debug`         | `DEBUG`                | `debug`               |

### Nesting Builds Prefixes

The property name in the parent class becomes the prefix for nested config fields:

| Path                                                       | Env key                    | Properties key              |
|------------------------------------------------------------|----------------------------|-----------------------------|
| `AppConfig::database` -> `DatabaseConfig::url`             | `DATABASE_URL`             | `database.url`              |
| `AppConfig::database` -> `DatabaseConfig::maxPoolSize`     | `DATABASE_MAX_POOL_SIZE`   | `database.max.pool.size`    |
| `AppConfig::server` -> `ServerConfig::port`                | `SERVER_PORT`              | `server.port`               |

### `@Key` Override

`@Key("CUSTOM_KEY")` overrides the resolved key entirely (prefix is not applied):

```kotlin
@ConfigSpec
data class MyConfig(
    @Key("CUSTOM_DB_URL") val databaseUrl: String  // always looked up as "CUSTOM_DB_URL"
)
```

## Sources

Sources are checked in declaration order (highest to lowest priority). The first non-null value wins.

### `EnvSource`

Reads from system environment variables. Uses `SCREAMING_SNAKE_CASE` keys.

```kotlin
+EnvSource()
```

### `DotEnvSource`

Reads from a `.env` file. Uses `SCREAMING_SNAKE_CASE` keys. Silently ignored if the file doesn't exist.

```env
# .env
DATABASE_URL=jdbc:postgresql://localhost:5432/mydb
DATABASE_PASSWORD="s3cret"
SERVER_PORT=9090
```

```kotlin
+DotEnvSource()           // defaults to ".env"
+DotEnvSource("app.env")  // custom path
```

### `PropertiesSource`

Reads from `.properties` files. Uses `dot.notation` keys with `SCREAMING_SNAKE_CASE` fallback.

```properties
# config.properties
database.url=jdbc:postgresql://localhost:5432/mydb
database.port=5432
server.host=0.0.0.0
```

```kotlin
+loadPropertiesFile("config.properties")
```

On JVM, you can also load from the classpath:

```kotlin
import io.github.fioldev.konfigure.sources.loadPropertiesResource

+loadPropertiesResource("application.properties")
```

The built-in `.properties` parser is pure Kotlin (no `java.util.Properties`):
- Skips blank lines and lines starting with `#` or `!`
- Splits on first `=` or `:` delimiter
- Trims key and value whitespace

### `TomlSource`

Reads from TOML files. Nested tables are flattened to `dot.notation` keys with `SCREAMING_SNAKE_CASE` fallback.

```toml
# config.toml
[database]
url = "jdbc:postgresql://localhost:5432/mydb"
port = 5432
max_pool_size = 10

[server]
host = "0.0.0.0"
port = 8080
debug = false
```

```kotlin
import io.github.fioldev.konfigure.sources.loadTomlFile

+loadTomlFile("config.toml")
```

The built-in TOML parser is pure Kotlin (zero dependencies):
- Tables (`[section]`) and nested tables (`[section.subsection]`)
- Basic strings (`"..."`), literal strings (`'...'`), and bare values
- Inline tables (`{ key = "val", key2 = "val2" }`)
- Comments (`#`)

### `YamlSource`

Reads from YAML files. Indentation-based nesting is flattened to `dot.notation` keys with `SCREAMING_SNAKE_CASE` fallback.

```yaml
# config.yaml
database:
  url: jdbc:postgresql://localhost:5432/mydb
  port: 5432
  max_pool_size: 10

server:
  host: 0.0.0.0
  port: 8080
  debug: false
```

```kotlin
import io.github.fioldev.konfigure.sources.loadYamlFile

+loadYamlFile("config.yaml")
+loadYamlFile("config.yml")
```

The built-in YAML parser is pure Kotlin (zero dependencies):
- Key-value pairs with `:` separator
- Indentation-based nesting (spaces)
- Quoted strings (`"..."` and `'...'`)
- Comments (`#`)

### `MapSource` (testing)

For unit tests, use `MapSource` from `konfigure-test`:

```kotlin
import io.github.fioldev.konfigure.test.MapSource

val loader = ConfigLoader {
    sources {
        +MapSource.screamingSnake(
            "DATABASE_URL" to "jdbc:test",
            "DATABASE_PASSWORD" to "test-secret"
        )
    }
}
```

Available factory methods:
- `MapSource.of(...)` -- raw keys (matched as-is)
- `MapSource.screamingSnake(...)` -- `SCREAMING_SNAKE_CASE` resolution
- `MapSource.dotNotation(...)` -- `dot.notation` resolution

## Annotations

| Annotation     | Target   | Description                                                        |
|----------------|----------|--------------------------------------------------------------------|
| `@ConfigSpec`  | Class    | Marks a data class for KSP processing. Must be a `data class`.    |
| `@Secret`      | Property | Masks the value as `***` in error messages and logs.               |
| `@Key("NAME")` | Property | Overrides the automatically resolved key with a custom name.       |

## Supported Field Types

| Type      | Converter           |
|-----------|---------------------|
| `String`  | identity            |
| `Int`     | `String::toInt`     |
| `Long`    | `String::toLong`    |
| `Double`  | `String::toDouble`  |
| `Float`   | `String::toFloat`   |
| `Boolean` | `String::toBoolean` |
| Nested `@ConfigSpec` | recursive load |

Unsupported types (e.g. `List<T>`, `Map<K,V>`, arbitrary classes) trigger a **compile-time error**.

## Error Handling

Konfigure collects **all** errors before throwing. You never get a single missing-field error only to discover more after fixing it.

```kotlin
val result = loader.loadAppConfig()

when (result) {
    is ConfigResult.Success -> println(result.value)
    is ConfigResult.Failure -> {
        result.errors.forEach { error ->
            println(error.message)
            // "Required configuration key 'DATABASE_URL' is missing"
            // "Failed to convert 'SERVER_PORT' value 'abc' to Int: ..."
        }
    }
}

// Or throw immediately:
val config = loader.loadAppConfig().getOrThrow()
```

### Error Types

| Error               | When                                                                 |
|---------------------|----------------------------------------------------------------------|
| `MissingRequired`   | A required field (no default) is not found in any source             |
| `ConversionFailed`  | A value was found but couldn't be converted to the target type       |
| `NestedFailure`     | A nested `@ConfigSpec` config had errors                             |

## KSP Compile-Time Validations

The KSP processor emits a **compilation error** (not a warning) for:

- `@ConfigSpec` applied to a non-data class
- A field type that is another `@ConfigSpec` class but is not itself annotated with `@ConfigSpec`
- A field type that is unsupported (e.g. `List<T>`, `Map<K,V>`, arbitrary classes)

## What KSP Generates

For each `@ConfigSpec` class, the processor generates:

**1. A schema object** with one `FieldDescriptor` per property:

```kotlin
object DatabaseConfigSchema {
    val url = FieldDescriptor<String>(
        propertyName = "url",
        envKey = "URL",
        required = true,
        secret = false,
        default = null,
        convert = { it }
    )
    val port = FieldDescriptor<Int>(
        propertyName = "port",
        envKey = "PORT",
        required = false,
        secret = false,
        default = 5432,
        convert = String::toInt
    )
    // ...
}
```

**2. A typed loader extension** on `ConfigLoader`:

```kotlin
fun ConfigLoader.loadDatabaseConfig(prefix: String? = null): ConfigResult<DatabaseConfig>
```

Nested `@ConfigSpec` types recursively call their own generated loader, passing the resolved prefix down.

## Module Structure

```
konfigure/
+-- konfigure-annotations/    # @ConfigSpec, @Secret, @Key -- zero deps, commonMain
+-- konfigure-core/           # FieldDescriptor, ConfigSource, ConfigLoader,
|                             # ConfigResult, key resolution utils -- commonMain
+-- konfigure-ksp/            # KSP processor -- JVM only, runs at build time
+-- konfigure-sources/        # EnvSource, DotEnvSource, PropertiesSource
|                             # with expect/actual per platform
+-- konfigure-test/           # MapSource for unit testing configs
```

## Platform Support

| Platform          | EnvSource | DotEnvSource | PropertiesSource |
|-------------------|-----------|--------------|------------------|
| JVM               | Yes       | Yes          | Yes              |
| JS (Node.js)      | Yes       | Yes          | Yes              |
| Linux (x64)       | Yes       | Yes          | Yes              |
| macOS (arm64)     | Yes       | Yes          | Yes              |
| iOS               | Yes       | Yes          | Yes              |

## License

MIT License -- see [LICENSE](LICENSE) file for details.
