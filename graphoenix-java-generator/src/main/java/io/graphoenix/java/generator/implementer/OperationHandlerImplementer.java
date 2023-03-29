package io.graphoenix.java.generator.implementer;

import com.google.common.reflect.TypeToken;
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
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.core.error.GraphQLErrorType;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.core.handler.ArgumentBuilder;
import io.graphoenix.core.handler.BaseOperationHandler;
import io.graphoenix.core.handler.GraphQLVariablesProcessor;
import io.graphoenix.core.handler.MutationDataLoader;
import io.graphoenix.core.handler.QueryDataLoader;
import io.graphoenix.core.schema.JsonSchemaValidator;
import io.graphoenix.core.utils.DocumentUtil;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.dto.type.OperationType;
import io.graphoenix.spi.handler.MutationHandler;
import io.graphoenix.spi.handler.OperationHandler;
import io.graphoenix.spi.handler.QueryHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.json.JsonValue;
import jakarta.json.bind.Jsonb;
import jakarta.json.spi.JsonProvider;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.tinylog.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.util.AbstractMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static io.graphoenix.core.error.GraphQLErrorType.MUTATION_TYPE_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.QUERY_TYPE_NOT_EXIST;
import static io.graphoenix.core.utils.TypeNameUtil.TYPE_NAME_UTIL;
import static io.graphoenix.java.generator.utils.TypeUtil.TYPE_UTIL;
import static io.graphoenix.spi.dto.type.OperationType.MUTATION;
import static io.graphoenix.spi.dto.type.OperationType.QUERY;

@ApplicationScoped
public class OperationHandlerImplementer {

    private final IGraphQLDocumentManager manager;
    private final TypeManager typeManager;
    private final GraphQLConfig graphQLConfig;

    @Inject
    public OperationHandlerImplementer(IGraphQLDocumentManager manager, TypeManager typeManager, GraphQLConfig graphQLConfig) {
        this.manager = manager;
        this.typeManager = typeManager;
        this.graphQLConfig = graphQLConfig;
    }

    public void writeToFiler(Filer filer) throws IOException {
        this.buildQueryImplementClass().writeTo(filer);
        Logger.info("QueryHandlerImpl build success");
        this.buildMutationImplementClass().writeTo(filer);
        Logger.info("MutationHandlerImpl build success");
    }

    private JavaFile buildQueryImplementClass() {
        TypeSpec typeSpec = buildOperationHandlerImpl(QUERY);
        return JavaFile.builder(graphQLConfig.getHandlerPackageName(), typeSpec).build();
    }

    private JavaFile buildMutationImplementClass() {
        TypeSpec typeSpec = buildOperationHandlerImpl(MUTATION);
        return JavaFile.builder(graphQLConfig.getHandlerPackageName(), typeSpec).build();
    }

