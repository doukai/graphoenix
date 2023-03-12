package io.graphoenix.java.generator.implementer;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.error.ElementProcessException;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.spi.annotation.MutationOperation;
import io.graphoenix.spi.annotation.QueryOperation;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.vavr.collection.HashMap;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.tinylog.Logger;
import reactor.core.publisher.Mono;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static io.graphoenix.core.error.ElementProcessErrorType.UNSUPPORTED_OPERATION_METHOD_RETURN_TYPE;
import static io.graphoenix.core.error.GraphQLErrorType.MUTATION_TYPE_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.QUERY_TYPE_NOT_EXIST;

@ApplicationScoped
public class OperationInterfaceImplementer {

    private final IGraphQLDocumentManager manager;
    private final TypeManager typeManager;
    private final GraphQLConfig graphQLConfig;

    @Inject
    public OperationInterfaceImplementer(IGraphQLDocumentManager manager, TypeManager typeManager, GraphQLConfig graphQLConfig) {
        this.manager = manager;
        this.typeManager = typeManager;
        this.graphQLConfig = graphQLConfig;
    }

    public void writeToFiler(PackageElement packageElement, TypeElement typeElement, TypeMirror operationDAO, String suffix, Filer filer) throws IOException {
        this.buildImplementClass(packageElement, typeElement, operationDAO, suffix).writeTo(filer);
        Logger.info("{} build success", typeElement.getSimpleName().toString() + "Impl");
    }

    public JavaFile buildImplementClass(PackageElement packageElement, TypeElement typeElement, TypeMirror operationDAO, String suffix) {
        TypeSpec.Builder builder = TypeSpec.classBuilder(ClassName.get(packageElement.getQualifiedName().toString(), typeElement.getSimpleName().toString() + "Impl"))
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(ApplicationScoped.class)
                .superclass(operationDAO)
                .addSuperinterface(typeElement.asType())
                .addFields(buildFileContentFields(typeElement))
                .addStaticBlock(buildFileContentFieldInitializeCodeBlock(packageElement, typeElement, suffix))
                .addMethods(typeElement.getEnclosedElements()
                        .stream()
                        .filter(element -> element.getKind().equals(ElementKind.METHOD))
                        .map(element -> executableElementToMethodSpec(typeElement, (ExecutableElement) element))
                        .collect(Collectors.toList())
                );

        return JavaFile.builder(packageElement.getQualifiedName().toString(), builder.build()).build();
    }

    private List<FieldSpec> buildFileContentFields(TypeElement typeElement) {
        return typeElement.getEnclosedElements().stream()
                .map(element -> buildFileContentField(typeElement, element))
                .collect(Collectors.toList());
    }

    private FieldSpec buildFileContentField(TypeElement typeElement, Element element) {
        return FieldSpec.builder(
                TypeName.get(String.class),
                element.getSimpleName().toString().concat("_" + typeElement.getEnclosedElements().indexOf(element)),
                Modifier.PRIVATE,
                Modifier.STATIC,
                Modifier.FINAL
        ).build();
    }

    private CodeBlock buildFileContentFieldInitializeCodeBlock(PackageElement packageElement, TypeElement typeElement, String suffix) {
        ClassName typeClassName = ClassName.get(packageElement.getQualifiedName().toString(), typeElement.getSimpleName().toString() + "Impl");
        CodeBlock.Builder builder = CodeBlock.builder();
        typeElement.getEnclosedElements()
                .forEach(element ->
                        builder.addStatement(
                                "$L = fileToString($T.class,$S)",
                                element.getSimpleName().toString()
                                        .concat("_" + typeElement.getEnclosedElements().indexOf(element)),
                                typeClassName,
                                typeElement.getSimpleName().toString()
                                        .concat("_")
                                        .concat(element.getSimpleName().toString())
                                        .concat("_" + typeElement.getEnclosedElements().indexOf(element))
                                        .concat(".")
                                        .concat(suffix)
                        )
                );
        return builder.build();
    }

