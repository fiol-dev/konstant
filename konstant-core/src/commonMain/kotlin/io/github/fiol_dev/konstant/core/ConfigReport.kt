package io.github.fiol_dev.konstant.core

/**
 * The outcome of [ConfigLoader.explain]: the load [result] plus, for each field in load
 * order, its key, the value used and where it came from. [toString] renders a table.
 */
public class ConfigReport<out T>(
    /** What the load returned, including any errors. */
    public val result: ConfigResult<T>,
    /** One entry per field, in load order. */
    public val entries: List<Entry>,
) {
    /**
     * One field. [key] is the field's key in `dot.notation` (`api.port`), or its `@Key` as
     * written, the same for every entry whichever source supplied it. [origin] is the source
     * that supplied it (`#2 TomlSource(config.toml)`, numbered in priority order), [DEFAULT] or
     * [MISSING]. [sourceKey] is the key that source was asked for when it differs from [key],
     * such as `API_PORT` in environment variables. [value] is the raw text, `***` for secrets,
     * or null when missing. A plain class rather than a data class, so fields can be added
     * later; entries still compare by value.
     */
    public class Entry(
        public val key: String,
        public val value: String?,
        public val origin: String,
        public val sourceKey: String? = null,
    ) {
        override fun equals(other: Any?): Boolean =
            other is Entry && other.key == key && other.value == value && other.origin == origin &&
                other.sourceKey == sourceKey

        override fun hashCode(): Int =
            ((key.hashCode() * 31 + value.hashCode()) * 31 + origin.hashCode()) * 31 + sourceKey.hashCode()

        override fun toString(): String =
            "Entry(key=$key, value=$value, origin=$origin" + (sourceKey?.let { ", sourceKey=$it" } ?: "") + ")"
    }

    override fun toString(): String {
        if (entries.isEmpty()) return "(no fields)"
        val keyWidth = entries.maxOf { it.key.length }
        val valueWidth = entries.maxOf { (it.value ?: "").length }
        return entries.joinToString("\n") { entry ->
            val origin = entry.origin + (entry.sourceKey?.let { " as $it" } ?: "")
            (entry.key.padEnd(keyWidth) + "  " + (entry.value ?: "").padEnd(valueWidth) + "  " + origin).trimEnd()
        }
    }

    public companion object {
        /** [Entry.origin] of a field that no source had, so its default was used. */
        public const val DEFAULT: String = "default"

        /** [Entry.origin] of a required field that no source had. */
        public const val MISSING: String = "missing"
    }
}
