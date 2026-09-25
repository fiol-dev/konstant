package io.github.fiol_dev.konstant.sources

import io.github.fiol_dev.konstant.sources.parser.PropertiesParser
import kotlin.test.Test
import kotlin.test.assertEquals

class PropertiesParserTest {

    @Test
    fun parse_basicKeyValue() {
        val content = "database.url=jdbc:postgresql://localhost:5432/mydb"
        val props = PropertiesParser.parse(content)
        assertEquals("jdbc:postgresql://localhost:5432/mydb", props["database.url"])
    }

    @Test
    fun parse_colonDelimiter() {
        val content = "database.url: jdbc:postgresql://localhost"
        val props = PropertiesParser.parse(content)
        assertEquals("jdbc:postgresql://localhost", props["database.url"])
    }

    @Test
    fun parse_skipsComments() {
        val content = """
            # This is a comment
            ! This is also a comment
            database.url=test
        """.trimIndent()
        val props = PropertiesParser.parse(content)
        assertEquals(1, props.size)
        assertEquals("test", props["database.url"])
    }

    @Test
    fun parse_skipsBlankLines() {
        val content = """
            key1=value1

            key2=value2
        """.trimIndent()
        val props = PropertiesParser.parse(content)
        assertEquals(2, props.size)
    }

    @Test
    fun parse_trimsWhitespace() {
        val content = "  key  =  value  "
        val props = PropertiesParser.parse(content)
        assertEquals("value", props["key"])
    }

    @Test
    fun parse_dotNotationKeys() {
        val content = """
            database.url=jdbc:test
            database.port=5432
            database.max.pool.size=10
        """.trimIndent()
        val props = PropertiesParser.parse(content)
        assertEquals("jdbc:test", props["database.url"])
        assertEquals("5432", props["database.port"])
        assertEquals("10", props["database.max.pool.size"])
    }

    @Test
    fun parse_screamingSnakeKeys() {
        val content = """
            DATABASE_URL=jdbc:test
            DATABASE_PORT=5432
        """.trimIndent()
        val props = PropertiesParser.parse(content)
        assertEquals("jdbc:test", props["DATABASE_URL"])
        assertEquals("5432", props["DATABASE_PORT"])
    }

    @Test
    fun parse_whitespaceSeparator() {
        val content = """
            key1 value one
            key2	tabbed
            key3 = spaced
            key4 :colon
            lonely
        """.trimIndent()
        val props = PropertiesParser.parse(content)
        assertEquals("value one", props["key1"])
        assertEquals("tabbed", props["key2"])
        assertEquals("spaced", props["key3"])
        assertEquals("colon", props["key4"])
        assertEquals("", props["lonely"])
    }

    @Test
    fun parse_lineContinuation() {
        val content = "fruits=apple, \\\n    banana, \\\n\tcherry\nnext=1"
        val props = PropertiesParser.parse(content)
        assertEquals("apple, banana, cherry", props["fruits"])
        assertEquals("1", props["next"])
    }

    @Test
    fun parse_evenBackslashesDoNotContinue() {
        val content = "path=C:\\\\\nnext=1"
        val props = PropertiesParser.parse(content)
        assertEquals("C:\\", props["path"])
        assertEquals("1", props["next"])
    }

    @Test
    fun parse_continuationLineStartingWithHashIsNotAComment() {
        val content = "key=a\\\n  #b"
        assertEquals("a#b", PropertiesParser.parse(content)["key"])
    }

    @Test
    fun parse_escapes() {
        val content = """
            tab=a\tb
            newline=a\nb\r\f
            unicode=caf\u00e9 \u00E9
            specials=\\ \: \= \# \! x
            path=C:\\dir\\file
        """.trimIndent()
        val props = PropertiesParser.parse(content)
        assertEquals("a\tb", props["tab"])
        assertEquals("a\nb\r\u000C", props["newline"])
        assertEquals("café é", props["unicode"])
        assertEquals("\\ : = # ! x", props["specials"])
        assertEquals("C:\\dir\\file", props["path"])
    }

    @Test
    fun parse_escapedSeparatorsInKeys() {
        val content = """
            my\ key=1
            a\=b\:c=2
            trailing=value\
        """.trimIndent() + " "
        val props = PropertiesParser.parse(content)
        assertEquals("1", props["my key"])
        assertEquals("2", props["a=b:c"])
        assertEquals("value ", props["trailing"])
    }

    @Test
    fun parse_bangAndHashCommentsWithLeadingWhitespace() {
        val content = "   # hash\n\t! bang\nkey=value # not a comment"
        val props = PropertiesParser.parse(content)
        assertEquals(1, props.size)
        assertEquals("value # not a comment", props["key"])
    }
}
