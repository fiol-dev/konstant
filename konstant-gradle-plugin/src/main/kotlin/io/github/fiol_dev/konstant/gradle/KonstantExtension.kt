package io.github.fiol_dev.konstant.gradle

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

/**
 * ```kotlin
 * konstant {
 *     packageName = "com.example.config"
 *     bake("config/app.toml")
 *     bake("config/app.{env}.toml", optional = true)
 * }
 * ```
 */
abstract class KonstantExtension {
    /** Package of the generated object. Required when anything is baked. */
    abstract val packageName: Property<String>

    /** Name of the generated object, `KonstantBaked` by default. */
    abstract val objectName: Property<String>

    /**
     * Replaces `{env}` in baked paths. Defaults to the `konstant.env` Gradle property
     * (`-Pkonstant.env=prod`), or `dev` when that isn't set.
     */
    abstract val environment: Property<String>

    internal abstract val bakedFiles: ListProperty<String>

    /**
     * Bakes a config file into the app. Files baked later take priority over earlier ones.
     * [path] is relative to the project directory and may contain `{env}`. A missing file
     * fails the build unless [optional] is true.
     */
    fun bake(path: String, optional: Boolean = false) {
        bakedFiles.add(BakedFile(path, optional).encode())
    }
}

internal data class BakedFile(val path: String, val optional: Boolean) {
    fun encode(): String = "${if (optional) "?" else "!"}$path"

    companion object {
        fun decode(value: String): BakedFile = BakedFile(value.substring(1), value[0] == '?')
    }
}
