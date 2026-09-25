# Module konstant-core

The runtime every Konstant app needs. `ConfigLoader` reads values from a stack of
`ConfigSource`s in priority order, converts and validates them, and returns a
`ConfigResult`: either the loaded config or every `ConfigError` found. `ConfigLoader.explain`
also reports where each value came from. `Konstant` holds the loaded config for apps that
want global access, and `configField` / `field` expose single values as property delegates.

Declarations marked `@InternalKonstantApi` exist for generated code and may change without notice.

# Package io.github.fiol_dev.konstant.core

Loading, results, errors, sources and the global holder.
