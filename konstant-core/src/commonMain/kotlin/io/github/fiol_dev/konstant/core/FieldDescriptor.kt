package io.github.fiol_dev.konstant.core

/** Describes one config field for [ConfigLoader.resolve]. Built by generated code. */
@InternalKonstantApi
public class FieldDescriptor<T>(
    public val propertyName: String,
    public val typeName: String,
    public val secret: Boolean,
    public val default: T?,
    public val hasDefault: Boolean,
    public val customKey: String? = null,
    public val convert: (String) -> T,
    /** Builds the value from [ConfigSource.children] when the key itself is absent (Map fields). */
    public val convertChildren: ((Map<String, String>) -> T)? = null,
    /** Returns an error message when a converted value breaks a validation annotation. */
    public val validate: ((T) -> String?)? = null,
)
