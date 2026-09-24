package io.github.fiol_dev.konstant.ksptests

import io.github.fiol_dev.konstant.core.ConfigLoader
import io.github.fiol_dev.konstant.core.ConfigReport
import io.github.fiol_dev.konstant.core.ConfigResult
import io.github.fiol_dev.konstant.test.MapSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ExplainTest {

    @Test
    fun reportsWinningSourceDefaultsAndMaskedSecrets() {
        val loader = ConfigLoader {
            sources {
                +MapSource.screamingSnake("PASSWORD" to "hunter2")
                +MapSource.screamingSnake("CUSTOM_API_KEY" to "k", "PASSWORD" to "ignored")
            }
        }

        val report = loader.explain { loadAnnotatedConfig() }

        assertIs<ConfigResult.Success<AnnotatedConfig>>(report.result)
        assertEquals(
            listOf(
                ConfigReport.Entry("CUSTOM_API_KEY", "k", "#2 MapSource"),
                ConfigReport.Entry("PASSWORD", "***", "#1 MapSource"),
                ConfigReport.Entry("PIN", "***", ConfigReport.DEFAULT),
            ),
            report.entries,
        )
        assertTrue("hunter2" !in report.toString(), report.toString())
    }

    @Test
    fun reportsNestedKeysAndMissingValues() {
        val report = ConfigLoader { sources { +MapSource.screamingSnake("APP_NAME" to "Demo") } }
            .explain { loadAppConfig() }

        assertIs<ConfigResult.Failure>(report.result)
        assertEquals(
            listOf("APP_NAME" to "#1 MapSource", "DATABASE_URL" to ConfigReport.MISSING),
            report.entries.take(2).map { it.key to it.origin },
        )
        assertEquals(null, report.entries[1].value)
    }

    @Test
    fun eachReportCoversOnlyItsOwnLoad() {
        val loader = ConfigLoader { sources { +MapSource.screamingSnake("DATABASE_URL" to "u") } }

        loader.explain { loadAppConfig() }
        val report = loader.explain { loadDatabaseConfig() }

        assertEquals(listOf("URL", "MAX_POOL_SIZE", "POOL_SIZE"), report.entries.map { it.key })
    }
}
