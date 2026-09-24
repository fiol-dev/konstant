package io.github.fiol_dev.konstant.core

import kotlin.properties.ReadOnlyProperty
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
inline fun <reified T : Any, V> configField(
    noinline selector: (T) -> V,
): ReadOnlyProperty<Any?, V> = ConfigFieldDelegate(T::class, selector)

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
fun <T, V> T.field(selector: (T) -> V): ReadOnlyProperty<Any?, V> =
    InstanceFieldDelegate(this, selector)

@PublishedApi
internal class ConfigFieldDelegate<T : Any, V>(
    private val type: kotlin.reflect.KClass<T>,
    private val selector: (T) -> V,
) : ReadOnlyProperty<Any?, V> {

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
