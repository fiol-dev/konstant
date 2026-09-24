package io.github.fiol_dev.konstant.ksptests

import io.github.fiol_dev.konstant.core.ConfigLoader
import io.github.fiol_dev.konstant.ksptests.baked.KonstantBaked
import io.github.fiol_dev.konstant.test.MapSource
import kotlin.test.Test
import kotlin.test.assertEquals

/** Files baked by the Gradle plugin, see this module's build.gradle.kts and baked/. */
class BakedConfigTest {

    @Test
    fun environmentFileOverridesBaseFile() {
        val config = ConfigLoader { sources { +KonstantBaked.sources } }.loadBakedAppConfig().getOrThrow()

        assertEquals("dev", KonstantBaked.ENVIRONMENT)
        assertEquals("https://dev.example.com", config.apiUrl)
        assertEquals(1, config.retries)
    }

    @Test
    fun runtimeSourcesStillOverrideBakedValues() {
        val config = ConfigLoader {
            sources {
                +MapSource.screamingSnake("RETRIES" to "5")
                +KonstantBaked.sources
            }
        }.loadBakedAppConfig().getOrThrow()

        assertEquals(5, config.retries)
    }
}
