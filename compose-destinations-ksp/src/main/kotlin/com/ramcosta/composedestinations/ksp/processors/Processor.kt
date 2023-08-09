package com.ramcosta.composedestinations.ksp.processors

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Modifier
import com.ramcosta.composedestinations.codegen.CodeGenerator
import com.ramcosta.composedestinations.codegen.commons.ACTIVITY_DESTINATION_ANNOTATION_QUALIFIED
import com.ramcosta.composedestinations.codegen.commons.CORE_BOTTOM_SHEET_DESTINATION_STYLE
import com.ramcosta.composedestinations.codegen.commons.CORE_PACKAGE_NAME
import com.ramcosta.composedestinations.codegen.commons.DESTINATION_ANNOTATION_QUALIFIED
import com.ramcosta.composedestinations.codegen.commons.IllegalDestinationsSetup
import com.ramcosta.composedestinations.codegen.commons.NAV_GRAPH_ANNOTATION_QUALIFIED
import com.ramcosta.composedestinations.codegen.commons.NAV_HOST_GRAPH_ANNOTATION_QUALIFIED
import com.ramcosta.composedestinations.codegen.commons.NAV_TYPE_SERIALIZER_ANNOTATION_QUALIFIED
import com.ramcosta.composedestinations.codegen.facades.Logger
import com.ramcosta.composedestinations.codegen.model.ClassKind
import com.ramcosta.composedestinations.codegen.model.Importable
import com.ramcosta.composedestinations.codegen.model.NavTypeSerializer
import com.ramcosta.composedestinations.ksp.codegen.KspCodeOutputStreamMaker
import com.ramcosta.composedestinations.ksp.codegen.KspLogger
import com.ramcosta.composedestinations.ksp.commons.DestinationMappingUtils
import com.ramcosta.composedestinations.ksp.commons.MutableKSFileSourceMapper
import com.ramcosta.composedestinations.ksp.commons.findActualClassDeclaration

