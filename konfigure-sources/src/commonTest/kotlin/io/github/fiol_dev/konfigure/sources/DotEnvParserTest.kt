package io.github.fiol_dev.konfigure.sources

import io.github.fiol_dev.konfigure.sources.parser.DotEnvParser
import kotlin.test.Test
import kotlin.test.assertEquals

class DotEnvParserTest {

    @Test
    fun parse_basicKeyValue() {
        val content = "DATABASE_URL=jdbc:postgresql://localhost"
        val result = DotEnvParser.parse(content)
        assertEquals("jdbc:postgresql://localhost", result["DATABASE_URL"])
    }

    @Test
    fun parse_quotedValues() {
        val content = """
            KEY1="hello world"
            KEY2='single quoted'
        """.trimIndent()
        val result = DotEnvParser.parse(content)
        assertEquals("hello world", result["KEY1"])
        assertEquals("single quoted", result["KEY2"])
    }

    @Test
    fun parse_skipsComments() {
        val content = """
            # comment
            KEY=value
        """.trimIndent()
        val result = DotEnvParser.parse(content)
        assertEquals(1, result.size)
        assertEquals("value", result["KEY"])
    }

    @Test
    fun parse_skipsBlankLines() {
        val content = """
            KEY1=val1

            KEY2=val2
        """.trimIndent()
        val result = DotEnvParser.parse(content)
        assertEquals(2, result.size)
    }
}
