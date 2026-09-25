package io.github.fiol_dev.konstant.annotations

/**
 * Marks a data class as a config spec. The KSP processor generates a `ConfigLoader.load<Name>()`
 * function and a `Konstant.init<Name>` function for it. Each primary-constructor parameter is a
 * field; a parameter with a default is optional, and a nullable one without a default defaults
 * to null. A parameter whose type is another `@ConfigSpec` class is loaded as a nested spec,
 * with its keys prefixed by the parameter name (`database.url`, `DATABASE_URL`).
 *
 * ```kotlin
 * @ConfigSpec
 * data class AppConfig(
 *     val port: Int = 8080,
 *     val database: DatabaseConfig,
 * )
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
public annotation class ConfigSpec
