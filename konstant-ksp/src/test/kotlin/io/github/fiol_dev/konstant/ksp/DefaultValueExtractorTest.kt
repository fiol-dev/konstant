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
    fun returnsEmptyWhenClassIsMissing() {
        assertTrue(defaults("data class Other(val x: Int = 1)").isEmpty())
    }
}
