package io.github.fiol_dev.konstant.ksptests

import io.github.fiol_dev.konstant.core.ConfigException
import io.github.fiol_dev.konstant.core.Konstant
import io.github.fiol_dev.konstant.test.MapSource
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame

class KonstantHolderTest {

    @AfterTest
    fun cleanup() {
        Konstant.reset()
    }

    @Test
    fun initInstallsRootAndEveryNestedSpec() {
        val config = Konstant.initAppConfig {
            sources { +MapSource.screamingSnake("DATABASE_URL" to "jdbc:x", "DATABASE_POOL_SIZE" to "3") }
        }

        assertSame(config, Konstant.get<AppConfig>())
        assertSame(config.database, Konstant.get<DatabaseConfig>())
        assertEquals(3, Konstant.get<PoolConfig>().size)
    }

    @Test
    fun failedLoadThrowsAndInstallsNothing() {
        assertFailsWith<ConfigException> {
            Konstant.initAppConfig { sources { +MapSource.screamingSnake() } }
        }
        assertFalse(Konstant.isInitialized)
    }

    @Test
    fun nestedConfigsAreListedAtEveryDepth() {
        val config = AppConfig(database = DatabaseConfig(url = "u", pool = PoolConfig(2)))

        assertEquals(listOf(config.database, config.database.pool), config.konstantNestedConfigs())
    }
}
