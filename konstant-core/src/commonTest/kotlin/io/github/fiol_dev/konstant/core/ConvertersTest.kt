@file:OptIn(InternalKonstantApi::class)

package io.github.fiol_dev.konstant.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class ConvertersTest {

    private enum class Color { RED, DARK_BLUE }

    @Test
    fun booleanAcceptsCommonSpellings() {
        listOf("true", "TRUE", "yes", "on", "1", " true ").forEach { assertEquals(true, Converters.boolean(it)) }
        listOf("false", "No", "off", "0").forEach { assertEquals(false, Converters.boolean(it)) }
    }

    @Test
    fun booleanRejectsAnythingElse() {
        assertFailsWith<IllegalArgumentException> { Converters.boolean("maybe") }
        assertFailsWith<IllegalArgumentException> { Converters.boolean("") }
    }

    @Test
    fun enumMatchesCaseInsensitivelyAndAcceptsDashes() {
        assertEquals(Color.RED, Converters.enumOf(Color.entries, "red"))
        assertEquals(Color.DARK_BLUE, Converters.enumOf(Color.entries, "dark-blue"))
        val error = assertFailsWith<IllegalArgumentException> { Converters.enumOf(Color.entries, "green") }
        assertEquals("expected one of RED, DARK_BLUE", error.message)
    }

    @Test
    fun durationParsesShortAndIsoForms() {
        assertEquals(30.seconds, Converters.duration("30s"))
        assertEquals(1.hours + 30.minutes, Converters.duration("1h 30m"))
        assertEquals(90.seconds, Converters.duration("PT1M30S"))
        assertFailsWith<IllegalArgumentException> { Converters.duration("soon") }
    }

    @Test
    fun listSplitsOnTopLevelCommas() {
        assertEquals(listOf("a", "b", "c"), Converters.list("a, b ,c") { it })
        assertEquals(listOf("a,b", "c"), Converters.list("[\"a,b\", 'c']") { it })
        assertEquals(listOf(1, 2), Converters.list("[1, 2]") { it.toInt() })
        assertEquals(emptyList(), Converters.list("  ") { it })
        assertEquals(emptyList(), Converters.list("[]") { it })
    }

    @Test
    fun mapParsesKeyValuePairs() {
        assertEquals(mapOf("a" to 1, "b" to 2), Converters.map("a=1, b=2") { it.toInt() })
        assertEquals(mapOf("a" to "x=y"), Converters.map("{ \"a\" = \"x=y\" }") { it })
        assertEquals(mapOf("k" to "v"), Converters.map("k: v") { it })
        assertEquals(emptyMap(), Converters.map("") { it })
        assertFailsWith<IllegalArgumentException> { Converters.map("novalue") { it } }
    }

    @Test
    fun apostrophesInsideValuesAreNotQuotes() {
        assertEquals(listOf("O'Brien", "Smith"), Converters.list("O'Brien, Smith") { it })
        assertEquals(mapOf("greeting" to "don't", "retries" to "3"), Converters.map("greeting=don't, retries=3") { it })
        assertEquals(mapOf("a" to "x,y"), Converters.map("a=\"x,y\"") { it })
    }

    @Test
    fun listAllowsTrailingComma() {
        assertEquals(listOf(8080, 8081), Converters.list("[8080, 8081,]") { it.toInt() })
    }

    @Test
    fun mapEntriesConvertsValues() {
        assertEquals(mapOf("a" to 1), Converters.mapEntries(mapOf("a" to "1")) { it.toInt() })
    }

    @Test
    fun doubleQuotedItemsUnescapeQuotesAndBackslashes() {
        assertEquals(listOf("x\", \"y", "C:\\dir\\", "a\\b"), Converters.list("[\"x\\\", \\\"y\", \"C:\\\\dir\\\\\", 'a\\b']") { it })
    }
}
