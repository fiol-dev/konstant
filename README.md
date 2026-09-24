# Konstant

A compile-time, type-safe configuration library for Kotlin Multiplatform inspired by [pydantic-settings](https://docs.pydantic.dev/latest/concepts/pydantic_settings/).

Define your config as annotated data classes. A KSP processor generates all schema and loader code at build time -- **no reflection at runtime**.

## Features

- **Type-safe**: Config fields are fully typed with compile-time validation
- **Multiplatform**: JVM, JS (Node.js), Linux, macOS, iOS
- **Multiple sources**: Environment variables, `.env` files, `.properties`, TOML, YAML, JSON, maps (for testing)
- **Source priority**: Stack multiple sources; first non-null value wins
- **Nested configs**: Compose config classes with automatic prefix resolution
- **Error aggregation**: All errors collected before throwing -- never fail-fast on the first missing field
- **Secret masking**: `@Secret` fields are masked as `***` in error messages and logs
- **Zero runtime reflection**: KSP generates everything at compile time
- **Live reload**: Optional `StateFlow` of validated updates that keeps the last good config on errors

## Quick Start

### 1. Define your config

```kotlin
import io.github.fiol_dev.konstant.annotations.ConfigSpec
import io.github.fiol_dev.konstant.annotations.Key
import io.github.fiol_dev.konstant.annotations.Secret

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
import io.github.fiol_dev.konstant.core.ConfigLoader
import io.github.fiol_dev.konstant.sources.*

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

**Option A: Global holder** -- load once at startup, read any spec anywhere.

KSP generates `Konstant.init<Spec>` for every `@ConfigSpec`. It loads the config (throwing `ConfigException` on errors) and makes the root and every nested spec readable through `Konstant.get`:

```kotlin
import io.github.fiol_dev.konstant.core.Konstant

// At application startup (main, Application.onCreate, DI init, etc.)
fun main() {
    Konstant.initAppConfig {
        sources {
            +EnvSource()
            +loadTomlFile("config.toml")
        }
    }
    startApp()
}

// Anywhere else in your codebase, nested specs included
class UserRepository {
    private val dbUrl = Konstant.get<DatabaseConfig>().url
}

class HttpServer {
    private val port = Konstant.get<ServerConfig>().port
}
```

The config is published once as an immutable snapshot, so reads are thread-safe on every platform. Calling `init` a second time fails; tests call `Konstant.reset()` between cases. If one spec type appears twice (say `primary` and `replica` databases), `Konstant.get` refuses to pick one, so read it through its parent instead.

**With Koin** -- add `konstant-koin` and declare the specs you inject:

```kotlin
import io.github.fiol_dev.konstant.koin.config

val configModule = module {
    config<AppConfig>()
    config<DatabaseConfig>()
}

class UserRepository(private val db: DatabaseConfig)
```

Each `config<T>()` is a Koin single that reads `Konstant.get<T>()` on first injection, so call `Konstant.initAppConfig { ... }` before anything injects a config.

**Option B: Property delegates** -- lazy, cached field extraction.

```kotlin
import io.github.fiol_dev.konstant.core.configField

// From the global holder (after Konstant.initAppConfig at startup)
class UserRepository {
    private val dbUrl: String by configField<AppConfig, String> { it.database.url }
    private val maxPool: Int by configField<AppConfig, Int> { it.database.maxPoolSize }
}

// From a specific config instance (no global holder needed)
import io.github.fiol_dev.konstant.core.field

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
            implementation("io.github.fiol-dev.konstant:konstant-annotations:0.0.1-alpha1")
            implementation("io.github.fiol-dev.konstant:konstant-core:0.0.1-alpha1")
            implementation("io.github.fiol-dev.konstant:konstant-sources:0.0.1-alpha1")
        }
        commonTest.dependencies {
            implementation("io.github.fiol-dev.konstant:konstant-test:0.0.1-alpha1")
        }
    }
}

