@file:OptIn(InternalKonstantApi::class)

package io.github.fiol_dev.konstant.core

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ConfigDelegateTest {

    private val appConfig = TestAppConfig(
        appName = "DelegateApp",
        db = TestDbConfig(url = "jdbc:delegate", port = 9999),
        server = TestServerConfig(host = "192.168.1.1", debug = false),
    )

    @BeforeTest
    fun setup() {
        Konstant.reset()
        Konstant.install(appConfig, listOf(appConfig.db, appConfig.server))
    }

    @AfterTest
    fun cleanup() {
        Konstant.reset()
    }

    // -- configField<T, V> { } delegate (global holder) --

    @Test
    fun configField_extractsField() {
        val appName: String by configField<TestAppConfig, String> { it.appName }
        assertEquals("DelegateApp", appName)
    }

    @Test
    fun configField_extractsNestedField() {
        val dbUrl: String by configField<TestAppConfig, String> { it.db.url }
        val dbPort: Int by configField<TestAppConfig, Int> { it.db.port }

        assertEquals("jdbc:delegate", dbUrl)
        assertEquals(9999, dbPort)
    }

    @Test
    fun configField_deeplyNestedField() {
        val debug: Boolean by configField<TestAppConfig, Boolean> { it.server.debug }
        assertEquals(false, debug)
    }

    @Test
    fun configField_fromSpecificType() {
        val url: String by configField<TestDbConfig, String> { it.url }
        assertEquals("jdbc:delegate", url)
    }

    // -- .field { } delegate (instance-based) --

    @Test
    fun field_delegate_extractsField() {
        val name: String by appConfig.field { it.appName }
        assertEquals("DelegateApp", name)
    }

    @Test
    fun field_delegate_extractsNestedField() {
        val host: String by appConfig.field { it.server.host }
        assertEquals("192.168.1.1", host)
    }

    @Test
    fun field_delegate_onNestedInstance() {
        val port: Int by appConfig.db.field { it.port }
        assertEquals(9999, port)
    }

    @Test
    fun field_delegate_cachesValue() {
        var callCount = 0
        val delegate = appConfig.field {
            callCount++
            it.appName
        }

        val obj = object {
            val name: String by delegate
        }

        // Access twice — selector should only run once
        assertEquals("DelegateApp", obj.name)
        assertEquals("DelegateApp", obj.name)
        assertEquals(1, callCount)
    }
}
