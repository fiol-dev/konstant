package io.github.fioldev.konfigure.ksp

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class ConfigSpecProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return ConfigSpecProcessor(
            codeGenerator = environment.codeGenerator,
            logger = environment.logger,
        )
    }
}
