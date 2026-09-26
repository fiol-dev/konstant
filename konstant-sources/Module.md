# Module konstant-sources

Built-in sources for environment variables, `.env` files and `.properties` files, on every
platform. Each source has companion factories: `fromFile`, `fromResource` and
`fromString`. Resources are read from the classpath on the JVM, from assets on Android,
from the main bundle on Apple platforms, and from files on Node and Linux.

On Android, reading assets needs the application context: call `initKonstantAndroid` in
`Application.onCreate`, or register `KonstantInitProvider` in your manifest. Sources built with
`fromString` or `fromFile` need neither, so nothing is added to your manifest.

# Package io.github.fiol_dev.konstant.sources

`EnvSource`, `DotEnvSource`, `PropertiesSource` and the Android initializer.
