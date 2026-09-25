package io.github.fiol_dev.konstant.sources

import io.github.fiol_dev.konstant.sources.parser.DotEnvParser
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

    @Test
    fun parse_ignoresExportPrefix() {
        val content = """
            export DATABASE_URL=jdbc:test
            export  PORT = 5432
            exported=yes
        """.trimIndent()
        val result = DotEnvParser.parse(content)
        assertEquals("jdbc:test", result["DATABASE_URL"])
        assertEquals("5432", result["PORT"])
        assertEquals("yes", result["exported"])
    }

    @Test
    fun parse_stripsInlineCommentsFromUnquotedValues() {
        val content = """
            KEY=value # a comment
            URL=http://host/#anchor
            EMPTY= # nothing here
            HASH=#literal
        """.trimIndent()
        val result = DotEnvParser.parse(content)
        assertEquals("value", result["KEY"])
        assertEquals("http://host/#anchor", result["URL"])
        assertEquals("", result["EMPTY"])
        assertEquals("#literal", result["HASH"])
    }

    @Test
    fun parse_keepsHashInsideQuotes() {
        val content = """
            A="value # not a comment" # comment
            B='value # not a comment' # comment
        """.trimIndent()
        val result = DotEnvParser.parse(content)
        assertEquals("value # not a comment", result["A"])
        assertEquals("value # not a comment", result["B"])
    }

    @Test
    fun parse_unescapesDoubleQuotedValues() {
        val content = """KEY="line1\nline2\ttab\r \"quoted\" back\\slash \x""""
        val result = DotEnvParser.parse(content)
        assertEquals("line1\nline2\ttab\r \"quoted\" back\\slash \\x", result["KEY"])
    }

    @Test
    fun parse_singleQuotedValuesAreLiteral() {
        val content = """KEY='C:\new\table \"x\"'"""
        val result = DotEnvParser.parse(content)
        assertEquals("C:\\new\\table \\\"x\\\"", result["KEY"])
    }

    @Test
    fun parse_unterminatedQuoteIsKeptAsIs() {
        val result = DotEnvParser.parse("KEY=\"abc")
        assertEquals("\"abc", result["KEY"])
    }
}
