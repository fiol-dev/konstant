@file:OptIn(InternalKonstantApi::class)

package io.github.fiol_dev.konstant.yaml

import io.github.fiol_dev.konstant.core.Converters
import io.github.fiol_dev.konstant.core.InternalKonstantApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class YamlSourceTest {

    private val yaml = """
        title: My App
        port: 8080
        debug: true
        empty: ~
        tags: [a, "b,c", 3]
        hosts:
          - one.example.com
          - two.example.com
        defaults: &defaults
          timeout: 30
        database:
          <<: *defaults
          url: "jdbc:x"
          pool:
            size: 10
        description: |
          line1
          line2
        servers:
          - name: one
          - name: two
    """.trimIndent()

    private val entries = YamlSource.flatten(yaml)

    @Test
    fun flattensScalarsAndMappings() {
        assertEquals("My App", entries["title"])
        assertEquals("8080", entries["port"])
        assertEquals("true", entries["debug"])
        assertEquals("jdbc:x", entries["database.url"])
        assertEquals("10", entries["database.pool.size"])
        assertEquals("30", entries["defaults.timeout"])
        assertEquals("30", entries["database.timeout"]) // merged from the anchor
        assertEquals("line1\nline2\n", entries["description"])
    }

    @Test
    fun skipsNulls() {
        assertTrue("empty" !in entries)
    }

    @Test
    fun rendersScalarListsAsQuotedLists() {
        assertEquals("[\"a\", \"b,c\", \"3\"]", entries["tags"])
        assertEquals("[\"one.example.com\", \"two.example.com\"]", entries["hosts"])
    }

    @Test
    fun listItemsWithQuotesCommasAndBackslashesRoundTrip() {
        val raw = YamlSource.flatten("a: ['x\", \"y', 'C:\\dir\\', z, ~]")["a"]!!
        assertEquals(listOf("x\", \"y", "C:\\dir\\", "z"), Converters.list(raw) { it })
    }

    @Test
    fun numbersListsOfMappings() {
        assertEquals("one", entries["servers.0.name"])
        assertEquals("two", entries["servers.1.name"])
    }

    @Test
    fun sourceLooksUpDotKeys() {
        val source = YamlSource.fromString(yaml)
        assertEquals("jdbc:x", source.get("database.url"))
        assertEquals(mapOf("size" to "10"), source.children("database.pool"))
    }

    @Test
    fun emptyInputGivesNoEntries() {
        assertTrue(YamlSource.flatten("").isEmpty())
        assertTrue(YamlSource.flatten("# only a comment\n").isEmpty())
    }
}
