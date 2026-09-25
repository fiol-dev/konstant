package io.github.fiol_dev.konstant.annotations

/**
 * Looks this field up under [name] instead of the key derived from the property name. The
 * name is used exactly as written, in every source and key format, and nested-spec prefixes
 * are not added to it.
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
public annotation class Key(val name: String)
