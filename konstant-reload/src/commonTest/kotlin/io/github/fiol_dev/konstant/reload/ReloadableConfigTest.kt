package io.github.fiol_dev.konstant.reload

import io.github.fiol_dev.konstant.core.ConfigError
import io.github.fiol_dev.konstant.core.ConfigException
import io.github.fiol_dev.konstant.core.ConfigLoader
import io.github.fiol_dev.konstant.core.ConfigResult
import io.github.fiol_dev.konstant.core.ConfigSource
import io.github.fiol_dev.konstant.core.KeyFormat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class ReloadableConfigTest {

    private data class Settings(val port: Int)

    private class FakeSource(var port: String?) : ReloadableSource {
        override val keyFormat = KeyFormat.DOT_NOTATION
        override fun get(key: String): String? = if (key == "port") port else null
        override val changes = MutableSharedFlow<Unit>()
    }

    // Stands in for a generated loadSettings()
    private fun ConfigLoader.loadSettings(): ConfigResult<Settings> {
        val raw = sources.firstNotNullOfOrNull { it.get("port") }
            ?: return ConfigResult.Failure(listOf(ConfigError.MissingRequired("port")))
        val port = raw.toIntOrNull()
            ?: return ConfigResult.Failure(listOf(ConfigError.ConversionFailed("port", raw, "Int", "not a number")))
        return ConfigResult.Success(Settings(port))
    }

    private fun reloadable(source: FakeSource) =
        ReloadableConfig.load(load = { loadSettings() }) { sources { +source } }

    @Test
    fun loadsOnCreation() {
        assertEquals(Settings(8080), reloadable(FakeSource("8080")).current)
    }

    @Test
    fun failsOnCreationWhenInvalid() {
        assertFailsWith<ConfigException> { reloadable(FakeSource("nope")) }
    }

    @Test
    fun reloadPublishesValidChanges() = runTest {
        val source = FakeSource("8080")
        val config = reloadable(source)
        assertEquals(ReloadResult.Unchanged(Settings(8080)), config.reload())
        source.port = "9090"
        assertEquals(ReloadResult.Updated(Settings(9090)), config.reload())
        assertEquals(Settings(9090), config.config.value)
    }

    @Test
    fun invalidReloadKeepsLastGoodConfigAndReportsIt() = runTest {
        val source = FakeSource("8080")
        val config = reloadable(source)
        val failure = backgroundScope.async { config.failures.first() }
        runCurrent()
        source.port = "oops"
        val result = assertIs<ReloadResult.Failed<Settings>>(config.reload())
        assertIs<ConfigException>(result.error)
        assertEquals(Settings(8080), config.current)
        assertIs<ConfigException>(failure.await())
    }

    @Test
    fun sourceThatThrowsIsReportedNotPropagated() = runTest {
        var fail = false
        val config = ReloadableConfig.load(load = { loadSettings() }) {
            if (fail) throw IllegalArgumentException("config.toml not found")
            sources { +FakeSource("8080") }
        }
        fail = true
        assertIs<IllegalArgumentException>(assertIs<ReloadResult.Failed<Settings>>(config.reload()).error)
        assertEquals(Settings(8080), config.current)
    }

    @Test
    fun errorsInTriggeredReloadsDoNotCancelTheScope() = runTest {
        var fail = false
        val config = ReloadableConfig.load(load = { loadSettings() }) {
            if (fail) TODO("not implemented yet")
            sources { +FakeSource("8080") }
        }
        val triggers = MutableSharedFlow<Unit>()
        val job = config.reloadOn(backgroundScope, triggers)
        runCurrent()
        fail = true
        triggers.emit(Unit)
        runCurrent()
        assertTrue(job.isActive)
        assertEquals(Settings(8080), config.current)
    }

    @Test
    fun reloadEveryPollsOnSchedule() = runTest {
        val source = FakeSource("8080")
        val config = reloadable(source)
        config.reloadEvery(backgroundScope, 10.seconds)
        source.port = "9090"
        advanceTimeBy(5.seconds)
        assertEquals(8080, config.current.port)
        advanceTimeBy(6.seconds)
        assertEquals(9090, config.current.port)
    }

    @Test
    fun watchReloadsWhenAWatchedSourceChanges() = runTest {
        val source = FakeSource("8080")
        val config = reloadable(source)
        config.watch(backgroundScope, source)
        runCurrent()
        source.port = "7070"
        source.changes.emit(Unit)
        runCurrent()
        assertEquals(7070, config.current.port)
    }

    @Test
    fun watchNeedsASource() = runTest {
        val config = reloadable(FakeSource("8080"))
        assertFailsWith<IllegalArgumentException> { config.watch(backgroundScope) }
    }

    @Test
    fun reloadEveryRejectsNonPositivePeriod() = runTest {
        val config = reloadable(FakeSource("8080"))
        assertFailsWith<IllegalArgumentException> { config.reloadEvery(backgroundScope, 0.seconds) }
    }
}
