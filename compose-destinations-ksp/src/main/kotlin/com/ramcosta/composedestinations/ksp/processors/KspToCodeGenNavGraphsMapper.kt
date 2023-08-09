package com.ramcosta.composedestinations.ksp.processors

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.ramcosta.composedestinations.codegen.commons.DESTINATION_ANNOTATION_DEEP_LINKS_ARGUMENT
import com.ramcosta.composedestinations.codegen.commons.DESTINATION_ANNOTATION_ROUTE_ARGUMENT
import com.ramcosta.composedestinations.codegen.commons.IllegalDestinationsSetup
import com.ramcosta.composedestinations.codegen.commons.NAV_GRAPH_ANNOTATION
import com.ramcosta.composedestinations.codegen.commons.NAV_GRAPH_ANNOTATION_DEFAULT_NAME
import com.ramcosta.composedestinations.codegen.commons.NAV_GRAPH_ANNOTATION_QUALIFIED
import com.ramcosta.composedestinations.codegen.commons.NAV_HOST_GRAPH_ANNOTATION
import com.ramcosta.composedestinations.codegen.commons.NAV_HOST_GRAPH_ANNOTATION_QUALIFIED
import com.ramcosta.composedestinations.codegen.model.Importable
import com.ramcosta.composedestinations.codegen.model.ImportedRoute
import com.ramcosta.composedestinations.codegen.model.NavTypeSerializer
import com.ramcosta.composedestinations.codegen.model.RawNavGraphGenParams
import com.ramcosta.composedestinations.ksp.commons.DestinationMappingUtils
import com.ramcosta.composedestinations.ksp.commons.MutableKSFileSourceMapper
import com.ramcosta.composedestinations.ksp.commons.findActualClassDeclaration
import com.ramcosta.composedestinations.ksp.commons.findAllRequireOptInAnnotations
import com.ramcosta.composedestinations.ksp.commons.findArgumentValue
import com.ramcosta.composedestinations.ksp.commons.getNavArgsDelegateType
import com.ramcosta.composedestinations.ksp.commons.isNothing
import com.ramcosta.composedestinations.ksp.commons.toDeepLink
import com.ramcosta.composedestinations.ksp.commons.toGenVisibility
import com.ramcosta.composedestinations.ksp.commons.toImportable

