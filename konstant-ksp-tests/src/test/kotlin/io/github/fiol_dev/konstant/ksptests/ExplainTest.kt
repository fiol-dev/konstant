package io.github.fiol_dev.konstant.ksptests

import io.github.fiol_dev.konstant.core.ConfigLoader
import io.github.fiol_dev.konstant.core.ConfigReport
import io.github.fiol_dev.konstant.core.ConfigResult
import io.github.fiol_dev.konstant.sources.PropertiesSource
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
                ConfigReport.Entry("password", "***", "#1 MapSource", sourceKey = "PASSWORD"),
                ConfigReport.Entry("pin", "***", ConfigReport.DEFAULT),
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
            listOf("app.name" to "#1 MapSource", "database.url" to ConfigReport.MISSING),
            report.entries.take(2).map { it.key to it.origin },
        )
        assertEquals(null, report.entries[1].value)
    }

    @Test
    fun eachReportCoversOnlyItsOwnLoad() {
        val loader = ConfigLoader { sources { +MapSource.screamingSnake("DATABASE_URL" to "u") } }

        loader.explain { loadAppConfig() }
        val report = loader.explain { loadDatabaseConfig() }

        assertEquals(listOf("url", "max.pool.size", "pool.size"), report.entries.map { it.key })
    }

    @Test
    fun namesFoundAndMissingKeysTheSameWayAndLabelsFiles() {
        val loader = ConfigLoader {
            sources {
                +MapSource.screamingSnake("DATABASE_MAX_POOL_SIZE" to "5")
                +PropertiesSource.fromString("database.url=jdbc:x", origin = "config/app.properties")
                +PropertiesSource.fromString("", origin = "config/app.dev.properties")
            }
        }

        val report = loader.explain { loadAppConfig() }

        assertEquals(
            listOf(
                ConfigReport.Entry("app.name", "MyApp", ConfigReport.DEFAULT),
                ConfigReport.Entry("database.url", "jdbc:x", "#2 PropertiesSource(config/app.properties)"),
                ConfigReport.Entry("database.max.pool.size", "5", "#1 MapSource", sourceKey = "DATABASE_MAX_POOL_SIZE"),
                ConfigReport.Entry("database.pool.size", "1", ConfigReport.DEFAULT),
            ),
            report.entries,
        )
        assertTrue("#1 MapSource as DATABASE_MAX_POOL_SIZE" in report.toString(), report.toString())
    }
}
