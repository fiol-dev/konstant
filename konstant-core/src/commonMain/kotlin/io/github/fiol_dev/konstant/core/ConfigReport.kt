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
     * or null when missing.
     */
    public data class Entry(val key: String, val value: String?, val origin: String)

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
