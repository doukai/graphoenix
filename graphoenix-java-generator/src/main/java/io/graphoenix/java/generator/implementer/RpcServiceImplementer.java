package io.graphoenix.java.generator.implementer;

import com.google.common.base.CaseFormat;
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
import io.graphoenix.core.schema.JsonSchemaValidator;
import io.graphoenix.core.utils.CodecUtil;
import io.graphoenix.core.utils.DocumentUtil;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
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

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static io.graphoenix.core.error.GraphQLErrorType.MUTATION_TYPE_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.QUERY_TYPE_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.UNSUPPORTED_FIELD_TYPE;
import static io.graphoenix.core.error.GraphQLErrorType.UNSUPPORTED_OPERATION_TYPE;
import static io.graphoenix.spi.constant.Hammurabi.INTROSPECTION_PREFIX;
import static io.graphoenix.spi.dto.type.OperationType.MUTATION;
import static io.graphoenix.spi.dto.type.OperationType.QUERY;

@ApplicationScoped
public class RpcServiceImplementer {

    private final IGraphQLDocumentManager manager;
    private final TypeManager typeManager;
    private GraphQLConfig graphQLConfig;
    private List<String> queryInvokeClassNames;
    private List<String> mutationInvokeClassNames;

    @Inject
    public RpcServiceImplementer(IGraphQLDocumentManager manager, TypeManager typeManager) {
        this.manager = manager;
        this.typeManager = typeManager;
    }

    public RpcServiceImplementer setConfiguration(GraphQLConfig graphQLConfig) {
        this.graphQLConfig = graphQLConfig;
        this.typeManager.setGraphQLConfig(graphQLConfig);
        return this;
    }

    public void writeToFiler(Filer filer) throws IOException {
        queryInvokeClassNames = manager.getFields(manager.getQueryOperationTypeName().orElseThrow(() -> new GraphQLErrors(QUERY_TYPE_NOT_EXIST)))
                .filter(fieldDefinitionContext -> fieldDefinitionContext.directives() != null)
                .filter(fieldDefinitionContext -> fieldDefinitionContext.directives().directive().stream().anyMatch(directiveContext -> directiveContext.name().getText().equals("invoke")))
                .map(typeManager::getClassName)
                .distinct()
                .collect(Collectors.toList());

        mutationInvokeClassNames = manager.getFields(manager.getMutationOperationTypeName().orElseThrow(() -> new GraphQLErrors(MUTATION_TYPE_NOT_EXIST)))
                .filter(fieldDefinitionContext -> fieldDefinitionContext.directives() != null)
                .filter(fieldDefinitionContext -> fieldDefinitionContext.directives().directive().stream().anyMatch(directiveContext -> directiveContext.name().getText().equals("invoke")))
                .map(typeManager::getClassName)
                .distinct()
                .collect(Collectors.toList());

        this.buildQueryTypeServiceImplClass().writeTo(filer);
        Logger.info("QueryTypeServiceImpl build success");
        this.buildMutationTypeServiceImplClass().writeTo(filer);
        Logger.info("MutationTypeServiceImpl build success");
        this.buildGrpcServerProducerClass().writeTo(filer);
        Logger.info("GrpcServerProducer build success");
    }

    private JavaFile buildGrpcServerProducerClass() {
        TypeSpec typeSpec = buildGrpcServerBuilder();
        return JavaFile.builder(graphQLConfig.getHandlerPackageName(), typeSpec).build();
    }

    private JavaFile buildQueryTypeServiceImplClass() {
        TypeSpec typeSpec = buildQueryTypeServiceImpl(QUERY);
        return JavaFile.builder(graphQLConfig.getHandlerPackageName(), typeSpec).build();
    }

    private JavaFile buildMutationTypeServiceImplClass() {
        TypeSpec typeSpec = buildQueryTypeServiceImpl(MUTATION);
        return JavaFile.builder(graphQLConfig.getHandlerPackageName(), typeSpec).build();
    }