class Processor(
    private val codeGenerator: KSPCodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>,
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        Logger.instance = KspLogger(logger)

        val composableDestinations = resolver.getComposableDestinations()
        val activityDestinations = resolver.getActivityDestinations()
        val navGraphAnnotations = resolver.getNavGraphAnnotations()
        val navHostGraphAnnotations = resolver.getNavHostGraphAnnotations()

        if (!composableDestinations.iterator().hasNext() &&
            !activityDestinations.iterator().hasNext() &&
            !navGraphAnnotations.iterator().hasNext() &&
            !navHostGraphAnnotations.iterator().hasNext()
        ) {
            return emptyList()
        }

        val navTypeSerializers = resolver.getNavTypeSerializers()
        val codeGenConfig = ConfigParser(options).parse()
        val destinationMappingUtils = DestinationMappingUtils(resolver)

        val mutableKSFileSourceMapper = MutableKSFileSourceMapper()
        val classesToNavGraphsMapper = KspToCodeGenNavGraphsMapper(
            resolver,
            destinationMappingUtils,
            mutableKSFileSourceMapper,
            navTypeSerializers.associateBy { it.genericType }
        )
        val navGraphs = classesToNavGraphsMapper.map(navGraphAnnotations, navHostGraphAnnotations)

        val functionsToDestinationsMapper = KspToCodeGenDestinationsMapper(
            resolver,
            destinationMappingUtils,
            mutableKSFileSourceMapper,
            navTypeSerializers.associateBy { it.genericType }
        )
        val kspCodeOutputStreamMaker = KspCodeOutputStreamMaker(codeGenerator, mutableKSFileSourceMapper)
        val destinations = functionsToDestinationsMapper.map(composableDestinations.map { it.immutable() }, activityDestinations)

        CodeGenerator(
            codeGenerator = kspCodeOutputStreamMaker,
            isBottomSheetDependencyPresent = resolver.isBottomSheetDepPresent(),
            codeGenConfig = codeGenConfig
        ).generate(destinations, navGraphs, navTypeSerializers)

        return emptyList()
    }

    private class DestinationAnnotationsPath {
        var annotations: Sequence<KSAnnotation> = emptySequence()
        var function: KSFunctionDeclaration? = null

        fun copy(): DestinationAnnotationsPath {
            return DestinationAnnotationsPath().also {
                it.annotations = annotations
                it.function = function
            }
        }

        override fun toString(): String {
            return "DestinationAnnotationsPath(annotations=${annotations.toList().map { it.shortName.asString() }}, function=${function?.qualifiedName?.asString()})"
        }

        fun immutable() = DestinationAnnotationsPath(annotations.toList(), function!!)
    }

    private fun Resolver.getComposableDestinations(
        name: String = DESTINATION_ANNOTATION_QUALIFIED,
        annotationsPath: DestinationAnnotationsPath = DestinationAnnotationsPath()
    ): Sequence<DestinationAnnotationsPath> {
        val symbolsWithAnnotation = getSymbolsWithAnnotation(name)

        return symbolsWithAnnotation.flatMap {
            addPath(
                name,
                it,
                annotationsPath.copy()
            )
        }
    }

    private fun Resolver.addPath(
        name: String,
        annotated: KSAnnotated,
        annotationsPath: DestinationAnnotationsPath
    ) : Sequence<DestinationAnnotationsPath> {
        return when {
            annotated is KSFunctionDeclaration -> {
                annotated.annotations.filter { it.annotationType.resolve().declaration.qualifiedName!!.asString() == name }.map { annotation ->
                    annotationsPath.copy().also {
                        it.annotations += annotation
                        it.function = annotated
                    }
                }
            }

            annotated is KSClassDeclaration && Modifier.ANNOTATION in annotated.modifiers -> {
                annotationsPath.annotations += annotated.annotations.find { it.annotationType.resolve().declaration.qualifiedName!!.asString() == name }!!
                getComposableDestinations(annotated.qualifiedName!!.asString(), annotationsPath)
            }

            else -> emptySequence()
        }
    }

    private fun Resolver.getActivityDestinations(name: String = ACTIVITY_DESTINATION_ANNOTATION_QUALIFIED): Sequence<KSClassDeclaration> {
        val symbolsWithAnnotation = getSymbolsWithAnnotation(name)

        return symbolsWithAnnotation
            .filterIsInstance<KSClassDeclaration>()
            .filter { Modifier.ANNOTATION !in it.modifiers } + getAnnotationActivityDestinations(symbolsWithAnnotation)
    }

    private fun Resolver.getAnnotationActivityDestinations(symbolsWithAnnotation: Sequence<KSAnnotated>): Sequence<KSClassDeclaration> {
        return symbolsWithAnnotation.filterIsInstance<KSClassDeclaration>()
            .filter { Modifier.ANNOTATION in it.modifiers && it.qualifiedName != null }
            .flatMap {
                getActivityDestinations(it.qualifiedName!!.asString())
            }
    }

    private fun Resolver.getNavGraphAnnotations(): Sequence<KSClassDeclaration> {
        return getSymbolsWithAnnotation(NAV_GRAPH_ANNOTATION_QUALIFIED)
            .filterIsInstance<KSClassDeclaration>()
    }

    private fun Resolver.getNavHostGraphAnnotations(): Sequence<KSClassDeclaration> {
        return getSymbolsWithAnnotation(NAV_HOST_GRAPH_ANNOTATION_QUALIFIED)
            .filterIsInstance<KSClassDeclaration>()
    }

    private fun Resolver.getNavTypeSerializers(): List<NavTypeSerializer> {
        return getSymbolsWithAnnotation(NAV_TYPE_SERIALIZER_ANNOTATION_QUALIFIED)
            .filterIsInstance<KSClassDeclaration>().map { serializer ->
                if (serializer.classKind != KSPClassKind.CLASS && serializer.classKind != KSPClassKind.OBJECT) {
                    throw IllegalDestinationsSetup("${serializer.simpleName}: Type serializers must be either class or object!")
                }

                var superType: KSType? = null
                for (type in serializer.superTypes) {
                    val resolvedType = type.resolve()
                    val resolvedTypeString = resolvedType.declaration.qualifiedName?.asString()
                    if (resolvedTypeString ==
                        "$CORE_PACKAGE_NAME.navargs.DestinationsNavTypeSerializer") {
                        superType = resolvedType
                        break
                    }
                }

                if (superType == null) {
                    throw IllegalDestinationsSetup("${serializer.simpleName}: Type serializers must implement DestinationsNavTypeSerializer!")
                }

                val genericType = superType.arguments.first().type?.resolve()?.findActualClassDeclaration()
                    ?: throw IllegalDestinationsSetup("${serializer.simpleName} type serializer has an issue with its type argument!")

                NavTypeSerializer(
                    classKind = if (serializer.classKind == KSPClassKind.CLASS) ClassKind.CLASS else ClassKind.OBJECT,
                    serializerType = Importable(serializer.simpleName.asString(), serializer.qualifiedName!!.asString()),
                    genericType = Importable(genericType.simpleName.asString(), genericType.qualifiedName!!.asString()),
                )
            }.toList()
    }

    private fun Resolver.isBottomSheetDepPresent(): Boolean {
        return getClassDeclarationByName("$CORE_PACKAGE_NAME.bottomsheet.spec.$CORE_BOTTOM_SHEET_DESTINATION_STYLE") != null
    }
}

typealias KSPClassKind = com.google.devtools.ksp.symbol.ClassKind
typealias KSPCodeGenerator = com.google.devtools.ksp.processing.CodeGenerator
