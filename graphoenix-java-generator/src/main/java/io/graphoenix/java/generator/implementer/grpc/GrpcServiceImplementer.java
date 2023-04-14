package io.graphoenix.java.generator.implementer.grpc;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import com.google.common.collect.Streams;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.core.handler.ArgumentBuilder;
import io.graphoenix.core.handler.GraphQLRequestHandler;
import io.graphoenix.core.schema.JsonSchemaValidator;
import io.graphoenix.core.utils.CodecUtil;
import io.graphoenix.core.utils.DocumentUtil;
import io.graphoenix.core.utils.GraphQLResponseUtil;
import io.graphoenix.java.generator.implementer.TypeManager;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.constant.Hammurabi;
import io.graphoenix.spi.dto.type.OperationType;
import io.graphoenix.spi.handler.OperationHandler;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.json.bind.Jsonb;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.tinylog.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.graphoenix.core.error.GraphQLErrorType.MUTATION_TYPE_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.QUERY_TYPE_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.UNSUPPORTED_FIELD_TYPE;
import static io.graphoenix.core.error.GraphQLErrorType.UNSUPPORTED_OPERATION_TYPE;
import static io.graphoenix.core.utils.TypeNameUtil.TYPE_NAME_UTIL;
import static io.graphoenix.java.generator.utils.TypeUtil.TYPE_UTIL;
import static io.graphoenix.spi.dto.type.OperationType.MUTATION;
import static io.graphoenix.spi.dto.type.OperationType.QUERY;

@ApplicationScoped
public class GrpcServiceImplementer {

    private final IGraphQLDocumentManager manager;
    private final TypeManager typeManager;
    private final GrpcNameUtil grpcNameUtil;
    private final GraphQLConfig graphQLConfig;

    @Inject
    public GrpcServiceImplementer(IGraphQLDocumentManager manager, TypeManager typeManager, GrpcNameUtil grpcNameUtil, GraphQLConfig graphQLConfig) {
        this.manager = manager;
        this.typeManager = typeManager;
        this.grpcNameUtil = grpcNameUtil;
        this.graphQLConfig = graphQLConfig;
    }

    public void writeToFiler(Filer filer) throws IOException {

        manager.getQueryOperationTypeName()
                .flatMap(manager::getObject)
                .orElseThrow(() -> new GraphQLErrors(QUERY_TYPE_NOT_EXIST))
                .fieldsDefinition().fieldDefinition().stream()
                .map(fieldDefinitionContext -> new AbstractMap.SimpleEntry<>(manager.getPackageName(fieldDefinitionContext).orElseGet(graphQLConfig::getPackageName), fieldDefinitionContext))
                .collect(
                        Collectors.groupingBy(
                                Map.Entry<String, GraphqlParser.FieldDefinitionContext>::getKey,
                                Collectors.mapping(
                                        Map.Entry<String, GraphqlParser.FieldDefinitionContext>::getValue,
                                        Collectors.toList()
                                )
                        )
                )
                .forEach((packageName, fieldDefinitionContextList) -> {
                            try {
                                this.buildTypeServiceImplClass(packageName, QUERY, fieldDefinitionContextList).writeTo(filer);
                                Logger.info("{}.GrpcQueryTypeServiceImpl build success", packageName);
                            } catch (IOException e) {
                                Logger.error(e);
                            }
                        }
                );

        manager.getMutationOperationTypeName()
                .flatMap(manager::getObject)
                .orElseThrow(() -> new GraphQLErrors(MUTATION_TYPE_NOT_EXIST))
                .fieldsDefinition().fieldDefinition().stream()
                .map(fieldDefinitionContext -> new AbstractMap.SimpleEntry<>(manager.getPackageName(fieldDefinitionContext).orElseGet(graphQLConfig::getPackageName), fieldDefinitionContext))
                .collect(
                        Collectors.groupingBy(
                                Map.Entry<String, GraphqlParser.FieldDefinitionContext>::getKey,
                                Collectors.mapping(
                                        Map.Entry<String, GraphqlParser.FieldDefinitionContext>::getValue,
                                        Collectors.toList()
                                )
                        )
                )
                .forEach((packageName, fieldDefinitionContextList) -> {
                            try {
                                this.buildTypeServiceImplClass(packageName, MUTATION, fieldDefinitionContextList).writeTo(filer);
                                Logger.info("{}.GrpcMutationTypeServiceImpl build success", packageName);
                            } catch (IOException e) {
                                Logger.error(e);
                            }
                        }
                );

        manager.getOperationTypeDefinition()
                .map(operationTypeDefinitionContext -> operationTypeDefinitionContext.typeName().name().getText())
                .map(manager::getObject)
                .flatMap(Optional::stream)
                .flatMap(objectTypeDefinitionContext -> objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream())
                .map(fieldDefinitionContext -> new AbstractMap.SimpleEntry<>(manager.getPackageName(fieldDefinitionContext).orElseGet(graphQLConfig::getPackageName), fieldDefinitionContext))
                .collect(
                        Collectors.groupingBy(
                                Map.Entry<String, GraphqlParser.FieldDefinitionContext>::getKey,
                                Collectors.mapping(
                                        Map.Entry<String, GraphqlParser.FieldDefinitionContext>::getValue,
                                        Collectors.toList()
                                )
                        )
                )
                .forEach((packageName, fieldDefinitionContextList) -> {
                            try {
                                this.buildGraphQLServiceImplClass(packageName).writeTo(filer);
                                Logger.info("{}.GrpcGraphQLServiceImpl build success", packageName);
                                this.buildGrpcServerProducerClass(packageName).writeTo(filer);
                                Logger.info("{}.GrpcServerProducer build success", packageName);
                            } catch (IOException e) {
                                Logger.error(e);
                            }
                        }
                );
    }

