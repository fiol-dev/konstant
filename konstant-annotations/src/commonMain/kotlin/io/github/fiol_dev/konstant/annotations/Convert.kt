package io.github.fiol_dev.konstant.annotations

import kotlin.reflect.KClass

/**
 * Converts this field with a custom converter: an `object` implementing
 * `io.github.fiol_dev.konstant.core.ValueConverter<T>`, where `T` is the field's type.
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
public annotation class Convert(val with: KClass<*>)
