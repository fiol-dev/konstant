package io.github.fiol_dev.konstant.sources

import java.lang.reflect.Member
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The ABI dumps in api/ don't cover the Android target, so this pins its Android-only public
 * API instead. If a change here is intended, update the expected list in the same PR.
 */
class AndroidApiTest {

    @Test
    fun androidPublicApiIsUnchanged() {
        val expected = listOf(
            "io.github.fiol_dev.konstant.sources.KonstantInitProvider",
            "  <init>()",
            "  delete(android.net.Uri, java.lang.String, [Ljava.lang.String;): int",
            "  getType(android.net.Uri): java.lang.String",
            "  insert(android.net.Uri, android.content.ContentValues): android.net.Uri",
            "  onCreate(): boolean",
            "  query(android.net.Uri, [Ljava.lang.String;, java.lang.String, [Ljava.lang.String;, java.lang.String): android.database.Cursor",
            "  update(android.net.Uri, android.content.ContentValues, java.lang.String, [Ljava.lang.String;): int",
            "io.github.fiol_dev.konstant.sources.PlatformFile_androidKt",
            "  readFileText(java.lang.String): java.lang.String",
            "io.github.fiol_dev.konstant.sources.PlatformResource_androidKt",
            "  initKonstantAndroid(android.content.Context): void",
            "  readResourceText(java.lang.String): java.lang.String",
        )
        val actual = listOf(
            "io.github.fiol_dev.konstant.sources.KonstantInitProvider",
            "io.github.fiol_dev.konstant.sources.PlatformFile_androidKt",
            "io.github.fiol_dev.konstant.sources.PlatformResource_androidKt",
        ).flatMap { name -> listOf(name) + publicMembers(Class.forName(name)).map { "  $it" } }
        assertEquals(expected.joinToString("\n"), actual.joinToString("\n"))
    }

    private fun publicMembers(type: Class<*>): List<String> {
        val members: List<Member> = type.declaredConstructors.toList() + type.declaredMethods.toList()
        return members
            .filter { Modifier.isPublic(it.modifiers) && !it.isSynthetic }
            .map { member ->
                val params = when (member) {
                    is Method -> member.parameterTypes
                    else -> (member as java.lang.reflect.Constructor<*>).parameterTypes
                }.joinToString { it.name }
                if (member is Method) "${member.name}($params): ${member.returnType.name}" else "<init>($params)"
            }
            .sorted()
    }
}
