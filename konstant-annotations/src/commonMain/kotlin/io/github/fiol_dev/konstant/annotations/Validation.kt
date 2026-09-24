package io.github.fiol_dev.konstant.annotations

/** The number (Int, Long, Double or Float) must be within [min]..[max], inclusive. */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
public annotation class Range(
    val min: Double = Double.NEGATIVE_INFINITY,
    val max: Double = Double.POSITIVE_INFINITY,
)

/** The String's length, or the List, Set or Map's size, must be within [min]..[max], inclusive. */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
public annotation class Size(
    val min: Int = 0,
    val max: Int = Int.MAX_VALUE,
)

/** The String must contain a non-whitespace character. */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
public annotation class NotBlank

/** The whole String must match [regex]. */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
public annotation class Pattern(val regex: String)
