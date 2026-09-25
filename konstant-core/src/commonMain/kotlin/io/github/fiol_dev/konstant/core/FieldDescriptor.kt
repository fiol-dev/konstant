package io.github.fiol_dev.konstant.core

/** Describes one config field for [ConfigLoader.resolve]. Built by generated code. */
@InternalKonstantApi
public class FieldDescriptor<T>(
    /** The property name in the spec, e.g. `maxPoolSize`. */
    public val propertyName: String,
    /** The type shown in [ConfigError.ConversionFailed.targetType]. */
    public val typeName: String,
    /** True for a `@Secret` field, whose value is shown as `***`. */
    public val secret: Boolean,
    /** The value used when no source has the key, if [hasDefault]. */
    public val default: T?,
    /** Whether the field is optional: it has a default or is nullable. */
    public val hasDefault: Boolean,
    /** The `@Key` name, used instead of a derived key. */
    public val customKey: String? = null,
    /** Converts the raw text; throws on invalid input. */
    public val convert: (String) -> T,
    /** Builds the value from [ConfigSource.children] when the key itself is absent (Map fields). */
    public val convertChildren: ((Map<String, String>) -> T)? = null,
    /** Returns an error message when a converted value breaks a validation annotation. */
    public val validate: ((T) -> String?)? = null,
)