    private JavaFile buildGrpcServerProducerClass(String packageName) {
        TypeSpec typeSpec = buildGrpcServerBuilder(packageName);
        return JavaFile.builder(packageName.concat(".grpc"), typeSpec).build();
    }

    private JavaFile buildTypeServiceImplClass(String packageName, OperationType operationType, List<GraphqlParser.FieldDefinitionContext> fieldDefinitionContextList) {
        TypeSpec typeSpec = buildOperationTypeServiceImpl(packageName, operationType, fieldDefinitionContextList);
        return JavaFile.builder(packageName.concat(".grpc"), typeSpec).build();
    }

    private JavaFile buildGraphQLServiceImplClass(String packageName) {
        TypeSpec typeSpec = buildGraphQLServiceImpl(packageName);
        return JavaFile.builder(packageName.concat(".grpc"), typeSpec).build();
    }

    private TypeSpec buildGrpcServerBuilder(String packageName) {
        String grpcPackageName = packageName.concat(".grpc");
        return TypeSpec.classBuilder("GrpcServerProducer")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(ApplicationScoped.class)
                .addField(
                        FieldSpec.builder(
                                ClassName.get("io.graphoenix.grpc.server.config", "GrpcServerConfig"),
                                "grpcServerConfig",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).build()
                )
                .addMethod(
                        MethodSpec.constructorBuilder()
                                .addModifiers(Modifier.PUBLIC)
                                .addAnnotation(Inject.class)
                                .addParameter(ClassName.get("io.graphoenix.grpc.server.config", "GrpcServerConfig"), "grpcServerConfig")
                                .addStatement("this.grpcServerConfig = grpcServerConfig")
                                .build()
                )
                .addMethod(
                        MethodSpec.methodBuilder("server")
                                .addModifiers(Modifier.PUBLIC)
                                .addAnnotation(Produces.class)
                                .returns(Server.class)
                                .addStatement("return $T.forPort(grpcServerConfig.getPort()).addService(new $T()).addService(new $T()).addService(new $T()).build()",
                                        ClassName.get(ServerBuilder.class),
                                        ClassName.get(grpcPackageName, "GrpcQueryTypeServiceImpl"),
                                        ClassName.get(grpcPackageName, "GrpcMutationTypeServiceImpl"),
                                        ClassName.get(grpcPackageName, "GrpcGraphQLServiceImpl")
                                )
                                .build()
                )
                .build();
    }

