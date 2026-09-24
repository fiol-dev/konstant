@file:OptIn(ExperimentalAtomicApi::class)

package io.github.fiol_dev.konstant.core

import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.reflect.KClass

/**
 * Global access to the loaded config. Load it once at startup with the generated
 * `init<Spec>` function, then read any spec, root or nested, from anywhere:
 *
 * ```kotlin
 * // At startup
 * Konstant.initAppConfig {
 *     sources {
 *         +EnvSource()
 *         +loadTomlResource("config.toml")
 *     }
 * }
 *
 * // Anywhere else, no register() calls needed for nested specs
 * val db = Konstant.get<DatabaseConfig>()
 * val port = Konstant[AppConfig::class].server.port
 * ```
 *
 * The configs are published once as an immutable snapshot, so reads are safe from any
 * thread on every platform. Initializing twice fails; call [reset] between tests.
 */
public object Konstant {
    private val snapshot = AtomicReference(Snapshot.EMPTY)

    /** True once a config has been installed. */
    public val isInitialized: Boolean
        get() = snapshot.load().configs.isNotEmpty()

    /**
     * Publishes [root] and its [nested] specs. Called by the generated `Konstant.init<Spec>`
     * functions, which pass every nested spec; prefer those.
     *
     * @throws IllegalStateException if a config was already installed.
     */
    public fun install(root: Any, nested: List<Any> = emptyList()) {
        val configs = mutableMapOf<KClass<*>, Any>()
        val ambiguous = mutableSetOf<KClass<*>>()
        for (part in nested) {
            val type = part::class
            if (type in configs && configs[type] !== part) ambiguous += type
            configs[type] = part
        }
        configs[root::class] = root
        ambiguous -= root::class
        val next = Snapshot(configs - ambiguous, ambiguous)
        check(snapshot.compareAndSet(Snapshot.EMPTY, next)) {
            "Konstant is already initialized. Call Konstant.reset() first (for example between tests)."
        }
    }

    /** The loaded config of type [T], root or nested. */
    public inline fun <reified T : Any> get(): T = get(T::class)

    public operator fun <T : Any> get(type: KClass<T>): T =
        getOrNull(type) ?: error(missingMessage(type))

    /** The loaded config of type [T], or null if there is none. */
    public inline fun <reified T : Any> getOrNull(): T? = getOrNull(T::class)

    @Suppress("UNCHECKED_CAST")
    public fun <T : Any> getOrNull(type: KClass<T>): T? = snapshot.load().configs[type] as T?

    /** Clears the loaded config. Meant for tests. */
    public fun reset() {
        snapshot.store(Snapshot.EMPTY)
    }

    private fun missingMessage(type: KClass<*>): String {
        val current = snapshot.load()
        return when {
            type in current.ambiguous ->
                "${type.simpleName} appears more than once in the loaded config. " +
                    "Read it through its parent, e.g. Konstant.get<Root>().field."
            current.configs.isEmpty() ->
                "Konstant is not initialized. Call the generated Konstant.init${type.simpleName} { ... } " +
                    "(or init for your root spec) at startup."
            else -> "No config of type ${type.simpleName} was loaded."
        }
    }

    // ---- Deprecated manual registry ----

    @Deprecated("Use the generated Konstant.init<Spec> { ... }, which loads and installs the config.")
    public val loader: ConfigLoader
        get() = legacyLoader.load() ?: error("Konstant not initialized. Call Konstant.init { ... } first.")

    private val legacyLoader = AtomicReference<ConfigLoader?>(null)

    @Deprecated("Use the generated Konstant.init<Spec> { ... }, which loads and installs the config.")
    public fun init(block: ConfigLoaderBuilder.() -> Unit): ConfigLoader {
        val l = ConfigLoader(block)
        legacyLoader.store(l)
        return l
    }

    @Deprecated("Nested specs are installed automatically by the generated Konstant.init<Spec> { ... }.")
    public inline fun <reified T : Any> register(config: T) {
        @Suppress("DEPRECATION")
        register(T::class, config)
    }

    @Deprecated("Nested specs are installed automatically by the generated Konstant.init<Spec> { ... }.")
    public fun <T : Any> register(type: KClass<T>, config: T) {
        while (true) {
            val current = snapshot.load()
            val next = Snapshot(current.configs + (type to config), current.ambiguous - type)
            if (snapshot.compareAndSet(current, next)) return
        }
    }

    @Deprecated("Use getOrNull<T>() != null.")
    public fun <T : Any> has(config: T): Boolean = snapshot.load().configs.containsKey(config::class)

    private class Snapshot(val configs: Map<KClass<*>, Any>, val ambiguous: Set<KClass<*>>) {
        companion object {
            val EMPTY = Snapshot(emptyMap(), emptySet())
        }
    }
}
