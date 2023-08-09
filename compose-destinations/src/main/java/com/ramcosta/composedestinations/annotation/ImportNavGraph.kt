package com.ramcosta.composedestinations.annotation

import com.ramcosta.composedestinations.spec.DestinationStyle
import com.ramcosta.composedestinations.spec.NavGraphSpec
import kotlin.reflect.KClass

@Repeatable
annotation class ImportNavGraph(
    val graph: KClass<out NavGraphSpec>,
    val deepLinks: Array<DeepLink> = [],
    val defaultTransitions: KClass<out DestinationStyle.Animated> = NoOverride::class,
    val start: Boolean = false,
) {
    companion object {
        internal object NoOverride: DestinationStyle.Animated()
    }
}