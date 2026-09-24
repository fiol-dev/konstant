package io.github.fiol_dev.konstant.json

import io.github.fiol_dev.konstant.core.Converters
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class JsonSourceTest {

    private val json = """
        {
          // comments and trailing commas are allowed
          "title": "My App",
          "port": 8080,
          "ratio": 1.50,
          "debug": true,
          "missing": null,
          "tags": ["a", "b,c", 3, null],
          "database": { "url": "jdbc:x", "max": { "pool": 10 } },
          "servers": [ { "name": "one" }, { "name": "two" } ],
        }
    """.trimIndent()

    private val entries = JsonSource.flatten(json)

    @Test
    fun flattensScalarsAndObjects() {
        assertEquals("My App", entries["title"])
        assertEquals("8080", entries["port"])
        assertEquals("true", entries["debug"])
        assertEquals("jdbc:x", entries["database.url"])
        assertEquals("10", entries["database.max.pool"])
    }

    @Test
    fun keepsNumbersAsWritten() {
        assertEquals("1.50", entries["ratio"])
    }

    @Test
    fun skipsNulls() {
        assertTrue("missing" !in entries)
    }

    @Test
    fun rendersScalarArraysAsQuotedLists() {
        assertEquals("[\"a\", \"b,c\", 3]", entries["tags"])
        assertEquals(listOf("a", "b,c", "3"), Converters.list(entries.getValue("tags")) { it })
    }

    @Test
    fun listItemsWithQuotesAndBackslashesRoundTrip() {
        val raw = JsonSource.flatten("""{"a": ["x\", \"y", "C:\\dir\\", "z"]}""").getValue("a")
        assertEquals(listOf("x\", \"y", "C:\\dir\\", "z"), Converters.list(raw) { it })
    }

    @Test
    fun numbersArraysOfObjects() {
        assertEquals("one", entries["servers.0.name"])
        assertEquals("two", entries["servers.1.name"])
    }

    @Test
    fun sourceLooksUpDotAndScreamingSnakeKeys() {
        val source = JsonSource.fromString(json)
        assertEquals("jdbc:x", source.get("database.url"))
        assertEquals(mapOf("url" to "jdbc:x", "max.pool" to "10"), source.children("database"))
    }

    @Test
    fun emptyInputGivesNoEntries() {
        assertTrue(JsonSource.flatten("").isEmpty())
        assertTrue(JsonSource.flatten("{}").isEmpty())
        assertTrue(JsonSource.flatten("// only a comment\n/* and another */").isEmpty())
    }

    @Test
    fun ignoresByteOrderMark() {
        assertEquals("1", JsonSource.flatten("\uFEFF{\"a\": 1}")["a"])
    }

    @Test
    fun invalidJsonThrows() {
        assertFailsWith<IllegalArgumentException> { JsonSource.flatten("{\"a\": ") }
    }
}
