package io.graphoenix.java.generator.implementer;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.error.ElementProcessException;
import io.graphoenix.core.error.GraphQLErrorType;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.handler.GeneratorHandler;
import io.vavr.collection.HashMap;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.tinylog.Logger;
import reactor.core.publisher.Mono;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
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
    private final GeneratorHandler generatorHandler;

    @Inject
    public OperationInterfaceImplementer(IGraphQLDocumentManager manager, TypeManager typeManager, GraphQLConfig graphQLConfig, GeneratorHandler generatorHandler) {
        this.manager = manager;
        this.typeManager = typeManager;
        this.graphQLConfig = graphQLConfig;
        this.generatorHandler = generatorHandler;
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
                                        this.buildImplementClass(packageName, interfaceName, operationList, generatorHandler.extension()).writeTo(filer);
                                        Logger.info("{} build success", packageName.concat(".").concat(interfaceName) + "Impl");

                                        for (GraphqlParser.OperationDefinitionContext operationDefinitionContext : operationList) {
                                            String methodName = typeManager.getMethodName(operationDefinitionContext);
                                            int index = getIndex(operationDefinitionContext);
                                            String resourceName = interfaceName.concat("_").concat(methodName).concat("_").concat(String.valueOf(index)).concat(".").concat(generatorHandler.extension());
                                            String content;
                                            if (operationDefinitionContext.operationType() == null || operationDefinitionContext.operationType().QUERY() != null) {
                                                content = generatorHandler.query(operationDefinitionContext);
                                            } else if (operationDefinitionContext.operationType().MUTATION() != null) {
                                                content = generatorHandler.mutation(operationDefinitionContext);
                                            } else {
                                                throw new GraphQLErrors(GraphQLErrorType.UNSUPPORTED_OPERATION_TYPE);
                                            }
                                            FileObject fileObject = filer.createResource(
                                                    StandardLocation.CLASS_OUTPUT,
                                                    packageName,
                                                    resourceName
                                            );
                                            Writer writer = fileObject.openWriter();
                                            writer.write(content);
                                            writer.close();
                                            Logger.info("{} build success", resourceName);
                                            Logger.debug("resource content:\r\n{}", content);
                                        }
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

    private FieldSpec buildFileContentField(GraphqlParser.OperationDefinitionContext operationDefinitionContext) {
        String methodName = typeManager.getMethodName(operationDefinitionContext);
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
                                typeManager.getMethodName(operationDefinitionContext)
                                        .concat("_")
                                        .concat(String.valueOf(getIndex(operationDefinitionContext))),
                                typeClassName,
                                interfaceName
                                        .concat("_")
                                        .concat(typeManager.getMethodName(operationDefinitionContext))
                                        .concat("_")
                                        .concat(String.valueOf(getIndex(operationDefinitionContext)))
                                        .concat(".")
                                        .concat(suffix)
                        )
                );
        return builder.build();
    }

    private MethodSpec executableElementToMethodSpec(GraphqlParser.OperationDefinitionContext operationDefinitionContext) {
        TypeName typeName = TYPE_NAME_UTIL.toClassName(typeManager.getReturnClassName(operationDefinitionContext));
        List<AbstractMap.SimpleEntry<String, String>> parameters = typeManager.getParameters(operationDefinitionContext);

        MethodSpec.Builder builder = MethodSpec.methodBuilder(typeManager.getMethodName(operationDefinitionContext))
                .addModifiers(Modifier.PUBLIC)
                .addParameters(
                        parameters.stream()
                                .map(entry -> ParameterSpec.builder(TYPE_NAME_UTIL.toTypeName(entry.getValue()), entry.getKey()).build())
                                .collect(Collectors.toList())
                )
                .returns(typeName)
                .addException(ClassName.get(Exception.class));

        if (parameters.size() == 0) {
            builder.addStatement(getCodeBlock(operationDefinitionContext, CodeBlock.of("$T.empty().toJavaMap()", ClassName.get(HashMap.class))));
        } else {
            CodeBlock mapOf = CodeBlock.of(
                    "$T.of($L).toJavaMap()",
                    ClassName.get(HashMap.class),
                    CodeBlock.join(
                            parameters.stream().map(entry -> CodeBlock.of("$S, (Object)$L", entry.getKey(), entry.getKey())).collect(Collectors.toList()),
                            ", "
                    )
            );
            builder.addStatement(getCodeBlock(operationDefinitionContext, mapOf));
        }
        return builder.build();
    }

    private CodeBlock getCodeBlock(GraphqlParser.OperationDefinitionContext operationDefinitionContext, CodeBlock mapOf) {
        String fieldName = operationDefinitionContext.selectionSet().selection(0).field().name().getText();
        String methodName = typeManager.getMethodName(operationDefinitionContext);
        int index = getIndex(operationDefinitionContext);
        String typeName = typeManager.getReturnClassName(operationDefinitionContext);
        String className = TYPE_NAME_UTIL.getClassName(typeName);
        String[] argumentTypeNames = TYPE_NAME_UTIL.getArgumentTypeNames(typeName);
        String queryTypeName = manager.getQueryOperationTypeName().orElseThrow(() -> new GraphQLErrors(QUERY_TYPE_NOT_EXIST));
        String mutationTypeName = manager.getMutationOperationTypeName().orElseThrow(() -> new GraphQLErrors(MUTATION_TYPE_NOT_EXIST));

        if (argumentTypeNames.length > 0) {
            if (className.equals(Mono.class.getCanonicalName())) {
                typeName = TYPE_NAME_UTIL.getArgumentTypeName0(typeName);
                className = TYPE_NAME_UTIL.getClassName(typeName);
                argumentTypeNames = TYPE_NAME_UTIL.getArgumentTypeNames(typeName);
                if (operationDefinitionContext.operationType() == null || operationDefinitionContext.operationType().QUERY() != null) {
                    if (argumentTypeNames.length > 0) {
                        Optional<ClassName> collectionImplementationClassName = getCollectionImplementationClassName(className);
                        if (collectionImplementationClassName.isPresent()) {
                            return CodeBlock.of(
                                    "return findAsync($L, $L, $T.class).map($T::$L).map($T::new)",
                                    methodName.concat("_").concat(String.valueOf(index)),
                                    mapOf,
                                    ClassName.get(graphQLConfig.getObjectTypePackageName(), queryTypeName),
                                    ClassName.get(graphQLConfig.getObjectTypePackageName(), queryTypeName),
                                    typeManager.getFieldGetterMethodName(fieldName),
                                    collectionImplementationClassName.get()
                            );
                        }
                    }
                    return CodeBlock.of(
                            "return findAsync($L, $L, $T.class).map($T::$L)",
                            methodName.concat("_").concat(String.valueOf(index)),
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
                                    "return saveAsync($L, $L, $T.class).map($T::$L).map($T::new)",
                                    methodName.concat("_").concat(String.valueOf(index)),
                                    mapOf,
                                    ClassName.get(graphQLConfig.getObjectTypePackageName(), mutationTypeName),
                                    ClassName.get(graphQLConfig.getObjectTypePackageName(), mutationTypeName),
                                    typeManager.getFieldGetterMethodName(fieldName),
                                    collectionImplementationClassName.get()
                            );
                        }
                    }
                    return CodeBlock.of(
                            "return saveAsync($L, $L, $T.class).map($T::$L)",
                            methodName.concat("_").concat(String.valueOf(index)),
                            mapOf,
                            ClassName.get(graphQLConfig.getObjectTypePackageName(), mutationTypeName),
                            ClassName.get(graphQLConfig.getObjectTypePackageName(), mutationTypeName),
                            typeManager.getFieldGetterMethodName(fieldName)
                    );
                }
            } else if (className.equals(PublisherBuilder.class.getCanonicalName())) {
                typeName = TYPE_NAME_UTIL.getArgumentTypeName0(typeName);
                className = TYPE_NAME_UTIL.getClassName(typeName);
                argumentTypeNames = TYPE_NAME_UTIL.getArgumentTypeNames(typeName);
                if (operationDefinitionContext.operationType() == null || operationDefinitionContext.operationType().QUERY() != null) {
                    if (argumentTypeNames.length > 0) {
                        Optional<ClassName> collectionImplementationClassName = getCollectionImplementationClassName(className);
                        if (collectionImplementationClassName.isPresent()) {
                            return CodeBlock.of(
                                    "return findAsyncBuilder($L, $L, $T.class).map($T::$L).map($T::new)",
                                    methodName.concat("_").concat(String.valueOf(index)),
                                    mapOf,
                                    ClassName.get(graphQLConfig.getObjectTypePackageName(), queryTypeName),
                                    ClassName.get(graphQLConfig.getObjectTypePackageName(), queryTypeName),
                                    typeManager.getFieldGetterMethodName(fieldName),
                                    collectionImplementationClassName.get()
                            );
                        }
                    }
                    return CodeBlock.of(
                            "return findAsyncBuilder($L, $L, $T.class).map($T::$L)",
                            methodName.concat("_").concat(String.valueOf(index)),
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
                                    "return saveAsyncBuilder($L, $L, $T.class).map($T::$L).map($T::new)",
                                    methodName.concat("_").concat(String.valueOf(index)),
                                    mapOf,
                                    ClassName.get(graphQLConfig.getObjectTypePackageName(), mutationTypeName),
                                    ClassName.get(graphQLConfig.getObjectTypePackageName(), mutationTypeName),
                                    typeManager.getFieldGetterMethodName(fieldName),
                                    collectionImplementationClassName.get()
                            );
                        }
                    }
                    return CodeBlock.of(
                            "return saveAsyncBuilder($L, $L, $T.class).map($T::$L)",
                            methodName.concat("_").concat(String.valueOf(index)),
                            mapOf,
                            ClassName.get(graphQLConfig.getObjectTypePackageName(), mutationTypeName),
                            ClassName.get(graphQLConfig.getObjectTypePackageName(), mutationTypeName),
                            typeManager.getFieldGetterMethodName(fieldName)
                    );
                }
            } else {
                typeName = TYPE_NAME_UTIL.getArgumentTypeName0(typeName);
                className = TYPE_NAME_UTIL.getClassName(typeName);
                if (operationDefinitionContext.operationType() == null || operationDefinitionContext.operationType().QUERY() != null) {
                    Optional<ClassName> collectionImplementationClassName = getCollectionImplementationClassName(className);
                    return collectionImplementationClassName
                            .map(collectionClassName ->
                                    CodeBlock.of(
                                            "return new $T(find($L, $L, $T.class).$L())",
                                            collectionClassName,
                                            methodName.concat("_").concat(String.valueOf(index)),
                                            mapOf,
                                            ClassName.get(graphQLConfig.getObjectTypePackageName(), queryTypeName),
                                            typeManager.getFieldGetterMethodName(fieldName)
                                    )
                            ).orElseGet(() ->
                                    CodeBlock.of(
                                            "return find($L, $L, $T.class).$L()",
                                            methodName.concat("_").concat(String.valueOf(index)),
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
                                            "return new $T(save($L, $L, $T.class).$L())",
                                            collectionClassName,
                                            methodName.concat("_").concat(String.valueOf(index)),
                                            mapOf,
                                            ClassName.get(graphQLConfig.getObjectTypePackageName(), mutationTypeName),
                                            typeManager.getFieldGetterMethodName(fieldName)
                                    )
                            ).orElseGet(() ->
                                    CodeBlock.of(
                                            "return save($L, $L, $T.class).$L()",
                                            methodName.concat("_").concat(String.valueOf(index)),
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
                        "return find($L, $L, $T.class).$L()",
                        methodName.concat("_").concat(String.valueOf(index)),
                        mapOf,
                        ClassName.get(graphQLConfig.getObjectTypePackageName(), queryTypeName),
                        typeManager.getFieldGetterMethodName(fieldName)
                );
            } else if (operationDefinitionContext.operationType().MUTATION() != null) {
                return CodeBlock.of(
                        "return save($L, $L, $T.class).$L()",
                        methodName.concat("_").concat(String.valueOf(index)),
                        mapOf,
                        ClassName.get(graphQLConfig.getObjectTypePackageName(), mutationTypeName),
                        typeManager.getFieldGetterMethodName(fieldName)
                );
            }
        }
        throw new ElementProcessException(UNSUPPORTED_OPERATION_METHOD_RETURN_TYPE.bind(typeName));
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
                return Optional.of(TYPE_NAME_UTIL.toClassName(className));
            }
        }
    }

    private int getIndex(GraphqlParser.OperationDefinitionContext operationDefinitionContext) {
        String[] nameArray = operationDefinitionContext.name().getText().split("_");
        return Integer.parseInt(nameArray[nameArray.length - 1]);
    }
}