    private TypeSpec buildOperationHandlerImpl(OperationType type) {
        String className;
        TypeName superinterface;
        switch (type) {
            case QUERY:
                className = "QueryHandlerImpl";
                superinterface = ClassName.get(QueryHandler.class);
                break;
            case MUTATION:
                className = "MutationHandlerImpl";
                superinterface = ClassName.get(MutationHandler.class);
                break;
            default:
                throw new GraphQLErrors(GraphQLErrorType.UNSUPPORTED_OPERATION_TYPE);
        }

        TypeSpec.Builder builder = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(ApplicationScoped.class)
                .superclass(BaseOperationHandler.class)
                .addSuperinterface(superinterface)
                .addField(
                        FieldSpec.builder(
                                ClassName.get(GraphQLConfig.class),
                                "graphQLConfig",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).build()
                )
                .addField(
                        FieldSpec.builder(
                                ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(IGraphQLDocumentManager.class)),
                                "manager",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).build()
                )
                .addField(
                        FieldSpec.builder(
                                ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(GraphQLVariablesProcessor.class)),
                                "variablesProcessor",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).build()
                )
                .addField(
                        FieldSpec.builder(
                                ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(OperationHandler.class)),
                                "defaultOperationHandler",
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
                                ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(graphQLConfig.getHandlerPackageName(), "ConnectionHandler")),
                                "connectionHandler",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).build()
                )
                .addField(
                        FieldSpec.builder(
                                ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(graphQLConfig.getHandlerPackageName(), "SelectionFilter")),
                                "selectionFilter",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).build()
                )
                .addField(
                        FieldSpec.builder(
                                ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(JsonProvider.class)),
                                "jsonProvider",
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
                .addField(
                        FieldSpec.builder(
                                ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(graphQLConfig.getHandlerPackageName(), "QueryAfterHandler")),
                                "queryHandler",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).build()
                )
                .addField(
                        FieldSpec.builder(
                                ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(QueryDataLoader.class)),
                                "queryDataLoader",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).build()
                )
                .addMethod(buildDefaultOperationMethod(type))
                .addMethod(buildOperationMethod(type))
                .addMethod(buildConstructor(type))
                .addMethods(buildMethods(type));

        switch (type) {
            case QUERY:
                builder.addFields(buildQueryFields());
                break;
            case MUTATION:
                builder.addField(
                                FieldSpec.builder(
                                        ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(graphQLConfig.getHandlerPackageName(), "MutationBeforeHandler")),
                                        "mutationBeforeHandler",
                                        Modifier.PRIVATE,
                                        Modifier.FINAL
                                ).build()
                        ).addField(
                                FieldSpec.builder(
                                        ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(graphQLConfig.getHandlerPackageName(), "MutationAfterHandler")),
                                        "mutationAfterHandler",
                                        Modifier.PRIVATE,
                                        Modifier.FINAL
                                ).build()
                        ).addField(
                                FieldSpec.builder(
                                        ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(MutationDataLoader.class)),
                                        "mutationDataLoader",
                                        Modifier.PRIVATE,
                                        Modifier.FINAL
                                ).build()
                        )
                        .addFields(buildMutationFields())
                        .addField(
                                FieldSpec.builder(
                                        ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(JsonSchemaValidator.class)),
                                        "validator",
                                        Modifier.PRIVATE,
                                        Modifier.FINAL
                                ).build()
                        );
                break;
            default:
                throw new GraphQLErrors(GraphQLErrorType.UNSUPPORTED_OPERATION_TYPE);
        }
        return builder.build();
    }

    private Set<MethodSpec> buildMethods(OperationType type) {
        switch (type) {
            case QUERY:
                return manager.getFields(manager.getQueryOperationTypeName().orElseThrow(() -> new GraphQLErrors(QUERY_TYPE_NOT_EXIST)))
                        .map(this::buildMethod)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
            case MUTATION:
                return manager.getFields(manager.getMutationOperationTypeName().orElseThrow(() -> new GraphQLErrors(MUTATION_TYPE_NOT_EXIST)))
                        .map(this::buildMethod)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
            default:
                throw new GraphQLErrors(GraphQLErrorType.UNSUPPORTED_OPERATION_TYPE);
        }
    }

    private Set<FieldSpec> buildQueryFields() {
        return manager.getFields(manager.getQueryOperationTypeName().orElseThrow(() -> new GraphQLErrors(QUERY_TYPE_NOT_EXIST)))
                .filter(manager::isInvokeField)
                .map(typeManager::getClassName)
                .distinct()
                .collect(Collectors.toList()).stream()
                .map(className ->
                        FieldSpec.builder(ParameterizedTypeName.get(ClassName.get(Provider.class), TYPE_NAME_UTIL.toClassName(className)), typeManager.typeToLowerCamelName(TYPE_NAME_UTIL.toClassName(className).simpleName()))
                                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                                .build()
                )
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<FieldSpec> buildMutationFields() {
        return manager.getFields(manager.getMutationOperationTypeName().orElseThrow(() -> new GraphQLErrors(MUTATION_TYPE_NOT_EXIST)))
                .filter(fieldDefinitionContext -> fieldDefinitionContext.directives() != null)
                .filter(manager::isInvokeField)
                .map(typeManager::getClassName)
                .distinct()
                .collect(Collectors.toList()).stream()
                .map(className ->
                        FieldSpec.builder(ParameterizedTypeName.get(ClassName.get(Provider.class), TYPE_NAME_UTIL.toClassName(className)), typeManager.typeToLowerCamelName(TYPE_NAME_UTIL.toClassName(className).simpleName()))
                                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                                .build()
                )
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private MethodSpec buildDefaultOperationMethod(OperationType type) {
        String operationName;
        switch (type) {
            case QUERY:
                operationName = "query";
                break;
            case MUTATION:
                operationName = "mutation";
                break;
            default:
                throw new GraphQLErrors(GraphQLErrorType.UNSUPPORTED_OPERATION_TYPE);
        }

        return MethodSpec.methodBuilder(operationName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(ParameterSpec.builder(ClassName.get(String.class), "graphQL").build())
                .addParameter(ParameterSpec.builder(ParameterizedTypeName.get(Map.class, String.class, JsonValue.class), "variables").build())
                .returns(ParameterizedTypeName.get(ClassName.get(Mono.class), ClassName.get(JsonValue.class)))
                .addStatement("return $L(defaultOperationHandler.get(), graphQL, variables)", operationName)
                .build();
    }

    private MethodSpec buildOperationMethod(OperationType type) {
        String operationName;
        String operationTypeName;
        switch (type) {
            case QUERY:
                operationName = "query";
                operationTypeName = manager.getQueryOperationTypeName().orElseThrow(() -> new GraphQLErrors(QUERY_TYPE_NOT_EXIST));
                break;
            case MUTATION:
                operationName = "mutation";
                operationTypeName = manager.getMutationOperationTypeName().orElseThrow(() -> new GraphQLErrors(MUTATION_TYPE_NOT_EXIST));
                break;
            default:
                throw new GraphQLErrors(GraphQLErrorType.UNSUPPORTED_OPERATION_TYPE);
        }

        MethodSpec.Builder builder = MethodSpec.methodBuilder(operationName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(ParameterSpec.builder(ClassName.get(OperationHandler.class), "operationHandler").build())
                .addParameter(ParameterSpec.builder(ClassName.get(String.class), "graphQL").build())
                .addParameter(ParameterSpec.builder(ParameterizedTypeName.get(Map.class, String.class, JsonValue.class), "variables").build())
                .returns(ParameterizedTypeName.get(ClassName.get(Mono.class), ClassName.get(JsonValue.class)))
                .addStatement("manager.get().registerFragment(graphQL)")
                .addStatement("$T operationDefinitionContext = variablesProcessor.get().buildVariables(graphQL, variables)", ClassName.get(GraphqlParser.OperationDefinitionContext.class));
        if (type.equals(MUTATION)) {
            builder.addAnnotation(Transactional.class)
                    .addStatement("validator.get().validateOperation(operationDefinitionContext)")
                    .addStatement("$T mutationLoader = mutationDataLoader.get()", ClassName.get(MutationDataLoader.class))
                    .addStatement("$T queryLoader = queryDataLoader.get()", ClassName.get(QueryDataLoader.class))
                    .addStatement(
                            CodeBlock.join(
                                    List.of(
                                            CodeBlock.of("return mutationBeforeHandler.get().handle(operationDefinitionContext, mutationLoader)"),
                                            CodeBlock.builder()
                                                    .add(".flatMap(operation ->\n")
                                                    .indent()
                                                    .add("operationHandler.$L($T.DOCUMENT_UTIL.graphqlToOperation(operation.toString()))\n", operationName, ClassName.get(DocumentUtil.class))
                                                    .add(".map(jsonString -> jsonProvider.get().createReader(new $T(jsonString)).readObject())\n", ClassName.get(StringReader.class))
                                                    .add(".flatMap(jsonObject -> mutationAfterHandler.get().handle(operationDefinitionContext, mutationLoader.then(), operation, jsonObject))\n")
                                                    .unindent()
                                                    .add(")")
                                                    .build(),
                                            CodeBlock.of(".onErrorResume(throwable -> mutationLoader.compensating().then($T.error(throwable)))", ClassName.get(Mono.class)),
                                            CodeBlock.of(".flatMap(jsonObject -> queryHandler.get().handle(jsonObject, operationDefinitionContext, queryLoader))"),
                                            CodeBlock.of(".map(jsonValue -> connectionHandler.get().$L(jsonValue, operationDefinitionContext))", typeManager.typeToLowerCamelName(operationTypeName)),
                                            CodeBlock.of(".flatMap(jsonValue -> invoke(jsonValue, operationDefinitionContext))")
                                    ),
                                    System.lineSeparator()
                            )
                    );
        } else if (type.equals(QUERY)) {
            builder.addStatement("$T queryLoader = queryDataLoader.get()", ClassName.get(QueryDataLoader.class))
                    .addStatement(
                            CodeBlock.join(
                                    List.of(
                                            CodeBlock.of("return operationHandler.$L(operationDefinitionContext).map(jsonString -> jsonProvider.get().createReader(new $T(jsonString)).readObject())",
                                                    operationName,
                                                    ClassName.get(StringReader.class)
                                            ),
                                            CodeBlock.of(".flatMap(jsonObject -> queryHandler.get().handle(jsonObject, operationDefinitionContext, queryLoader))"),
                                            CodeBlock.of(".map(jsonValue -> connectionHandler.get().$L(jsonValue, operationDefinitionContext))", typeManager.typeToLowerCamelName(operationTypeName)),
                                            CodeBlock.of(".flatMap(jsonValue -> invoke(jsonValue, operationDefinitionContext))")
                                    ),
                                    System.lineSeparator()
                            )
                    );
        } else {
            throw new GraphQLErrors(GraphQLErrorType.UNSUPPORTED_OPERATION_TYPE);
        }
        return builder.build();
    }

    private MethodSpec buildMethod(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(fieldDefinitionContext.name().getText())
                .addModifiers(Modifier.PRIVATE)
                .addParameter(ClassName.get(JsonValue.class), "jsonValue")
                .addParameter(ClassName.get(GraphqlParser.SelectionContext.class), "selectionContext")
                .returns(ParameterizedTypeName.get(ClassName.get(Mono.class), ClassName.get(JsonValue.class)));

        boolean fieldTypeIsList = manager.fieldTypeIsList(fieldDefinitionContext.type());
        String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
        String fieldTypeParameterName = typeManager.typeToLowerCamelName(manager.getFieldTypeName(fieldDefinitionContext.type()));

        if (manager.isInvokeField(fieldDefinitionContext)) {
            String className = typeManager.getClassName(fieldDefinitionContext);
            String methodName = typeManager.getMethodName(fieldDefinitionContext);
            List<AbstractMap.SimpleEntry<String, String>> parameters = typeManager.getParameters(fieldDefinitionContext);
            String returnClassName = typeManager.getReturnClassName(fieldDefinitionContext);

            builder.addStatement("$T $L = $L.get().$L($L)",
                    TYPE_UTIL.getTypeName(returnClassName),
                    fieldDefinitionContext.name().getText(),
                    typeManager.typeToLowerCamelName(TYPE_NAME_UTIL.toClassName(className).simpleName()),
                    methodName,
                    CodeBlock.join(parameters.stream()
                            .map(parameter ->
                                    CodeBlock.of("argumentBuilder.get().getArgument(selectionContext, $S, $T.class)",
                                            parameter.getKey(),
                                            TYPE_UTIL.getClassName(parameter.getValue())
                                    )
                            )
                            .collect(Collectors.toList()), ", ")
            );

            CodeBlock invokeCodeBlock;
            if (manager.isObject(fieldTypeName)) {
                String filterMethodName;
                if (fieldTypeIsList) {
                    filterMethodName = fieldTypeParameterName.concat("List");
                } else {
                    filterMethodName = fieldTypeParameterName;
                }
                if (TYPE_UTIL.getClassName(returnClassName).canonicalName().equals(PublisherBuilder.class.getName())) {
                    invokeCodeBlock = CodeBlock.of("return $T.from($L.buildRs()).map(item-> selectionFilter.get().$L(item, selectionContext.field().selectionSet()))", ClassName.get(Mono.class), fieldDefinitionContext.name().getText(), filterMethodName);
                } else if (TYPE_UTIL.getClassName(returnClassName).canonicalName().equals(Mono.class.getName())) {
                    invokeCodeBlock = CodeBlock.of("return $L.map(item-> selectionFilter.get().$L(item, selectionContext.field().selectionSet()))", fieldDefinitionContext.name().getText(), filterMethodName);
                } else if (TYPE_UTIL.getClassName(returnClassName).canonicalName().equals(Flux.class.getName())) {
                    invokeCodeBlock = CodeBlock.of("return $L.collectList().map(item-> selectionFilter.get().$L(item, selectionContext.field().selectionSet()))", fieldDefinitionContext.name().getText(), filterMethodName);
                } else {
                    invokeCodeBlock = CodeBlock.of("return $T.just($L).map(item-> selectionFilter.get().$L(item, selectionContext.field().selectionSet()))", ClassName.get(Mono.class), fieldDefinitionContext.name().getText(), filterMethodName);
                }
                builder.addStatement(invokeCodeBlock);
            } else {
                String filterMethodName;
                if (fieldTypeIsList) {
                    filterMethodName = "toJsonValueList";
                } else {
                    filterMethodName = "toJsonValue";
                }
                if (TYPE_UTIL.getClassName(returnClassName).canonicalName().equals(PublisherBuilder.class.getName())) {
                    invokeCodeBlock = CodeBlock.of("return $T.from($L.buildRs()).map(item-> $L(item))", ClassName.get(Mono.class), fieldDefinitionContext.name().getText(), filterMethodName);
                } else if (TYPE_UTIL.getClassName(returnClassName).canonicalName().equals(Mono.class.getName())) {
                    invokeCodeBlock = CodeBlock.of("return $L.map(item-> $L(item))", fieldDefinitionContext.name().getText(), filterMethodName);
                } else if (TYPE_UTIL.getClassName(returnClassName).canonicalName().equals(Flux.class.getName())) {
                    invokeCodeBlock = CodeBlock.of("return $L.collectList().map(item-> $L(item))", fieldDefinitionContext.name().getText(), filterMethodName);
                } else {
                    invokeCodeBlock = CodeBlock.of("return $T.just($L).map(item-> $L(item))", ClassName.get(Mono.class), fieldDefinitionContext.name().getText(), filterMethodName);
                }
                builder.addStatement(invokeCodeBlock);
            }
        } else {
            if (manager.isObject(fieldTypeName)) {
                if (fieldTypeIsList) {
                    builder.addStatement(
                                    "$T type = new $T<$T>() {}.getType()",
                                    ClassName.get(Type.class),
                                    ClassName.get(TypeToken.class),
                                    typeManager.typeContextToTypeName(fieldDefinitionContext.type())
                            )
                            .addStatement(
                                    "$T $L = jsonb.get().fromJson(jsonValue.toString(), type)",
                                    typeManager.typeContextToTypeName(fieldDefinitionContext.type()),
                                    fieldTypeParameterName.concat("List")
                            )
                            .beginControlFlow("if($L == null)", fieldTypeParameterName.concat("List"))
                            .addStatement("return $T.just($T.NULL)", ClassName.get(Mono.class), ClassName.get(JsonValue.class))
                            .endControlFlow()
                            .addStatement(
                                    CodeBlock.of("return $T.fromIterable($L).flatMap(item -> invokeHandler.get().$L(item, selectionContext.field().selectionSet())).collectList().map(invoked -> selectionFilter.get().$L(invoked, selectionContext.field().selectionSet()))",
                                            ClassName.get(Flux.class),
                                            fieldTypeParameterName.concat("List"),
                                            fieldTypeParameterName,
                                            fieldTypeParameterName.concat("List")
                                    )

                            );
                } else {
                    builder.addStatement(
                            "$T $L = jsonb.get().fromJson(jsonValue.toString(), $T.class)",
                            typeManager.typeContextToTypeName(fieldDefinitionContext.type()),
                            fieldTypeParameterName,
                            typeManager.typeContextToTypeName(fieldDefinitionContext.type())
                    ).addStatement(
                            CodeBlock.of("return invokeHandler.get().$L($L, selectionContext.field().selectionSet()).map(invoked -> selectionFilter.get().$L(invoked, selectionContext.field().selectionSet()))",
                                    fieldTypeParameterName,
                                    fieldTypeParameterName,
                                    fieldTypeParameterName
                            )
                    );
                }
            } else {
                builder.addStatement("return $T.just(jsonValue)", ClassName.get(Mono.class));
            }
        }
        return builder.build();
    }

    private MethodSpec buildConstructor(OperationType type) {
        MethodSpec.Builder builder = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Inject.class)
                .addParameter(ClassName.get(GraphQLConfig.class), "graphQLConfig")
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(IGraphQLDocumentManager.class)), "manager")
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(GraphQLVariablesProcessor.class)), "variablesProcessor")
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(graphQLConfig.getHandlerPackageName(), "InvokeHandler")), "invokeHandler")
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(graphQLConfig.getHandlerPackageName(), "ConnectionHandler")), "connectionHandler")
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(graphQLConfig.getHandlerPackageName(), "SelectionFilter")), "selectionFilter")
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(JsonProvider.class)), "jsonProvider")
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(Jsonb.class)), "jsonb")
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(ArgumentBuilder.class)), "argumentBuilder")
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(QueryDataLoader.class)), "queryDataLoader")
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(graphQLConfig.getHandlerPackageName(), "QueryAfterHandler")), "queryHandler")
                .addStatement("this.graphQLConfig = graphQLConfig")
                .addStatement("this.manager = manager")
                .addStatement("this.variablesProcessor = variablesProcessor")
                .addStatement("this.defaultOperationHandler = $T.ofNullable(graphQLConfig.getDefaultOperationHandlerName()).map(name -> $T.getProvider($T.class, name)).orElseGet(() -> $T.getProvider($T.class))",
                        ClassName.get(Optional.class),
                        ClassName.get(BeanContext.class),
                        ClassName.get(OperationHandler.class),
                        ClassName.get(BeanContext.class),
                        ClassName.get(OperationHandler.class)
                )
                .addStatement("this.invokeHandler = invokeHandler")
                .addStatement("this.connectionHandler = connectionHandler")
                .addStatement("this.selectionFilter = selectionFilter")
                .addStatement("this.jsonProvider = jsonProvider")
                .addStatement("this.jsonb = jsonb")
                .addStatement("this.argumentBuilder = argumentBuilder")
                .addStatement("this.queryDataLoader = queryDataLoader")
                .addStatement("this.queryHandler = queryHandler");

        switch (type) {
            case QUERY:
                manager.getFields(manager.getQueryOperationTypeName().orElseThrow(() -> new GraphQLErrors(QUERY_TYPE_NOT_EXIST)))
                        .filter(manager::isInvokeField)
                        .map(typeManager::getClassName)
                        .distinct()
                        .collect(Collectors.toList())
                        .forEach(className ->
                                builder.addParameter(
                                        ParameterizedTypeName.get(ClassName.get(Provider.class), TYPE_NAME_UTIL.toClassName(className)),
                                        typeManager.typeToLowerCamelName(TYPE_NAME_UTIL.toClassName(className).simpleName())
                                ).addStatement("this.$L = $L",
                                        typeManager.typeToLowerCamelName(TYPE_NAME_UTIL.toClassName(className).simpleName()),
                                        typeManager.typeToLowerCamelName(TYPE_NAME_UTIL.toClassName(className).simpleName())
                                )
                        );
                manager.getFields(manager.getQueryOperationTypeName().orElseThrow(() -> new GraphQLErrors(QUERY_TYPE_NOT_EXIST)))
                        .forEach(fieldDefinitionContext ->
                                builder.addStatement("put($S, this::$L)",
                                        fieldDefinitionContext.name().getText(),
                                        fieldDefinitionContext.name().getText()
                                )
                        );
                break;
            case MUTATION:
                builder.addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(MutationDataLoader.class)), "mutationDataLoader")
                        .addStatement("this.mutationDataLoader = mutationDataLoader")
                        .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(graphQLConfig.getHandlerPackageName(), "MutationBeforeHandler")), "mutationBeforeHandler")
                        .addStatement("this.mutationBeforeHandler = mutationBeforeHandler")
                        .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(graphQLConfig.getHandlerPackageName(), "MutationAfterHandler")), "mutationAfterHandler")
                        .addStatement("this.mutationAfterHandler = mutationAfterHandler")
                        .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(JsonSchemaValidator.class)), "validator")
                        .addStatement("this.validator = validator");
                manager.getFields(manager.getMutationOperationTypeName().orElseThrow(() -> new GraphQLErrors(MUTATION_TYPE_NOT_EXIST)))
                        .filter(manager::isInvokeField)
                        .map(typeManager::getClassName)
                        .distinct()
                        .collect(Collectors.toList())
                        .forEach(className ->
                                builder.addParameter(
                                        ParameterizedTypeName.get(ClassName.get(Provider.class), TYPE_NAME_UTIL.toClassName(className)),
                                        typeManager.typeToLowerCamelName(TYPE_NAME_UTIL.toClassName(className).simpleName())
                                ).addStatement("this.$L = $L",
                                        typeManager.typeToLowerCamelName(TYPE_NAME_UTIL.toClassName(className).simpleName()),
                                        typeManager.typeToLowerCamelName(TYPE_NAME_UTIL.toClassName(className).simpleName())
                                )
                        );
                manager.getFields(manager.getMutationOperationTypeName().orElseThrow(() -> new GraphQLErrors(MUTATION_TYPE_NOT_EXIST)))
                        .forEach(fieldDefinitionContext ->
                                builder.addStatement("put($S, this::$L)",
                                        fieldDefinitionContext.name().getText(),
                                        fieldDefinitionContext.name().getText()
                                )
                        );
                break;
            default:
                throw new GraphQLErrors(GraphQLErrorType.UNSUPPORTED_OPERATION_TYPE);
        }
        return builder.build();
    }
}
