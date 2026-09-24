package io.github.fiol_dev.konstant.json

import io.github.fiol_dev.konstant.core.KeyFormat
import io.github.fiol_dev.konstant.core.MapBackedSource
import io.github.fiol_dev.konstant.sources.readFileText
import io.github.fiol_dev.konstant.sources.readResourceText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * JSON source parsed by kotlinx-serialization-json. Objects become dotted keys
 * (`{"database": {"url": "x"}}` is `database.url`), arrays of scalars become `["a", 1]` lists,
 * and arrays of objects are numbered (`servers.0.name`). Numbers keep their text as written,
 * and `null` values are skipped, so the spec's default applies.
 */
public class JsonSource(entries: Map<String, String>) : MapBackedSource(entries) {
    override val keyFormat: KeyFormat = KeyFormat.DOT_NOTATION
    override val fallbackKeyFormats: List<KeyFormat> = listOf(KeyFormat.SCREAMING_SNAKE)

    public companion object {
        /** @throws IllegalArgumentException if [content] is not valid JSON. */
        public fun fromString(content: String): JsonSource = JsonSource(flatten(content))

        public fun fromFile(path: String): JsonSource = fromString(readFileText(path))

        /** Reads a bundled file (see `readResourceText` for each platform); [optional] allows it to be missing. */
        public fun fromResource(path: String, optional: Boolean = false): JsonSource {
            val text = readResourceText(path)
                ?: if (optional) "" else throw IllegalArgumentException("Bundled resource not found: $path")
            return fromString(text)
        }

        // Config files are hand-written, so tolerate comments and trailing commas
        @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
        private val json = Json {
            allowComments = true
            allowTrailingComma = true
        }

        internal fun flatten(content: String): Map<String, String> {
            if (content.isBlank()) return emptyMap()
            val out = linkedMapOf<String, String>()
            collect(json.parseToJsonElement(content), "", out)
            return out
        }

        private fun collect(element: JsonElement, key: String, out: MutableMap<String, String>) {
            when (element) {
                is JsonNull -> Unit
                is JsonPrimitive -> if (key.isNotEmpty()) out[key] = element.content
                is JsonObject -> element.forEach { (k, v) -> collect(v, join(key, k), out) }
                is JsonArray ->
                    if (element.all { it is JsonPrimitive }) {
                        // Null items are dropped, the same as null values in an object
                        if (key.isNotEmpty()) out[key] = element.filter { it !is JsonNull }
                            .joinToString(prefix = "[", postfix = "]") { item(it as JsonPrimitive) }
                    } else {
                        element.forEachIndexed { i, item -> collect(item, join(key, i.toString()), out) }
                    }
            }
        }

        private fun item(value: JsonPrimitive): String = if (value.isString) quote(value.content) else value.content

        private fun join(prefix: String, key: String) = if (prefix.isEmpty()) key else "$prefix.$key"

        // Escaped the way Converters.list unquotes it, so items may contain `"`, `,` or a trailing `\`
        private fun quote(s: String): String = "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
    }
}
