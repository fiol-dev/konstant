package io.github.fioldev.konfigure.sources

import io.github.fioldev.konfigure.sources.parser.TomlParser
import kotlin.test.Test
import kotlin.test.assertEquals

class TomlParserTest {

    @Test
    fun parse_basicKeyValue() {
        val content = """
            title = "My App"
            debug = true
            port = 8080
        """.trimIndent()
        val result = TomlParser.parse(content)
        assertEquals("My App", result["title"])
        assertEquals("true", result["debug"])
        assertEquals("8080", result["port"])
    }

    @Test
    fun parse_table() {
        val content = """
            [database]
            url = "jdbc:postgresql://localhost"
            port = 5432
        """.trimIndent()
        val result = TomlParser.parse(content)
        assertEquals("jdbc:postgresql://localhost", result["database.url"])
        assertEquals("5432", result["database.port"])
    }

    @Test
    fun parse_nestedTables() {
        val content = """
            [server]
            host = "0.0.0.0"

            [server.ssl]
            enabled = true
            port = 443
        """.trimIndent()
        val result = TomlParser.parse(content)
        assertEquals("0.0.0.0", result["server.host"])
        assertEquals("true", result["server.ssl.enabled"])
        assertEquals("443", result["server.ssl.port"])
    }

    @Test
    fun parse_multipleTables() {
        val content = """
            [database]
            url = "jdbc:test"
            port = 5432

            [server]
            host = "localhost"
            port = 8080
        """.trimIndent()
        val result = TomlParser.parse(content)
        assertEquals("jdbc:test", result["database.url"])
        assertEquals("5432", result["database.port"])
        assertEquals("localhost", result["server.host"])
        assertEquals("8080", result["server.port"])
    }

    @Test
    fun parse_comments() {
        val content = """
            # This is a comment
            key = "value" # inline comment
            other = 42
        """.trimIndent()
        val result = TomlParser.parse(content)
        assertEquals("value", result["key"])
        assertEquals("42", result["other"])
        assertEquals(2, result.size)
    }

    @Test
    fun parse_literalString() {
        val content = "path = 'C:\\Users\\test'"
        val result = TomlParser.parse(content)
        assertEquals("C:\\Users\\test", result["path"])
    }

    @Test
    fun parse_escapedString() {
        val content = """key = "hello\nworld""""
        val result = TomlParser.parse(content)
        assertEquals("hello\nworld", result["key"])
    }

    @Test
    fun parse_inlineTable() {
        val content = """point = { x = 1, y = 2 }"""
        val result = TomlParser.parse(content)
        assertEquals("1", result["point.x"])
        assertEquals("2", result["point.y"])
    }

    @Test
    fun parse_blankLinesIgnored() {
        val content = """
            key1 = "a"

            key2 = "b"

        """.trimIndent()
        val result = TomlParser.parse(content)
        assertEquals(2, result.size)
    }

    @Test
    fun parse_dottedKeys() {
        val content = """
            [database]
            max.pool.size = 10
        """.trimIndent()
        val result = TomlParser.parse(content)
        assertEquals("10", result["database.max.pool.size"])
    }
}
