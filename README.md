# Konstant

A compile-time, type-safe configuration library for Kotlin Multiplatform inspired by [pydantic-settings](https://docs.pydantic.dev/latest/concepts/pydantic_settings/).

Define your config as annotated data classes. A KSP processor generates all schema and loader code at build time -- **no reflection at runtime**.

## Features

- **Type-safe**: Config fields are fully typed with compile-time validation
- **Multiplatform**: Android, iOS, macOS, JVM, Linux, JS and Wasm (Node.js and browser)
- **Multiple sources**: Environment variables, `.env` files, `.properties`, TOML, YAML, JSON, maps (for testing)
- **Source priority**: Stack multiple sources; first non-null value wins
- **Nested configs**: Compose config classes with automatic prefix resolution
- **Error aggregation**: All errors collected before throwing -- never fail-fast on the first missing field
- **Secret masking**: `@Secret` values show as `***` in errors and `explain` reports
- **Zero runtime reflection**: KSP generates everything at compile time
- **Live reload**: Optional `StateFlow` of validated updates that keeps the last good config on errors
- **Baked config**: A Gradle plugin compiles per-environment config files into the app

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
import io.github.fiol_dev.konstant.toml.TomlSource // konstant-toml module

val loader = ConfigLoader {
    sources {
        +EnvSource()                             // highest priority
        +DotEnvSource.fromFile(".env")
        +TomlSource.fromFile("config.toml")
        +PropertiesSource.fromFile("config.properties") // lowest
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
            +TomlSource.fromFile("config.toml")
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
import io.github.fiol_dev.konstant.core.field

// From the global holder (after Konstant.initAppConfig at startup)
class UserRepository {
    private val dbUrl: String by configField<AppConfig, String> { it.database.url }
    private val maxPool: Int by configField<AppConfig, Int> { it.database.maxPoolSize }
}

// From a specific config instance (no global holder needed)
val appConfig: AppConfig = loader.loadAppConfig().getOrThrow()

class HttpServer {
    private val port: Int by appConfig.field { it.server.port }
    private val debug: Boolean by appConfig.field { it.server.debug }
}
```

Each delegate reads the config the first time it is used and then keeps that value, so `configField` delegates created before a `Konstant.reset()` keep returning the old config.

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

All artifacts use the group `io.github.fiol-dev.konstant` and share one version.

| Artifact              | What it adds                                                       |
|-----------------------|--------------------------------------------------------------------|
| `konstant-core`       | Loader, results, errors, global holder (brings `konstant-annotations`) |
| `konstant-ksp`        | KSP processor that generates the loaders (build time only)         |
| `konstant-sources`    | `EnvSource`, `DotEnvSource`, `PropertiesSource`                    |
| `konstant-toml`       | `TomlSource` (no wasmJs build)                                     |
| `konstant-yaml`       | `YamlSource`                                                       |
| `konstant-json`       | `JsonSource`                                                       |
| `konstant-reload`     | `ReloadableConfig`, `RemoteSource`                                 |
| `konstant-koin`       | Koin `config<T>()` definitions                                     |
| `konstant-test`       | `MapSource` for tests                                              |

```toml
# gradle/libs.versions.toml
[versions]
konstant = "<version>"
ksp = "<ksp version matching your Kotlin>"

[libraries]
konstant-core = { module = "io.github.fiol-dev.konstant:konstant-core", version.ref = "konstant" }
konstant-sources = { module = "io.github.fiol-dev.konstant:konstant-sources", version.ref = "konstant" }
konstant-toml = { module = "io.github.fiol-dev.konstant:konstant-toml", version.ref = "konstant" }
konstant-test = { module = "io.github.fiol-dev.konstant:konstant-test", version.ref = "konstant" }
konstant-ksp = { module = "io.github.fiol-dev.konstant:konstant-ksp", version.ref = "konstant" }

[plugins]
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

**Kotlin Multiplatform.** Run the processor once on common code and compile its output into `commonMain`, so every target shares the generated loaders:

```kotlin
// build.gradle.kts
plugins {
    kotlin("multiplatform")
    alias(libs.plugins.ksp)
}

kotlin {
    jvm()
    iosArm64()
    // other targets as needed

    sourceSets {
        commonMain {
            kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
            dependencies {
                implementation(libs.konstant.core)
                implementation(libs.konstant.sources)
                implementation(libs.konstant.toml) // optional formats
            }
        }
        commonTest.dependencies {
            implementation(libs.konstant.test)
        }
    }
}

dependencies {
    add("kspCommonMainMetadata", libs.konstant.ksp)
}

// Generate the loaders before any target compiles
tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompilationTask<*>>().configureEach {
    if (name != "kspCommonMainKotlinMetadata") dependsOn("kspCommonMainKotlinMetadata")
}
```

**JVM or Android only.** Use the plain `ksp` configuration:

```kotlin
dependencies {
    implementation(libs.konstant.core)
    implementation(libs.konstant.sources)
    ksp(libs.konstant.ksp)
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

A nested field with a default (`val replica: DbConfig = DbConfig(url = "jdbc:local")`) uses that default when the nested spec's only errors are missing required keys. The whole default is used in that case, so any nested keys that were set are ignored; a value that fails to convert or validate is still reported.

### Optional sections

Make a nested field nullable for a section that may be switched off, such as OIDC in a local build:

```kotlin
@ConfigSpec
data class AuthConfig(
    val oidc: OidcConfig?,                          // null when no OIDC_* key is set
    val flags: FeatureFlags? = FeatureFlags(),      // or a default of your choice
)
```

The section is absent, and gets its default (`null` if none is given), when no source has any of its keys. Once any of its keys is set, the section loads as usual, so a missing required key such as `OIDC_CLIENT_ID` is an error. In `explain` reports an absent section is one `default` line.

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

Reads from a `.env` file. Uses `SCREAMING_SNAKE_CASE` keys. A missing file gives an empty source unless you pass `optional = false`.

```env
# .env
DATABASE_URL=jdbc:postgresql://localhost:5432/mydb
DATABASE_PASSWORD="s3cret"
SERVER_PORT=9090
```

```kotlin
+DotEnvSource.fromFile()           // defaults to ".env"
+DotEnvSource.fromFile("app.env")  // custom path
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
+PropertiesSource.fromFile("config.properties")
```

To load a file bundled with the app instead, see [Bundled resources](#bundled-resources-mobile).

The built-in `.properties` parser is pure Kotlin (no `java.util.Properties`):
- Skips blank lines and lines starting with `#` or `!`
- Splits on the first unescaped `=`, `:` or whitespace (`key=value`, `key: value`, `key value`)
- Continues a line ending in a backslash onto the next one
- Unescapes `\uXXXX`, `\t`, `\n`, `\r`, `\f` and escaped separators (`\=`, `\:`, `\ `, `\#`, `\\`)
- Trims leading and trailing whitespace from values

The `.env` parser accepts a leading `export `, strips ` # comments` from unquoted values, unescapes `\n`, `\r`, `\t`, `\"` and `\\` in double-quoted values, and takes single-quoted values literally.

### TOML, YAML and JSON (`konstant-toml`, `konstant-yaml`, `konstant-json`)

TOML, YAML and JSON files are read by optional modules, so apps only pay for the formats they use. They support the full TOML 1.0, YAML 1.2 and JSON specs (arrays of tables, multi-line strings, anchors and merge keys, block scalars, flow collections). They are backed by [ktoml](https://github.com/orchestr7/ktoml), [kaml](https://github.com/charleskorn/kaml) and [kotlinx-serialization-json](https://github.com/Kotlin/kotlinx.serialization) and work on every Konstant target, except that `konstant-toml` has no wasmJs build yet (ktoml's wasm artifact is broken with Kotlin 2.3):

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

Nested tables and mappings become `dot.notation` keys, arrays become lists, and entries of an array of tables (or a YAML list of mappings, or a JSON array of objects) are numbered, as in `servers.0.name`. JSON files may contain comments and trailing commas, and `null` values are skipped so the spec's default applies.

### Bundled resources (mobile)

Apps usually ship config inside the package rather than as files on disk. Every file-based source has `fromResource`, which reads it from wherever each platform bundles files:

| Platform      | Location                                               |
|---------------|--------------------------------------------------------|
| Android       | `src/main/assets/<path>`                               |
| iOS / macOS   | the main bundle's resources (add the file to the app target) |
| JVM           | the classpath, e.g. `src/main/resources/<path>`        |
| JS (Node), Linux | a file at `<path>` relative to the working directory |

```kotlin
import io.github.fiol_dev.konstant.sources.*
import io.github.fiol_dev.konstant.toml.TomlSource

val loader = ConfigLoader {
    sources {
        +EnvSource()
        +TomlSource.fromResource("config.local.toml", optional = true) // missing file = empty source
        +TomlSource.fromResource("config.toml")
    }
}
```

`YamlSource`, `JsonSource`, `PropertiesSource` and `DotEnvSource` have the same `fromResource`.

**Android.** Reading assets needs the application context. Call `initKonstantAndroid(context)` in `Application.onCreate` before loading:

```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        initKonstantAndroid(this)
    }
}
```

Or let a content provider do it at app start by declaring it in your manifest:

```xml
<provider
    android:name="io.github.fiol_dev.konstant.sources.KonstantInitProvider"
    android:authorities="${applicationId}.konstant-init"
    android:exported="false" />
```

Konstant adds nothing to your manifest itself, so apps that only use `fromString`, `fromFile` or baked config need neither. Unit tests on the JVM call `initKonstantAndroid` too.

### Baked config (Gradle plugin)

The `io.github.fiol-dev.konstant` Gradle plugin compiles config files into the app, choosing files per environment at build time. This suits values that differ per flavor or stage but shouldn't be read from disk at runtime.

The plugin is published to Maven Central (from 0.1.0-alpha3) with the same version as the libraries, so `mavenCentral()` must be in your plugin repositories:

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}
```

```kotlin
// build.gradle.kts
plugins {
    id("io.github.fiol-dev.konstant") version "<version>"
}

konstant {
    packageName.set("com.example.config")
    bake("config/app.toml")                          // always baked
    bake("config/app.{env}.toml", optional = true)   // overrides app.toml when present
}
```

`{env}` is the environment: `dev` by default, set with `./gradlew build -Pkonstant.env=prod` or `environment.set(...)`. A missing file fails the build unless it is `optional`. Files can be `.toml`, `.yaml`/`.yml`, `.properties` or `.env` (not `.json`); later files override earlier ones. Baking `.toml` or `.yaml` files needs the `konstant-toml` or `konstant-yaml` dependency, since the generated code reads them with those modules' sources.

The plugin generates `KonstantBaked` (rename it with `objectName`) and adds it to `commonMain` in Kotlin Multiplatform projects, or to `main` in projects that apply `org.jetbrains.kotlin.jvm` or `org.jetbrains.kotlin.android`:

```kotlin
val loader = ConfigLoader {
    sources {
        +EnvSource()              // runtime values still win
        +KonstantBaked.sources
    }
}
println(KonstantBaked.ENVIRONMENT) // "prod"
```

The module using the baked sources needs `konstant-sources` as a dependency, plus `konstant-toml` or `konstant-yaml` when it bakes `.toml` or `.yaml` files. `konstant-toml` has no wasmJs build yet, so projects with a wasmJs target should bake `.yaml`, `.properties` or `.env` files instead of `.toml`.

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
- `MapSource.of(...)` -- raw keys (the property name as written, e.g. `maxPoolSize`)
- `MapSource.screamingSnake(...)` -- `SCREAMING_SNAKE_CASE` keys, like `EnvSource`
- `MapSource.dotNotation(...)` -- `dot.notation` keys, like `PropertiesSource`

Like the file-based sources, `MapSource` falls back to a case-insensitive match and fills `Map` fields from nested keys. Call `Konstant.reset()` between tests that use the global holder.

### Custom sources

Any `ConfigSource` can join the stack. Map-backed sources can extend `MapBackedSource`, which adds case-insensitive lookup and `Map` field support:

```kotlin
class SystemPropertiesSource : MapBackedSource(
    System.getProperties().entries.associate { (k, v) -> k.toString() to v.toString() }
) {
    override val keyFormat = KeyFormat.DOT_NOTATION
}
```

`keyFormat` picks how property names become keys (`SCREAMING_SNAKE`, `DOT_NOTATION` or `RAW`), and `fallbackKeyFormats` lists formats to try next. Implement `children(key)` only if the source can list nested entries (`server.headers.a`, `server.headers.b`) for `Map` fields; `MapBackedSource` does this for you. Keys under a `Map` field's prefix all become map entries, so don't give the map a sibling field whose key starts with the same prefix (a `pool` map next to a `poolSize` field in dot notation).

## Annotations

| Annotation     | Target   | Description                                                        |
|----------------|----------|--------------------------------------------------------------------|
| `@ConfigSpec`  | Class    | Marks a data class for KSP processing. Must be a `data class`.    |
| `@Secret`      | Property | Shows the value as `***` in errors and `explain` reports.          |
| `@Key("NAME")` | Property | Overrides the automatically resolved key with a custom name.       |
| `@Convert(X::class)` | Property | Converts the value with your `ValueConverter` ([Custom Converters](#custom-converters)). |
| `@Range`, `@Size`, `@NotBlank`, `@Pattern` | Property | Validate the value ([Validation](#validation)). |

`@Secret` only affects what Konstant prints. Your config is your own data class, so `println(config)` or logging it still shows the secret; override `toString` if that matters.

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
| Nested `@ConfigSpec`        | loaded recursively with a key prefix; its default, if any, is used when only required keys are missing |
| Nullable nested `@ConfigSpec` | an optional section: its default (or `null`) when none of its keys is set ([Optional sections](#optional-sections)) |

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

`getOrThrow` throws a `ConfigException` whose message lists every error on its own line, and whose `errors` holds them. `getOrNull`, `getOrElse`, `map` and `onFailure` cover the other common cases. Errors of nested configs carry the full prefixed key (`DATABASE_URL`). When a key is missing, the error names it in `SCREAMING_SNAKE` form whatever format your sources use, so `DATABASE_URL` means `database.url` in a TOML file.

An `init` block that calls `require` is reported as a `ValidationFailed` error keyed by the config's prefix (or its class name for the root). Only `IllegalArgumentException` is turned into an error; `check` and other exceptions propagate.

### Which source won?

`explain` runs a load and lists every field's key, the value used (secrets as `***`) and where it came from, with sources numbered in priority order:

```kotlin
println(loader.explain { loadAppConfig() })
// app.name           Demo                      #1 EnvSource as APP_NAME
// database.url       jdbc:postgresql://db/app  #3 TomlSource(config/app.dev.toml)
// database.password  ***                       #2 DotEnvSource(.env)
// ...
// server.port        8080                      default
```

Keys are always shown in `dot.notation` (or as written in `@Key`), and `as …` gives the name the source used when it differs. Sources read with `fromFile`, `fromResource` or the Gradle plugin show their file; for `fromString`, pass `origin = "…"` to label it.

The report also carries the load `result`, so it works for failed loads too (missing fields show as `missing`).

### Error Types

| Error               | When                                                                 |
|---------------------|----------------------------------------------------------------------|
| `MissingRequired`   | A required field (no default) is not found in any source             |
| `ConversionFailed`  | A value was found but couldn't be converted to the target type       |
| `ValidationFailed`  | A value broke a validation annotation, or the class's `init` rejected it |
| `NestedFailure`     | Not produced by generated loaders, which report nested errors with prefixed keys; available to hand-written loaders |

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

The first load throws a `ConfigException` if it fails, since there is no good config to fall back on yet. After that, reloads never throw: `reload()` is a suspend function that returns `Updated`, `Unchanged` or `Failed`, and reloads run one at a time so a slow one never overwrites a newer result. `context` is where reloads read their sources; pass `Dispatchers.IO` when reloads start from the main thread. `Dispatchers.IO` does not exist on JS and Wasm; use `Dispatchers.Default` there, or in common code.

`Konstant.get` still returns the config installed at startup, so read reloadable values through `appConfig`.

### Remote config

`RemoteSource` holds values that the app pushes in from any remote config service. Put it first so remote values override the bundled defaults, and `watch` it so each `update` reloads the config. Keys the service stops sending fall back to the next source:

```kotlin
// Firebase Remote Config through the GitLive KMP SDK (dev.gitlive:firebase-config).
// Start from the values activated in an earlier session, then fetch fresh ones.
val remote = RemoteSource(Firebase.remoteConfig.all.mapValues { it.value.asString() }, name = "Firebase")
val appConfig = ReloadableConfig.load(load = { loadAppConfig() }) {
    sources {
        +remote
        +TomlSource.fromResource("config.toml")
    }
}
appConfig.watch(scope, remote)

scope.launch {
    try {
        Firebase.remoteConfig.fetchAndActivate()
        remote.update(Firebase.remoteConfig.all.mapValues { it.value.asString() })
    } catch (e: Exception) {
        log.warn("Remote config fetch failed, keeping current values", e)
    }
}
```

Keys are matched in dot notation (`server.port`) and, as a fallback, in SCREAMING_SNAKE case, ignoring case. Firebase parameter keys cannot contain dots, so name them in snake case (`server_port`, `database_pool_size`). A `Map` field can still come from Firebase as one inline value (`server_headers` = `a=1, b=2`). Any other source whose values change can implement `ReloadableSource` and report changes on its `changes` flow; `watch` it the same way, with the instance that the `sources` block adds.

## KSP Compile-Time Validations

The KSP processor emits a **compilation error** (not a warning) for:

- `@ConfigSpec` applied to a non-data class
- A field type that is unsupported (e.g. a class without `@ConfigSpec`, `Map` with non-`String` keys, nullable collection elements)
- A `@ConfigSpec` class that is local, `inner`, or private or protected (itself or a class it is nested in)
- `@Convert` naming something other than an object implementing `ValueConverter` of the field's type
- A validation annotation on a type it doesn't apply to, or an invalid `@Pattern` regex

## What KSP Generates

For each `@ConfigSpec` class, the processor generates the declarations below. They are `public`, or `internal` for an internal class, so modules in explicit API mode compile. For a class nested in another, the names join the enclosing class names with `_`: `Outer.Db` gets `Outer_DbSchema`, `loadOuter_Db` and `initOuter_Db`.

**1. A schema object** (`DatabaseConfigSchema`) with one field descriptor per property, which the loader reads. Its shape is internal and may change between releases, so application code shouldn't use it.

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
+-- konstant-annotations/    # @ConfigSpec, @Key, @Secret, @Convert, @Range, @Size, @NotBlank, @Pattern
+-- konstant-core/           # ConfigLoader, ConfigResult, ConfigError, ConfigReport, ConfigSource,
|                            # ValueConverter, Konstant holder, configField/field delegates
+-- konstant-ksp/            # KSP processor -- JVM only, runs at build time
+-- konstant-sources/        # EnvSource, DotEnvSource, PropertiesSource, per-platform file access
+-- konstant-toml/           # TOML 1.0 source (ktoml)
+-- konstant-yaml/           # YAML 1.2 source (kaml)
+-- konstant-json/           # JSON source (kotlinx-serialization-json)
+-- konstant-reload/         # ReloadableConfig (StateFlow of validated reloads), RemoteSource
+-- konstant-koin/           # Koin integration: config<T>() definitions
+-- konstant-test/           # MapSource for unit testing configs
+-- konstant-gradle-plugin/  # Gradle plugin baking config files per environment
```

## Platform Support

| Platform                   | Environment variables | Files (`fromFile`) | Bundled resources |
|----------------------------|-----------------------|--------------------|-------------------|
| Android                    | Yes                   | Yes                | Yes (assets)      |
| iOS (arm64, x64, simulator arm64), macOS (arm64) | Yes | Yes            | Yes (main bundle) |
| JVM                        | Yes                   | Yes                | Yes (classpath)   |
| Linux (x64)                | Yes                   | Yes                | Yes (files)       |
| JS and Wasm on Node.js     | Yes                   | Yes                | Yes (files)       |
| JS and Wasm in the browser | No (always empty)     | No                 | No                |

Android needs minSdk 24. Wasm on Node.js reads files through `process.getBuiltinModule`, so it needs Node.js 20.16 or 22.3 and later; on older versions file sources fail and resources read as missing. Windows, watchOS, tvOS and Linux arm64 are not built yet.

In the browser, bake config into the app with the [Gradle plugin](#baked-config-gradle-plugin) or pass text to a source's `fromString`. Parsing, loading and validation work the same on every platform.

## API reference and stability

`./gradlew dokkaGenerate` builds the API reference for every module into `build/dokka/html`.

Konstant is in alpha, so the API can still change between releases. Declarations marked `@InternalKonstantApi` exist for generated code; using them needs an explicit opt-in and they can change in any release. Everything else is tracked in each module's `api/` dump, and changes are listed in [CHANGELOG.md](CHANGELOG.md).

## Contributing

Each library module keeps a dump of its public API in its `api/` folder, and CI fails when the code no longer matches it. After an intended public API change, run `./gradlew updateKotlinAbi` (on macOS, which builds every target) and commit the updated dumps with the change.

`./gradlew publishToMavenLocal :konstant-gradle-plugin:publishToMavenLocal` installs a local build without signing keys; only release builds sign. JVM and Android artifacts target Java 11 bytecode whatever JDK builds them.

## License

MIT License -- see [LICENSE](LICENSE) file for details.
