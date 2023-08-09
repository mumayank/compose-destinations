package com.ramcosta.composedestinations.codegen.model

sealed interface ImportedRoute {
    val generatedType: Importable
    val navArgs: RawNavArgsClass?
    val requireOptInAnnotationTypes: List<Importable>
    val isDestination: Boolean
    val isStart: Boolean

    val additionalDeepLinks: List<DeepLink>

    class Destination(
        override val isStart: Boolean,
        override val generatedType: Importable,
        override val navArgs: RawNavArgsClass?,
        override val requireOptInAnnotationTypes: List<Importable>,
        override val additionalDeepLinks: List<DeepLink>,
        val overriddenDestinationStyleType: DestinationStyleType?,
        val additionalComposableWrappers: List<Importable>
    ) : ImportedRoute {
        override val isDestination: Boolean = true
    }

    class NavGraph(
        override val isStart: Boolean,
        override val generatedType: Importable,
        override val navArgs: RawNavArgsClass?,
        override val additionalDeepLinks: List<DeepLink>,
        override val requireOptInAnnotationTypes: List<Importable>,
        val overriddenDefaultTransitions: OverrideDefaultTransitions,
    ) : ImportedRoute {
        override val isDestination: Boolean = false

        sealed interface OverrideDefaultTransitions {
            data object NoOverride: OverrideDefaultTransitions
            class Override(val importable: Importable?): OverrideDefaultTransitions
        }
    }

}