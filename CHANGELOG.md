# Changelog

## 0.1.0-alpha2 (unreleased)

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
