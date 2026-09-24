package io.github.fiol_dev.konstant.core

/**
 * Custom conversion for a field annotated with `@Convert(MyConverter::class)`. Implement it as
 * an `object`. Throw [IllegalArgumentException] for invalid input; the message becomes the
 * error's cause.
 *
 * ```kotlin
 * object UrlConverter : ValueConverter<Url> {
 *     override fun convert(raw: String): Url = Url(raw)
 * }
 * ```
 */
public fun interface ValueConverter<out T> {
    public fun convert(raw: String): T
}
