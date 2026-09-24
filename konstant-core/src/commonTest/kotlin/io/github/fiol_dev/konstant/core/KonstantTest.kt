package io.github.fiol_dev.konstant.core

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
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

data class TestReplicatedConfig(
    val primary: TestDbConfig,
    val replica: TestDbConfig,
)

class KonstantTest {

    private val config = TestAppConfig(
        appName = "TestApp",
        db = TestDbConfig(url = "jdbc:test", port = 5432),
        server = TestServerConfig(host = "localhost", debug = true),
    )

    // Konstant is global, so start clean whatever ran before
    @BeforeTest
    fun setup() {
        Konstant.reset()
    }

    @AfterTest
    fun cleanup() {
        Konstant.reset()
    }

    @Test
    fun installedRootAndNestedConfigsAreReadable() {
        Konstant.install(config, listOf(config.db, config.server))

        assertTrue(Konstant.isInitialized)
        assertSame(config, Konstant.get<TestAppConfig>())
        assertEquals("jdbc:test", Konstant.get<TestDbConfig>().url)
        assertEquals("localhost", Konstant[TestServerConfig::class].host)
    }

    @Test
    fun readingBeforeInitExplainsWhatToCall() {
        assertFalse(Konstant.isInitialized)
        val error = assertFailsWith<IllegalStateException> { Konstant.get<TestDbConfig>() }
        assertTrue("not initialized" in error.message.orEmpty(), error.message)
    }

    @Test
    fun missingTypeReturnsNullFromGetOrNull() {
        Konstant.install(config)

        assertNull(Konstant.getOrNull<TestDbConfig>())
        assertFailsWith<IllegalStateException> { Konstant.get<TestDbConfig>() }
    }

    @Test
    fun secondInstallFailsUntilReset() {
        Konstant.install(config)

        assertFailsWith<IllegalStateException> { Konstant.install(config) }

        Konstant.reset()
        Konstant.install(config.copy(appName = "Other"))
        assertEquals("Other", Konstant.get<TestAppConfig>().appName)
    }

    @Test
    fun typeUsedTwiceIsNotGuessed() {
        val replicated = TestReplicatedConfig(TestDbConfig("a", 1), TestDbConfig("b", 2))
        Konstant.install(replicated, listOf(replicated.primary, replicated.replica))

        val error = assertFailsWith<IllegalStateException> { Konstant.get<TestDbConfig>() }
        assertTrue("more than once" in error.message.orEmpty(), error.message)
        assertEquals("b", Konstant.get<TestReplicatedConfig>().replica.url)
    }

    @Test
    fun sameInstanceListedTwiceIsNotAmbiguous() {
        Konstant.install(config, listOf(config.db, config.db))

        assertSame(config.db, Konstant.get<TestDbConfig>())
    }

    @Suppress("DEPRECATION")
    @Test
    fun deprecatedRegisterStillWorks() {
        Konstant.register(config)
        Konstant.register(config.db)

        assertSame(config, Konstant.get<TestAppConfig>())
        assertTrue(Konstant.has(config.db))
    }
}
