package io.github.fiol_dev.konstant.core

import kotlin.concurrent.Volatile
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/**
 * Property delegate that lazily extracts a field from a registered config.
 *
 * ```kotlin
 * val dbUrl: String by configField<AppConfig, String> { it.database.url }
 * val port: Int by configField<AppConfig, Int> { it.server.port }
 *
 * class UserRepository {
 *     private val dbUrl: String by configField<AppConfig, String> { it.database.url }
 * }
 * ```
 */
public inline fun <reified T : Any, V> configField(
    noinline selector: (T) -> V,
): ReadOnlyProperty<Any?, V> = configField(T::class, selector)

/** [configField] for when the config type is only known as a [KClass]. */
public fun <T : Any, V> configField(type: KClass<T>, selector: (T) -> V): ReadOnlyProperty<Any?, V> =
    ConfigFieldDelegate(type, selector)

/**
 * Property delegate backed by a specific config instance (not the global registry).
 *
 * ```kotlin
 * val appConfig: AppConfig = loader.loadAppConfig().getOrThrow()
 *
 * val dbUrl: String by appConfig.field { it.database.url }
 * val port: Int by appConfig.field { it.server.port }
 * ```
 */
public fun <T, V> T.field(selector: (T) -> V): ReadOnlyProperty<Any?, V> =
    InstanceFieldDelegate(this, selector)

// The cache is volatile so a value read on one thread is seen by the others. Two threads may
// both run the selector the first time; that is harmless because it reads the same snapshot.
internal class ConfigFieldDelegate<T : Any, V>(
    private val type: KClass<T>,
    private val selector: (T) -> V,
) : ReadOnlyProperty<Any?, V> {

    @Volatile
    private var cached: Any? = UNINITIALIZED

    override fun getValue(thisRef: Any?, property: KProperty<*>): V {
        if (cached === UNINITIALIZED) {
            cached = selector(Konstant.get(type))
        }
        @Suppress("UNCHECKED_CAST")
        return cached as V
    }

    private companion object {
        private val UNINITIALIZED = Any()
    }
}

internal class InstanceFieldDelegate<T, V>(
    private val instance: T,
    private val selector: (T) -> V,
) : ReadOnlyProperty<Any?, V> {

    @Volatile
    private var cached: Any? = UNINITIALIZED

    override fun getValue(thisRef: Any?, property: KProperty<*>): V {
        if (cached === UNINITIALIZED) {
            cached = selector(instance)
        }
        @Suppress("UNCHECKED_CAST")
        return cached as V
    }

    private companion object {
        private val UNINITIALIZED = Any()
    }
}
