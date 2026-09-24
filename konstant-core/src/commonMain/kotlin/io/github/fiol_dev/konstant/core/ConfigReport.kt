package io.github.fiol_dev.konstant.core

/**
 * The outcome of [ConfigLoader.explain]: the load [result] plus, for each field in load
 * order, its key, the value used and where it came from. [toString] renders a table.
 */
public class ConfigReport<out T>(
    public val result: ConfigResult<T>,
    public val entries: List<Entry>,
) {
    /**
     * One field. [origin] is the source that supplied it (`#2 TomlSource`, numbered in
     * priority order), [DEFAULT] or [MISSING]. [value] is the raw text, `***` for secrets,
     * or null when missing. A plain class rather than a data class, so fields can be added
     * later; entries still compare by value.
     */
    public class Entry(public val key: String, public val value: String?, public val origin: String) {
        override fun equals(other: Any?): Boolean =
            other is Entry && other.key == key && other.value == value && other.origin == origin

        override fun hashCode(): Int = (key.hashCode() * 31 + value.hashCode()) * 31 + origin.hashCode()

        override fun toString(): String = "Entry(key=$key, value=$value, origin=$origin)"
    }

    override fun toString(): String {
        if (entries.isEmpty()) return "(no fields)"
        val keyWidth = entries.maxOf { it.key.length }
        val valueWidth = entries.maxOf { (it.value ?: "").length }
        return entries.joinToString("\n") { entry ->
            entry.key.padEnd(keyWidth) + "  " + (entry.value ?: "").padEnd(valueWidth) + "  " + entry.origin
        }
    }

    public companion object {
        public const val DEFAULT: String = "default"
        public const val MISSING: String = "missing"
    }
}
