@file:OptIn(InternalKonstantApi::class)

package io.github.fiol_dev.konstant.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ValidatorsTest {

    @Test
    fun range() {
        assertNull(Validators.range(5.0, 1.0, 10.0))
        assertEquals("must be between 1 and 10", Validators.range(11.0, 1.0, 10.0))
        assertEquals("must be at least 0.5", Validators.range(0.1, 0.5, Double.POSITIVE_INFINITY))
        assertEquals("must be at most 3", Validators.range(4.0, Double.NEGATIVE_INFINITY, 3.0))
        assertEquals("must be a number", Validators.range(Double.NaN, 0.0, 1.0))
    }

    @Test
    fun size() {
        assertNull(Validators.size(3, 1, 5))
        assertEquals("size must be between 1 and 5", Validators.size(0, 1, 5))
        assertEquals("size must be at least 2", Validators.size(1, 2, Int.MAX_VALUE))
        assertEquals("size must be at most 2", Validators.size(3, 0, 2))
    }

    @Test
    fun notBlankAndPattern() {
        assertNull(Validators.notBlank("x"))
        assertEquals("must not be blank", Validators.notBlank(" \t"))
        assertNull(Validators.pattern("ab-12", "[a-z]+-\\d+"))
        assertEquals("must match [a-z]+", Validators.pattern("ab1", "[a-z]+"))
    }
}
