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
import graphql.parser.antlr.GraphqlParser;
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
import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static io.graphoenix.core.error.ElementProcessErrorType.UNSUPPORTED_OPERATION_METHOD_RETURN_TYPE;
import static io.graphoenix.core.error.GraphQLErrorType.MUTATION_TYPE_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.QUERY_TYPE_NOT_EXIST;
import static io.graphoenix.core.utils.TypeNameUtil.TYPE_NAME_UTIL;

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

    public void writeToFiler(Filer filer) throws IOException {
        manager.getOperationDefinitions()
                .map(operationDefinitionContext -> {
                            String[] operationNameArray = operationDefinitionContext.name().getText().split("_");
                            String packageName = String.join(".", Arrays.copyOfRange(operationNameArray, 0, operationNameArray.length - 4));
                            String interfaceName = operationNameArray[operationNameArray.length - 3];
                            return new AbstractMap.SimpleEntry<>(
                                    packageName,
                                    new AbstractMap.SimpleEntry<>(
                                            interfaceName,
                                            operationDefinitionContext
                                    )
                            );
                        }
                )
                .collect(
                        Collectors.groupingBy(
                                Map.Entry<String, AbstractMap.SimpleEntry<String, GraphqlParser.OperationDefinitionContext>>::getKey,
                                Collectors.mapping(
                                        Map.Entry<String, AbstractMap.SimpleEntry<String, GraphqlParser.OperationDefinitionContext>>::getValue,
                                        Collectors.groupingBy(
                                                Map.Entry<String, GraphqlParser.OperationDefinitionContext>::getKey,
                                                Collectors.mapping(
                                                        Map.Entry<String, GraphqlParser.OperationDefinitionContext>::getValue,
                                                        Collectors.toList()
                                                )
                                        )
                                )
                        )
                )
                .forEach((packageName, interfaceMap) ->
                        interfaceMap.forEach((interfaceName, operationList) -> {
                                    try {
                                        this.buildImplementClass(packageName, interfaceName, operationList, "").writeTo(filer);
                                        Logger.info("{} build success", packageName.concat(".").concat(interfaceName) + "Impl");
                                    } catch (IOException e) {
                                        Logger.error(e);
                                    }
                                }
                        )
                );
    }

    public JavaFile buildImplementClass(String packageName, String interfaceName, List<GraphqlParser.OperationDefinitionContext> operationDefinitionContextList, String suffix) {
        TypeSpec.Builder builder = TypeSpec.classBuilder(ClassName.get(packageName, interfaceName.concat("Impl")))
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(ApplicationScoped.class)
                .addSuperinterface(ClassName.get(packageName, interfaceName))
                .addFields(buildFileContentFields(operationDefinitionContextList))
                .addStaticBlock(buildContentFieldInitializeCodeBlock(packageName, interfaceName, operationDefinitionContextList, suffix))
                .addMethods(
                        operationDefinitionContextList.stream()
                                .sorted(Comparator.comparingInt(this::getIndex))
                                .map(this::executableElementToMethodSpec)
                                .collect(Collectors.toList())
                );

        return JavaFile.builder(packageName, builder.build()).build();
    }

    private List<FieldSpec> buildFileContentFields(List<GraphqlParser.OperationDefinitionContext> operationDefinitionContextList) {
        return operationDefinitionContextList.stream()
                .sorted(Comparator.comparingInt(this::getIndex))
                .map(this::buildFileContentField)
                .collect(Collectors.toList());
    }

    private int getIndex(GraphqlParser.OperationDefinitionContext operationDefinitionContext) {
        String[] nameArray = operationDefinitionContext.name().getText().split("_");
        return Integer.parseInt(nameArray[nameArray.length - 1]);
    }

    private String getMethodName(GraphqlParser.OperationDefinitionContext operationDefinitionContext) {
        String[] nameArray = operationDefinitionContext.name().getText().split("_");
        return String.valueOf(nameArray[nameArray.length - 2]);
    }

    private FieldSpec buildFileContentField(GraphqlParser.OperationDefinitionContext operationDefinitionContext) {
        String methodName = getMethodName(operationDefinitionContext);
        int index = getIndex(operationDefinitionContext);
        return FieldSpec.builder(
                TypeName.get(String.class),
                methodName.concat("_").concat(String.valueOf(index)),
                Modifier.PRIVATE,
                Modifier.STATIC,
                Modifier.FINAL
        ).build();
    }

    private CodeBlock buildContentFieldInitializeCodeBlock(String packageName, String interfaceName, List<GraphqlParser.OperationDefinitionContext> operationDefinitionContextList, String suffix) {
        ClassName typeClassName = ClassName.get(packageName, interfaceName.concat("Impl"));
        CodeBlock.Builder builder = CodeBlock.builder();
        operationDefinitionContextList.stream()
                .sorted(Comparator.comparingInt(this::getIndex))
                .forEach(operationDefinitionContext ->
                        builder.addStatement(
                                "$L = fileToString($T.class, $S)",
                                getMethodName(operationDefinitionContext)
                                        .concat("_")
                                        .concat(String.valueOf(getIndex(operationDefinitionContext))),
                                typeClassName,
                                interfaceName
                                        .concat("_")
                                        .concat(getMethodName(operationDefinitionContext))
                                        .concat("_")
                                        .concat(String.valueOf(getIndex(operationDefinitionContext)))
                                        .concat(".")
                                        .concat(suffix)
                        )
                );
        return builder.build();
    }

    private MethodSpec executableElementToMethodSpec(GraphqlParser.OperationDefinitionContext operationDefinitionContext) {
        TypeName typeName = TYPE_NAME_UTIL.bestGuess(typeManager.getReturnClassName(operationDefinitionContext));
        List<AbstractMap.SimpleEntry<String, String>> parameters = typeManager.getParameters(operationDefinitionContext);

        MethodSpec.Builder builder = MethodSpec.methodBuilder(typeManager.getMethodName(operationDefinitionContext))
                .addModifiers(Modifier.PUBLIC)
                .addParameters(
                        parameters.stream()
                                .map(entry -> ParameterSpec.builder(TYPE_NAME_UTIL.bestGuess(entry.getValue()), entry.getKey()).build())
                                .collect(Collectors.toList())
                )
                .returns(typeName)
                .addException(ClassName.get(Exception.class));

        if (parameters.size() == 0) {
            builder.addStatement(getCodeBlock(operationDefinitionContext));
        } else {
            CodeBlock mapOf = CodeBlock.join(
                    parameters.stream()
                            .map(entry ->
                                    CodeBlock.of(
                                            "$S, (Object)$L",
                                            entry.getKey(),
                                            entry.getKey()
                                    )
                            )
                            .collect(Collectors.toList()),
                    ", ");
            builder.addStatement(getCodeBlock(operationDefinitionContext, mapOf));
        }
        return builder.build();
    }

    private CodeBlock getCodeBlock(GraphqlParser.OperationDefinitionContext operationDefinitionContext, CodeBlock mapOf) {
        String fieldName = operationDefinitionContext.selectionSet().selection(0).field().name().getText();
        String methodName = typeManager.getMethodName(operationDefinitionContext);
        int index = getIndex(operationDefinitionContext);
        String returnTypeName = typeManager.getReturnClassName(operationDefinitionContext);
        String className = TYPE_NAME_UTIL.getClassName(returnTypeName);
        String[] argumentTypeNames = TYPE_NAME_UTIL.getArgumentTypeNames(returnTypeName);
        String queryTypeName = manager.getQueryOperationTypeName().orElseThrow(() -> new GraphQLErrors(QUERY_TYPE_NOT_EXIST));
        String mutationTypeName = manager.getMutationOperationTypeName().orElseThrow(() -> new GraphQLErrors(MUTATION_TYPE_NOT_EXIST));

        if (argumentTypeNames.length > 0) {
            if (className.equals(Mono.class.getCanonicalName())) {
                returnTypeName = TYPE_NAME_UTIL.getArgumentTypeName0(className);
                className = TYPE_NAME_UTIL.getClassName(returnTypeName);
                argumentTypeNames = TYPE_NAME_UTIL.getArgumentTypeNames(returnTypeName);
                if (operationDefinitionContext.operationType() == null || operationDefinitionContext.operationType().QUERY() != null) {
                    if (argumentTypeNames.length > 0) {
                        Optional<ClassName> collectionImplementationClassName = getCollectionImplementationClassName(className);
                        if (collectionImplementationClassName.isPresent()) {
                            return CodeBlock.of(
                                    "return findAsync($L, $T.of($L).toJavaMap(), $T.class).map($T::$L).map($T::new)",
                                    methodName.concat("_").concat(String.valueOf(index)),
                                    ClassName.get(HashMap.class),
                                    mapOf,
                                    ClassName.get(graphQLConfig.getObjectTypePackageName(), queryTypeName),
                                    ClassName.get(graphQLConfig.getObjectTypePackageName(), queryTypeName),
                                    typeManager.getFieldGetterMethodName(fieldName),
                                    collectionImplementationClassName.get()
                            );
                        }
                    }
                    return CodeBlock.of(
                            "return findAsync($L, $T.of($L).toJavaMap(), $T.class).map($T::$L)",
                            methodName.concat("_").concat(String.valueOf(index)),
                            ClassName.get(HashMap.class),
                            mapOf,
                            ClassName.get(graphQLConfig.getObjectTypePackageName(), queryTypeName),
                            ClassName.get(graphQLConfig.getObjectTypePackageName(), queryTypeName),
                            typeManager.getFieldGetterMethodName(fieldName)
                    );
                } else if (operationDefinitionContext.operationType().MUTATION() != null) {
                    if (argumentTypeNames.length > 0) {
                        Optional<ClassName> collectionImplementationClassName = getCollectionImplementationClassName(className);
                        if (collectionImplementationClassName.isPresent()) {
                            return CodeBlock.of(
                                    "return saveAsync($L, $T.of($L).toJavaMap(), $T.class).map($T::$L).map($T::new)",
                                    methodName.concat("_").concat(String.valueOf(index)),
                                    ClassName.get(HashMap.class),
                                    mapOf,
                                    ClassName.get(graphQLConfig.getObjectTypePackageName(), mutationTypeName),
                                    ClassName.get(graphQLConfig.getObjectTypePackageName(), mutationTypeName),
                                    typeManager.getFieldGetterMethodName(fieldName),
                                    collectionImplementationClassName.get()
                            );
                        }
                    }
                    return CodeBlock.of(
                            "return saveAsync($L, $T.of($L).toJavaMap(), $T.class).map($T::$L)",
                            methodName.concat("_").concat(String.valueOf(index)),
                            ClassName.get(HashMap.class),
                            mapOf,
                            ClassName.get(graphQLConfig.getObjectTypePackageName(), mutationTypeName),
                            ClassName.get(graphQLConfig.getObjectTypePackageName(), mutationTypeName),
                            typeManager.getFieldGetterMethodName(fieldName)
                    );
                }
            } else if (className.equals(PublisherBuilder.class.getCanonicalName())) {
                returnTypeName = TYPE_NAME_UTIL.getArgumentTypeName0(className);
                className = TYPE_NAME_UTIL.getClassName(returnTypeName);
                argumentTypeNames = TYPE_NAME_UTIL.getArgumentTypeNames(returnTypeName);
                if (operationDefinitionContext.operationType() == null || operationDefinitionContext.operationType().QUERY() != null) {
                    if (argumentTypeNames.length > 0) {
                        Optional<ClassName> collectionImplementationClassName = getCollectionImplementationClassName(className);
                        if (collectionImplementationClassName.isPresent()) {
                            return CodeBlock.of(
                                    "return findAsyncBuilder($L, $T.of($L).toJavaMap(), $T.class).map($T::$L).map($T::new)",
                                    methodName.concat("_").concat(String.valueOf(index)),
                                    ClassName.get(HashMap.class),
                                    mapOf,
                                    ClassName.get(graphQLConfig.getObjectTypePackageName(), queryTypeName),
                                    ClassName.get(graphQLConfig.getObjectTypePackageName(), queryTypeName),
                                    typeManager.getFieldGetterMethodName(fieldName),
                                    collectionImplementationClassName.get()
                            );
                        }
                    }
                    return CodeBlock.of(
                            "return findAsyncBuilder($L, $T.of($L).toJavaMap(), $T.class).map($T::$L)",
                            methodName.concat("_").concat(String.valueOf(index)),
                            ClassName.get(HashMap.class),
                            mapOf,
                            ClassName.get(graphQLConfig.getObjectTypePackageName(), queryTypeName),
                            ClassName.get(graphQLConfig.getObjectTypePackageName(), queryTypeName),
                            typeManager.getFieldGetterMethodName(fieldName)
                    );
                } else if (operationDefinitionContext.operationType().MUTATION() != null) {
                    if (argumentTypeNames.length > 0) {
                        Optional<ClassName> collectionImplementationClassName = getCollectionImplementationClassName(className);
                        if (collectionImplementationClassName.isPresent()) {
                            return CodeBlock.of(
                                    "return saveAsyncBuilder($L, $T.of($L).toJavaMap(), $T.class).map($T::$L).map($T::new)",
                                    methodName.concat("_").concat(String.valueOf(index)),
                                    ClassName.get(HashMap.class),
                                    mapOf,
                                    ClassName.get(graphQLConfig.getObjectTypePackageName(), mutationTypeName),
                                    ClassName.get(graphQLConfig.getObjectTypePackageName(), mutationTypeName),
                                    typeManager.getFieldGetterMethodName(fieldName),
                                    collectionImplementationClassName.get()
                            );
                        }
                    }
                    return CodeBlock.of(
                            "return saveAsyncBuilder($L, $T.of($L).toJavaMap(), $T.class).map($T::$L)",
                            methodName.concat("_").concat(String.valueOf(index)),
                            ClassName.get(HashMap.class),
                            mapOf,
                            ClassName.get(graphQLConfig.getObjectTypePackageName(), mutationTypeName),
                            ClassName.get(graphQLConfig.getObjectTypePackageName(), mutationTypeName),
                            typeManager.getFieldGetterMethodName(fieldName)
                    );
                }
            } else {
                returnTypeName = TYPE_NAME_UTIL.getArgumentTypeName0(className);
                className = TYPE_NAME_UTIL.getClassName(returnTypeName);
                if (operationDefinitionContext.operationType() == null || operationDefinitionContext.operationType().QUERY() != null) {
                    Optional<ClassName> collectionImplementationClassName = getCollectionImplementationClassName(className);
                    return collectionImplementationClassName
                            .map(collectionClassName ->
                                    CodeBlock.of(
                                            "return new $T(find($L, $T.of($L).toJavaMap(), $T.class).$L())",
                                            collectionClassName,
                                            methodName.concat("_").concat(String.valueOf(index)),
                                            ClassName.get(HashMap.class),
                                            mapOf,
                                            ClassName.get(graphQLConfig.getObjectTypePackageName(), queryTypeName),
                                            typeManager.getFieldGetterMethodName(fieldName)
                                    )
                            ).orElseGet(() ->
                                    CodeBlock.of(
                                            "return find($L, $T.of($L).toJavaMap(), $T.class).$L()",
                                            methodName.concat("_").concat(String.valueOf(index)),
                                            ClassName.get(HashMap.class),
                                            mapOf,
                                            ClassName.get(graphQLConfig.getObjectTypePackageName(), queryTypeName),
                                            typeManager.getFieldGetterMethodName(fieldName)
                                    )
                            );
                } else if (operationDefinitionContext.operationType().MUTATION() != null) {
                    Optional<ClassName> collectionImplementationClassName = getCollectionImplementationClassName(className);
                    return collectionImplementationClassName
                            .map(collectionClassName ->
                                    CodeBlock.of(
                                            "return new $T(save($L, $T.of($L).toJavaMap(), $T.class).$L())",
                                            collectionClassName,
                                            methodName.concat("_").concat(String.valueOf(index)),
                                            ClassName.get(HashMap.class),
                                            mapOf,
                                            ClassName.get(graphQLConfig.getObjectTypePackageName(), mutationTypeName),
                                            typeManager.getFieldGetterMethodName(fieldName)
                                    )
                            ).orElseGet(() ->
                                    CodeBlock.of(
                                            "return save($L, $T.of($L).toJavaMap(), $T.class).$L()",
                                            methodName.concat("_").concat(String.valueOf(index)),
                                            ClassName.get(HashMap.class),
                                            mapOf,
                                            ClassName.get(graphQLConfig.getObjectTypePackageName(), mutationTypeName),
                                            typeManager.getFieldGetterMethodName(fieldName)
                                    )
                            );
                }
            }
        } else {
            if (operationDefinitionContext.operationType() == null || operationDefinitionContext.operationType().QUERY() != null) {
                return CodeBlock.of(
                        "return find($L, $T.of($L).toJavaMap(), $T.class).$L()",
                        methodName.concat("_").concat(String.valueOf(index)),
                        ClassName.get(HashMap.class),
                        mapOf,
                        ClassName.get(graphQLConfig.getObjectTypePackageName(), queryTypeName),
                        typeManager.getFieldGetterMethodName(fieldName)
                );
            } else if (operationDefinitionContext.operationType().MUTATION() != null) {
                return CodeBlock.of(
                        "return save($L, $T.of($L).toJavaMap(), $T.class).$L()",
                        methodName.concat("_").concat(String.valueOf(index)),
                        ClassName.get(HashMap.class),
                        mapOf,
                        ClassName.get(graphQLConfig.getObjectTypePackageName(), mutationTypeName),
                        typeManager.getFieldGetterMethodName(fieldName)
                );
            }
        }
        throw new ElementProcessException(UNSUPPORTED_OPERATION_METHOD_RETURN_TYPE.bind(returnTypeName));
    }

    private CodeBlock getCodeBlock(GraphqlParser.OperationDefinitionContext operationDefinitionContext) {
        String methodName = getMethodName(operationDefinitionContext);
        int index = getIndex(operationDefinitionContext);
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

    private Optional<ClassName> getCollectionImplementationClassName(String className) {
        if (className.equals(Collection.class.getCanonicalName())) {
            return Optional.empty();
        } else {
            if (className.equals(List.class.getCanonicalName())) {
                return Optional.of(ClassName.get(ArrayList.class));
            } else if (className.equals(Set.class.getCanonicalName())) {
                return Optional.of(ClassName.get(LinkedHashSet.class));
            } else {
                return Optional.of(TYPE_NAME_UTIL.bestGuess(className));
            }
        }
    }
}
