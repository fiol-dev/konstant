package io.github.fiol_dev.konstant.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ConfigErrorTest {

    @Test
    fun errorsCompareByValue() {
        assertEquals(ConfigError.MissingRequired("port"), ConfigError.MissingRequired("port"))
        assertEquals(ConfigError.MissingRequired("port").hashCode(), ConfigError.MissingRequired("port").hashCode())
        assertNotEquals<ConfigError>(ConfigError.MissingRequired("port"), ConfigError.MissingRequired("host"))
        assertEquals(
            ConfigError.NestedFailure("db", listOf(ConfigError.ValidationFailed("db.port", "0", "too small"))),
            ConfigError.NestedFailure("db", listOf(ConfigError.ValidationFailed("db.port", "0", "too small"))),
        )
    }

    @Test
    fun differentKindsWithTheSameKeyAreNotEqual() {
        assertNotEquals<ConfigError>(
            ConfigError.ValidationFailed("port", null, "x"),
            ConfigError.ConversionFailed("port", "x", "Int", "x"),
        )
    }

    @Test
    fun toStringNamesTheKindAndFields() {
        assertEquals("MissingRequired(key=port)", ConfigError.MissingRequired("port").toString())
        assertEquals(
            "ConversionFailed(key=port, rawValue=abc, targetType=Int, cause=bad)",
            ConfigError.ConversionFailed("port", "abc", "Int", "bad").toString(),
        )
    }

    @Test
    fun reportEntriesCompareByValue() {
        assertEquals(ConfigReport.Entry("a", null, "missing"), ConfigReport.Entry("a", null, "missing"))
        assertNotEquals(ConfigReport.Entry("a", "1", "default"), ConfigReport.Entry("a", "2", "default"))
        assertEquals("Entry(key=a, value=null, origin=missing)", ConfigReport.Entry("a", null, "missing").toString())
    }
}
