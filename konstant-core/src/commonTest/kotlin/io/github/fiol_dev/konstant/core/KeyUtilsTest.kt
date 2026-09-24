package io.github.fiol_dev.konstant.core

import kotlin.test.Test
import kotlin.test.assertEquals

class KeyUtilsTest {

    @Test
    fun camelToScreamingSnake_simple() {
        assertEquals("MAX_POOL_SIZE", KeyUtils.camelToScreamingSnake("maxPoolSize"))
    }

    @Test
    fun camelToScreamingSnake_singleWord() {
        assertEquals("URL", KeyUtils.camelToScreamingSnake("url"))
    }

    @Test
    fun camelToScreamingSnake_alreadyUppercase() {
        assertEquals("U_R_L", KeyUtils.camelToScreamingSnake("uRL"))
    }

    @Test
    fun camelToDotNotation_simple() {
        assertEquals("max.pool.size", KeyUtils.camelToDotNotation("maxPoolSize"))
    }

    @Test
    fun camelToDotNotation_singleWord() {
        assertEquals("url", KeyUtils.camelToDotNotation("url"))
    }

    @Test
    fun resolveKey_screamingSnake_withPrefix() {
        assertEquals(
            "DATABASE_MAX_POOL_SIZE",
            KeyUtils.resolveKey("maxPoolSize", "DATABASE", KeyFormat.SCREAMING_SNAKE)
        )
    }

    @Test
    fun resolveKey_dotNotation_withPrefix() {
        assertEquals(
            "database.max.pool.size",
            KeyUtils.resolveKey("maxPoolSize", "DATABASE", KeyFormat.DOT_NOTATION)
        )
    }

    @Test
    fun resolveKey_withCustomKey() {
        assertEquals(
            "CUSTOM_KEY",
            KeyUtils.resolveKey("maxPoolSize", "DATABASE", KeyFormat.SCREAMING_SNAKE, "CUSTOM_KEY")
        )
    }

    @Test
    fun resolveKey_noPrefix() {
        assertEquals(
            "MAX_POOL_SIZE",
            KeyUtils.resolveKey("maxPoolSize", null, KeyFormat.SCREAMING_SNAKE)
        )
    }

    @Test
    fun resolvePrefix_noParent() {
        assertEquals("DATABASE", KeyUtils.resolvePrefix("database", null))
    }

    @Test
    fun resolvePrefix_withParent() {
        assertEquals("APP_DATABASE", KeyUtils.resolvePrefix("database", "APP"))
    }

    @Test
    fun resolveKey_dotNotation_withNestedPrefix() {
        assertEquals(
            "database.pool.max.size",
            KeyUtils.resolveKey("maxSize", "DATABASE_POOL", KeyFormat.DOT_NOTATION)
        )
    }
}