    private MethodSpec executableElementToMethodSpec(TypeElement typeElement, ExecutableElement executableElement) {
        TypeName typeName = ClassName.get(executableElement.getReturnType());

        MethodSpec.Builder builder = MethodSpec.methodBuilder(executableElement.getSimpleName().toString())
                .addModifiers(Modifier.PUBLIC)
                .addParameters(
                        executableElement.getParameters().stream()
                                .map(variableElement -> ParameterSpec.builder(TypeName.get(variableElement.asType()), variableElement.getSimpleName().toString()).build())
                                .collect(Collectors.toList())
                )
                .returns(typeName)
                .addException(ClassName.get(Exception.class));

        if (executableElement.getParameters().size() == 0) {
            builder.addStatement(getCodeBlock(typeElement, executableElement));
        } else {
            CodeBlock mapOf = CodeBlock.join(
                    executableElement.getParameters().stream()
                            .map(parameter ->
                                    CodeBlock.of(
                                            "$S, (Object)$L",
                                            parameter.getSimpleName().toString(),
                                            parameter.getSimpleName().toString()
                                    )
                            )
                            .collect(Collectors.toList()),
                    ", ");
            builder.addStatement(getCodeBlock(typeElement, executableElement, mapOf));
        }
        return builder.build();
    }

