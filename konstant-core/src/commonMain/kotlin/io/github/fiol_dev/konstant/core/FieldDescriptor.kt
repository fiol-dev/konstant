package io.github.fiol_dev.konstant.core

public data class FieldDescriptor<T>(
    public val propertyName: String,
    public val envKey: String,
    public val typeName: String,
    public val required: Boolean,
    public val secret: Boolean,
    public val default: T?,
    public val hasDefault: Boolean,
    public val customKey: String? = null,
    public val convert: (String) -> T,
)
