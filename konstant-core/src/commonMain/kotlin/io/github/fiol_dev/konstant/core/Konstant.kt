package io.github.fiol_dev.konstant.core

import kotlin.reflect.KClass

/**
 * Global config registry. Initialize once at startup, then access typed configs
 * anywhere in your codebase.
 *
 * ```kotlin
 * // At startup
 * Konstant.init {
 *     sources {
 *         +EnvSource()
 *         +loadTomlFile("config.toml")
 *     }
 * }
 * Konstant.register(loader.loadAppConfig().getOrThrow())
 *
 * // Anywhere else
 * val db = Konstant.get<DatabaseConfig>()
 * val port = Konstant[AppConfig::class].server.port
 * ```
 */
object Konstant {
    private val configs = mutableMapOf<KClass<*>, Any>()
    private var _loader: ConfigLoader? = null

    @Suppress("unused")
    val loader: ConfigLoader
        get() = _loader ?: error("Konstant not initialized. Call Konstant.init { ... } first.")

    /**
     * Initialize the global [ConfigLoader] with the given source configuration.
     */
    @Suppress("unused")
    fun init(block: ConfigLoaderBuilder.() -> Unit): ConfigLoader {
        val l = ConfigLoader(block)
        _loader = l
        return l
    }

    /**
     * Register a loaded config instance for global access.
     */
    inline fun <reified T : Any> register(config: T) {
        register(T::class, config)
    }

    fun <T : Any> register(type: KClass<T>, config: T) {
        configs[type] = config
    }

    /**
     * Retrieve a previously registered config by type.
     */
    inline fun <reified T : Any> get(): T = get(T::class)

    @Suppress("UNCHECKED_CAST")
    operator fun <T : Any> get(type: KClass<T>): T {
        return configs[type] as? T
            ?: error("No config registered for ${type.simpleName}. Call Konstant.register(...) first.")
    }

    /**
     * Check if a config type has been registered.
     */
    fun <T : Any> has(config: T): Boolean = configs.containsKey(config::class)

    /**
     * Clear all registered configs and the loader. Useful for testing.
     */
    fun reset() {
        configs.clear()
        _loader = null
    }
}
