package io.github.fiol_dev.konstant.ksptests

import io.github.fiol_dev.konstant.core.ConfigError
import io.github.fiol_dev.konstant.core.ConfigLoader
import io.github.fiol_dev.konstant.core.ConfigResult
import io.github.fiol_dev.konstant.core.ConfigSource
import io.github.fiol_dev.konstant.core.InternalKonstantApi
import io.github.fiol_dev.konstant.core.Konstant
import io.github.fiol_dev.konstant.test.MapSource
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame

class RegressionTest {

    private fun loader(vararg stack: ConfigSource) = ConfigLoader {
        sources { stack.forEach { +it } }
    }

    private fun <T> ConfigResult<T>.errors(): List<ConfigError> =
        assertIs<ConfigResult.Failure>(this).errors

    @AfterTest
    fun cleanup() {
        Konstant.reset()
    }

    @OptIn(InternalKonstantApi::class)
    @Test
    fun commentsAndLiteralsInTheConstructorDontChangeDefaults() {
        val config = loader(MapSource.screamingSnake()).loadCommentedConfig().getOrThrow()

        assertEquals(CommentedConfig(), config)
        assertEquals(8080, CommentedConfigSchema.port.default)
        assertEquals("localhost", CommentedConfigSchema.host.default)
        assertEquals("a // not a comment /* nor this */", config.name)
        assertEquals("\"'", config.quote)
        assertEquals("raw \"quoted\", // kept", config.raw)
        assertEquals(3, CommentedConfigSchema.last.default)
        assertEquals("COMMENTED_KEYED,1", CommentedConfigSchema.keyed.customKey)
    }

    @Test
    fun fieldsNamedLikeGeneratedLocalsDontShadowThem() {
        val config = loader(
            MapSource.screamingSnake("PREFIX" to "x", "PORT" to "5", "ERRORS" to "2", "E" to "f")
        ).loadShadowingConfig().getOrThrow()

        assertEquals(ShadowingConfig(prefix = "x", errors = 2, e = "f", port = 5), config)
    }

    @Test
    fun fieldNamedPrefixDoesntChangeTheLoaderPrefix() {
        val config = loader(
            MapSource.screamingSnake("SVC_PREFIX" to "x", "SVC_PORT" to "5", "X_PORT" to "9")
        ).loadShadowingConfig(prefix = "SVC").getOrThrow()

        assertEquals("x", config.prefix)
        assertEquals(5, config.port)
    }

    @Test
    fun fieldNamedErrorsDoesntHideOtherErrors() {
        val errors = loader(
            MapSource.screamingSnake("ERRORS" to "1", "PORT" to "nope")
        ).loadShadowingConfig().errors()

        assertEquals(listOf("PORT"), errors.map { it.key })
    }

    @Test
    fun nestedDefaultIsUsedWhenNoneOfItsKeysAreSet() {
        val config = loader(
            MapSource.screamingSnake("BACKUP_URL" to "http://backup")
        ).loadClientConfig().getOrThrow()

        assertEquals(EndpointConfig("http://default"), config.endpoint)
        assertEquals(EndpointConfig("http://backup"), config.backup)
    }

    @Test
    fun nestedKeysOverrideTheNestedDefault() {
        val config = loader(
            MapSource.screamingSnake("ENDPOINT_URL" to "http://set", "BACKUP_URL" to "http://backup")
        ).loadClientConfig().getOrThrow()

        assertEquals(EndpointConfig("http://set"), config.endpoint)
    }

    @Test
    fun invalidNestedValueIsReportedDespiteTheDefault() {
        val errors = loader(
            MapSource.screamingSnake("ENDPOINT_TIMEOUT_MS" to "soon", "BACKUP_URL" to "http://backup")
        ).loadClientConfig().errors()

        assertEquals(setOf("ENDPOINT_URL", "ENDPOINT_TIMEOUT_MS"), errors.map { it.key }.toSet())
        assertIs<ConfigError.ConversionFailed>(errors.single { it.key == "ENDPOINT_TIMEOUT_MS" })
    }

    @Test
    fun nestedFieldWithoutDefaultIsStillRequired() {
        val errors = loader(MapSource.screamingSnake()).loadClientConfig().errors()

        assertEquals(listOf("BACKUP_URL"), errors.map { it.key })
    }

    @Test
    fun loadsSpecsNestedInOtherClasses() {
        val config = loader(MapSource.screamingSnake("DB_URL" to "jdbc:x")).loadOuterConfig().getOrThrow()

        assertEquals(OuterConfig(db = OuterConfig.Db("jdbc:x")), config)
        // Two nested classes named Db get their own loaders and their own defaults
        val outerDb = loader(MapSource.screamingSnake("URL" to "jdbc:y")).loadOuterConfig_Db().getOrThrow()
        assertEquals(OuterConfig.Db("jdbc:y", 5432), outerDb)
        val otherDb = loader(MapSource.screamingSnake()).loadOtherSpecs_Db().getOrThrow()
        assertEquals(OtherSpecs.Db("other-host", 1), otherDb)
    }

    @Test
    fun initInstallsSpecsNestedInOtherClasses() {
        val config = Konstant.initOuterConfig { sources { +MapSource.screamingSnake("DB_URL" to "jdbc:x") } }

        assertSame(config.db, Konstant.get<OuterConfig.Db>())
    }

    @Test
    fun internalSpecLoads() {
        assertEquals(InternalConfig(), loader(MapSource.screamingSnake()).loadInternalConfig().getOrThrow())
    }

    @OptIn(InternalKonstantApi::class)
    @Test
    fun customKeyWithQuotesDollarsAndBackslashesIsKeptVerbatim() {
        val key = "we\"ird\$key\\x\ty"
        assertEquals(key, EscapedKeyConfigSchema.weird.customKey)

        val config = loader(MapSource.of(key to "v")).loadEscapedKeyConfig().getOrThrow()

        assertEquals("v", config.weird)
    }
}
