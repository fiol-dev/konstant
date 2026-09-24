package io.github.fiol_dev.konstant.koin

import io.github.fiol_dev.konstant.core.Konstant
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFails
import kotlin.test.assertSame

data class DbConfig(val url: String)
data class AppConfig(val name: String, val db: DbConfig)

class Repository(val db: DbConfig)

class KonstantKoinTest {

    private val config = AppConfig("app", DbConfig("jdbc:x"))

    @BeforeTest
    fun setup() {
        Konstant.reset()
    }

    @AfterTest
    fun cleanup() {
        Konstant.reset()
    }

    @Test
    fun injectsRootAndNestedConfigs() {
        Konstant.install(config, listOf(config.db))
        val koin = koinApplication {
            modules(
                module {
                    config<AppConfig>()
                    config<DbConfig>()
                    single { Repository(get()) }
                }
            )
        }.koin

        assertSame(config, koin.get<AppConfig>())
        assertSame(config.db, koin.get<Repository>().db)
    }

    @Test
    fun moduleCanBeDeclaredBeforeConfigIsLoaded() {
        val koin = koinApplication { modules(module { config<DbConfig>() }) }.koin

        Konstant.install(config, listOf(config.db))

        assertSame(config.db, koin.get<DbConfig>())
    }

    @Test
    fun injectingBeforeInitFails() {
        val koin = koinApplication { modules(module { config<DbConfig>() }) }.koin

        assertFails { koin.get<DbConfig>() }
    }
}