    private CodeBlock getCodeBlock(TypeElement typeElement, ExecutableElement executableElement, CodeBlock mapOf) {
        TypeName typeName = ClassName.get(executableElement.getReturnType());
        if (typeName instanceof ParameterizedTypeName) {
            ClassName rawType = ((ParameterizedTypeName) typeName).rawType;
            if (rawType.canonicalName().equals(Mono.class.getCanonicalName())) {
                TypeName argumentType = ((ParameterizedTypeName) typeName).typeArguments.get(0);
                if (executableElement.getAnnotation(QueryOperation.class) != null) {
                    String queryTypeName = manager.getQueryOperationTypeName().orElseThrow(() -> new GraphQLErrors(QUERY_TYPE_NOT_EXIST));
                    String value = executableElement.getAnnotation(QueryOperation.class).value();
                    if (argumentType instanceof ParameterizedTypeName) {
                        Optional<ClassName> collectionImplementationClassName = getCollectionImplementationClassName(((ParameterizedTypeName) argumentType).rawType);
                        if (collectionImplementationClassName.isPresent()) {
                            return CodeBlock.of(
                                    "return findAsync($L, $T.of($L).toJavaMap(), $T.class).map($T::$L).map($T::new)",
                                    executableElement.getSimpleName().toString()
                                            .concat("_" + typeElement.getEnclosedElements().indexOf(executableElement)),
                                    ClassName.get(HashMap.class),
                                    mapOf,
                                    ClassName.get(graphQLConfig.getObjectTypePackageName(), queryTypeName),
                                    ClassName.get(graphQLConfig.getObjectTypePackageName(), queryTypeName),
                                    typeManager.getFieldGetterMethodName(value),
                                    collectionImplementationClassName.get()
                            );
                        }
                    }
                    return CodeBlock.of(
                            "return findAsync($L, $T.of($L).toJavaMap(), $T.class).map($T::$L)",
                            executableElement.getSimpleName().toString()
                                    .concat("_" + typeElement.getEnclosedElements().indexOf(executableElement)),
                            ClassName.get(HashMap.class),
                            mapOf,
                            ClassName.get(graphQLConfig.getObjectTypePackageName(), queryTypeName),
                            ClassName.get(graphQLConfig.getObjectTypePackageName(), queryTypeName),
                            typeManager.getFieldGetterMethodName(value)
                    );
                } else if (executableElement.getAnnotation(MutationOperation.class) != null) {
                    String mutationTypeName = manager.getMutationOperationTypeName().orElseThrow(() -> new GraphQLErrors(MUTATION_TYPE_NOT_EXIST));
                    String value = executableElement.getAnnotation(MutationOperation.class).value();
                    if (argumentType instanceof ParameterizedTypeName) {
                        Optional<ClassName> collectionImplementationClassName = getCollectionImplementationClassName(((ParameterizedTypeName) argumentType).rawType);
                        if (collectionImplementationClassName.isPresent()) {
                            return CodeBlock.of(
                                    "return saveAsync($L, $T.of($L).toJavaMap(), $T.class).map($T::$L).map($T::new)",
                                    executableElement.getSimpleName().toString()
                                            .concat("_" + typeElement.getEnclosedElements().indexOf(executableElement)),
                                    ClassName.get(HashMap.class),
                                    mapOf,
                                    ClassName.get(graphQLConfig.getObjectTypePackageName(), mutationTypeName),
                                    ClassName.get(graphQLConfig.getObjectTypePackageName(), mutationTypeName),
                                    typeManager.getFieldGetterMethodName(value),
                                    collectionImplementationClassName.get()
                            );
                        }
                    }
                    return CodeBlock.of(
                            "return saveAsync($L, $T.of($L).toJavaMap(), $T.class).map($T::$L)",
                            executableElement.getSimpleName().toString()
                                    .concat("_" + typeElement.getEnclosedElements().indexOf(executableElement)),
                            ClassName.get(HashMap.class),
                            mapOf,
                            ClassName.get(graphQLConfig.getObjectTypePackageName(), mutationTypeName),
                            ClassName.get(graphQLConfig.getObjectTypePackageName(), mutationTypeName),
                            typeManager.getFieldGetterMethodName(value)
                    );
                }
            } else if (rawType.canonicalName().equals(PublisherBuilder.class.getCanonicalName())) {
                TypeName argumentType = ((ParameterizedTypeName) typeName).typeArguments.get(0);
                if (executableElement.getAnnotation(QueryOperation.class) != null) {
                    String queryTypeName = manager.getQueryOperationTypeName().orElseThrow(() -> new GraphQLErrors(QUERY_TYPE_NOT_EXIST));
                    String value = executableElement.getAnnotation(QueryOperation.class).value();
                    if (argumentType instanceof ParameterizedTypeName) {
                        Optional<ClassName> collectionImplementationClassName = getCollectionImplementationClassName(((ParameterizedTypeName) argumentType).rawType);
                        if (collectionImplementationClassName.isPresent()) {
                            return CodeBlock.of(
                                    "return findAsyncBuilder($L, $T.of($L).toJavaMap(), $T.class).map($T::$L).map($T::new)",
                                    executableElement.getSimpleName().toString()
                                            .concat("_" + typeElement.getEnclosedElements().indexOf(executableElement)),
                                    ClassName.get(HashMap.class),
                                    mapOf,
                                    ClassName.get(graphQLConfig.getObjectTypePackageName(), queryTypeName),
                                    ClassName.get(graphQLConfig.getObjectTypePackageName(), queryTypeName),
                                    typeManager.getFieldGetterMethodName(value),
                                    collectionImplementationClassName.get()
                            );
                        }
                    }
                    return CodeBlock.of(
                            "return findAsyncBuilder($L, $T.of($L).toJavaMap(), $T.class).map($T::$L)",
                            executableElement.getSimpleName().toString()
                                    .concat("_" + typeElement.getEnclosedElements().indexOf(executableElement)),
                            ClassName.get(HashMap.class),
                            mapOf,
                            ClassName.get(graphQLConfig.getObjectTypePackageName(), queryTypeName),
                            ClassName.get(graphQLConfig.getObjectTypePackageName(), queryTypeName),
                            typeManager.getFieldGetterMethodName(value)
                    );
                } else if (executableElement.getAnnotation(MutationOperation.class) != null) {
                    String mutationTypeName = manager.getMutationOperationTypeName().orElseThrow(() -> new GraphQLErrors(MUTATION_TYPE_NOT_EXIST));
                    String value = executableElement.getAnnotation(MutationOperation.class).value();
                    if (argumentType instanceof ParameterizedTypeName) {
                        Optional<ClassName> collectionImplementationClassName = getCollectionImplementationClassName(((ParameterizedTypeName) argumentType).rawType);
                        if (collectionImplementationClassName.isPresent()) {
                            return CodeBlock.of(
                                    "return saveAsyncBuilder($L, $T.of($L).toJavaMap(), $T.class).map($T::$L).map($T::new)",
                                    executableElement.getSimpleName().toString()
                                            .concat("_" + typeElement.getEnclosedElements().indexOf(executableElement)),
                                    ClassName.get(HashMap.class),
                                    mapOf,
                                    ClassName.get(graphQLConfig.getObjectTypePackageName(), mutationTypeName),
                                    ClassName.get(graphQLConfig.getObjectTypePackageName(), mutationTypeName),
                                    typeManager.getFieldGetterMethodName(value),
                                    collectionImplementationClassName.get()
                            );
                        }
                    }
                    return CodeBlock.of(
                            "return saveAsyncBuilder($L, $T.of($L).toJavaMap(), $T.class).map($T::$L)",
                            executableElement.getSimpleName().toString()
                                    .concat("_" + typeElement.getEnclosedElements().indexOf(executableElement)),
                            ClassName.get(HashMap.class),
                            mapOf,
                            ClassName.get(graphQLConfig.getObjectTypePackageName(), mutationTypeName),
                            ClassName.get(graphQLConfig.getObjectTypePackageName(), mutationTypeName),
                            typeManager.getFieldGetterMethodName(value)
                    );
                }
            } else {
                if (executableElement.getAnnotation(QueryOperation.class) != null) {
                    String queryTypeName = manager.getQueryOperationTypeName().orElseThrow(() -> new GraphQLErrors(QUERY_TYPE_NOT_EXIST));
                    String value = executableElement.getAnnotation(QueryOperation.class).value();
                    Optional<ClassName> collectionImplementationClassName = getCollectionImplementationClassName(rawType);
                    return collectionImplementationClassName
                            .map(className ->
                                    CodeBlock.of(
                                            "return new $T(find($L, $T.of($L).toJavaMap(), $T.class).$L())",
                                            className,
                                            executableElement.getSimpleName().toString()
                                                    .concat("_" + typeElement.getEnclosedElements().indexOf(executableElement)),
                                            ClassName.get(HashMap.class),
                                            mapOf,
                                            ClassName.get(graphQLConfig.getObjectTypePackageName(), queryTypeName),
                                            typeManager.getFieldGetterMethodName(value)
                                    )
                            ).orElseGet(() ->
                                    CodeBlock.of(
                                            "return find($L, $T.of($L).toJavaMap(), $T.class).$L()",
                                            executableElement.getSimpleName().toString()
                                                    .concat("_" + typeElement.getEnclosedElements().indexOf(executableElement)),
                                            ClassName.get(HashMap.class),
                                            mapOf,
                                            ClassName.get(graphQLConfig.getObjectTypePackageName(), queryTypeName),
                                            typeManager.getFieldGetterMethodName(value)
                                    )
                            );
                } else if (executableElement.getAnnotation(MutationOperation.class) != null) {
                    String mutationTypeName = manager.getMutationOperationTypeName().orElseThrow(() -> new GraphQLErrors(MUTATION_TYPE_NOT_EXIST));
                    String value = executableElement.getAnnotation(MutationOperation.class).value();
                    Optional<ClassName> collectionImplementationClassName = getCollectionImplementationClassName(rawType);
                    return collectionImplementationClassName
                            .map(className ->
                                    CodeBlock.of(
                                            "return new $T(save($L, $T.of($L).toJavaMap(), $T.class).$L())",
                                            className,
                                            executableElement.getSimpleName().toString()
                                                    .concat("_" + typeElement.getEnclosedElements().indexOf(executableElement)),
                                            ClassName.get(HashMap.class),
                                            mapOf,
                                            ClassName.get(graphQLConfig.getObjectTypePackageName(), mutationTypeName),
                                            typeManager.getFieldGetterMethodName(value)
                                    )
                            ).orElseGet(() ->
                                    CodeBlock.of(
                                            "return save($L, $T.of($L).toJavaMap(), $T.class).$L()",
                                            executableElement.getSimpleName().toString()
                                                    .concat("_" + typeElement.getEnclosedElements().indexOf(executableElement)),
                                            ClassName.get(HashMap.class),
                                            mapOf,
                                            ClassName.get(graphQLConfig.getObjectTypePackageName(), mutationTypeName),
                                            typeManager.getFieldGetterMethodName(value)
                                    )
                            );
                }
            }
        } else {
            if (executableElement.getAnnotation(QueryOperation.class) != null) {
                String queryTypeName = manager.getQueryOperationTypeName().orElseThrow(() -> new GraphQLErrors(QUERY_TYPE_NOT_EXIST));
                String value = executableElement.getAnnotation(QueryOperation.class).value();
                return CodeBlock.of(
                        "return find($L, $T.of($L).toJavaMap(), $T.class).$L()",
                        executableElement.getSimpleName().toString()
                                .concat("_" + typeElement.getEnclosedElements().indexOf(executableElement)),
                        ClassName.get(HashMap.class),
                        mapOf,
                        ClassName.get(graphQLConfig.getObjectTypePackageName(), queryTypeName),
                        typeManager.getFieldGetterMethodName(value)
                );
            } else if (executableElement.getAnnotation(MutationOperation.class) != null) {
                String mutationTypeName = manager.getMutationOperationTypeName().orElseThrow(() -> new GraphQLErrors(MUTATION_TYPE_NOT_EXIST));
                String value = executableElement.getAnnotation(MutationOperation.class).value();
                return CodeBlock.of(
                        "return save($L, $T.of($L).toJavaMap(), $T.class).$L()",
                        executableElement.getSimpleName().toString()
                                .concat("_" + typeElement.getEnclosedElements().indexOf(executableElement)),
                        ClassName.get(HashMap.class),
                        mapOf,
                        ClassName.get(graphQLConfig.getObjectTypePackageName(), mutationTypeName),
                        typeManager.getFieldGetterMethodName(value)
                );
            }
        }
        throw new ElementProcessException(UNSUPPORTED_OPERATION_METHOD_RETURN_TYPE.bind(executableElement.getReturnType().toString()));
    }

