package io.graphoenix.java.generator.implementer;

import com.squareup.javapoet.AnnotationSpec;
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
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.core.error.ElementProcessException;
import io.graphoenix.core.error.GraphQLErrorType;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.core.handler.PackageManager;
import io.graphoenix.core.utils.FileUtil;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.dao.OperationDAO;
import io.graphoenix.spi.handler.GeneratorHandler;
import io.vavr.collection.HashMap;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.tinylog.Logger;
import reactor.core.publisher.Mono;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.graphoenix.core.error.ElementProcessErrorType.UNSUPPORTED_OPERATION_METHOD_RETURN_TYPE;
import static io.graphoenix.core.error.GraphQLErrorType.MUTATION_TYPE_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.QUERY_TYPE_NOT_EXIST;
import static io.graphoenix.core.utils.TypeNameUtil.TYPE_NAME_UTIL;

@ApplicationScoped
public class OperationInterfaceImplementer {

    private final IGraphQLDocumentManager manager;
    private final PackageManager packageManager;
    private final TypeManager typeManager;
    private final GraphQLConfig graphQLConfig;
    private final Map<String, GeneratorHandler> generatorHandlerMap;

    @Inject
    public OperationInterfaceImplementer(IGraphQLDocumentManager manager, PackageManager packageManager, TypeManager typeManager, GraphQLConfig graphQLConfig) {
        this.manager = manager;
        this.packageManager = packageManager;
        this.typeManager = typeManager;
        this.graphQLConfig = graphQLConfig;
        this.generatorHandlerMap = BeanContext.getMap(GeneratorHandler.class);
    }

