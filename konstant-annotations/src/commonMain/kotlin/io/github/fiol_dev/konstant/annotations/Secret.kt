package io.github.fiol_dev.konstant.annotations

/**
 * Hides this field's value as `***` in `ConfigError` values and messages and in
 * `ConfigLoader.explain` reports. A conversion error's cause becomes `invalid value`, since
 * parser messages may echo the input. The loaded config object is not changed: its `toString`
 * is your data class's own and still prints the real value.
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
public annotation class Secret