internal class KspToCodeGenNavGraphsMapper(
    private val resolver: Resolver,
    private val destinationMappingUtils: DestinationMappingUtils,
    private val mutableKSFileSourceMapper: MutableKSFileSourceMapper,
    private val navTypeSerializersByType: Map<Importable, NavTypeSerializer>,
) {

    fun map(
        navGraphAnnotations: Sequence<KSClassDeclaration>,
        navHostGraphAnnotations: Sequence<KSClassDeclaration>
    ): List<RawNavGraphGenParams> {
        return navGraphAnnotations.map { it.mapToRawNavGraphGenParams(false) }.toList() +
                navHostGraphAnnotations.map { it.mapToRawNavGraphGenParams(true) }
    }

    private fun KSClassDeclaration.mapToRawNavGraphGenParams(isNavHostGraph: Boolean): RawNavGraphGenParams {
        if (classKind != KSPClassKind.ANNOTATION_CLASS) {
            throw IllegalDestinationsSetup("Classes annotated with `@NavGraph` must be annotation classes!")
        }

        primaryConstructor?.parameters?.firstOrNull {
            it.name?.asString() == "start" &&
                    it.type.toString() == "Boolean" &&
                    it.hasDefault
        }
            ?: throw IllegalDestinationsSetup(
                "Classes annotated with `@NavGraph` must contain " +
                        "a single parameter like: `val start: Boolean = false`!"
            )

        val navGraphAnnotation = if (isNavHostGraph) {
            annotations.first {
                it.shortName.asString() == NAV_HOST_GRAPH_ANNOTATION
            }
        } else {
            annotations.first {
                it.shortName.asString() == NAV_GRAPH_ANNOTATION
            }
        }

        val navGraphAnnotationNameArg = navGraphAnnotation
            .findArgumentValue<String>(DESTINATION_ANNOTATION_ROUTE_ARGUMENT)
        val navGraphAnnotationDefaultArg = navGraphAnnotation
            .findArgumentValue<Boolean>("default")!!
        val navGraphVisibility = navGraphAnnotation
            .findArgumentValue<KSType>("visibility")!!
            .toGenVisibility()
        val navGraphDefaultTransitions = navGraphAnnotation
            .findArgumentValue<KSType>("defaultTransitions")
            ?.findActualClassDeclaration()
            ?.takeIf { !it.isNothing }
            ?.toImportable()

        val deepLinks = navGraphAnnotation
            .findArgumentValue<ArrayList<KSAnnotation>>(DESTINATION_ANNOTATION_DEEP_LINKS_ARGUMENT)
            ?.map { it.toDeepLink() }
            .orEmpty()

        val importedRoutes = declarations.firstOrNull {
            it is KSClassDeclaration && it.isCompanionObject
        }?.annotations?.mapNotNull {
            when (it.shortName.asString()) {
                "ImportDestination" -> it.getImportedDestination()
                "ImportNavGraph" -> it.getImportedNavGraph()
                else -> null
            }
        }.orEmpty()

        val navArgs = navGraphAnnotation
            .getNavArgsDelegateType(resolver, navTypeSerializersByType)

        if (isNavHostGraph && navGraphDefaultTransitions == null) {
            throw IllegalDestinationsSetup(
                "A $NAV_HOST_GRAPH_ANNOTATION needs a non Nothing::class as defaultTransitions! " +
                        "Use `NoTransitions` if you wish to have no animations as default for this nav graph ${simpleName.asString()}"
            )
        }

        var parentGraphAnnotationResolved: KSType? = null
        val parentGraphAnnotation = annotations.firstOrNull { annotationOfAnnotation ->
            if (annotationOfAnnotation.shortName.asString() == NAV_GRAPH_ANNOTATION) return@firstOrNull false

            val resolved = annotationOfAnnotation.annotationType.resolve()
            resolved.declaration.annotations.any {
                val annotationShortName = it.shortName.asString()
                if (annotationShortName != NAV_GRAPH_ANNOTATION && annotationShortName != NAV_HOST_GRAPH_ANNOTATION) {
                    return@any false
                }

                val annotationQualifiedName =
                    it.annotationType.resolve().declaration.qualifiedName?.asString()
                annotationQualifiedName == NAV_GRAPH_ANNOTATION_QUALIFIED
                        || annotationQualifiedName == NAV_HOST_GRAPH_ANNOTATION_QUALIFIED
            }.also {
                if (it) parentGraphAnnotationResolved = resolved
            }
        }

        val isParentStart = parentGraphAnnotation?.arguments?.first()?.value as? Boolean?
        val parent = parentGraphAnnotationResolved?.let {
            Importable(
                it.declaration.simpleName.asString(),
                it.declaration.qualifiedName!!.asString()
            )
        }

        if (isNavHostGraph && parent != null) {
            throw IllegalDestinationsSetup("NavHostGraph's cannot have a parent navigation graph! (${simpleName.asString()}, parent = ${parent.simpleName})")
        }

        containingFile?.let { mutableKSFileSourceMapper[it.filePath] = containingFile }
        navArgs?.file?.let { mutableKSFileSourceMapper[it.filePath] = it }

        return RawNavGraphGenParams(
            sourceIds = listOfNotNull(containingFile?.filePath, navArgs?.file?.filePath),
            routeOverride = if (navGraphAnnotationNameArg == NAV_GRAPH_ANNOTATION_DEFAULT_NAME) null else navGraphAnnotationNameArg,
            default = navGraphAnnotationDefaultArg,
            isNavHostGraph = isNavHostGraph,
            defaultTransitions = navGraphDefaultTransitions,
            annotationType = Importable(
                this.simpleName.asString(),
                this.qualifiedName!!.asString()
            ),
            parent = parent,
            isParentStart = isParentStart,
            deepLinks = deepLinks,
            navArgs = navArgs?.type,
            visibility = navGraphVisibility,
            importedRoutes = importedRoutes.toList() //TODO RACOSTA
        )
    }

    private fun KSAnnotation.getImportedDestination(): ImportedRoute.Destination {
        val destinationType = findArgumentValue<KSType>("destination")!!
        val importable = destinationType.let {
            Importable(
                it.declaration.simpleName.asString(),
                it.declaration.qualifiedName!!.asString()
            )
        }

        val superType =
            (destinationType.declaration as KSClassDeclaration).superTypes.first().resolve()
        val navArgs = if (superType.declaration.simpleName.asString() == "TypedDestinationSpec") {
            superType.arguments.first().type!!.resolve()
                .getNavArgsDelegateType(resolver, navTypeSerializersByType)?.type
        } else {
            null
        }

        val deepLinks = findArgumentValue<ArrayList<KSAnnotation>>(DESTINATION_ANNOTATION_DEEP_LINKS_ARGUMENT)
            ?.map { it.toDeepLink() }
            .orEmpty()

        return ImportedRoute.Destination(
            isStart = findArgumentValue<Boolean>("start")!!,
            generatedType = importable,
            navArgs = navArgs,
            requireOptInAnnotationTypes = destinationType.declaration.findAllRequireOptInAnnotations(),
            additionalDeepLinks = deepLinks,
            overriddenDestinationStyleType = destinationMappingUtils.getDestinationStyleType(this, "@ImportDestination of ${importable.preferredSimpleName}", allowNothing = true),
            additionalComposableWrappers = destinationMappingUtils.getDestinationWrappers(this)!!,
        )
    }

    private fun KSAnnotation.getImportedNavGraph(): ImportedRoute.NavGraph {
        val graphType = findArgumentValue<KSType>("graph")!!
        val importable = graphType.let {
            Importable(
                it.declaration.simpleName.asString(),
                it.declaration.qualifiedName!!.asString()
            )
        }

        val superType =
            (graphType.declaration as KSClassDeclaration).superTypes.first().resolve()

        val navArgs =
            if (superType.declaration.simpleName.asString() == "TypedNavGraphSpec") {
                superType.arguments.first().type!!.resolve()
                    .getNavArgsDelegateType(resolver, navTypeSerializersByType)?.type
            } else {
                null
            }

        val deepLinks = findArgumentValue<ArrayList<KSAnnotation>>(DESTINATION_ANNOTATION_DEEP_LINKS_ARGUMENT)
            ?.map { it.toDeepLink() }
            .orEmpty()

        val navGraphDefaultTransitions = findArgumentValue<KSType>("defaultTransitions")
            ?.findActualClassDeclaration()
            ?.takeIf { !it.isNothing }
            ?.toImportable()

        return ImportedRoute.NavGraph(
            isStart = findArgumentValue<Boolean>("start")!!,
            generatedType = importable,
            navArgs = navArgs,
            requireOptInAnnotationTypes = graphType.declaration.findAllRequireOptInAnnotations(),
            additionalDeepLinks = deepLinks,
            overriddenDefaultTransitions = if (navGraphDefaultTransitions?.qualifiedName == "com.ramcosta.composedestinations.annotation.ImportNavGraph.Companion.NoOverride") {
                ImportedRoute.NavGraph.OverrideDefaultTransitions.NoOverride
            } else {
                ImportedRoute.NavGraph.OverrideDefaultTransitions.Override(navGraphDefaultTransitions)
            }
        )
    }
}
