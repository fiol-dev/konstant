package io.github.fiol_dev.konstant.koin

import io.github.fiol_dev.konstant.core.Konstant
import org.koin.core.definition.KoinDefinition
import org.koin.core.module.Module

/**
 * Declares the loaded config of type [T] (root or nested spec) as a Koin single.
 *
 * The value is read from [Konstant] when it is first injected, so the module can be declared
 * before `Konstant.init<Spec>` runs, as long as init happens before anything injects it.
 *
 * ```kotlin
 * val configModule = module {
 *     config<AppConfig>()
 *     config<DatabaseConfig>()
 * }
 *
 * class UserRepository(private val db: DatabaseConfig)
 * val dataModule = module { singleOf(::UserRepository) }
 * ```
 */
public inline fun <reified T : Any> Module.config(): KoinDefinition<T> = single { Konstant.get<T>() }