dependencies {
    add("kspCommonMainMetadata", "io.github.fiol-dev.konstant:konstant-ksp:0.0.1-alpha1")
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
| `AppConfig::database` -> `DatabaseConfig::pool` -> `PoolConfig::size` | `DATABASE_POOL_SIZE` | `database.pool.size`   |

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

To load a file bundled with the app instead, see [Bundled resources](#bundled-resources-mobile).

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
import io.github.fiol_dev.konstant.sources.loadTomlFile

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
import io.github.fiol_dev.konstant.sources.loadYamlFile

+loadYamlFile("config.yaml")
+loadYamlFile("config.yml")
```

The built-in YAML parser is pure Kotlin (zero dependencies):
- Key-value pairs with `:` separator
- Indentation-based nesting (spaces)
- Quoted strings (`"..."` and `'...'`)
- Comments (`#`)

### Full TOML, YAML and JSON (`konstant-toml`, `konstant-yaml`, `konstant-json`)

The built-in parsers above cover common config files. For the full TOML 1.0 and YAML 1.2 specs (arrays of tables, multi-line strings, anchors and merge keys, block scalars, flow collections), add the optional modules. For JSON config files, add `konstant-json`. They are backed by [ktoml](https://github.com/orchestr7/ktoml), [kaml](https://github.com/charleskorn/kaml) and [kotlinx-serialization-json](https://github.com/Kotlin/kotlinx.serialization) and work on every Konstant target, except that `konstant-toml` has no wasmJs build yet (ktoml's wasm artifact is broken with Kotlin 2.3):

```kotlin
// build.gradle.kts
commonMain.dependencies {
    implementation("io.github.fiol-dev.konstant:konstant-toml:<version>")
    implementation("io.github.fiol-dev.konstant:konstant-yaml:<version>")
    implementation("io.github.fiol-dev.konstant:konstant-json:<version>")
}
```

```kotlin
import io.github.fiol_dev.konstant.toml.TomlSource
import io.github.fiol_dev.konstant.json.JsonSource
import io.github.fiol_dev.konstant.yaml.YamlSource

val loader = ConfigLoader {
    sources {
        +EnvSource()
        +JsonSource.fromResource("config.local.json", optional = true)
        +YamlSource.fromResource("config.yaml", optional = true)
        +TomlSource.fromResource("config.toml")   // or fromFile / fromString
    }
}
```

Keys work the same as with the built-in sources: tables and mappings become `dot.notation` keys, arrays become lists, and entries of an array of tables (or a YAML list of mappings, or a JSON array of objects) are numbered, as in `servers.0.name`. JSON files may contain comments and trailing commas, and `null` values are skipped so the spec's default applies. Import these classes by name rather than with `*` when you also use `konstant-sources`, which has classes with the same names.

### Bundled resources (mobile)

Apps usually ship config inside the package rather than as files on disk. The `load*Resource` functions read it from wherever each platform bundles files:

| Platform      | Location                                               |
|---------------|--------------------------------------------------------|
| Android       | `src/main/assets/<path>`                               |
| iOS / macOS   | the main bundle's resources (add the file to the app target) |
| JVM           | the classpath, e.g. `src/main/resources/<path>`        |
| JS (Node), Linux | a file at `<path>` relative to the working directory |

```kotlin
import io.github.fiol_dev.konstant.sources.source.*

val loader = ConfigLoader {
    sources {
        +EnvSource()
        +loadTomlResource("config.local.toml", optional = true) // missing file = empty source
        +loadTomlResource("config.toml")
    }
}
```

`loadYamlResource`, `loadPropertiesResource` and `loadDotEnvResource` work the same way. On Android no `Context` is needed: the library registers a small startup provider that captures the application context. If you remove that provider, call `initKonstantAndroid(context)` before loading.

### Baked config (Gradle plugin)

The `io.github.fiol-dev.konstant` Gradle plugin compiles config files into the app, choosing files per environment at build time. This suits values that differ per flavor or stage but shouldn't be read from disk at runtime.

```kotlin
// build.gradle.kts
plugins {
    id("io.github.fiol-dev.konstant")
}

konstant {
    packageName.set("com.example.config")
    bake("config/app.toml")                          // always baked
    bake("config/app.{env}.toml", optional = true)   // overrides app.toml when present
}
```

`{env}` is the environment: `dev` by default, set with `./gradlew build -Pkonstant.env=prod` or `environment.set(...)`. A missing file fails the build unless it is `optional`. Files can be `.toml`, `.yaml`/`.yml`, `.properties` or `.env`; later files override earlier ones.

The plugin generates `KonstantBaked` (rename it with `objectName`) and adds it to `commonMain` (or `main` on JVM and Android projects):

```kotlin
val loader = ConfigLoader {
    sources {
        +EnvSource()              // runtime values still win
        +KonstantBaked.sources
    }
}
println(KonstantBaked.ENVIRONMENT) // "prod"
```

The module using the baked sources needs `konstant-sources` as a dependency.

### `MapSource` (testing)

For unit tests, use `MapSource` from `konstant-test`:

```kotlin
import io.github.fiol_dev.konstant.test.MapSource

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

| Type                        | Accepted values                                                          |
|-----------------------------|--------------------------------------------------------------------------|
| `String`                    | any text                                                                 |
| `Int`, `Long`, `Double`, `Float` | numbers (surrounding whitespace is ignored)                         |
| `Boolean`                   | `true/false`, `yes/no`, `on/off`, `1/0`, case-insensitive                 |
| `kotlin.time.Duration`      | `30s`, `1h 30m`, `500ms` or ISO-8601 (`PT30S`)                           |
| any `enum`                  | constant name, case-insensitive, `-` treated as `_` (`warn-only` → `WARN_ONLY`) |
| `List<T>`, `Set<T>`         | comma-separated (`a, b`) or an inline array (`["a", "b"]`)               |
| `Map<String, T>`            | `key=value` pairs (`a=1, b=2`), a TOML table or a YAML mapping           |
| `T?` (any of the above)     | optional: `null` when no source has the key and there is no default      |
| Nested `@ConfigSpec`        | loaded recursively with a key prefix                                     |

`T` in collections is any of the scalar types above. Other types trigger a **compile-time error**.

## Custom Converters

For a type Konstant doesn't know, write an `object` implementing `ValueConverter<T>` and point the field at it with `@Convert`:

```kotlin
object HostPortConverter : ValueConverter<HostPort> {
    override fun convert(raw: String): HostPort {
        val (host, port) = raw.split(':').also { require(it.size == 2) { "expected host:port" } }
        return HostPort(host, port.toInt())
    }
}

@ConfigSpec
data class ProxyConfig(
    @Convert(HostPortConverter::class) val upstream: HostPort,
    @Convert(HostPortConverter::class) val backup: HostPort? = null,
)
```

Throw `IllegalArgumentException` for bad input; its message becomes the error's cause.

## Validation

Annotate fields to check values after conversion. Failures are reported together with every other error as `ConfigError.ValidationFailed`.

| Annotation                 | Applies to                         | Rule                                  |
|----------------------------|------------------------------------|---------------------------------------|
| `@Range(min, max)`         | `Int`, `Long`, `Double`, `Float`   | `min <= value <= max` (either bound optional) |
| `@Size(min, max)`          | `String` length, `List`/`Set`/`Map` size | `min <= size <= max`            |
| `@NotBlank`                | `String`                           | contains a non-whitespace character   |
| `@Pattern(regex)`          | `String`                           | the whole value matches `regex`       |

Rules that span several fields go in the class's `init` block. A failing `require` becomes a `ValidationFailed` error for the whole config instead of an exception:

```kotlin
@ConfigSpec
data class WorkerConfig(
    @Range(min = 1.0) val minWorkers: Int = 1,
    @Range(min = 1.0, max = 64.0) val maxWorkers: Int = 4,
) {
    init {
        require(minWorkers <= maxWorkers) { "minWorkers must not exceed maxWorkers" }
    }
}
```

## Error Handling

Konstant collects **all** errors before throwing. You never get a single missing-field error only to discover more after fixing it.

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

### Which source won?

`explain` runs a load and lists every field's key, the value used (secrets as `***`) and where it came from, with sources numbered in priority order:

```kotlin
println(loader.explain { loadAppConfig() })
// APP_NAME           Demo                      #1 EnvSource
// DATABASE_URL       jdbc:postgresql://db/app  #3 TomlSource
// DATABASE_PASSWORD  ***                       #2 DotEnvSource
// ...
// SERVER_PORT        8080                      default
```

The report also carries the load `result`, so it works for failed loads too (missing fields show as `missing`).

### Error Types

| Error               | When                                                                 |
|---------------------|----------------------------------------------------------------------|
| `MissingRequired`   | A required field (no default) is not found in any source             |
| `ConversionFailed`  | A value was found but couldn't be converted to the target type       |
| `ValidationFailed`  | A value broke a validation annotation, or the class's `init` rejected it |
| `NestedFailure`     | A nested `@ConfigSpec` config had errors                             |

## Reloading at Runtime

`konstant-reload` keeps a config up to date while the app runs. Every reload reads the sources again and validates the result. A valid config is published to a `StateFlow`; an invalid one is skipped, the last good config stays, and the error goes to `failures`:

```kotlin
// build.gradle.kts
commonMain.dependencies {
    implementation("io.github.fiol-dev.konstant:konstant-reload:<version>")
}
```

```kotlin
val appConfig = ReloadableConfig.load(load = { loadAppConfig() }, context = Dispatchers.IO) {
    sources {
        +EnvSource()
        +TomlSource.fromFile("config.toml")   // read again on every reload
    }
}

// Collect failures before starting reloads: they are not replayed
scope.launch { appConfig.failures.collect { log.warn("Config reload skipped", it) } }
scope.launch { appConfig.config.collect { applyLogLevel(it.logLevel) } }

appConfig.reloadEvery(scope, 30.seconds)        // poll
appConfig.reloadOn(scope, fileChangedEvents)    // or reload on your own trigger
val port = appConfig.current.server.port
```

The first load throws a `ConfigException` if it fails, since there is no good config to fall back on yet. After that, reloads never throw: `reload()` is a suspend function that returns `Updated`, `Unchanged` or `Failed`, and reloads run one at a time so a slow one never overwrites a newer result. `context` is where reloads read their sources; pass `Dispatchers.IO` (or `Dispatchers.Default` where there is no IO dispatcher) when reloads start from the main thread.

Remote sources implement `ReloadableSource`, whose `changes` flow emits when their values change. Create the source outside the `sources` block, add it with `+remote`, and pass the same instance to `appConfig.watch(scope, remote)`.

`Konstant.get` still returns the config installed at startup, so read reloadable values through `appConfig`.

## KSP Compile-Time Validations

The KSP processor emits a **compilation error** (not a warning) for:

- `@ConfigSpec` applied to a non-data class
- A field type that is another `@ConfigSpec` class but is not itself annotated with `@ConfigSpec`
- A field type that is unsupported (e.g. arbitrary classes, `Map` with non-`String` keys, nullable collection elements)
- A nullable nested `@ConfigSpec` field
- `@Convert` naming something other than an object implementing `ValueConverter` of the field's type
- A validation annotation on a type it doesn't apply to, or an invalid `@Pattern` regex

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

**3. Holder functions** for [global access](#3-access-config-fields-anywhere-in-your-codebase):

```kotlin
fun Konstant.initDatabaseConfig(prefix: String? = null, block: ConfigLoaderBuilder.() -> Unit): DatabaseConfig
fun Konstant.initDatabaseConfig(loader: ConfigLoader, prefix: String? = null): DatabaseConfig
fun DatabaseConfig.konstantNestedConfigs(): List<Any> // every nested spec, at any depth
```

## Module Structure

```
konstant/
+-- konstant-annotations/    # @ConfigSpec, @Secret, @Key -- zero deps, commonMain
+-- konstant-core/           # FieldDescriptor, ConfigSource, ConfigLoader,
|                             # ConfigResult, key resolution utils -- commonMain
+-- konstant-ksp/            # KSP processor -- JVM only, runs at build time
+-- konstant-sources/        # EnvSource, DotEnvSource, PropertiesSource
|                             # with expect/actual per platform
+-- konstant-test/           # MapSource for unit testing configs
+-- konstant-koin/           # Koin integration: config<T>() definitions
+-- konstant-toml/           # Full TOML 1.0 source (ktoml)
+-- konstant-yaml/           # Full YAML 1.2 source (kaml)
+-- konstant-json/           # JSON source (kotlinx-serialization-json)
+-- konstant-reload/         # ReloadableConfig: StateFlow of validated reloads
+-- konstant-gradle-plugin/  # Gradle plugin baking config files per environment
```

## Platform Support

| Platform                   | Environment variables | Files (`fromFile`) | Bundled resources |
|----------------------------|-----------------------|--------------------|-------------------|
| Android                    | Yes                   | Yes                | Yes (assets)      |
| iOS, macOS (arm64)         | Yes                   | Yes                | Yes (main bundle) |
| JVM                        | Yes                   | Yes                | Yes (classpath)   |
| Linux (x64)                | Yes                   | Yes                | Yes (files)       |
| JS and Wasm on Node.js     | Yes                   | Yes                | Yes (files)       |
| JS and Wasm in the browser | No (always empty)     | No                 | No                |

In the browser, bake config into the app with the [Gradle plugin](#baked-config-gradle-plugin) or pass text to a source's `fromString`. Parsing, loading and validation work the same on every platform.

## License

MIT License -- see [LICENSE](LICENSE) file for details.
