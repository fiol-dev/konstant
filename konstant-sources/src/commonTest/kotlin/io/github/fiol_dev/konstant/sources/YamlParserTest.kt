package io.github.fiol_dev.konstant.sources

import io.github.fiol_dev.konstant.sources.parser.YamlParser
import kotlin.test.Test
import kotlin.test.assertEquals

class YamlParserTest {

    @Test
    fun parse_basicKeyValue() {
        val content = """
            title: My App
            debug: true
            port: 8080
        """.trimIndent()
        val result = YamlParser.parse(content)
        assertEquals("My App", result["title"])
        assertEquals("true", result["debug"])
        assertEquals("8080", result["port"])
    }

    @Test
    fun parse_nestedMapping() {
        val content = """
            database:
              url: jdbc:postgresql://localhost
              port: 5432
        """.trimIndent()
        val result = YamlParser.parse(content)
        assertEquals("jdbc:postgresql://localhost", result["database.url"])
        assertEquals("5432", result["database.port"])
    }

    @Test
    fun parse_deeplyNested() {
        val content = """
            app:
              database:
                url: jdbc:test
                port: 5432
              server:
                host: localhost
                port: 8080
        """.trimIndent()
        val result = YamlParser.parse(content)
        assertEquals("jdbc:test", result["app.database.url"])
        assertEquals("5432", result["app.database.port"])
        assertEquals("localhost", result["app.server.host"])
        assertEquals("8080", result["app.server.port"])
    }

    @Test
    fun parse_comments() {
        val content = """
            # Full line comment
            key: value # inline comment
            other: 42
        """.trimIndent()
        val result = YamlParser.parse(content)
        assertEquals("value", result["key"])
        assertEquals("42", result["other"])
        assertEquals(2, result.size)
    }

    @Test
    fun parse_quotedStrings() {
        val content = """
            name: "hello world"
            path: 'C:\Users\test'
        """.trimIndent()
        val result = YamlParser.parse(content)
        assertEquals("hello world", result["name"])
        assertEquals("C:\\Users\\test", result["path"])
    }

    @Test
    fun parse_blankLinesIgnored() {
        val content = """
            key1: a

            key2: b

        """.trimIndent()
        val result = YamlParser.parse(content)
        assertEquals(2, result.size)
    }

    @Test
    fun parse_multipleSections() {
        val content = """
            database:
              url: jdbc:test
              password: secret
            server:
              host: 0.0.0.0
              port: 9090
        """.trimIndent()
        val result = YamlParser.parse(content)
        assertEquals("jdbc:test", result["database.url"])
        assertEquals("secret", result["database.password"])
        assertEquals("0.0.0.0", result["server.host"])
        assertEquals("9090", result["server.port"])
    }

    @Test
    fun parse_colonInValue() {
        val content = "url: http://localhost:8080/api"
        val result = YamlParser.parse(content)
        assertEquals("http://localhost:8080/api", result["url"])
    }

    @Test
    fun parse_siblingAfterNested() {
        val content = """
            database:
              url: test
            appName: MyApp
        """.trimIndent()
        val result = YamlParser.parse(content)
        assertEquals("test", result["database.url"])
        assertEquals("MyApp", result["appName"])
    }

    @Test
    fun parse_booleanValues() {
        val content = """
            enabled: true
            verbose: false
        """.trimIndent()
        val result = YamlParser.parse(content)
        assertEquals("true", result["enabled"])
        assertEquals("false", result["verbose"])
    }
}
