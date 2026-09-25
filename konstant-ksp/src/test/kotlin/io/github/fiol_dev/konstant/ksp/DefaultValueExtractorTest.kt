package io.github.fiol_dev.konstant.ksp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultValueExtractorTest {

    private fun defaults(source: String, className: String = "Config") =
        DefaultValueExtractor.parseConstructorDefaults(source, className)

    @Test
    fun extractsSimpleDefaults() {
        val result = defaults(
            """
            data class Config(
                val url: String,
                val port: Int = 5432,
                val debug: Boolean = false,
            )
            """
        )
        assertEquals(mapOf("port" to "5432", "debug" to "false"), result)
    }

    @Test
    fun keepsStringDefaultsWithCommasParensAndEquals() {
        val result = defaults("""data class Config(val label: String = "a, (b) = \"c\"", val n: Int = 1)""")
        assertEquals("\"a, (b) = \\\"c\\\"\"", result["label"])
        assertEquals("1", result["n"])
    }

    @Test
    fun skipsAnnotationsWithArguments() {
        val result = defaults(
            """
            data class Config(
                @Key("DB_API_KEY") val apiKey: String = "",
                @Secret val password: String,
            )
            """
        )
        assertEquals(mapOf("apiKey" to "\"\""), result)
    }

    @Test
    fun handlesGenericTypesAndCallExpressions() {
        val result = defaults(
            """
            data class Config(
                val tags: Map<String, Int> = mapOf("a" to 1, "b" to 2),
                val timeout: Long = 60 * 1000L,
            )
            """
        )
        assertEquals("mapOf(\"a\" to 1, \"b\" to 2)", result["tags"])
        assertEquals("60 * 1000L", result["timeout"])
    }

    @Test
    fun picksTheRequestedClassAmongSeveral() {
        val source = """
            data class Other(val x: Int = 1)
            data class Config(val y: Int = 2)
        """
        assertEquals(mapOf("y" to "2"), defaults(source))
    }

    @Test
    fun ignoresCommentsAroundParameters() {
        val result = defaults(
            """
            data class Config(
                /** Port, don't change it (see "docs") */
                val port: Int = 8080, // http, isn't it
                /* a, b /* nested */ c */ val host: String = "h" /* trailing */,
                // val commented: Int = 1,
                val last: Int = 3 // no trailing comma
            )
            """
        )
        assertEquals(mapOf("port" to "8080", "host" to "\"h\"", "last" to "3"), result)
    }

    @Test
    fun keepsCommentMarkersInsideStringsAndCharLiterals() {
        val result = defaults(
            """
            data class Config(
                val url: String = "http://x/*y*/",
                val raw: String = ${"\"\"\""}a, "b" // c${"\"\"\""},
                val chars: String = '"'.toString() + '\''.toString() + ','.toString(),
                val n: Int = 1,
            )
            """
        )
        assertEquals("\"http://x/*y*/\"", result["url"])
        assertEquals("\"\"\"a, \"b\" // c\"\"\"", result["raw"])
        assertEquals("'\"'.toString() + '\\''.toString() + ','.toString()", result["chars"])
        assertEquals("1", result["n"])
    }

    @Test
    fun handlesAnnotationArgumentsWithCommasAndModifiers() {
        val result = defaults(
            """
            data class Config(
                @Key("a,b") @field:Secret val x: Int = 1,
                private val y: (Int) -> Int = { it },
                val z: Int = 2,
            )
            """
        )
        assertEquals(mapOf("x" to "1", "y" to "{ it }", "z" to "2"), result)
    }

    @Test
    fun findsNestedClassFromItsLine() {
        val source = """
            class A {
                data class Db(val port: Int = 1)
            }
            object B {
                data class Db(val port: Int = 2)
            }
        """
        assertEquals(mapOf("port" to "1"), defaults(source, "Db"))
        assertEquals(mapOf("port" to "2"), DefaultValueExtractor.parseConstructorDefaults(source, "Db", fromLine = 5))
    }

    @Test
    fun returnsEmptyWhenClassIsMissing() {
        assertTrue(defaults("data class Other(val x: Int = 1)").isEmpty())
    }
}
