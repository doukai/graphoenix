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
import io.graphoenix.core.error.GraphQLErrorType;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.core.handler.ArgumentBuilder;
import io.graphoenix.core.handler.BaseOperationHandler;
import io.graphoenix.core.handler.GraphQLVariablesProcessor;
import io.graphoenix.core.schema.JsonSchemaValidator;
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
import java.util.Set;
import java.util.stream.Collectors;

import static io.graphoenix.core.error.GraphQLErrorType.MUTATION_TYPE_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.QUERY_TYPE_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.UNSUPPORTED_OPERATION_TYPE;
import static io.graphoenix.spi.dto.type.OperationType.MUTATION;
import static io.graphoenix.spi.dto.type.OperationType.QUERY;

@ApplicationScoped
public class OperationHandlerImplementer {

    private final IGraphQLDocumentManager manager;
    private final TypeManager typeManager;
    private GraphQLConfig graphQLConfig;

    @Inject
    public OperationHandlerImplementer(IGraphQLDocumentManager manager, TypeManager typeManager) {
        this.manager = manager;
        this.typeManager = typeManager;
    }

    public OperationHandlerImplementer setConfiguration(GraphQLConfig graphQLConfig) {
        this.graphQLConfig = graphQLConfig;
        this.typeManager.setGraphQLConfig(graphQLConfig);
        return this;
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
                .addMethod(buildOperationMethod(type))
                .addMethod(buildConstructor(type))
                .addMethods(buildMethods(type));

        switch (type) {
            case QUERY:
                builder.addField(
                        FieldSpec.builder(
                                ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(graphQLConfig.getHandlerPackageName(), "RpcQueryHandler")),
                                "grpcQueryHandler",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).build()
                ).addField(
                        FieldSpec.builder(
                                ParameterizedTypeName.get(ClassName.get(Provider.class), ParameterizedTypeName.get(ClassName.get(Mono.class), ClassName.get(graphQLConfig.getHandlerPackageName(), "RpcQueryDataLoader"))),
                                "queryDataLoader",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).build()
                ).addFields(buildQueryFields());
                break;
            case MUTATION:
                builder.addField(
                        FieldSpec.builder(
                                ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(graphQLConfig.getHandlerPackageName(), "RpcMutationHandler")),
                                "grpcMutationHandler",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).build()
                ).addField(
                        FieldSpec.builder(
                                ParameterizedTypeName.get(ClassName.get(Provider.class), ParameterizedTypeName.get(ClassName.get(Mono.class), ClassName.get(graphQLConfig.getHandlerPackageName(), "RpcMutationDataLoader"))),
                                "mutationDataLoader",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).build()
                ).addFields(buildMutationFields())
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
                .filter(fieldDefinitionContext -> fieldDefinitionContext.directives() != null)
                .filter(fieldDefinitionContext -> fieldDefinitionContext.directives().directive().stream().anyMatch(directiveContext -> directiveContext.name().getText().equals("invoke")))
                .map(typeManager::getClassName)
                .distinct()
                .collect(Collectors.toList()).stream()
                .map(className ->
                        FieldSpec.builder(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.bestGuess(className)), typeManager.typeToLowerCamelName(ClassName.bestGuess(className).simpleName()))
                                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                                .build()
                )
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<FieldSpec> buildMutationFields() {
        return manager.getFields(manager.getMutationOperationTypeName().orElseThrow(() -> new GraphQLErrors(MUTATION_TYPE_NOT_EXIST)))
                .filter(fieldDefinitionContext -> fieldDefinitionContext.directives() != null)
                .filter(fieldDefinitionContext -> fieldDefinitionContext.directives().directive().stream().anyMatch(directiveContext -> directiveContext.name().getText().equals("invoke")))
                .map(typeManager::getClassName)
                .distinct()
                .collect(Collectors.toList()).stream()
                .map(className ->
                        FieldSpec.builder(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.bestGuess(className)), typeManager.typeToLowerCamelName(ClassName.bestGuess(className).simpleName()))
                                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                                .build()
                )
                .collect(Collectors.toCollection(LinkedHashSet::new));
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
                .addParameter(ParameterSpec.builder(ClassName.get(String.class), "graphQL").build())
                .addParameter(ParameterSpec.builder(ParameterizedTypeName.get(Map.class, String.class, JsonValue.class), "variables").build())
                .returns(ParameterizedTypeName.get(ClassName.get(Mono.class), ClassName.get(JsonValue.class)))
                .addStatement("manager.get().registerFragment(graphQL)")
                .addStatement("$T operationDefinitionContext = variablesProcessor.get().buildVariables(graphQL, variables)", ClassName.get(GraphqlParser.OperationDefinitionContext.class));
        if (type.equals(MUTATION)) {
            builder.addStatement("validator.get().validateOperation(operationDefinitionContext)");
        }
        builder.addStatement("$T result = operationHandler.get().$L(operationDefinitionContext).map(jsonString -> jsonProvider.get().createReader(new $T(jsonString)).readObject())",
                ParameterizedTypeName.get(Mono.class, JsonValue.class),
                operationName,
                ClassName.get(StringReader.class)
        ).addStatement(
                CodeBlock.join(
                        List.of(
                                CodeBlock.of("return result.flatMap(jsonValue -> invoke(connectionHandler.get().$L(jsonValue, $S, operationDefinitionContext), operationDefinitionContext))", typeManager.typeToLowerCamelName(operationTypeName), operationTypeName),
                                CodeBlock.of(".flatMap(jsonValue -> queryDataLoader.get().map(loader -> loader.dispatch()).thenReturn(jsonValue))")
                        ),
                        System.lineSeparator()
                )
        );
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
        CodeBlock grpcCodeBlock = CodeBlock.of(".flatMap(filtered -> queryDataLoader.get().flatMap(loader -> Mono.fromRunnable(() -> grpcQueryHandler.get().$L(filtered, selectionContext.field().selectionSet(), loader))).thenReturn(filtered))", fieldTypeParameterName);

        if (manager.isInvokeField(fieldDefinitionContext)) {
            String className = typeManager.getClassName(fieldDefinitionContext);
            String methodName = typeManager.getMethodName(fieldDefinitionContext);
            List<AbstractMap.SimpleEntry<String, String>> parameters = typeManager.getParameters(fieldDefinitionContext);
            String returnClassName = typeManager.getReturnClassName(fieldDefinitionContext);

            builder.addStatement("$T result = $L.get().$L($L)",
                    typeManager.getTypeNameByString(returnClassName),
                    typeManager.typeToLowerCamelName(ClassName.bestGuess(className).simpleName()),
                    methodName,
                    CodeBlock.join(parameters.stream()
                            .map(parameter ->
                                    CodeBlock.of("argumentBuilder.get().getArgument(selectionContext, $S, $T.class)",
                                            parameter.getKey(),
                                            typeManager.getClassNameByString(parameter.getValue())
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
                if (typeManager.getClassNameByString(returnClassName).canonicalName().equals(PublisherBuilder.class.getName())) {
                    invokeCodeBlock = CodeBlock.of("return $T.from(result.buildRs()).map(item-> selectionFilter.get().$L(item, selectionContext.field().selectionSet()))", ClassName.get(Mono.class), filterMethodName);
                } else if (typeManager.getClassNameByString(returnClassName).canonicalName().equals(Mono.class.getName())) {
                    invokeCodeBlock = CodeBlock.of("return result.map(item-> selectionFilter.get().$L(item, selectionContext.field().selectionSet()))", filterMethodName);
                } else if (typeManager.getClassNameByString(returnClassName).canonicalName().equals(Flux.class.getName())) {
                    invokeCodeBlock = CodeBlock.of("return result.collectList().map(item-> selectionFilter.get().$L(item, selectionContext.field().selectionSet()))", filterMethodName);
                } else {
                    invokeCodeBlock = CodeBlock.of("return $T.just(result).map(item-> selectionFilter.get().$L(item, selectionContext.field().selectionSet()))", ClassName.get(Mono.class), filterMethodName);
                }
                builder.addStatement(
                        CodeBlock.join(
                                List.of(
                                        invokeCodeBlock,
                                        grpcCodeBlock
                                ),
                                System.lineSeparator()
                        )
                );
            } else {
                String filterMethodName;
                if (fieldTypeIsList) {
                    filterMethodName = "toJsonValueList";
                } else {
                    filterMethodName = "toJsonValue";
                }
                if (typeManager.getClassNameByString(returnClassName).canonicalName().equals(PublisherBuilder.class.getName())) {
                    invokeCodeBlock = CodeBlock.of("return $T.from(result.buildRs()).map(item-> $L(item))", ClassName.get(Mono.class), filterMethodName);
                } else if (typeManager.getClassNameByString(returnClassName).canonicalName().equals(Mono.class.getName())) {
                    invokeCodeBlock = CodeBlock.of("return result.map(item-> $L(item))", filterMethodName);
                } else if (typeManager.getClassNameByString(returnClassName).canonicalName().equals(Flux.class.getName())) {
                    invokeCodeBlock = CodeBlock.of("return result.collectList().map(item-> $L(item))", filterMethodName);
                } else {
                    invokeCodeBlock = CodeBlock.of("return $T.just(result).map(item-> $L(item))", ClassName.get(Mono.class), filterMethodName);
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
                    ).addStatement(
                            "$T result = jsonb.get().fromJson(jsonValue.toString(), type)",
                            typeManager.typeContextToTypeName(fieldDefinitionContext.type())
                    ).beginControlFlow("if(result == null)")
                            .addStatement("return $T.just($T.NULL)", ClassName.get(Mono.class), ClassName.get(JsonValue.class))
                            .endControlFlow()
                            .addStatement(
                                    CodeBlock.join(
                                            List.of(
                                                    CodeBlock.of("return $T.fromIterable(result).flatMap(item -> invokeHandler.get().$L(item)).collectList().map(invoked -> selectionFilter.get().$L(invoked, selectionContext.field().selectionSet()))",
                                                            ClassName.get(Flux.class),
                                                            fieldTypeParameterName,
                                                            fieldTypeParameterName.concat("List")
                                                    ),
                                                    grpcCodeBlock
                                            ),
                                            System.lineSeparator()
                                    )

                            );
                } else {
                    builder.addStatement(
                            "$T result = jsonb.get().fromJson(jsonValue.toString(), $T.class)",
                            typeManager.typeContextToTypeName(fieldDefinitionContext.type()),
                            typeManager.typeContextToTypeName(fieldDefinitionContext.type())
                    ).addStatement(
                            CodeBlock.join(
                                    List.of(
                                            CodeBlock.of("return invokeHandler.get().$L(result).map(invoked -> selectionFilter.get().$L(invoked, selectionContext.field().selectionSet()))",
                                                    fieldTypeParameterName,
                                                    fieldTypeParameterName
                                            ),
                                            grpcCodeBlock
                                    ),
                                    System.lineSeparator()
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
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(IGraphQLDocumentManager.class)), "manager")
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(GraphQLVariablesProcessor.class)), "variablesProcessor")
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(OperationHandler.class)), "operationHandler")
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(graphQLConfig.getHandlerPackageName(), "InvokeHandler")), "invokeHandler")
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(graphQLConfig.getHandlerPackageName(), "ConnectionHandler")), "connectionHandler")
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(graphQLConfig.getHandlerPackageName(), "SelectionFilter")), "selectionFilter")
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(JsonProvider.class)), "jsonProvider")
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(Jsonb.class)), "jsonb")
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(ArgumentBuilder.class)), "argumentBuilder")
                .addStatement("this.manager = manager")
                .addStatement("this.variablesProcessor = variablesProcessor")
                .addStatement("this.operationHandler = operationHandler")
                .addStatement("this.invokeHandler = invokeHandler")
                .addStatement("this.connectionHandler = connectionHandler")
                .addStatement("this.selectionFilter = selectionFilter")
                .addStatement("this.jsonProvider = jsonProvider")
                .addStatement("this.jsonb = jsonb")
                .addStatement("this.argumentBuilder = argumentBuilder");

        switch (type) {
            case QUERY:
                builder.addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ParameterizedTypeName.get(ClassName.get(Mono.class), ClassName.get(graphQLConfig.getHandlerPackageName(), "RpcQueryDataLoader"))), "queryDataLoader")
                        .addStatement("this.queryDataLoader = queryDataLoader")
                        .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(graphQLConfig.getHandlerPackageName(), "RpcQueryHandler")), "grpcQueryHandler")
                        .addStatement("this.grpcQueryHandler = grpcQueryHandler");
                manager.getFields(manager.getQueryOperationTypeName().orElseThrow(() -> new GraphQLErrors(QUERY_TYPE_NOT_EXIST)))
                        .filter(fieldDefinitionContext -> fieldDefinitionContext.directives() != null)
                        .filter(fieldDefinitionContext -> fieldDefinitionContext.directives().directive().stream().anyMatch(directiveContext -> directiveContext.name().getText().equals("invoke")))
                        .map(typeManager::getClassName)
                        .distinct()
                        .collect(Collectors.toList())
                        .forEach(className ->
                                builder.addParameter(
                                        ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.bestGuess(className)),
                                        typeManager.typeToLowerCamelName(ClassName.bestGuess(className).simpleName())
                                ).addStatement("this.$L = $L",
                                        typeManager.typeToLowerCamelName(ClassName.bestGuess(className).simpleName()),
                                        typeManager.typeToLowerCamelName(ClassName.bestGuess(className).simpleName())
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
                builder.addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ParameterizedTypeName.get(ClassName.get(Mono.class), ClassName.get(graphQLConfig.getHandlerPackageName(), "RpcMutationDataLoader"))), "mutationDataLoader")
                        .addStatement("this.mutationDataLoader = mutationDataLoader")
                        .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(graphQLConfig.getHandlerPackageName(), "RpcMutationHandler")), "grpcMutationHandler")
                        .addStatement("this.grpcMutationHandler = grpcMutationHandler")
                        .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(JsonSchemaValidator.class)), "validator")
                        .addStatement("this.validator = validator");
                manager.getFields(manager.getMutationOperationTypeName().orElseThrow(() -> new GraphQLErrors(MUTATION_TYPE_NOT_EXIST)))
                        .filter(fieldDefinitionContext -> fieldDefinitionContext.directives() != null)
                        .filter(fieldDefinitionContext -> fieldDefinitionContext.directives().directive().stream().anyMatch(directiveContext -> directiveContext.name().getText().equals("invoke")))
                        .map(typeManager::getClassName)
                        .distinct()
                        .collect(Collectors.toList())
                        .forEach(className ->
                                builder.addParameter(
                                        ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.bestGuess(className)),
                                        typeManager.typeToLowerCamelName(ClassName.bestGuess(className).simpleName())
                                ).addStatement("this.$L = $L",
                                        typeManager.typeToLowerCamelName(ClassName.bestGuess(className).simpleName()),
                                        typeManager.typeToLowerCamelName(ClassName.bestGuess(className).simpleName())
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