    private TypeSpec buildOperationTypeServiceImpl(String packageName, OperationType operationType, List<GraphqlParser.FieldDefinitionContext> fieldDefinitionContextList) {
        String grpcPackageName = packageName.concat(".grpc");
        String className;
        String superClassName;
        String serviceName;
        String requestHandlerName;
        switch (operationType) {
            case QUERY:
                className = "GrpcQueryTypeServiceImpl";
                superClassName = "ReactorQueryTypeServiceGrpc";
                serviceName = "QueryTypeServiceImplBase";
                requestHandlerName = "GrpcQueryRequestHandler";
                break;
            case MUTATION:
                className = "GrpcMutationTypeServiceImpl";
                superClassName = "ReactorMutationTypeServiceGrpc";
                serviceName = "MutationTypeServiceImplBase";
                requestHandlerName = "GrpcMutationRequestHandler";
                break;
            default:
                throw new GraphQLErrors(UNSUPPORTED_OPERATION_TYPE);
        }

        TypeSpec.Builder builder = TypeSpec.classBuilder(className)
                .superclass(ClassName.get(grpcPackageName, superClassName, serviceName))
                .addModifiers(Modifier.PUBLIC)
                .addField(
                        FieldSpec.builder(
                                ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(graphQLConfig.getGrpcHandlerPackageName(), requestHandlerName)),
                                "requestHandler",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).build()
                )
                .addField(
                        FieldSpec.builder(
                                ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(graphQLConfig.getGrpcHandlerPackageName(), "GrpcInputObjectHandler")),
                                "inputObjectHandler",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).build()
                )
                .addField(
                        FieldSpec.builder(
                                ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(graphQLConfig.getGrpcHandlerPackageName(), "GrpcObjectHandler")),
                                "objectHandler",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).build()
                )
                .addField(
                        FieldSpec.builder(
                                ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(OperationHandler.class)),
                                "operationHandler",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).build()
                )
                .addField(
                        FieldSpec.builder(
                                ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(graphQLConfig.getHandlerPackageName(), "InvokeHandler")),
                                "invokeHandler",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).build()
                )
                .addField(
                        FieldSpec.builder(
                                ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(Jsonb.class)),
                                "jsonb",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).build()
                )
                .addField(
                        FieldSpec.builder(
                                ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(ArgumentBuilder.class)),
                                "argumentBuilder",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).build()
                )
                .addFields(buildInvokeFields(fieldDefinitionContextList))
                .addMethod(buildConstructor(operationType, fieldDefinitionContextList))
                .addMethods(buildTypeMethods(packageName, operationType, fieldDefinitionContextList));

        if (operationType.equals(MUTATION)) {
            builder.addField(
                    FieldSpec.builder(
                            ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(JsonSchemaValidator.class)),
                            "validator",
                            Modifier.PRIVATE,
                            Modifier.FINAL
                    ).build()
            );
        }
        return builder.build();
    }

    private MethodSpec buildConstructor(OperationType operationType, List<GraphqlParser.FieldDefinitionContext> fieldDefinitionContextList) {
        String requestHandlerName;
        switch (operationType) {
            case QUERY:
                requestHandlerName = "GrpcQueryRequestHandler";
                break;
            case MUTATION:
                requestHandlerName = "GrpcMutationRequestHandler";
                break;
            default:
                throw new GraphQLErrors(UNSUPPORTED_OPERATION_TYPE);
        }

        MethodSpec.Builder builder = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addStatement("this.requestHandler = $T.getProvider($T.class)", ClassName.get(BeanContext.class), ClassName.get(graphQLConfig.getGrpcHandlerPackageName(), requestHandlerName))
                .addStatement("this.inputObjectHandler = $T.getProvider($T.class)", ClassName.get(BeanContext.class), ClassName.get(graphQLConfig.getGrpcHandlerPackageName(), "GrpcInputObjectHandler"))
                .addStatement("this.objectHandler = $T.getProvider($T.class)", ClassName.get(BeanContext.class), ClassName.get(graphQLConfig.getGrpcHandlerPackageName(), "GrpcObjectHandler"))
                .addStatement("this.operationHandler = $T.getProvider($T.class)", ClassName.get(BeanContext.class), ClassName.get(OperationHandler.class))
                .addStatement("this.invokeHandler = $T.getProvider($T.class)", ClassName.get(BeanContext.class), ClassName.get(graphQLConfig.getHandlerPackageName(), "InvokeHandler"))
                .addStatement("this.jsonb = $T.getProvider($T.class)", ClassName.get(BeanContext.class), ClassName.get(Jsonb.class))
                .addStatement("this.argumentBuilder = $T.getProvider($T.class)", ClassName.get(BeanContext.class), ClassName.get(ArgumentBuilder.class));

        Stream<String> invokeClassNames = io.vavr.collection.Stream.ofAll(fieldDefinitionContextList)
                .filter(manager::isInvokeField)
                .distinctBy(typeManager::getClassName)
                .map(typeManager::getClassName)
                .toJavaStream();

        switch (operationType) {
            case QUERY:
                invokeClassNames.forEach(className -> builder.addStatement("this.$L = $T.getProvider($T.class)", typeManager.typeToLowerCamelName(TYPE_NAME_UTIL.toClassName(className).simpleName()), ClassName.get(BeanContext.class), TYPE_NAME_UTIL.toClassName(className)));
                break;
            case MUTATION:
                invokeClassNames.forEach(className -> builder.addStatement("this.$L = $T.getProvider($T.class)", typeManager.typeToLowerCamelName(TYPE_NAME_UTIL.toClassName(className).simpleName()), ClassName.get(BeanContext.class), TYPE_NAME_UTIL.toClassName(className)));
                builder.addStatement("this.validator = $T.getProvider($T.class)", ClassName.get(BeanContext.class), ClassName.get(JsonSchemaValidator.class));
                break;
            default:
                throw new GraphQLErrors(UNSUPPORTED_OPERATION_TYPE);
        }
        return builder.build();
    }

    private Set<FieldSpec> buildInvokeFields(List<GraphqlParser.FieldDefinitionContext> fieldDefinitionContextList) {
        return io.vavr.collection.Stream.ofAll(fieldDefinitionContextList)
                .filter(manager::isInvokeField)
                .distinctBy(typeManager::getClassName)
                .map(typeManager::getClassName)
                .map(className ->
                        FieldSpec.builder(ParameterizedTypeName.get(ClassName.get(Provider.class), TYPE_NAME_UTIL.toClassName(className)), typeManager.typeToLowerCamelName(TYPE_NAME_UTIL.toClassName(className).simpleName()))
                                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                                .build()
                )
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<MethodSpec> buildTypeMethods(String packageName, OperationType operationType, List<GraphqlParser.FieldDefinitionContext> fieldDefinitionContextList) {
        return fieldDefinitionContextList.stream()
                .map(fieldDefinitionContext -> buildTypeMethod(packageName, operationType, fieldDefinitionContext))
                .collect(Collectors.toList());
    }

    private MethodSpec buildTypeMethod(String packageName, OperationType operationType, GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        String grpcPackageName = packageName.concat(".grpc");
        String requestParameterName = "request";
        String grpcHandlerMethodName = grpcNameUtil.getGrpcFieldName(fieldDefinitionContext);
        String grpcRequestClassName = grpcNameUtil.getGrpcRequestClassName(fieldDefinitionContext, operationType);
        String grpcResponseClassName = grpcNameUtil.getGrpcResponseClassName(fieldDefinitionContext, operationType);

        ParameterizedTypeName requestClassName = ParameterizedTypeName.get(ClassName.get(Mono.class), ClassName.get(grpcPackageName, grpcRequestClassName));
        ParameterizedTypeName responseClassName = ParameterizedTypeName.get(ClassName.get(Mono.class), ClassName.get(grpcPackageName, grpcResponseClassName));
        MethodSpec.Builder builder = MethodSpec.methodBuilder(grpcHandlerMethodName)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(requestClassName, requestParameterName)
                .returns(responseClassName);

        CodeBlock codeBlock;
        CodeBlock invokeCodeBlock;
        Optional<CodeBlock> wrapperCodeBlock;
        String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
        String grpcObjectHandlerMethodName = grpcNameUtil.getLowerCamelName(fieldDefinitionContext.type());
        String fieldGetterName = grpcNameUtil.getGetMethodName(fieldDefinitionContext);
        String grpcObjectName = grpcNameUtil.getGrpcTypeName(fieldDefinitionContext.type());
        String grpcFieldAddAllName = grpcNameUtil.getGrpcAddAllMethodName(fieldDefinitionContext);
        String grpcFieldSetterName = grpcNameUtil.getGrpcSetMethodName(fieldDefinitionContext);

        if (manager.isInvokeField(fieldDefinitionContext)) {
            String className = typeManager.getClassName(fieldDefinitionContext);
            String invokeMethodName = typeManager.getMethodName(fieldDefinitionContext);
            List<AbstractMap.SimpleEntry<String, String>> parameters = typeManager.getParameters(fieldDefinitionContext);
            String returnClassName = typeManager.getReturnClassName(fieldDefinitionContext);
            CodeBlock parametersCodeBlock = CodeBlock.join(parameters.stream()
                    .map(parameter ->
                            CodeBlock.of("argumentBuilder.get().getArgument(selectionContext, $S, $T.class)",
                                    parameter.getKey(),
                                    TYPE_UTIL.getClassName(parameter.getValue())
                            )
                    )
                    .collect(Collectors.toList()), ", ");

            Optional<CodeBlock> resultMapBlock;
            if (TYPE_UTIL.getClassName(returnClassName).canonicalName().equals(PublisherBuilder.class.getName())) {
                resultMapBlock = Optional.of(CodeBlock.of(".flatMap($L -> $T.from($L.buildRs()))", fieldDefinitionContext.name().getText(), ClassName.get(Mono.class), fieldDefinitionContext.name().getText()));
                invokeCodeBlock = CodeBlock.of(".map(selectionContext -> $L.get().$L($L))",
                        typeManager.typeToLowerCamelName(TYPE_NAME_UTIL.toClassName(className).simpleName()),
                        invokeMethodName,
                        parametersCodeBlock
                );
            } else if (TYPE_UTIL.getClassName(returnClassName).canonicalName().equals(Mono.class.getName())) {
                resultMapBlock = Optional.empty();
                invokeCodeBlock = CodeBlock.of(".flatMap(selectionContext -> $L.get().$L($L))",
                        typeManager.typeToLowerCamelName(TYPE_NAME_UTIL.toClassName(className).simpleName()),
                        invokeMethodName,
                        parametersCodeBlock
                );
            } else if (TYPE_UTIL.getClassName(returnClassName).canonicalName().equals(Flux.class.getName())) {
                resultMapBlock = Optional.of(CodeBlock.of(".flatMap($L -> $L.collectList())", fieldDefinitionContext.name().getText(), fieldDefinitionContext.name().getText()));
                invokeCodeBlock = CodeBlock.of(".map(selectionContext -> $L.get().$L($L))",
                        typeManager.typeToLowerCamelName(TYPE_NAME_UTIL.toClassName(className).simpleName()),
                        invokeMethodName,
                        parametersCodeBlock
                );
            } else {
                resultMapBlock = Optional.empty();
                invokeCodeBlock = CodeBlock.of(".map(selectionContext -> $L.get().$L($L))",
                        typeManager.typeToLowerCamelName(TYPE_NAME_UTIL.toClassName(className).simpleName()),
                        invokeMethodName,
                        parametersCodeBlock
                );
            }

            if (manager.fieldTypeIsList(fieldDefinitionContext.type())) {
                if (manager.isScalar(fieldTypeName)) {
                    if (fieldTypeName.equals("DateTime") || fieldTypeName.equals("Timestamp") || fieldTypeName.equals("Date") || fieldTypeName.equals("Time")) {
                        wrapperCodeBlock = Optional.of(
                                CodeBlock.of(".map($L -> $L.stream().map($T.CODEC_UTIL::encode).collect($T.toList()))",
                                        fieldDefinitionContext.name().getText(),
                                        fieldDefinitionContext.name().getText(),
                                        ClassName.get(CodecUtil.class)
                                )
                        );
                    } else {
                        wrapperCodeBlock = Optional.empty();
                    }
                } else if (manager.isEnum(fieldTypeName)) {
                    wrapperCodeBlock = Optional.of(
                            CodeBlock.of(".map($L -> $L.stream().map(item::ordinal).map($T::forNumber).collect($T.toList()))",
                                    fieldDefinitionContext.name().getText(),
                                    fieldDefinitionContext.name().getText(),
                                    ClassName.get(grpcPackageName, grpcObjectName),
                                    fieldGetterName,
                                    ClassName.get(Collectors.class)
                            )
                    );
                } else if (manager.isObject(fieldTypeName)) {
                    wrapperCodeBlock = Optional.of(
                            CodeBlock.of(".map($L -> $L.stream().map(objectHandler.get()::$L).collect($T.toList()))",
                                    fieldDefinitionContext.name().getText(),
                                    fieldDefinitionContext.name().getText(),
                                    grpcObjectHandlerMethodName,
                                    ClassName.get(Collectors.class)
                            )
                    );
                } else {
                    throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE);
                }

                codeBlock = CodeBlock.join(
                        Streams.concat(
                                Stream.of(CodeBlock.of("return $L.map(requestHandler.get()::$L)", requestParameterName, grpcHandlerMethodName)),
                                Stream.of(CodeBlock.of(".map($T.DOCUMENT_UTIL::graphqlToOperation)", ClassName.get(DocumentUtil.class))),
                                Stream.of(CodeBlock.of(".map(operationDefinitionContext -> operationDefinitionContext.selectionSet().selection(0))")),
                                Stream.of(invokeCodeBlock),
                                resultMapBlock.stream(),
                                wrapperCodeBlock.stream(),
                                Stream.of(
                                        CodeBlock.of(".map($L -> $T.newBuilder().$L($L).build())",
                                                fieldDefinitionContext.name().getText(),
                                                ClassName.get(grpcPackageName, grpcResponseClassName),
                                                grpcFieldAddAllName,
                                                fieldDefinitionContext.name().getText()
                                        )
                                )
                        ).collect(Collectors.toList()),
                        System.lineSeparator()
                );
            } else {
                if (manager.isScalar(fieldTypeName)) {
                    if (fieldTypeName.equals("DateTime") || fieldTypeName.equals("Timestamp") || fieldTypeName.equals("Date") || fieldTypeName.equals("Time")) {
                        wrapperCodeBlock = Optional.of(CodeBlock.of(".map($T.CODEC_UTIL::encode)", ClassName.get(CodecUtil.class)));
                    } else {
                        wrapperCodeBlock = Optional.empty();
                    }
                } else if (manager.isEnum(fieldTypeName)) {
                    wrapperCodeBlock = Optional.of(
                            CodeBlock.of(".map($L -> $T.forNumber($L.ordinal()))",
                                    fieldDefinitionContext.name().getText(),
                                    ClassName.get(grpcPackageName, grpcObjectName),
                                    fieldDefinitionContext.name().getText()
                            )
                    );
                } else if (manager.isObject(fieldTypeName)) {
                    wrapperCodeBlock = Optional.of(CodeBlock.of(".map(objectHandler.get()::$L)", grpcObjectHandlerMethodName));
                } else {
                    throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE);
                }
                codeBlock = CodeBlock.join(
                        Streams.concat(
                                Stream.of(CodeBlock.of("return $L.map(requestHandler.get()::$L)", requestParameterName, grpcHandlerMethodName)),
                                Stream.of(CodeBlock.of(".map($T.DOCUMENT_UTIL::graphqlToOperation)", ClassName.get(DocumentUtil.class))),
                                Stream.of(CodeBlock.of(".map(operationDefinitionContext -> operationDefinitionContext.selectionSet().selection(0))")),
                                Stream.of(invokeCodeBlock),
                                resultMapBlock.stream(),
                                wrapperCodeBlock.stream(),
                                Stream.of(
                                        CodeBlock.of(".map($L -> $T.newBuilder().$L($L).build())",
                                                fieldDefinitionContext.name().getText(),
                                                ClassName.get(grpcPackageName, grpcResponseClassName),
                                                grpcFieldSetterName,
                                                fieldDefinitionContext.name().getText()
                                        )
                                )
                        ).collect(Collectors.toList()),
                        System.lineSeparator()
                );
            }
        } else {
            ClassName operationClass;
            String operationMethodName;
            String typeInvokeMethodName = grpcNameUtil.getTypeInvokeMethodName(fieldDefinitionContext.type());
            switch (operationType) {
                case QUERY:
                    operationClass = ClassName.get(
                            packageName + ".dto.objectType",
                            manager.getQueryOperationTypeName().orElseThrow(() -> new GraphQLErrors(QUERY_TYPE_NOT_EXIST))
                    );
                    operationMethodName = "query";
                    break;
                case MUTATION:
                    operationClass = ClassName.get(
                            packageName + ".dto.objectType",
                            manager.getMutationOperationTypeName().orElseThrow(() -> new GraphQLErrors(MUTATION_TYPE_NOT_EXIST))
                    );
                    operationMethodName = "mutation";
                    break;
                default:
                    throw new GraphQLErrors(UNSUPPORTED_OPERATION_TYPE);
            }

            if (manager.fieldTypeIsList(fieldDefinitionContext.type())) {
                if (manager.isObject(fieldTypeName)) {
                    invokeCodeBlock = CodeBlock.of(".map(operationType -> $T.from(Mono.justOrEmpty(operationType.$L())).flatMap($T::fromIterable).flatMap(item -> invokeHandler.get().$L(item, operationDefinitionContext.selectionSet().selection(0).field().selectionSet())))",
                            ClassName.get(Flux.class),
                            fieldGetterName,
                            ClassName.get(Flux.class),
                            typeInvokeMethodName
                    );
                } else {
                    invokeCodeBlock = CodeBlock.of(".map(operationType -> operationType.$L())", fieldGetterName);
                }

                if (manager.isScalar(fieldTypeName)) {
                    if (fieldTypeName.equals("DateTime") || fieldTypeName.equals("Timestamp") || fieldTypeName.equals("Date") || fieldTypeName.equals("Time")) {
                        wrapperCodeBlock = Optional.of(
                                CodeBlock.of(".flatMap($L -> $L.map($T.CODEC_UTIL::encode).collectList())",
                                        fieldDefinitionContext.name().getText(),
                                        fieldDefinitionContext.name().getText(),
                                        ClassName.get(CodecUtil.class))
                        );
                    } else {
                        wrapperCodeBlock = Optional.empty();
                    }
                } else if (manager.isEnum(fieldTypeName)) {
                    wrapperCodeBlock = Optional.of(
                            CodeBlock.of(".flatMap($L -> $L.map(item -> $T.forNumber(item.ordinal())).collectList())",
                                    fieldDefinitionContext.name().getText(),
                                    fieldDefinitionContext.name().getText(),
                                    ClassName.get(grpcPackageName, grpcObjectName))
                    );
                } else if (manager.isObject(fieldTypeName)) {
                    wrapperCodeBlock = Optional.of(
                            CodeBlock.of(".flatMap($L -> $L.map(objectHandler.get()::$L).collectList())",
                                    fieldDefinitionContext.name().getText(),
                                    fieldDefinitionContext.name().getText(),
                                    grpcObjectHandlerMethodName
                            )
                    );
                } else {
                    throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE);
                }
                if (operationType.equals(MUTATION)) {
                    codeBlock = CodeBlock.join(
                            Streams.concat(
                                    Stream.of(CodeBlock.of("return $L.map(requestHandler.get()::$L)", requestParameterName, grpcHandlerMethodName)),
                                    Stream.of(CodeBlock.of(".map($T.DOCUMENT_UTIL::graphqlToOperation)", ClassName.get(DocumentUtil.class))),
                                    Stream.of(CodeBlock.of(".doOnSuccess(validator.get()::validateOperation)")),
                                    Stream.of(
                                            CodeBlock.builder()
                                                    .add(".flatMap(operationDefinitionContext -> \n")
                                                    .indent()
                                                    .add("operationHandler.get().$L(operationDefinitionContext)\n", operationMethodName)
                                                    .add(".map(jsonString -> jsonb.get().fromJson(jsonString, $T.class))\n", operationClass)
                                                    .add(invokeCodeBlock)
                                                    .unindent()
                                                    .add("\n)")
                                                    .build()
                                    ),
                                    wrapperCodeBlock.stream(),
                                    Stream.of(
                                            CodeBlock.of(".map($L -> $T.newBuilder().$L($L).build())",
                                                    fieldDefinitionContext.name().getText(),
                                                    ClassName.get(grpcPackageName, grpcResponseClassName),
                                                    grpcFieldAddAllName,
                                                    fieldDefinitionContext.name().getText()
                                            )
                                    )
                            ).collect(Collectors.toList()),
                            System.lineSeparator()
                    );
                } else {
                    codeBlock = CodeBlock.join(
                            Streams.concat(
                                    Stream.of(CodeBlock.of("return $L.map(requestHandler.get()::$L)", requestParameterName, grpcHandlerMethodName)),
                                    Stream.of(CodeBlock.of(".map($T.DOCUMENT_UTIL::graphqlToOperation)", ClassName.get(DocumentUtil.class))),
                                    Stream.of(
                                            CodeBlock.builder()
                                                    .add(".flatMap(operationDefinitionContext -> \n")
                                                    .indent()
                                                    .add("operationHandler.get().$L(operationDefinitionContext)\n", operationMethodName)
                                                    .add(".map(jsonString -> jsonb.get().fromJson(jsonString, $T.class))\n", operationClass)
                                                    .add(invokeCodeBlock)
                                                    .unindent()
                                                    .add("\n)")
                                                    .build()
                                    ),
                                    wrapperCodeBlock.stream(),
                                    Stream.of(
                                            CodeBlock.of(".map($L -> $T.newBuilder().$L($L).build())",
                                                    fieldDefinitionContext.name().getText(),
                                                    ClassName.get(grpcPackageName, grpcResponseClassName),
                                                    grpcFieldAddAllName,
                                                    fieldDefinitionContext.name().getText()
                                            )
                                    )
                            ).collect(Collectors.toList()),
                            System.lineSeparator()
                    );
                }
            } else {
                if (manager.isObject(fieldTypeName)) {
                    invokeCodeBlock = CodeBlock.of(".flatMap(operationType -> invokeHandler.get().$L(operationType.$L(), operationDefinitionContext.selectionSet().selection(0).field().selectionSet()))",
                            typeInvokeMethodName,
                            fieldGetterName
                    );
                } else {
                    invokeCodeBlock = CodeBlock.of(".map(operationType -> operationType.$L())", fieldGetterName);
                }

                if (manager.isScalar(fieldTypeName)) {
                    if (fieldTypeName.equals("DateTime") || fieldTypeName.equals("Timestamp") || fieldTypeName.equals("Date") || fieldTypeName.equals("Time")) {
                        wrapperCodeBlock = Optional.of(CodeBlock.of(".map($T.CODEC_UTIL::encode)", ClassName.get(CodecUtil.class)));
                    } else {
                        wrapperCodeBlock = Optional.empty();
                    }
                } else if (manager.isEnum(fieldTypeName)) {
                    wrapperCodeBlock = Optional.of(
                            CodeBlock.of(".map($L -> $T.forNumber($L.ordinal()))",
                                    fieldDefinitionContext.name().getText(),
                                    fieldDefinitionContext.name().getText(),
                                    ClassName.get(grpcPackageName, grpcObjectName))
                    );
                } else if (manager.isObject(fieldTypeName)) {
                    wrapperCodeBlock = Optional.of(CodeBlock.of(".map(objectHandler.get()::$L)", grpcObjectHandlerMethodName));
                } else {
                    throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE);
                }
                if (operationType.equals(MUTATION)) {
                    codeBlock = CodeBlock.join(
                            Streams.concat(
                                    Stream.of(CodeBlock.of("return $L.map(requestHandler.get()::$L)", requestParameterName, grpcHandlerMethodName)),
                                    Stream.of(CodeBlock.of(".map($T.DOCUMENT_UTIL::graphqlToOperation)", ClassName.get(DocumentUtil.class))),
                                    Stream.of(CodeBlock.of(".doOnNext(validator.get()::validateOperation)")),
                                    Stream.of(
                                            CodeBlock.builder()
                                                    .add(".flatMap(operationDefinitionContext -> \n")
                                                    .indent()
                                                    .add("operationHandler.get().$L(operationDefinitionContext)\n", operationMethodName)
                                                    .add(".map(jsonString -> jsonb.get().fromJson(jsonString, $T.class))\n", operationClass)
                                                    .add(invokeCodeBlock)
                                                    .unindent()
                                                    .add("\n)")
                                                    .build()
                                    ),
                                    wrapperCodeBlock.stream(),
                                    Stream.of(
                                            CodeBlock.of(".map($L -> $T.newBuilder().$L($L).build())",
                                                    fieldDefinitionContext.name().getText(),
                                                    ClassName.get(grpcPackageName, grpcResponseClassName),
                                                    grpcFieldSetterName,
                                                    fieldDefinitionContext.name().getText()
                                            )
                                    )
                            ).collect(Collectors.toList()),
                            System.lineSeparator()
                    );
                } else {
                    codeBlock = CodeBlock.join(
                            Streams.concat(
                                    Stream.of(CodeBlock.of("return $L.map(requestHandler.get()::$L)", requestParameterName, grpcHandlerMethodName)),
                                    Stream.of(CodeBlock.of(".map($T.DOCUMENT_UTIL::graphqlToOperation)", ClassName.get(DocumentUtil.class))),
                                    Stream.of(
                                            CodeBlock.builder()
                                                    .add(".flatMap(operationDefinitionContext -> \n")
                                                    .indent()
                                                    .add("operationHandler.get().$L(operationDefinitionContext)\n", operationMethodName)
                                                    .add(".map(jsonString -> jsonb.get().fromJson(jsonString, $T.class))\n", operationClass)
                                                    .add(invokeCodeBlock)
                                                    .unindent()
                                                    .add("\n)")
                                                    .build()
                                    ),
                                    wrapperCodeBlock.stream(),
                                    Stream.of(
                                            CodeBlock.of(".map($L -> $T.newBuilder().$L($L).build())",
                                                    fieldDefinitionContext.name().getText(),
                                                    ClassName.get(grpcPackageName, grpcResponseClassName),
                                                    grpcFieldSetterName,
                                                    fieldDefinitionContext.name().getText()
                                            )
                                    )
                            ).collect(Collectors.toList()),
                            System.lineSeparator()
                    );
                }
            }
        }
        return builder.addStatement(codeBlock).build();
    }

    private TypeSpec buildGraphQLServiceImpl(String packageName) {
        return TypeSpec.classBuilder("GrpcGraphQLServiceImpl")
                .superclass(ClassName.get(packageName.concat(".grpc"), "ReactorGraphQLServiceGrpc", "GraphQLServiceImplBase"))
                .addModifiers(Modifier.PUBLIC)
                .addField(
                        FieldSpec.builder(
                                ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(GraphQLRequestHandler.class)),
                                "graphQLRequestHandler",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).build()
                )
                .addMethod(buildGraphQLServiceConstructor())
                .addMethod(buildOperationMethod(packageName))
                .build();
    }

    private MethodSpec buildGraphQLServiceConstructor() {
        return MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addStatement("this.graphQLRequestHandler = $T.getProvider($T.class)", ClassName.get(BeanContext.class), ClassName.get(GraphQLRequestHandler.class))
                .build();
    }

    private MethodSpec buildOperationMethod(String packageName) {
        String grpcPackageName = packageName.concat(".grpc");
        String requestParameterName = "request";
        ParameterizedTypeName requestClassName = ParameterizedTypeName.get(ClassName.get(Mono.class), ClassName.get(grpcPackageName, "GraphQLRequest"));
        ParameterizedTypeName responseClassName = ParameterizedTypeName.get(ClassName.get(Mono.class), ClassName.get(grpcPackageName, "GraphQLResponse"));
        return MethodSpec.methodBuilder("operation")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(requestClassName, requestParameterName)
                .returns(responseClassName)
                .addStatement(
                        CodeBlock.join(
                                List.of(
                                        CodeBlock.of("return request.map($T::getRequest)", ClassName.get(grpcPackageName, "GraphQLRequest")),
                                        CodeBlock.of(".map(io.graphoenix.core.dto.GraphQLRequest::new)", ClassName.get(io.graphoenix.core.dto.GraphQLRequest.class)),
                                        CodeBlock.of(".flatMap(graphQLRequestHandler.get()::handle)"),
                                        CodeBlock.of(".onErrorResume(throwable -> Mono.just($T.GRAPHQL_RESPONSE_UTIL.error(throwable)))", ClassName.get(GraphQLResponseUtil.class)),
                                        CodeBlock.of(".map($T.newBuilder()::setResponse)", ClassName.get(grpcPackageName, "GraphQLResponse")),
                                        CodeBlock.of(".map($T.Builder::build)", ClassName.get(grpcPackageName, "GraphQLResponse")),
                                        CodeBlock.of(".contextWrite($T.of($T.REQUEST_ID, $T.randomNanoId()))", ClassName.get(Context.class), ClassName.get(Hammurabi.class), ClassName.get(NanoIdUtils.class))
                                ),
                                System.lineSeparator()
                        )
                )
                .build();
    }
}
