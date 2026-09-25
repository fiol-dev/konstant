package io.github.fiol_dev.konstant.ksptests

import io.github.fiol_dev.konstant.annotations.ConfigSpec
import io.github.fiol_dev.konstant.annotations.Key

// Comments, char literals and raw strings in the constructor must not change the defaults read from it
@ConfigSpec
public data class CommentedConfig(
    /** Port to listen on, don't change it (a doc comment with an apostrophe) */
    val port: Int = 8080,
    // a line comment with "quotes", commas and (parens) = 1
    val host: String = "localhost", // isn't the host
    /* block /* nested */ comment */ val name: String = "a // not a comment /* nor this */",
    val quote: String = '"'.toString() + '\''.toString(),
    val raw: String = """raw "quoted", // kept""",
    @Key("COMMENTED_KEYED,1") val keyed: Int = 1,
    val last: Int = 3 // http
)

// Field names that match the loader's own locals and parameter
@ConfigSpec
public data class ShadowingConfig(
    val prefix: String = "app",
    val errors: Int = 0,
    val e: String = "e",
    val config: String = "c",
    val port: Int = 1,
)

@ConfigSpec
public data class EndpointConfig(
    val url: String,
    val timeoutMs: Int = 100,
)

// A nested spec with a default: used when none of the nested keys are set
@ConfigSpec
public data class ClientConfig(
    val name: String = "client",
    val endpoint: EndpointConfig = EndpointConfig("http://default"),
    val backup: EndpointConfig,
)

// Specs nested in other declarations, two of them with the same simple name
@ConfigSpec
public data class OuterConfig(
    val name: String = "outer",
    val db: Db,
) {
    @ConfigSpec
    public data class Db(
        val url: String,
        val port: Int = 5432,
    )
}

public object OtherSpecs {
    @ConfigSpec
    public data class Db(
        val host: String = "other-host",
        val port: Int = 1,
    )
}

// Generated declarations are internal too
@ConfigSpec
internal data class InternalConfig(
    val level: Int = 2,
)

// Characters that need escaping in a Kotlin string literal
@ConfigSpec
public data class EscapedKeyConfig(
    @Key("we\"ird\$key\\x\ty") val weird: String = "d",
)
