# Module konstant-sources

Built-in sources for environment variables, `.env` files and `.properties` files, on every
platform. Each source has companion factories: `fromFile`, `fromResource` and
`fromString`. Resources are read from the classpath on the JVM, from assets on Android,
from the main bundle on Apple platforms, and from files on Node and Linux.

On Android the library registers `KonstantInitProvider`, which captures the application context
at startup. Call `initKonstantAndroid` yourself only when that provider is removed, or in tests.

# Package io.github.fiol_dev.konstant.sources

`EnvSource`, `DotEnvSource`, `PropertiesSource` and the Android initializer.
