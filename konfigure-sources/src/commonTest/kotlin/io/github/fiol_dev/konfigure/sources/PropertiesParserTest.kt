package io.github.fiol_dev.konfigure.sources

import io.github.fiol_dev.konfigure.sources.parser.PropertiesParser
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
}