    private CodeBlock getCodeBlock(TypeElement typeElement, ExecutableElement executableElement) {
        TypeName typeName = ClassName.get(executableElement.getReturnType());
        if (typeName instanceof ParameterizedTypeName) {
            ClassName rawType = ((ParameterizedTypeName) typeName).rawType;
            if (rawType.canonicalName().equals(Mono.class.getCanonicalName())) {
                TypeName argumentType = ((ParameterizedTypeName) typeName).typeArguments.get(0);
                if (executableElement.getAnnotation(QueryOperation.class) != null) {
                    String queryTypeName = manager.getQueryOperationTypeName().orElseThrow(() -> new GraphQLErrors(QUERY_TYPE_NOT_EXIST));
                    String value = executableElement.getAnnotation(QueryOperation.class).value();
                    if (argumentType instanceof ParameterizedTypeName) {
                        Optional<ClassName> collectionImplementationClassName = getCollectionImplementationClassName(((ParameterizedTypeName) argumentType).rawType);
                        if (collectionImplementationClassName.isPresent()) {
                            return CodeBlock.of(
                                    "return findAsync($L, new $T<>(), $T.class).map($T::$L).map($T::new)",
                                    executableElement.getSimpleName().toString()
                                            .concat("_" + typeElement.getEnclosedElements().indexOf(executableElement)),
                                    ClassName.get(java.util.HashMap.class),
                                    ClassName.get(graphQLConfig.getObjectTypePackageName(), queryTypeName),
                                    ClassName.get(graphQLConfig.getObjectTypePackageName(), queryTypeName),
                                    typeManager.getFieldGetterMethodName(value),
                                    collectionImplementationClassName.get()
                            );
                        }
                    }
                    return CodeBlock.of(
                            "return findAsync($L, new $T<>(), $T.class).map($T::$L)",
                            executableElement.getSimpleName().toString()
                                    .concat("_" + typeElement.getEnclosedElements().indexOf(executableElement)),
                            ClassName.get(java.util.HashMap.class),
                            ClassName.get(graphQLConfig.getObjectTypePackageName(), queryTypeName),
                            ClassName.get(graphQLConfig.getObjectTypePackageName(), queryTypeName),
                            typeManager.getFieldGetterMethodName(value)
                    );
                } else if (executableElement.getAnnotation(MutationOperation.class) != null) {
                    String mutationTypeName = manager.getMutationOperationTypeName().orElseThrow(() -> new GraphQLErrors(MUTATION_TYPE_NOT_EXIST));
                    String value = executableElement.getAnnotation(MutationOperation.class).value();
                    if (argumentType instanceof ParameterizedTypeName) {
                        Optional<ClassName> collectionImplementationClassName = getCollectionImplementationClassName(((ParameterizedTypeName) argumentType).rawType);
                        if (collectionImplementationClassName.isPresent()) {
                            return CodeBlock.of(
                                    "return saveAsync($L, $new $T<>(), $T.class).map($T::$L).map($T::new)",
                                    executableElement.getSimpleName().toString()
                                            .concat("_" + typeElement.getEnclosedElements().indexOf(executableElement)),
                                    ClassName.get(java.util.HashMap.class),
                                    ClassName.get(graphQLConfig.getObjectTypePackageName(), mutationTypeName),
                                    ClassName.get(graphQLConfig.getObjectTypePackageName(), mutationTypeName),
                                    typeManager.getFieldGetterMethodName(value),
                                    collectionImplementationClassName.get()
                            );
                        }
                    }
                    return CodeBlock.of(
                            "return saveAsync($L, $new $T<>(), $T.class).map($T::$L)",
                            executableElement.getSimpleName().toString()
                                    .concat("_" + typeElement.getEnclosedElements().indexOf(executableElement)),
                            ClassName.get(java.util.HashMap.class),
                            ClassName.get(graphQLConfig.getObjectTypePackageName(), mutationTypeName),
                            ClassName.get(graphQLConfig.getObjectTypePackageName(), mutationTypeName),
                            typeManager.getFieldGetterMethodName(value)
                    );
                }
            } else if (rawType.canonicalName().equals(PublisherBuilder.class.getCanonicalName())) {
                TypeName argumentType = ((ParameterizedTypeName) typeName).typeArguments.get(0);
                if (executableElement.getAnnotation(QueryOperation.class) != null) {
                    String queryTypeName = manager.getQueryOperationTypeName().orElseThrow(() -> new GraphQLErrors(QUERY_TYPE_NOT_EXIST));
                    String value = executableElement.getAnnotation(QueryOperation.class).value();
                    if (argumentType instanceof ParameterizedTypeName) {
                        Optional<ClassName> collectionImplementationClassName = getCollectionImplementationClassName(((ParameterizedTypeName) argumentType).rawType);
                        if (collectionImplementationClassName.isPresent()) {
                            return CodeBlock.of(
                                    "return findAsyncBuilder($L, new $T<>(), $T.class).map($T::$L).map($T::new)",
                                    executableElement.getSimpleName().toString()
                                            .concat("_" + typeElement.getEnclosedElements().indexOf(executableElement)),
                                    ClassName.get(java.util.HashMap.class),
                                    ClassName.get(graphQLConfig.getObjectTypePackageName(), queryTypeName),
                                    ClassName.get(graphQLConfig.getObjectTypePackageName(), queryTypeName),
                                    typeManager.getFieldGetterMethodName(value),
                                    collectionImplementationClassName.get()
                            );
                        }
                    }
                    return CodeBlock.of(
                            "return findAsyncBuilder($L, new $T<>(), $T.class).map($T::$L)",
                            executableElement.getSimpleName().toString()
                                    .concat("_" + typeElement.getEnclosedElements().indexOf(executableElement)),
                            ClassName.get(java.util.HashMap.class),
                            ClassName.get(graphQLConfig.getObjectTypePackageName(), queryTypeName),
                            ClassName.get(graphQLConfig.getObjectTypePackageName(), queryTypeName),
                            typeManager.getFieldGetterMethodName(value)
                    );
                } else if (executableElement.getAnnotation(MutationOperation.class) != null) {
                    String mutationTypeName = manager.getMutationOperationTypeName().orElseThrow(() -> new GraphQLErrors(MUTATION_TYPE_NOT_EXIST));
                    String value = executableElement.getAnnotation(MutationOperation.class).value();
                    if (argumentType instanceof ParameterizedTypeName) {
                        Optional<ClassName> collectionImplementationClassName = getCollectionImplementationClassName(((ParameterizedTypeName) argumentType).rawType);
                        if (collectionImplementationClassName.isPresent()) {
                            return CodeBlock.of(
                                    "return saveAsyncBuilder($L, new $T<>(), $T.class).map($T::$L).map($T::new)",
                                    executableElement.getSimpleName().toString()
                                            .concat("_" + typeElement.getEnclosedElements().indexOf(executableElement)),
                                    ClassName.get(java.util.HashMap.class),
                                    ClassName.get(graphQLConfig.getObjectTypePackageName(), mutationTypeName),
                                    ClassName.get(graphQLConfig.getObjectTypePackageName(), mutationTypeName),
                                    typeManager.getFieldGetterMethodName(value),
                                    collectionImplementationClassName.get()
                            );
                        }
                    }
                    return CodeBlock.of(
                            "return saveAsyncBuilder($L, new $T<>(), $T.class).map($T::$L)",
                            executableElement.getSimpleName().toString()
                                    .concat("_" + typeElement.getEnclosedElements().indexOf(executableElement)),
                            ClassName.get(java.util.HashMap.class),
                            ClassName.get(graphQLConfig.getObjectTypePackageName(), mutationTypeName),
                            ClassName.get(graphQLConfig.getObjectTypePackageName(), mutationTypeName),
                            typeManager.getFieldGetterMethodName(value)
                    );
                }
            } else {
                if (executableElement.getAnnotation(QueryOperation.class) != null) {
                    String queryTypeName = manager.getQueryOperationTypeName().orElseThrow(() -> new GraphQLErrors(QUERY_TYPE_NOT_EXIST));
                    String value = executableElement.getAnnotation(QueryOperation.class).value();
                    Optional<ClassName> collectionImplementationClassName = getCollectionImplementationClassName(rawType);
                    return collectionImplementationClassName
                            .map(className ->
                                    CodeBlock.of(
                                            "return new $T(find($L, new $T<>(), $T.class).$L())",
                                            className,
                                            executableElement.getSimpleName().toString()
                                                    .concat("_" + typeElement.getEnclosedElements().indexOf(executableElement)),
                                            ClassName.get(java.util.HashMap.class),
                                            ClassName.get(graphQLConfig.getObjectTypePackageName(), queryTypeName),
                                            typeManager.getFieldGetterMethodName(value)
                                    )
                            ).orElseGet(() ->
                                    CodeBlock.of(
                                            "return find($L, new $T<>(), $T.class).$L()",
                                            executableElement.getSimpleName().toString()
                                                    .concat("_" + typeElement.getEnclosedElements().indexOf(executableElement)),
                                            ClassName.get(java.util.HashMap.class),
                                            ClassName.get(graphQLConfig.getObjectTypePackageName(), queryTypeName),
                                            typeManager.getFieldGetterMethodName(value)
                                    )
                            );
                } else if (executableElement.getAnnotation(MutationOperation.class) != null) {
                    String mutationTypeName = manager.getMutationOperationTypeName().orElseThrow(() -> new GraphQLErrors(MUTATION_TYPE_NOT_EXIST));
                    String value = executableElement.getAnnotation(MutationOperation.class).value();
                    Optional<ClassName> collectionImplementationClassName = getCollectionImplementationClassName(rawType);
                    return collectionImplementationClassName
                            .map(className ->
                                    CodeBlock.of(
                                            "return new $T(save($L, new $T<>(), $T.class).$L())",
                                            className,
                                            executableElement.getSimpleName().toString()
                                                    .concat("_" + typeElement.getEnclosedElements().indexOf(executableElement)),
                                            ClassName.get(java.util.HashMap.class),
                                            ClassName.get(graphQLConfig.getObjectTypePackageName(), mutationTypeName),
                                            typeManager.getFieldGetterMethodName(value)
                                    )
                            ).orElseGet(() ->
                                    CodeBlock.of(
                                            "return save($L, new $T<>(), $T.class).$L()",
                                            executableElement.getSimpleName().toString()
                                                    .concat("_" + typeElement.getEnclosedElements().indexOf(executableElement)),
                                            ClassName.get(java.util.HashMap.class),
                                            ClassName.get(graphQLConfig.getObjectTypePackageName(), mutationTypeName),
                                            typeManager.getFieldGetterMethodName(value)
                                    )
                            );
                }
            }
        } else {
            if (executableElement.getAnnotation(QueryOperation.class) != null) {
                String queryTypeName = manager.getQueryOperationTypeName().orElseThrow(() -> new GraphQLErrors(QUERY_TYPE_NOT_EXIST));
                String value = executableElement.getAnnotation(QueryOperation.class).value();
                return CodeBlock.of(
                        "return find($L, new $T<>(), $T.class).$L()",
                        executableElement.getSimpleName().toString()
                                .concat("_" + typeElement.getEnclosedElements().indexOf(executableElement)),
                        ClassName.get(java.util.HashMap.class),
                        ClassName.get(graphQLConfig.getObjectTypePackageName(), queryTypeName),
                        typeManager.getFieldGetterMethodName(value)
                );
            } else if (executableElement.getAnnotation(MutationOperation.class) != null) {
                String mutationTypeName = manager.getMutationOperationTypeName().orElseThrow(() -> new GraphQLErrors(MUTATION_TYPE_NOT_EXIST));
                String value = executableElement.getAnnotation(MutationOperation.class).value();
                return CodeBlock.of(
                        "return save($L, new $T<>(), $T.class).$L()",
                        executableElement.getSimpleName().toString()
                                .concat("_" + typeElement.getEnclosedElements().indexOf(executableElement)),
                        ClassName.get(java.util.HashMap.class),
                        ClassName.get(graphQLConfig.getObjectTypePackageName(), mutationTypeName),
                        typeManager.getFieldGetterMethodName(value)
                );
            }
        }
        throw new ElementProcessException(UNSUPPORTED_OPERATION_METHOD_RETURN_TYPE.bind(executableElement.getReturnType().toString()));
    }

    private Optional<ClassName> getCollectionImplementationClassName(ClassName className) {
        if (className.canonicalName().equals(Collection.class.getCanonicalName())) {
            return Optional.empty();
        } else {
            if (className.canonicalName().equals(List.class.getCanonicalName())) {
                return Optional.of(ClassName.get(ArrayList.class));
            } else if (className.canonicalName().equals(Set.class.getCanonicalName())) {
                return Optional.of(ClassName.get(LinkedHashSet.class));
            } else {
                return Optional.of(className);
            }
        }
    }
}
