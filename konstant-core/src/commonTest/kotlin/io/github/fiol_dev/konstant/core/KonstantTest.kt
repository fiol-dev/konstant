package io.github.fiol_dev.konstant.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertTrue

data class TestDbConfig(
    val url: String,
    val port: Int,
)

data class TestServerConfig(
    val host: String,
    val debug: Boolean,
)

data class TestAppConfig(
    val appName: String,
    val db: TestDbConfig,
    val server: TestServerConfig,
)

class KonstantTest {

    private fun sampleConfig() = TestAppConfig(
        appName = "TestApp",
        db = TestDbConfig(url = "jdbc:test", port = 5432),
        server = TestServerConfig(host = "localhost", debug = true),
    )

    @Test
    fun register_and_get() {
        Konstant.reset()
        val cfg = sampleConfig()
        Konstant.register(cfg)

        val retrieved = Konstant.get<TestAppConfig>()
        assertEquals(cfg, retrieved)
    }

    @Test
    fun get_byClass() {
        Konstant.reset()
        val cfg = sampleConfig()
        Konstant.register(cfg)

        val retrieved = Konstant[TestAppConfig::class]
        assertEquals("TestApp", retrieved.appName)
    }

    @Test
    fun register_multiple_types() {
        Konstant.reset()
        val db = TestDbConfig("jdbc:test", 3306)
        val server = TestServerConfig("0.0.0.0", false)
        Konstant.register(db)
        Konstant.register(server)

        assertEquals("jdbc:test", Konstant.get<TestDbConfig>().url)
        assertEquals("0.0.0.0", Konstant.get<TestServerConfig>().host)
    }

    @Test
    fun get_unregistered_throws() {
        Konstant.reset()
        assertFails { Konstant.get<TestDbConfig>() }
    }

    @Test
    fun has_returnsCorrectly() {
        val testDbConfig = TestDbConfig("jdbc:test", 3306)
        Konstant.reset()
        assertFalse(Konstant.has(testDbConfig))
        Konstant.register(testDbConfig)
        assertTrue(Konstant.has(testDbConfig))
    }

    @Test
    fun reset_clearsAll() {
        val sampleConfig = sampleConfig()
        Konstant.reset()
        Konstant.register(sampleConfig)
        assertTrue(Konstant.has(sampleConfig))

        Konstant.reset()
        assertFalse(Konstant.has(sampleConfig))
    }
}
