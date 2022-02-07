package com.ramcosta.composedestinations.codegen.servicelocator

import com.ramcosta.composedestinations.codegen.InitialValidator
import com.ramcosta.composedestinations.codegen.commons.DestinationWithNavArgsMapper
import com.ramcosta.composedestinations.codegen.facades.CodeOutputStreamMaker
import com.ramcosta.composedestinations.codegen.facades.Logger
import com.ramcosta.composedestinations.codegen.model.CodeGenConfig
import com.ramcosta.composedestinations.codegen.model.Core
import com.ramcosta.composedestinations.codegen.writers.*
import com.ramcosta.composedestinations.codegen.writers.sub.NavArgResolver

internal interface ServiceLocatorAccessor {
    val logger: Logger
    val codeGenerator: CodeOutputStreamMaker
    val core: Core
    val codeGenConfig: CodeGenConfig
}

internal val ServiceLocatorAccessor.customNavTypeWriter get() = CustomNavTypesWriter(
    codeGenerator,
    logger
)

internal val ServiceLocatorAccessor.destinationsWriter get() = DestinationsWriter(
    codeGenerator,
    logger,
    core,
    NavArgResolver()
)

internal val ServiceLocatorAccessor.defaultModeWriter get() = DefaultModeWriter(
    codeGenerator,
    logger,
)

internal val ServiceLocatorAccessor.destinationsModeWriter get() = DestinationsModeWriter(
    codeGenerator,
)

internal val ServiceLocatorAccessor.navGraphsModeWriter get() = NavGraphsModeWriter(
    codeGenerator,
    codeGenConfig
)

internal val ServiceLocatorAccessor.coreExtensionsWriter get() = CoreExtensionsWriter(
    codeGenerator,
)

internal val ServiceLocatorAccessor.sealedDestinationWriter get() = SealedDestinationWriter(
    codeGenerator,
)

internal val ServiceLocatorAccessor.destinationWithNavArgsMapper get() = DestinationWithNavArgsMapper()

internal val ServiceLocatorAccessor.initialValidator get() = InitialValidator(
    codeGenConfig,
    logger,
    core
)