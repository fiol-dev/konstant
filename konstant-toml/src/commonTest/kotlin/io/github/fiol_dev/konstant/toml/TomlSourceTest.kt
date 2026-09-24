package io.github.fiol_dev.konstant.toml

import io.github.fiol_dev.konstant.core.Converters
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TomlSourceTest {

    private val toml = """
        title = "My App"
        port = 8080
        ratio = 1.5
        debug = true
        tags = ["a", "b,c", 3]
        point = { x = 1, y = 2 }
        a.b.c = "dotted"
        path = 'C:\Users\test'
        escaped = "tab\there"
        ml = ""${'"'}
        line1
        line2""${'"'}

        [database]
        url = "jdbc:x"
        max.pool.size = 10

        [server.ssl]
        enabled = true

        [[servers]]
        name = "one"

        [[servers]]
        name = "two"
    """.trimIndent()

    private val entries = TomlSource.flatten(toml)

    @Test
    fun flattensScalarsAndTables() {
        assertEquals("My App", entries["title"])
        assertEquals("8080", entries["port"])
        assertEquals("1.5", entries["ratio"])
        assertEquals("true", entries["debug"])
        assertEquals("jdbc:x", entries["database.url"])
        assertEquals("10", entries["database.max.pool.size"])
        assertEquals("true", entries["server.ssl.enabled"])
        assertEquals("dotted", entries["a.b.c"])
        assertEquals("1", entries["point.x"])
    }

    @Test
    fun keepsStringsExactly() {
        assertEquals("C:\\Users\\test", entries["path"])
        assertEquals("tab\there", entries["escaped"])
        assertEquals("line1\nline2", entries["ml"])
    }

    @Test
    fun rendersArraysAsQuotedLists() {
        assertEquals("[\"a\", \"b,c\", 3]", entries["tags"])
    }

    @Test
    fun listItemsWithQuotesCommasAndBackslashesRoundTrip() {
        val raw = TomlSource.flatten("a = [\"x\\\", \\\"y\", 'C:\\dir\\', 'z']")["a"]!!
        assertEquals(listOf("x\", \"y", "C:\\dir\\", "z"), Converters.list(raw) { it })
    }

    @Test
    fun formatsFloatsTheSameOnEveryPlatform() {
        val floats = TomlSource.flatten("whole = 3.0\nfrac = 1.5\nbig = 1e20")
        assertEquals("3.0", floats["whole"])
        assertEquals("1.5", floats["frac"])
        assertEquals(1e20, floats["big"]!!.toDouble())
    }

    @Test
    fun numbersArrayOfTables() {
        assertEquals("one", entries["servers.0.name"])
        assertEquals("two", entries["servers.1.name"])
    }

    @Test
    fun sourceLooksUpDotAndScreamingSnakeKeys() {
        val source = TomlSource.fromString(toml)
        assertEquals("jdbc:x", source.get("database.url"))
        assertEquals(mapOf("x" to "1", "y" to "2"), source.children("point"))
    }

    @Test
    fun emptyInputGivesNoEntries() {
        assertTrue(TomlSource.flatten("").isEmpty())
        assertTrue(TomlSource.flatten("# only a comment\n").isEmpty())
    }
}
