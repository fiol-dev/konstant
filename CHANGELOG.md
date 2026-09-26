# Changelog

## 0.1.0-alpha3 (unreleased)

### Added
- Nullable nested `@ConfigSpec` fields for optional sections: `val oidc: OidcConfig?` is null (or its default) when no source has any of its keys, and loads as usual once one is set.
- The Gradle plugin `io.github.fiol-dev.konstant` is published to Maven Central with the libraries.
- `fromString(content, origin = "…")` on every file source labels it in `explain` reports.

### Changed
- `explain` reports show every key in `dot.notation` (or its `@Key`), whether it was found, defaulted or missing, and name the key the source used with `as …` when it differs. File sources show their path: `#2 TomlSource(config/app.dev.toml)`, including files baked by the Gradle plugin.
- `konstant-sources` no longer adds `KonstantInitProvider` to your manifest. Apps that read assets with `fromResource` on Android call `initKonstantAndroid(context)` in `Application.onCreate`, or declare the provider in their manifest.
- JVM and Android artifacts target Java 11 bytecode whatever JDK builds them. Before, a build on a newer JDK produced jars that older runtimes could not load.
- `publishToMavenLocal` works without PGP keys; only builds with a `signingKey` sign.

### Fixed
- Android libraries now have a valid namespace (`io.github.fiol_dev.konstant.*`). In 0.1.0-alpha2 the namespace contained a hyphen, so AGP rejected every Konstant `.aar`.

## 0.1.0-alpha2

Do not use on Android: its `.aar` files have an invalid namespace. Use 0.1.0-alpha3 instead.


The first release meant for use. The API may still change before 1.0.

### Added
- Typed config classes with `@ConfigSpec`, loaded by KSP-generated `ConfigLoader.load<Spec>()` functions with no runtime reflection.
- Sources for environment variables, `.env` and `.properties` files (`konstant-sources`), and TOML, YAML and JSON modules (`konstant-toml`, `konstant-yaml`, `konstant-json`), each with `fromFile`, `fromResource` and `fromString`.
- Bundled resources on every platform: classpath, Android assets, Apple main bundle, Node and Linux files.
- Field types: strings, numbers, strict booleans, enums, `Duration`, lists, sets, maps, nullable fields and nested specs.
- Custom converters with `@Convert`, validation with `@Range`, `@Size`, `@NotBlank`, `@Pattern` and `init` blocks.
- `ConfigLoader.explain` shows which source supplied each value.
- A global holder (`Konstant.init<Spec>`, `Konstant.get`) and `configField` / `field` delegates.
- `konstant-koin` for Koin, `konstant-reload` for reloading and remote config, `konstant-test` with `MapSource`.
- A Gradle plugin that bakes per-environment config files into the app (not published yet).
- Targets: Android, JVM, iOS, macOS, Linux x64, JS and Wasm (Node.js and browser).
- API reference generated with Dokka (`./gradlew dokkaGenerate`).

### Fixed
- Constructor defaults with comments next to them no longer break the generated code.
- A config field named `prefix` or `errors` no longer changes other fields' keys or breaks the build.
- `@ConfigSpec` classes nested inside another class, and internal classes, now generate working code.
- `@Key` values with quotes, `$` or backslashes are escaped in generated code.
- Generated code compiles in modules that use explicit API mode.
- A default on a nested spec field is used when that spec's required keys are missing.
- `.env` files accept `export`, inline comments and escapes in double quotes.
- `.properties` files accept line continuations, escapes and whitespace separators.
- A backslash inside a single-quoted list item is kept as written.
- TOML and YAML files that start with a byte order mark load correctly.
- A missing optional `.env` file on JS and Wasm gives an empty source instead of an error.

## 0.1.0-alpha1

Tagged for testing only; not published.