    public void writeToFiler(Filer filer) throws IOException {
        manager.getOperationDefinitions()
                .filter(packageManager::isLocalPackage)
                .map(operationDefinitionContext -> {
                            String interfaceName = typeManager.getClassName(operationDefinitionContext);
                            return new AbstractMap.SimpleEntry<>(
                                    interfaceName,
                                    operationDefinitionContext
                            );
                        }
                )
                .collect(
                        Collectors.groupingBy(
                                Map.Entry<String, GraphqlParser.OperationDefinitionContext>::getKey,
                                Collectors.mapping(
                                        Map.Entry<String, GraphqlParser.OperationDefinitionContext>::getValue,
                                        Collectors.toList()
                                )
                        )
                )
                .forEach((interfaceName, operationList) -> {
                            int i = interfaceName.lastIndexOf(".");
                            String packageName = interfaceName.substring(0, i);
                            String name = interfaceName.substring(i + 1);
                            Stream.ofNullable(generatorHandlerMap)
                                    .flatMap(map -> map.entrySet().stream())
                                    .forEach(entry -> {
                                                try {
                                                    String generatorName = entry.getKey();
                                                    GeneratorHandler generatorHandler = entry.getValue();
                                                    this.buildImplementClass(generatorName, generatorHandler.operationDAOName(), packageName, name, operationList, generatorHandler.extension()).writeTo(filer);
                                                    Logger.info("{} build success", interfaceName.concat("Impl"));
                                                    for (GraphqlParser.OperationDefinitionContext operationDefinitionContext : operationList) {
                                                        String methodName = typeManager.getMethodName(operationDefinitionContext);
                                                        int index = getIndex(operationDefinitionContext);
                                                        String resourceName = name.concat("_").concat(methodName).concat("_").concat(String.valueOf(index)).concat(".").concat(generatorHandler.extension());
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
                                                                graphQLConfig.getOperationPackageName().concat(".").concat(generatorName),
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
                                    );
                        }
                );
    }

    public JavaFile buildImplementClass(String generatorName, String operationDAOName, String packageName, String interfaceName, List<GraphqlParser.OperationDefinitionContext> operationDefinitionContextList, String suffix) {
        TypeSpec.Builder builder = TypeSpec.classBuilder(interfaceName.concat("Impl"))
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(ApplicationScoped.class)
                .addSuperinterface(ClassName.get(packageName, interfaceName))
                .addField(
                        FieldSpec.builder(
                                ClassName.get(OperationDAO.class),
                                "operationDAO",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).build()
                )
                .addFields(buildFileContentFields(operationDefinitionContextList))
                .addStaticBlock(buildContentFieldInitializeCodeBlock(generatorName, interfaceName, operationDefinitionContextList, suffix))
                .addMethod(buildConstructor(operationDAOName))
                .addMethods(
                        operationDefinitionContextList.stream()
                                .sorted(Comparator.comparingInt(this::getIndex))
                                .map(this::executableElementToMethodSpec)
                                .collect(Collectors.toList())
                );
        if (graphQLConfig.getDefaultOperationHandlerName() != null && graphQLConfig.getDefaultOperationHandlerName().equals(generatorName)) {
            builder.addAnnotation(Default.class);
        }
        return JavaFile.builder(graphQLConfig.getOperationPackageName().concat(".").concat(generatorName), builder.build()).build();
    }

    private MethodSpec buildConstructor(String operationDAOName) {
        return MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Inject.class)
                .addParameter(
                        ParameterSpec.builder(ClassName.get(OperationDAO.class), "operationDAO")
                                .addAnnotation(AnnotationSpec.builder(Named.class).addMember("value", "$S", operationDAOName).build())
                                .build())
                .addStatement("this.operationDAO = operationDAO")
                .build();
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

    private CodeBlock buildContentFieldInitializeCodeBlock(String generatorName, String interfaceName, List<GraphqlParser.OperationDefinitionContext> operationDefinitionContextList, String suffix) {
        ClassName typeClassName = ClassName.get(graphQLConfig.getOperationPackageName().concat(".").concat(generatorName), interfaceName.concat("Impl"));
        CodeBlock.Builder builder = CodeBlock.builder();
        operationDefinitionContextList.stream()
                .sorted(Comparator.comparingInt(this::getIndex))
                .forEach(operationDefinitionContext ->
                        builder.addStatement(
                                "$L = $T.FILE_UTIL.fileToString($T.class, $S)",
                                typeManager.getMethodName(operationDefinitionContext)
                                        .concat("_")
                                        .concat(String.valueOf(getIndex(operationDefinitionContext))),
                                ClassName.get(FileUtil.class),
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
        TypeName typeName = TYPE_NAME_UTIL.toTypeName(typeManager.getReturnClassName(operationDefinitionContext));
        List<AbstractMap.SimpleEntry<String, String>> parameters = typeManager.getParameters(operationDefinitionContext);

        MethodSpec.Builder builder = MethodSpec.methodBuilder(typeManager.getMethodName(operationDefinitionContext))
                .addModifiers(Modifier.PUBLIC)
                .addParameters(
                        parameters.stream()
                                .map(entry -> ParameterSpec.builder(TYPE_NAME_UTIL.toTypeName(entry.getValue()), entry.getKey()).build())
                                .collect(Collectors.toList())
                )
                .returns(typeName);

        if (parameters.size() == 0) {
            builder.addStatement(getCodeBlock(operationDefinitionContext, CodeBlock.of("new $T<>()", ClassName.get(java.util.HashMap.class))));
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
        String resourceFieldName = methodName.concat("_").concat(String.valueOf(index));
        String typeName = typeManager.getReturnClassName(operationDefinitionContext);
        String className = TYPE_NAME_UTIL.getClassName(typeName);
        String[] argumentTypeNames = TYPE_NAME_UTIL.getArgumentTypeNames(typeName);
        String queryTypeName = manager.getQueryOperationTypeName().orElseThrow(() -> new GraphQLErrors(QUERY_TYPE_NOT_EXIST));
        String mutationTypeName = manager.getMutationOperationTypeName().orElseThrow(() -> new GraphQLErrors(MUTATION_TYPE_NOT_EXIST));

        if (argumentTypeNames.length > 0) {
            if (className.equals(Mono.class.getCanonicalName())) {
                String monoArgumentTypeName = TYPE_NAME_UTIL.getArgumentTypeName0(typeName);
                String[] monoArgumentTypeArgumentNames = TYPE_NAME_UTIL.getArgumentTypeNames(typeName);
                if (operationDefinitionContext.operationType() == null || operationDefinitionContext.operationType().QUERY() != null) {
                    if (monoArgumentTypeArgumentNames.length > 0) {
                        Optional<ClassName> collectionImplementationClassName = getCollectionImplementationClassName(monoArgumentTypeName);
                        if (collectionImplementationClassName.isPresent()) {
                            return CodeBlock.of(
                                    "return operationDAO.findAsync($L, $L, $T.class).mapNotNull($T::$L).mapNotNull($T::new)",
                                    resourceFieldName,
                                    mapOf,
                                    ClassName.get(graphQLConfig.getObjectTypePackageName(), queryTypeName),
                                    ClassName.get(graphQLConfig.getObjectTypePackageName(), queryTypeName),
                                    typeManager.getFieldGetterMethodName(fieldName),
                                    collectionImplementationClassName.get()
                            );
                        }
                    }
                    return CodeBlock.of(
                            "return operationDAO.findAsync($L, $L, $T.class).mapNotNull($T::$L)",
                            resourceFieldName,
                            mapOf,
                            ClassName.get(graphQLConfig.getObjectTypePackageName(), queryTypeName),
                            ClassName.get(graphQLConfig.getObjectTypePackageName(), queryTypeName),
                            typeManager.getFieldGetterMethodName(fieldName)
                    );
                } else if (operationDefinitionContext.operationType().MUTATION() != null) {
                    if (monoArgumentTypeArgumentNames.length > 0) {
                        Optional<ClassName> collectionImplementationClassName = getCollectionImplementationClassName(monoArgumentTypeName);
                        if (collectionImplementationClassName.isPresent()) {
                            return CodeBlock.of(
                                    "return operationDAO.saveAsync($L, $L, $T.class).mapNotNull($T::$L).mapNotNull($T::new)",
                                    resourceFieldName,
                                    mapOf,
                                    ClassName.get(graphQLConfig.getObjectTypePackageName(), mutationTypeName),
                                    ClassName.get(graphQLConfig.getObjectTypePackageName(), mutationTypeName),
                                    typeManager.getFieldGetterMethodName(fieldName),
                                    collectionImplementationClassName.get()
                            );
                        }
                    }
                    return CodeBlock.of(
                            "return operationDAO.saveAsync($L, $L, $T.class).mapNotNull($T::$L)",
                            resourceFieldName,
                            mapOf,
                            ClassName.get(graphQLConfig.getObjectTypePackageName(), mutationTypeName),
                            ClassName.get(graphQLConfig.getObjectTypePackageName(), mutationTypeName),
                            typeManager.getFieldGetterMethodName(fieldName)
                    );
                }
            } else if (className.equals(PublisherBuilder.class.getCanonicalName())) {
                String publisherArgumentTypeName = TYPE_NAME_UTIL.getArgumentTypeName0(typeName);
                String[] publisherArgumentTypeArgumentNames = TYPE_NAME_UTIL.getArgumentTypeNames(typeName);
                if (operationDefinitionContext.operationType() == null || operationDefinitionContext.operationType().QUERY() != null) {
                    if (publisherArgumentTypeArgumentNames.length > 0) {
                        Optional<ClassName> collectionImplementationClassName = getCollectionImplementationClassName(publisherArgumentTypeName);
                        if (collectionImplementationClassName.isPresent()) {
                            return CodeBlock.of(
                                    "return operationDAO.findAsyncBuilder($L, $L, $T.class).mapNotNull($T::$L).mapNotNull($T::new)",
                                    resourceFieldName,
                                    mapOf,
                                    ClassName.get(graphQLConfig.getObjectTypePackageName(), queryTypeName),
                                    ClassName.get(graphQLConfig.getObjectTypePackageName(), queryTypeName),
                                    typeManager.getFieldGetterMethodName(fieldName),
                                    collectionImplementationClassName.get()
                            );
                        }
                    }
                    return CodeBlock.of(
                            "return operationDAO.findAsyncBuilder($L, $L, $T.class).mapNotNull($T::$L)",
                            resourceFieldName,
                            mapOf,
                            ClassName.get(graphQLConfig.getObjectTypePackageName(), queryTypeName),
                            ClassName.get(graphQLConfig.getObjectTypePackageName(), queryTypeName),
                            typeManager.getFieldGetterMethodName(fieldName)
                    );
                } else if (operationDefinitionContext.operationType().MUTATION() != null) {
                    if (publisherArgumentTypeArgumentNames.length > 0) {
                        Optional<ClassName> collectionImplementationClassName = getCollectionImplementationClassName(publisherArgumentTypeName);
                        if (collectionImplementationClassName.isPresent()) {
                            return CodeBlock.of(
                                    "return operationDAO.saveAsyncBuilder($L, $L, $T.class).mapNotNull($T::$L).mapNotNull($T::new)",
                                    resourceFieldName,
                                    mapOf,
                                    ClassName.get(graphQLConfig.getObjectTypePackageName(), mutationTypeName),
                                    ClassName.get(graphQLConfig.getObjectTypePackageName(), mutationTypeName),
                                    typeManager.getFieldGetterMethodName(fieldName),
                                    collectionImplementationClassName.get()
                            );
                        }
                    }
                    return CodeBlock.of(
                            "return operationDAO.saveAsyncBuilder($L, $L, $T.class).mapNotNull($T::$L)",
                            resourceFieldName,
                            mapOf,
                            ClassName.get(graphQLConfig.getObjectTypePackageName(), mutationTypeName),
                            ClassName.get(graphQLConfig.getObjectTypePackageName(), mutationTypeName),
                            typeManager.getFieldGetterMethodName(fieldName)
                    );
                }
            } else {
                if (operationDefinitionContext.operationType() == null || operationDefinitionContext.operationType().QUERY() != null) {
                    Optional<ClassName> collectionImplementationClassName = getCollectionImplementationClassName(typeName);
                    return collectionImplementationClassName
                            .map(collectionClassName ->
                                    CodeBlock.of(
                                            "return new $T(operationDAO.find($L, $L, $T.class).$L())",
                                            collectionClassName,
                                            resourceFieldName,
                                            mapOf,
                                            ClassName.get(graphQLConfig.getObjectTypePackageName(), queryTypeName),
                                            typeManager.getFieldGetterMethodName(fieldName)
                                    )
                            ).orElseGet(() ->
                                    CodeBlock.of(
                                            "return operationDAO.find($L, $L, $T.class).$L()",
                                            resourceFieldName,
                                            mapOf,
                                            ClassName.get(graphQLConfig.getObjectTypePackageName(), queryTypeName),
                                            typeManager.getFieldGetterMethodName(fieldName)
                                    )
                            );
                } else if (operationDefinitionContext.operationType().MUTATION() != null) {
                    Optional<ClassName> collectionImplementationClassName = getCollectionImplementationClassName(typeName);
                    return collectionImplementationClassName
                            .map(collectionTypeName ->
                                    CodeBlock.of(
                                            "return new $T(operationDAO.save($L, $L, $T.class).$L())",
                                            collectionTypeName,
                                            resourceFieldName,
                                            mapOf,
                                            ClassName.get(graphQLConfig.getObjectTypePackageName(), mutationTypeName),
                                            typeManager.getFieldGetterMethodName(fieldName)
                                    )
                            ).orElseGet(() ->
                                    CodeBlock.of(
                                            "return operationDAO.save($L, $L, $T.class).$L()",
                                            resourceFieldName,
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
                        "return operationDAO.find($L, $L, $T.class).$L()",
                        resourceFieldName,
                        mapOf,
                        ClassName.get(graphQLConfig.getObjectTypePackageName(), queryTypeName),
                        typeManager.getFieldGetterMethodName(fieldName)
                );
            } else if (operationDefinitionContext.operationType().MUTATION() != null) {
                return CodeBlock.of(
                        "return operationDAO.save($L, $L, $T.class).$L()",
                        resourceFieldName,
                        mapOf,
                        ClassName.get(graphQLConfig.getObjectTypePackageName(), mutationTypeName),
                        typeManager.getFieldGetterMethodName(fieldName)
                );
            }
        }
        throw new ElementProcessException(UNSUPPORTED_OPERATION_METHOD_RETURN_TYPE.bind(typeName));
    }

    private Optional<ClassName> getCollectionImplementationClassName(String typeName) {
        String className = TYPE_NAME_UTIL.getClassName(typeName);
        if (className.equals(List.class.getCanonicalName())) {
            return Optional.of(ClassName.get(ArrayList.class));
        } else if (className.equals(Set.class.getCanonicalName())) {
            return Optional.of(ClassName.get(LinkedHashSet.class));
        } else {
            return Optional.empty();
        }
    }

    private int getIndex(GraphqlParser.OperationDefinitionContext operationDefinitionContext) {
        String[] nameArray = operationDefinitionContext.name().getText().split("_");
        return Integer.parseInt(nameArray[nameArray.length - 1]);
    }
}
