package io.github.fioldev.konfigure.core

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

class KonfigureTest {

    private fun sampleConfig() = TestAppConfig(
        appName = "TestApp",
        db = TestDbConfig(url = "jdbc:test", port = 5432),
        server = TestServerConfig(host = "localhost", debug = true),
    )

    @Test
    fun register_and_get() {
        Konfigure.reset()
        val cfg = sampleConfig()
        Konfigure.register(cfg)

        val retrieved = Konfigure.get<TestAppConfig>()
        assertEquals(cfg, retrieved)
    }

    @Test
    fun get_byClass() {
        Konfigure.reset()
        val cfg = sampleConfig()
        Konfigure.register(cfg)

        val retrieved = Konfigure[TestAppConfig::class]
        assertEquals("TestApp", retrieved.appName)
    }

    @Test
    fun register_multiple_types() {
        Konfigure.reset()
        val db = TestDbConfig("jdbc:test", 3306)
        val server = TestServerConfig("0.0.0.0", false)
        Konfigure.register(db)
        Konfigure.register(server)

        assertEquals("jdbc:test", Konfigure.get<TestDbConfig>().url)
        assertEquals("0.0.0.0", Konfigure.get<TestServerConfig>().host)
    }

    @Test
    fun get_unregistered_throws() {
        Konfigure.reset()
        assertFails { Konfigure.get<TestDbConfig>() }
    }

    @Test
    fun has_returnsCorrectly() {
        Konfigure.reset()
        assertFalse(Konfigure.has<TestDbConfig>())
        Konfigure.register(TestDbConfig("test", 5432))
        assertTrue(Konfigure.has<TestDbConfig>())
    }

    @Test
    fun reset_clearsAll() {
        Konfigure.reset()
        Konfigure.register(sampleConfig())
        assertTrue(Konfigure.has<TestAppConfig>())

        Konfigure.reset()
        assertFalse(Konfigure.has<TestAppConfig>())
    }
}
