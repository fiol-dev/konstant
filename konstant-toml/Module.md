# Module konstant-toml

`TomlSource` reads TOML config through ktoml. Tables become dot-separated keys
(`server.port`) and arrays of plain values become lists. This module does not support wasmJs, because ktoml's
wasm build is broken with Kotlin 2.3.

# Package io.github.fiol_dev.konstant.toml

The TOML source.
