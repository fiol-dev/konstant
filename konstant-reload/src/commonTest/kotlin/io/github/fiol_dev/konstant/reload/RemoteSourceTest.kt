package io.github.fiol_dev.konstant.reload

import io.github.fiol_dev.konstant.core.ConfigError
import io.github.fiol_dev.konstant.core.ConfigLoader
import io.github.fiol_dev.konstant.core.ConfigResult
import io.github.fiol_dev.konstant.core.ConfigSource
import io.github.fiol_dev.konstant.core.KeyFormat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class RemoteSourceTest {

    private class Defaults : ConfigSource {
        override val keyFormat = KeyFormat.DOT_NOTATION
        override fun get(key: String): String? = if (key == "server.port") "8080" else null
    }

    // Stands in for a generated loadServer()
    private fun ConfigLoader.loadPort(): ConfigResult<Int> {
        val raw = sources.firstNotNullOfOrNull { it.get("server.port") }
            ?: return ConfigResult.Failure(listOf(ConfigError.MissingRequired("server.port")))
        return ConfigResult.Success(raw.toInt())
    }

    @Test
    fun looksUpKeysCaseInsensitivelyAndListsChildren() {
        val remote = RemoteSource(mapOf("Server.Port" to "9090", "flags.dark" to "true"))
        assertEquals("9090", remote.get("server.port"))
        assertEquals(mapOf("dark" to "true"), remote.children("flags"))
        assertNull(remote.get("missing"))
    }

    @Test
    fun reportsOnStartAndOnlyRealChanges() = runTest {
        val remote = RemoteSource(mapOf("a" to "1"))
        val seen = mutableListOf<Unit>()
        backgroundScope.launch { remote.changes.toList(seen) }
        runCurrent()
        assertEquals(1, seen.size)
        remote.update(mapOf("a" to "1"))
        runCurrent()
        assertEquals(1, seen.size)
        remote.update(mapOf("a" to "2"))
        runCurrent()
        assertEquals(2, seen.size)
    }

    @Test
    fun updatesBeforeTheWatcherSubscribesAreNotLost() = runTest {
        val remote = RemoteSource()
        val config = ReloadableConfig.load(load = { loadPort() }) {
            sources {
                +remote
                +Defaults()
            }
        }
        remote.update(mapOf("server.port" to "7070"))
        config.watch(backgroundScope, remote)
        remote.update(mapOf("server.port" to "9090"))
        runCurrent()
        assertEquals(9090, config.current)
    }

    @Test
    fun snakeCaseKeysMatchTheScreamingSnakeFallback() {
        // Firebase keys cannot contain dots, so `server_port` is found as SERVER_PORT
        val remote = RemoteSource(mapOf("server_port" to "9090"))
        assertEquals(listOf(KeyFormat.SCREAMING_SNAKE), remote.fallbackKeyFormats)
        assertEquals("9090", remote.get("SERVER_PORT"))
    }

    @Test
    fun watchedRemoteValuesOverrideDefaultsAndFallBackWhenRemoved() = runTest {
        val remote = RemoteSource()
        val config = ReloadableConfig.load(load = { loadPort() }) {
            sources {
                +remote
                +Defaults()
            }
        }
        config.watch(backgroundScope, remote)
        runCurrent()
        assertEquals(8080, config.current)

        remote.update(mapOf("server.port" to "9090"))
        runCurrent()
        assertEquals(9090, config.current)

        remote.update(emptyMap())
        runCurrent()
        assertEquals(8080, config.current)
    }
}
