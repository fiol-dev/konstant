package io.github.fiol_dev.konstant.core

/**
 * Checks used by generated loaders for the validation annotations. Each returns an error
 * message, or null when the value is valid.
 */
@InternalKonstantApi
public object Validators {

    public fun range(value: Double, min: Double, max: Double): String? = when {
        value.isNaN() -> "must be a number"
        value < min && max == Double.POSITIVE_INFINITY -> "must be at least ${format(min)}"
        value > max && min == Double.NEGATIVE_INFINITY -> "must be at most ${format(max)}"
        value < min || value > max -> "must be between ${format(min)} and ${format(max)}"
        else -> null
    }

    public fun size(size: Int, min: Int, max: Int): String? = when {
        size < min && max == Int.MAX_VALUE -> "size must be at least $min"
        size > max && min == 0 -> "size must be at most $max"
        size < min || size > max -> "size must be between $min and $max"
        else -> null
    }

    public fun notBlank(value: String): String? = if (value.isBlank()) "must not be blank" else null

    public fun pattern(value: String, regex: String): String? =
        if (Regex(regex).matches(value)) null else "must match $regex"

    private fun format(d: Double): String = if (d % 1.0 == 0.0) d.toLong().toString() else d.toString()
}
