package com.ramcosta.composedestinations.codegen.model

import com.ramcosta.composedestinations.codegen.commons.toSnakeCase

interface NavGraphGenParams {
    val sourceIds: List<String>
    val name: String
    val baseRoute: String
    val annotationType: Importable
    val default: Boolean
    val isNavHostGraph: Boolean
    val defaultTransitions: Importable?
    val deepLinks: List<DeepLink>
    val navArgs: RawNavArgsClass?
    val parent: Importable?
    val isParentStart: Boolean?
    val visibility: Visibility
    val importedRoutes: List<ImportedRoute>
    val importedNavGraphs: List<ImportedRoute.NavGraph>
    val importedDestinations: List<ImportedRoute.Destination>
    val importedStartRoute: ImportedRoute?
}

data class RawNavGraphGenParams(
    override val annotationType: Importable,
    override val default: Boolean,
    override val isNavHostGraph: Boolean,
    override val defaultTransitions: Importable?,
    override val deepLinks: List<DeepLink>,
    override val navArgs: RawNavArgsClass?,
    override val sourceIds: List<String>,
    override val parent: Importable? = null,
    override val isParentStart: Boolean? = null,
    override val visibility: Visibility,
    override val importedRoutes: List<ImportedRoute>,
    private val routeOverride: String? = null,
) : NavGraphGenParams {

    override val importedNavGraphs: List<ImportedRoute.NavGraph> = importedRoutes.filterIsInstance<ImportedRoute.NavGraph>()
    override val importedDestinations: List<ImportedRoute.Destination> = importedRoutes.filterIsInstance<ImportedRoute.Destination>()
    override val importedStartRoute = importedRoutes.find { it.isStart }

    override val name: String = annotationType.simpleName.let {
        if (it.endsWith("NavGraph")) {
            it.replace("NavGraph", "Graph")
        } else if (it.endsWith("Graph")) {
            it
        } else {
            it + "Graph"
        }
    }

    override val baseRoute: String by lazy(LazyThreadSafetyMode.NONE) {
        routeOverride ?: name.replace("(?i)graph".toRegex(), "").toSnakeCase()
    }
}
