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
    fun reportsOnlyRealChanges() = runTest {
        val remote = RemoteSource(mapOf("a" to "1"))
        val seen = mutableListOf<Unit>()
        backgroundScope.launch { remote.changes.toList(seen) }
        runCurrent()
        remote.update(mapOf("a" to "1"))
        runCurrent()
        assertEquals(0, seen.size)
        remote.update(mapOf("a" to "2"))
        runCurrent()
        assertEquals(1, seen.size)
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
