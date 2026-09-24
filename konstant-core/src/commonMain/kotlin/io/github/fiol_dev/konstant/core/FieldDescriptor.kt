package io.github.fiol_dev.konstant.core

data class FieldDescriptor<T>(
    val propertyName: String,
    val envKey: String,
    val typeName: String,
    val required: Boolean,
    val secret: Boolean,
    val default: T?,
    val hasDefault: Boolean,
    val customKey: String? = null,
    val convert: (String) -> T,
)