    private TypeSpec buildGrpcServerBuilder() {
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
                                .addStatement("return $T.forPort(grpcServerConfig.getPort()).addService(new $T()).addService(new $T()).build()",
                                        ClassName.get(ServerBuilder.class),
                                        ClassName.get(graphQLConfig.getHandlerPackageName(), "QueryTypeServiceImpl"),
                                        ClassName.get(graphQLConfig.getHandlerPackageName(), "MutationTypeServiceImpl")
                                )
                                .build()
                )
                .build();
    }

    private TypeSpec buildQueryTypeServiceImpl(OperationType operationType) {
        String className;
        String superClassName;
        String serviceName;
        String requestHandlerName;
        Set<FieldSpec> fieldSpecs;
        List<MethodSpec> methodSpecs;
        switch (operationType) {
            case QUERY:
                className = "QueryTypeServiceImpl";
                superClassName = "ReactorQueryTypeServiceGrpc";
                serviceName = "QueryTypeServiceImplBase";
                requestHandlerName = "RpcQueryRequestHandler";
                fieldSpecs = buildQueryFields();
                methodSpecs = buildQueryTypeMethods();
                break;
            case MUTATION:
                className = "MutationTypeServiceImpl";
                superClassName = "ReactorMutationTypeServiceGrpc";
                serviceName = "MutationTypeServiceImplBase";
                requestHandlerName = "RpcMutationRequestHandler";
                fieldSpecs = buildMutationFields();
                methodSpecs = buildMutationTypeMethods();
                break;
            default:
                throw new GraphQLErrors(UNSUPPORTED_OPERATION_TYPE);
        }

        TypeSpec.Builder builder = TypeSpec.classBuilder(className)
                .superclass(ClassName.get(graphQLConfig.getGrpcPackageName(), superClassName, serviceName))
                .addModifiers(Modifier.PUBLIC)
                .addField(
                        FieldSpec.builder(
                                ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(graphQLConfig.getHandlerPackageName(), requestHandlerName)),
                                "requestHandler",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).build()
                )
                .addField(
                        FieldSpec.builder(
                                ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(graphQLConfig.getHandlerPackageName(), "RpcInputObjectHandler")),
                                "inputObjectHandler",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).build()
                )
                .addField(
                        FieldSpec.builder(
                                ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(graphQLConfig.getHandlerPackageName(), "RpcObjectHandler")),
                                "rpcObjectHandler",
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
                .addFields(fieldSpecs)
                .addMethod(buildConstructor(operationType))
                .addMethods(methodSpecs);

        if (operationType.equals(MUTATION)) {
            builder.addFields(buildMutationFields())
                    .addField(
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

    private MethodSpec buildConstructor(OperationType operationType) {
        String requestHandlerName;
        switch (operationType) {
            case QUERY:
                requestHandlerName = "RpcQueryRequestHandler";
                break;
            case MUTATION:
                requestHandlerName = "RpcMutationRequestHandler";
                break;
            default:
                throw new GraphQLErrors(UNSUPPORTED_OPERATION_TYPE);
        }

        MethodSpec.Builder builder = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addStatement("this.requestHandler = $T.getProvider($T.class)", ClassName.get(BeanContext.class), ClassName.get(graphQLConfig.getHandlerPackageName(), requestHandlerName))
                .addStatement("this.inputObjectHandler = $T.getProvider($T.class)", ClassName.get(BeanContext.class), ClassName.get(graphQLConfig.getHandlerPackageName(), "RpcInputObjectHandler"))
                .addStatement("this.rpcObjectHandler = $T.getProvider($T.class)", ClassName.get(BeanContext.class), ClassName.get(graphQLConfig.getHandlerPackageName(), "RpcObjectHandler"))
                .addStatement("this.operationHandler = $T.getProvider($T.class)", ClassName.get(BeanContext.class), ClassName.get(OperationHandler.class))
                .addStatement("this.invokeHandler = $T.getProvider($T.class)", ClassName.get(BeanContext.class), ClassName.get(graphQLConfig.getHandlerPackageName(), "InvokeHandler"))
                .addStatement("this.jsonb = $T.getProvider($T.class)", ClassName.get(BeanContext.class), ClassName.get(Jsonb.class))
                .addStatement("this.argumentBuilder = $T.getProvider($T.class)", ClassName.get(BeanContext.class), ClassName.get(ArgumentBuilder.class));

        switch (operationType) {
            case QUERY:
                queryInvokeClassNames.forEach(className -> builder.addStatement("this.$L = $T.getProvider($T.class)", typeManager.typeToLowerCamelName(ClassName.bestGuess(className).simpleName()), ClassName.get(BeanContext.class), ClassName.bestGuess(className)));
                break;
            case MUTATION:
                mutationInvokeClassNames.forEach(className -> builder.addStatement("this.$L = $T.getProvider($T.class)", typeManager.typeToLowerCamelName(ClassName.bestGuess(className).simpleName()), ClassName.get(BeanContext.class), ClassName.bestGuess(className)));
                builder.addStatement("this.validator = $T.getProvider($T.class)", ClassName.get(BeanContext.class), ClassName.get(JsonSchemaValidator.class));
                break;
            default:
                throw new GraphQLErrors(UNSUPPORTED_OPERATION_TYPE);
        }
        return builder.build();
    }

    private Set<FieldSpec> buildQueryFields() {
        return queryInvokeClassNames.stream()
                .map(className ->
                        FieldSpec.builder(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.bestGuess(className)), typeManager.typeToLowerCamelName(ClassName.bestGuess(className).simpleName()))
                                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                                .build()
                )
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<FieldSpec> buildMutationFields() {
        return mutationInvokeClassNames.stream()
                .map(className ->
                        FieldSpec.builder(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.bestGuess(className)), typeManager.typeToLowerCamelName(ClassName.bestGuess(className).simpleName()))
                                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                                .build()
                )
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<MethodSpec> buildQueryTypeMethods() {
        return manager.getQueryOperationTypeName().flatMap(manager::getObject)
                .orElseThrow(() -> new GraphQLErrors(QUERY_TYPE_NOT_EXIST))
                .fieldsDefinition().fieldDefinition().stream()
                .map(fieldDefinitionContext -> buildTypeMethod(fieldDefinitionContext, QUERY))
                .collect(Collectors.toList());
    }

    private List<MethodSpec> buildMutationTypeMethods() {
        return manager.getMutationOperationTypeName().flatMap(manager::getObject)
                .orElseThrow(() -> new GraphQLErrors(MUTATION_TYPE_NOT_EXIST))
                .fieldsDefinition().fieldDefinition().stream()
                .map(fieldDefinitionContext -> buildTypeMethod(fieldDefinitionContext, MUTATION))
                .collect(Collectors.toList());
    }

    private MethodSpec buildTypeMethod(GraphqlParser.FieldDefinitionContext fieldDefinitionContext, OperationType operationType) {
        String requestParameterName = "request";
        String rpcHandlerMethodName = getRpcHandlerMethodName(fieldDefinitionContext);
        String rpcRequestClassName = getRpcRequestClassName(fieldDefinitionContext, operationType);
        String rpcResponseClassName = getRpcResponseClassName(fieldDefinitionContext, operationType);
        ParameterizedTypeName requestClassName = ParameterizedTypeName.get(ClassName.get(Mono.class), ClassName.get(graphQLConfig.getGrpcPackageName(), rpcRequestClassName));
        ParameterizedTypeName responseClassName = ParameterizedTypeName.get(ClassName.get(Mono.class), ClassName.get(graphQLConfig.getGrpcPackageName(), rpcResponseClassName));
        MethodSpec.Builder builder = MethodSpec.methodBuilder(rpcHandlerMethodName)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(requestClassName, requestParameterName)
                .returns(responseClassName);

        CodeBlock codeBlock;
        CodeBlock invokeCodeBlock;
        CodeBlock wrapperCodeBlock;
        String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
        String rpcObjectHandlerMethodName = getRpcObjectHandlerMethodName(fieldDefinitionContext.type());
        String fieldGetterName = getFieldGetterName(fieldDefinitionContext);
        String rpcObjectName = getRpcObjectName(fieldDefinitionContext.type());
        String rpcFieldAddAllName = getRpcFieldAddAllName(fieldDefinitionContext);
        String rpcFieldSetterName = getRpcFieldSetterName(fieldDefinitionContext);

        if (manager.isInvokeField(fieldDefinitionContext)) {
            String className = typeManager.getClassName(fieldDefinitionContext);
            String invokeMethodName = typeManager.getMethodName(fieldDefinitionContext);
            List<AbstractMap.SimpleEntry<String, String>> parameters = typeManager.getParameters(fieldDefinitionContext);
            String returnClassName = typeManager.getReturnClassName(fieldDefinitionContext);
            invokeCodeBlock = CodeBlock.of(".map(selectionContext -> $L.get().$L($L))",
                    typeManager.typeToLowerCamelName(ClassName.bestGuess(className).simpleName()),
                    invokeMethodName,
                    CodeBlock.join(parameters.stream()
                            .map(parameter ->
                                    CodeBlock.of("argumentBuilder.get().getArgument(selectionContext, $S, $T.class)",
                                            parameter.getKey(),
                                            typeManager.getClassNameByString(parameter.getValue())
                                    )
                            )
                            .collect(Collectors.toList()), ", ")
            );

            CodeBlock resultMapBlock;
            if (typeManager.getClassNameByString(returnClassName).canonicalName().equals(PublisherBuilder.class.getName())) {
                resultMapBlock = CodeBlock.of(".flatMap(result -> $T.from(result.buildRs()))", ClassName.get(Mono.class));
            } else if (typeManager.getClassNameByString(returnClassName).canonicalName().equals(Mono.class.getName())) {
                resultMapBlock = CodeBlock.of(".flatMap(result -> result)");
            } else if (typeManager.getClassNameByString(returnClassName).canonicalName().equals(Flux.class.getName())) {
                resultMapBlock = CodeBlock.of(".flatMap(result -> result.collectList())");
            } else {
                resultMapBlock = CodeBlock.of(".flatMap($T::just)", ClassName.get(Mono.class));
            }

            if (manager.fieldTypeIsList(fieldDefinitionContext.type())) {
                if (manager.isScalar(fieldTypeName)) {
                    if (fieldTypeName.equals("DateTime") || fieldTypeName.equals("Timestamp") || fieldTypeName.equals("Date") || fieldTypeName.equals("Time")) {
                        wrapperCodeBlock = CodeBlock.of(".map(item -> item.stream().map($T.CODEC_UTIL::encode).collect($T.toList()))", ClassName.get(CodecUtil.class));
                    } else {
                        wrapperCodeBlock = CodeBlock.of(".map(item -> item)");
                    }
                } else if (manager.isEnum(fieldTypeName)) {
                    wrapperCodeBlock = CodeBlock.of(".map(item -> item.stream().map(item::ordinal).map($T::forNumber).collect($T.toList()))",
                            ClassName.get(graphQLConfig.getGrpcPackageName(), rpcObjectName),
                            fieldGetterName,
                            ClassName.get(Collectors.class)
                    );
                } else if (manager.isObject(fieldTypeName)) {
                    wrapperCodeBlock = CodeBlock.of(".map(item -> item.stream().map(rpcObjectHandler.get()::$L).collect($T.toList()))", rpcObjectHandlerMethodName, ClassName.get(Collectors.class));
                } else {
                    throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE);
                }

                codeBlock = CodeBlock.join(
                        List.of(
                                CodeBlock.of("return $L.map(requestHandler.get()::$L)", requestParameterName, rpcHandlerMethodName),
                                CodeBlock.of(".map($T.DOCUMENT_UTIL::graphqlToOperation)", ClassName.get(DocumentUtil.class)),
                                CodeBlock.of(".map(operationDefinitionContext -> operationDefinitionContext.selectionSet().selection(0))"),
                                invokeCodeBlock,
                                resultMapBlock,
                                wrapperCodeBlock,
                                CodeBlock.of(".map(item -> $T.newBuilder().$L(item).build())",
                                        ClassName.get(graphQLConfig.getGrpcPackageName(), rpcResponseClassName),
                                        rpcFieldAddAllName
                                )
                        ),
                        System.lineSeparator()
                );
            } else {
                if (manager.isScalar(fieldTypeName)) {
                    if (fieldTypeName.equals("DateTime") || fieldTypeName.equals("Timestamp") || fieldTypeName.equals("Date") || fieldTypeName.equals("Time")) {
                        wrapperCodeBlock = CodeBlock.of(".map($T.CODEC_UTIL::encode)", ClassName.get(CodecUtil.class));
                    } else {
                        wrapperCodeBlock = CodeBlock.of(".map(item -> item)");
                    }
                } else if (manager.isEnum(fieldTypeName)) {
                    wrapperCodeBlock = CodeBlock.of(".map(item -> $T.forNumber(item.ordinal()))",
                            ClassName.get(graphQLConfig.getGrpcPackageName(), rpcObjectName),
                            fieldGetterName
                    );
                } else if (manager.isObject(fieldTypeName)) {
                    wrapperCodeBlock = CodeBlock.of(".map(rpcObjectHandler.get()::$L)", rpcObjectHandlerMethodName);
                } else {
                    throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE);
                }
                codeBlock = CodeBlock.join(
                        List.of(
                                CodeBlock.of("return $L.map(requestHandler.get()::$L)", requestParameterName, rpcHandlerMethodName),
                                CodeBlock.of(".map($T.DOCUMENT_UTIL::graphqlToOperation)", ClassName.get(DocumentUtil.class)),
                                CodeBlock.of(".map(operationDefinitionContext -> operationDefinitionContext.selectionSet().selection(0))"),
                                invokeCodeBlock,
                                resultMapBlock,
                                wrapperCodeBlock,
                                CodeBlock.of(".map(item -> $T.newBuilder().$L(item).build())",
                                        ClassName.get(graphQLConfig.getGrpcPackageName(), rpcResponseClassName),
                                        rpcFieldSetterName
                                )
                        ),
                        System.lineSeparator()
                );
            }
        } else {
            ClassName operationClass;
            String operationMethodName;
            String typeInvokeMethodName = getTypeInvokeMethodName(fieldDefinitionContext.type());
            switch (operationType) {
                case QUERY:
                    operationClass = ClassName.get(
                            graphQLConfig.getObjectTypePackageName(),
                            manager.getQueryOperationTypeName().orElseThrow(() -> new GraphQLErrors(QUERY_TYPE_NOT_EXIST))
                    );
                    operationMethodName = "query";
                    break;
                case MUTATION:
                    operationClass = ClassName.get(
                            graphQLConfig.getObjectTypePackageName(),
                            manager.getMutationOperationTypeName().orElseThrow(() -> new GraphQLErrors(MUTATION_TYPE_NOT_EXIST))
                    );
                    operationMethodName = "mutation";
                    break;
                default:
                    throw new GraphQLErrors(UNSUPPORTED_OPERATION_TYPE);
            }

            if (manager.fieldTypeIsList(fieldDefinitionContext.type())) {
                if (manager.isObject(fieldTypeName)) {
                    invokeCodeBlock = CodeBlock.of(".map(operationType -> $T.from(Mono.justOrEmpty(operationType.$L())).flatMap($T::fromIterable).flatMap(invokeHandler.get()::$L))",
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
                        wrapperCodeBlock = CodeBlock.of(".flatMap(result -> result.map($T.CODEC_UTIL::encode).collectList())", ClassName.get(CodecUtil.class));
                    } else {
                        wrapperCodeBlock = CodeBlock.of(".flatMap(result -> result)");
                    }
                } else if (manager.isEnum(fieldTypeName)) {
                    wrapperCodeBlock = CodeBlock.of(".flatMap(result -> result.map(item -> $T.forNumber(item.ordinal())).collectList())", ClassName.get(graphQLConfig.getGrpcPackageName(), rpcObjectName));
                } else if (manager.isObject(fieldTypeName)) {
                    wrapperCodeBlock = CodeBlock.of(".flatMap(result -> result.map(rpcObjectHandler.get()::$L).collectList())", rpcObjectHandlerMethodName);
                } else {
                    throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE);
                }
                if (operationType.equals(MUTATION)) {
                    codeBlock = CodeBlock.join(
                            List.of(
                                    CodeBlock.of("return $L.map(requestHandler.get()::$L)", requestParameterName, rpcHandlerMethodName),
                                    CodeBlock.of(".map($T.DOCUMENT_UTIL::graphqlToOperation)", ClassName.get(DocumentUtil.class)),
                                    CodeBlock.of(".doOnSuccess(validator.get()::validateOperation)"),
                                    CodeBlock.of(".flatMap(operationHandler.get()::$L)", operationMethodName),
                                    CodeBlock.of(".map(jsonString -> jsonb.get().fromJson(jsonString, $T.class))", operationClass),
                                    invokeCodeBlock,
                                    wrapperCodeBlock,
                                    CodeBlock.of(".map(result -> $T.newBuilder().$L(result).build())",
                                            ClassName.get(graphQLConfig.getGrpcPackageName(), rpcResponseClassName),
                                            rpcFieldAddAllName
                                    )
                            ),
                            System.lineSeparator()
                    );
                } else {
                    codeBlock = CodeBlock.join(
                            List.of(
                                    CodeBlock.of("return $L.map(requestHandler.get()::$L)", requestParameterName, rpcHandlerMethodName),
                                    CodeBlock.of(".map($T.DOCUMENT_UTIL::graphqlToOperation)", ClassName.get(DocumentUtil.class)),
                                    CodeBlock.of(".flatMap(operationHandler.get()::$L)", operationMethodName),
                                    CodeBlock.of(".map(jsonString -> jsonb.get().fromJson(jsonString, $T.class))", operationClass),
                                    invokeCodeBlock,
                                    wrapperCodeBlock,
                                    CodeBlock.of(".map(result -> $T.newBuilder().$L(result).build())",
                                            ClassName.get(graphQLConfig.getGrpcPackageName(), rpcResponseClassName),
                                            rpcFieldAddAllName
                                    )
                            ),
                            System.lineSeparator()
                    );
                }
            } else {
                if (manager.isObject(fieldTypeName)) {
                    invokeCodeBlock = CodeBlock.of(".flatMap(operationType -> invokeHandler.get().$L(operationType.$L()))",
                            typeInvokeMethodName,
                            fieldGetterName
                    );
                } else {
                    invokeCodeBlock = CodeBlock.of(".map(operationType -> operationType.$L())", fieldGetterName);
                }

                if (manager.isScalar(fieldTypeName)) {
                    if (fieldTypeName.equals("DateTime") || fieldTypeName.equals("Timestamp") || fieldTypeName.equals("Date") || fieldTypeName.equals("Time")) {
                        wrapperCodeBlock = CodeBlock.of(".map($T.CODEC_UTIL::encode)", ClassName.get(CodecUtil.class));
                    } else {
                        wrapperCodeBlock = CodeBlock.of(".map(result -> result)");
                    }
                } else if (manager.isEnum(fieldTypeName)) {
                    wrapperCodeBlock = CodeBlock.of(".map(result -> $T.forNumber(result.ordinal()))", ClassName.get(graphQLConfig.getGrpcPackageName(), rpcObjectName));
                } else if (manager.isObject(fieldTypeName)) {
                    wrapperCodeBlock = CodeBlock.of(".map(rpcObjectHandler.get()::$L)", rpcObjectHandlerMethodName);
                } else {
                    throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE);
                }
                if (operationType.equals(MUTATION)) {
                    codeBlock = CodeBlock.join(
                            List.of(
                                    CodeBlock.of("return $L.map(requestHandler.get()::$L)", requestParameterName, rpcHandlerMethodName),
                                    CodeBlock.of(".map($T.DOCUMENT_UTIL::graphqlToOperation)", ClassName.get(DocumentUtil.class)),
                                    CodeBlock.of(".doOnSuccess(validator.get()::validateOperation)"),
                                    CodeBlock.of(".flatMap(operationHandler.get()::$L)", operationMethodName),
                                    CodeBlock.of(".map(jsonString -> jsonb.get().fromJson(jsonString, $T.class))", operationClass),
                                    invokeCodeBlock,
                                    wrapperCodeBlock,
                                    CodeBlock.of(".map(result -> $T.newBuilder().$L(result).build())",
                                            ClassName.get(graphQLConfig.getGrpcPackageName(), rpcResponseClassName),
                                            rpcFieldSetterName
                                    )
                            ),
                            System.lineSeparator()
                    );
                } else {
                    codeBlock = CodeBlock.join(
                            List.of(
                                    CodeBlock.of("return $L.map(requestHandler.get()::$L)", requestParameterName, rpcHandlerMethodName),
                                    CodeBlock.of(".map($T.DOCUMENT_UTIL::graphqlToOperation)", ClassName.get(DocumentUtil.class)),
                                    CodeBlock.of(".flatMap(operationHandler.get()::$L)", operationMethodName),
                                    CodeBlock.of(".map(jsonString -> jsonb.get().fromJson(jsonString, $T.class))", operationClass),
                                    invokeCodeBlock,
                                    wrapperCodeBlock,
                                    CodeBlock.of(".map(result -> $T.newBuilder().$L(result).build())",
                                            ClassName.get(graphQLConfig.getGrpcPackageName(), rpcResponseClassName),
                                            rpcFieldSetterName
                                    )
                            ),
                            System.lineSeparator()
                    );
                }
            }
        }
        return builder.addStatement(codeBlock).build();
    }

    private String getRpcObjectHandlerMethodName(GraphqlParser.TypeContext typeContext) {
        String name = manager.getFieldTypeName(typeContext);
        if (name.startsWith(INTROSPECTION_PREFIX)) {
            return "intro".concat(name.replaceFirst(INTROSPECTION_PREFIX, ""));
        }
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, name);
    }

    private String getRpcHandlerMethodName(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        String name = fieldDefinitionContext.name().getText();
        if (name.startsWith(INTROSPECTION_PREFIX)) {
            return "intro".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name.replaceFirst(INTROSPECTION_PREFIX, "")));
        }
        return name;
    }

    private String getRpcRequestClassName(GraphqlParser.FieldDefinitionContext fieldDefinitionContext, OperationType operationType) {
        String name = fieldDefinitionContext.name().getText();
        switch (operationType) {
            case QUERY:
                if (name.startsWith(INTROSPECTION_PREFIX)) {
                    return "QueryIntro".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name.replaceFirst(INTROSPECTION_PREFIX, ""))).concat("Request");
                }
                return "Query".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name.replaceFirst(INTROSPECTION_PREFIX, ""))).concat("Request");
            case MUTATION:
                if (name.startsWith(INTROSPECTION_PREFIX)) {
                    return "MutationIntro".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name.replaceFirst(INTROSPECTION_PREFIX, ""))).concat("Request");
                }
                return "Mutation".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name.replaceFirst(INTROSPECTION_PREFIX, ""))).concat("Request");
            default:
                throw new GraphQLErrors(UNSUPPORTED_OPERATION_TYPE);
        }
    }

    private String getRpcResponseClassName(GraphqlParser.FieldDefinitionContext fieldDefinitionContext, OperationType operationType) {
        String name = fieldDefinitionContext.name().getText();
        switch (operationType) {
            case QUERY:
                if (name.startsWith(INTROSPECTION_PREFIX)) {
                    return "QueryIntro".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name.replaceFirst(INTROSPECTION_PREFIX, ""))).concat("Response");
                }
                return "Query".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name.replaceFirst(INTROSPECTION_PREFIX, ""))).concat("Response");
            case MUTATION:
                if (name.startsWith(INTROSPECTION_PREFIX)) {
                    return "MutationIntro".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name.replaceFirst(INTROSPECTION_PREFIX, ""))).concat("Response");
                }
                return "Mutation".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name.replaceFirst(INTROSPECTION_PREFIX, ""))).concat("Response");
            default:
                throw new GraphQLErrors(UNSUPPORTED_OPERATION_TYPE);
        }
    }

    private String getFieldGetterName(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        String name = fieldDefinitionContext.name().getText();
        if (name.startsWith(INTROSPECTION_PREFIX)) {
            return "get".concat(name);
        }
        return "get".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name));
    }

    private String getTypeInvokeMethodName(GraphqlParser.TypeContext typeContext) {
        String name = manager.getFieldTypeName(typeContext);
        if (name.startsWith(INTROSPECTION_PREFIX)) {
            return "__".concat(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, name.replaceFirst(INTROSPECTION_PREFIX, "")));
        }
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, name);
    }

    private String getRpcFieldSetterName(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        String name = fieldDefinitionContext.name().getText();
        if (name.startsWith(INTROSPECTION_PREFIX)) {
            return "setIntro".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name.replaceFirst(INTROSPECTION_PREFIX, "")));
        }
        return "set".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name));
    }

    private String getRpcFieldAddAllName(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        String name = fieldDefinitionContext.name().getText();
        if (name.startsWith(INTROSPECTION_PREFIX)) {
            return "addAllIntro".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name.replaceFirst(INTROSPECTION_PREFIX, "")));
        }
        return "addAll".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name));
    }

    private String getRpcObjectName(GraphqlParser.TypeContext typeContext) {
        String name = manager.getFieldTypeName(typeContext);
        if (name.startsWith(INTROSPECTION_PREFIX)) {
            return "Intro".concat(name.replaceFirst(INTROSPECTION_PREFIX, ""));
        }
        return name;
    }
}
