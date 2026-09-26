package io.github.fiol_dev.konstant.ksptests

import io.github.fiol_dev.konstant.core.ConfigError
import io.github.fiol_dev.konstant.core.ConfigLoader
import io.github.fiol_dev.konstant.core.ConfigReport
import io.github.fiol_dev.konstant.core.ConfigResult
import io.github.fiol_dev.konstant.test.MapSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class OptionalNestedTest {

    private fun load(vararg pairs: Pair<String, String>) =
        ConfigLoader { sources { +MapSource.screamingSnake(*pairs) } }.loadAuthConfig()

    @Test
    fun absentSectionIsNullOrItsDefault() {
        val config = assertIs<ConfigResult.Success<AuthConfig>>(load()).value

        assertNull(config.oidc)
        assertEquals(FeatureFlags(beta = true), config.flags)
        assertEquals(emptyList(), config.konstantNestedConfigs().filterIsInstance<OidcConfig>())
    }

    @Test
    fun sectionWithAnyKeySetIsLoaded() {
        val config = assertIs<ConfigResult.Success<AuthConfig>>(
            load("OIDC_ISSUER" to "https://id", "OIDC_CLIENT_ID" to "app", "FLAGS_BETA" to "false"),
        ).value

        assertEquals(OidcConfig("https://id", "app", listOf("openid")), config.oidc)
        assertEquals(FeatureFlags(beta = false), config.flags)
    }

    @Test
    fun partlySetSectionReportsItsMissingKeys() {
        val failure = assertIs<ConfigResult.Failure>(load("OIDC_ISSUER" to "https://id"))

        assertEquals(listOf<ConfigError>(ConfigError.MissingRequired("OIDC_CLIENT_ID")), failure.errors)
    }

    @Test
    fun explainShowsAnAbsentSectionAsOneLine() {
        val report = ConfigLoader { sources { +MapSource.screamingSnake() } }.explain { loadAuthConfig() }

        assertEquals(
            listOf(
                ConfigReport.Entry("realm", "main", ConfigReport.DEFAULT),
                ConfigReport.Entry("oidc", null, ConfigReport.DEFAULT),
                ConfigReport.Entry("flags", null, ConfigReport.DEFAULT),
            ),
            report.entries,
        )
    }
}
