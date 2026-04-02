package io.github.fioldev.konfigure.core

data class FieldDescriptor<T>(
    val propertyName: String,
    val envKey: String,
    val required: Boolean,
    val secret: Boolean,
    val default: T?,
    val hasDefault: Boolean = default != null || !required,
    val customKey: String? = null,
    val convert: (String) -> T,
)
