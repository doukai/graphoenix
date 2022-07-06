package io.graphoenix.java.generator.implementer;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
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
import org.tinylog.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import java.io.IOException;
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
                                ClassName.get(GsonBuilder.class),
                                "gsonBuilder",
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
                .addMethod(buildOperationMethod(type))
                .addMethod(buildConstructor(type))
                .addMethods(buildMethods(type));

        if (type.equals(QUERY)) {
            builder.addFields(buildQueryFields());
        } else if (type.equals(MUTATION)) {
            builder.addFields(buildMutationFields())
                    .addField(
                            FieldSpec.builder(
                                    ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(JsonSchemaValidator.class)),
                                    "validator",
                                    Modifier.PRIVATE,
                                    Modifier.FINAL
                            ).build()
                    );
        } else {
            throw new GraphQLErrors(UNSUPPORTED_OPERATION_TYPE);
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
                .addParameter(ParameterSpec.builder(ParameterizedTypeName.get(Map.class, String.class, JsonElement.class), "variables").build())
                .returns(ParameterizedTypeName.get(ClassName.get(Mono.class), ClassName.get(JsonElement.class)))
                .addStatement("manager.get().registerFragment(graphQL)")
                .addStatement("$T operationDefinitionContext = variablesProcessor.get().buildVariables(graphQL, variables)", ClassName.get(GraphqlParser.OperationDefinitionContext.class));
        if (type.equals(MUTATION)) {
            builder.addStatement("validator.get().validateOperation(operationDefinitionContext)");
        }
        builder.addStatement("$T result = operationHandler.get().$L(operationDefinitionContext).map(jsonString -> $T.parseString(jsonString))",
                ParameterizedTypeName.get(Mono.class, JsonElement.class),
                operationName,
                ClassName.get(JsonParser.class)
        )
                .addStatement("return result.flatMap(jsonElement -> invoke(connectionHandler.get().$L(jsonElement, $S, operationDefinitionContext), operationDefinitionContext))", typeManager.typeToLowerCamelName(operationTypeName), operationTypeName);
        return builder.build();
    }

    private MethodSpec buildMethod(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {

        MethodSpec.Builder builder = MethodSpec.methodBuilder(fieldDefinitionContext.name().getText())
                .addModifiers(Modifier.PRIVATE)
                .addParameter(ClassName.get(JsonElement.class), "jsonElement")
                .addParameter(ClassName.get(GraphqlParser.SelectionContext.class), "selectionContext");

        boolean fieldTypeIsList = manager.fieldTypeIsList(fieldDefinitionContext.type());
        String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
        String fieldTypeParameterName = typeManager.typeToLowerCamelName(manager.getFieldTypeName(fieldDefinitionContext.type()));

        if (manager.isInvokeField(fieldDefinitionContext)) {
            builder.returns(ParameterizedTypeName.get(ClassName.get(Flux.class), ClassName.get(JsonElement.class)));
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
                                    CodeBlock.of("getArgument(selectionContext, $S, $T.class)",
                                            parameter.getKey(),
                                            typeManager.getClassNameByString(parameter.getValue())
                                    )
                            )
                            .collect(Collectors.toList()), ",")
            );

            if (manager.isObject(fieldTypeName)) {
                String filterMethodName;
                if (fieldTypeIsList) {
                    filterMethodName = fieldTypeParameterName.concat("List");
                } else {
                    filterMethodName = fieldTypeParameterName;
                }
                if (typeManager.getClassNameByString(returnClassName).canonicalName().equals(Flux.class.getName())) {
                    builder.addStatement("return result.map(item-> selectionFilter.get().$L(item, selectionContext.field().selectionSet()))", filterMethodName);
                } else if (typeManager.getClassNameByString(returnClassName).canonicalName().equals(Mono.class.getName())) {
                    builder.addStatement("return $T.from(result).map(item-> selectionFilter.get().$L(item, selectionContext.field().selectionSet()))", ClassName.get(Flux.class), filterMethodName);
                } else {
                    builder.addStatement("return $T.just(result).map(item-> selectionFilter.get().$L(item, selectionContext.field().selectionSet()))", ClassName.get(Flux.class), filterMethodName);
                }
            } else {
                String filterMethodName;
                if (fieldTypeIsList) {
                    filterMethodName = "toJsonPrimitiveList";
                } else {
                    filterMethodName = "toJsonPrimitive";
                }
                if (typeManager.getClassNameByString(returnClassName).canonicalName().equals(Flux.class.getName())) {
                    builder.addStatement("return result.map(item-> $L(item))", filterMethodName);
                } else if (typeManager.getClassNameByString(returnClassName).canonicalName().equals(Mono.class.getName())) {
                    builder.addStatement("return $T.from(result).map(item-> $L(item))", ClassName.get(Flux.class), filterMethodName);
                } else {
                    builder.addStatement("return $T.just(result).map(item-> $L(item))", ClassName.get(Flux.class), filterMethodName);
                }
            }
        } else {
            builder.returns(ClassName.get(JsonElement.class));
            if (manager.isObject(fieldTypeName)) {
                if (fieldTypeIsList) {
                    builder.addStatement(
                            "$T type = new $T<$T>() {}.getType()",
                            ClassName.get(Type.class),
                            ClassName.get(TypeToken.class),
                            typeManager.typeContextToTypeName(fieldDefinitionContext.type())
                    ).addStatement(
                            "$T result = $L.create().fromJson(jsonElement, type)",
                            typeManager.typeContextToTypeName(fieldDefinitionContext.type()),
                            "gsonBuilder"
                    ).beginControlFlow("if(result == null)")
                            .addStatement("return $T.INSTANCE", ClassName.get(JsonNull.class))
                            .endControlFlow()
                            .addStatement(
                                    "return selectionFilter.get().$L(result.stream().map(item -> invokeHandler.get().$L(item)).collect($T.toList()), selectionContext.field().selectionSet())",
                                    fieldTypeParameterName.concat("List"),
                                    fieldTypeParameterName,
                                    ClassName.get(Collectors.class)
                            );
                } else {
                    builder.addStatement(
                            "$T result = $L.create().fromJson(jsonElement, $T.class)",
                            typeManager.typeContextToTypeName(fieldDefinitionContext.type()),
                            "gsonBuilder",
                            typeManager.typeContextToTypeName(fieldDefinitionContext.type())
                    ).addStatement("return selectionFilter.get().$L(invokeHandler.get().$L(result), selectionContext.field().selectionSet())",
                            fieldTypeParameterName,
                            fieldTypeParameterName
                    );
                }
            } else {
                builder.addStatement("return jsonElement");
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
                .addStatement("this.gsonBuilder = new $T()", ClassName.get(GsonBuilder.class))
                .addStatement("this.manager = manager")
                .addStatement("this.variablesProcessor = variablesProcessor")
                .addStatement("this.operationHandler = operationHandler")
                .addStatement("this.invokeHandler = invokeHandler")
                .addStatement("this.connectionHandler = connectionHandler")
                .addStatement("this.selectionFilter = selectionFilter");
        if (type.equals(MUTATION)) {
            builder.addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(JsonSchemaValidator.class)), "validator")
                    .addStatement("this.validator = validator");
        }
        switch (type) {
            case QUERY:
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
                        .filter(fieldDefinitionContext -> fieldDefinitionContext.directives() == null || fieldDefinitionContext.directives().directive().stream().noneMatch(directiveContext -> directiveContext.name().getText().equals("invoke")))
                        .forEach(fieldDefinitionContext ->
                                builder.addStatement("put($S, this::$L)",
                                        fieldDefinitionContext.name().getText(),
                                        fieldDefinitionContext.name().getText()
                                )
                        );
                manager.getFields(manager.getQueryOperationTypeName().orElseThrow(() -> new GraphQLErrors(QUERY_TYPE_NOT_EXIST)))
                        .filter(fieldDefinitionContext -> fieldDefinitionContext.directives() != null)
                        .filter(fieldDefinitionContext -> fieldDefinitionContext.directives().directive().stream().anyMatch(directiveContext -> directiveContext.name().getText().equals("invoke")))
                        .forEach(fieldDefinitionContext ->
                                builder.addStatement("putFlux($S, this::$L)",
                                        fieldDefinitionContext.name().getText(),
                                        fieldDefinitionContext.name().getText()
                                )
                        );
                break;
            case MUTATION:
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
                        .filter(fieldDefinitionContext -> fieldDefinitionContext.directives() == null || fieldDefinitionContext.directives().directive().stream().noneMatch(directiveContext -> directiveContext.name().getText().equals("invoke")))
                        .forEach(fieldDefinitionContext ->
                                builder.addStatement("put($S, this::$L)",
                                        fieldDefinitionContext.name().getText(),
                                        fieldDefinitionContext.name().getText()
                                )
                        );
                manager.getFields(manager.getMutationOperationTypeName().orElseThrow(() -> new GraphQLErrors(MUTATION_TYPE_NOT_EXIST)))
                        .filter(fieldDefinitionContext -> fieldDefinitionContext.directives() != null)
                        .filter(fieldDefinitionContext -> fieldDefinitionContext.directives().directive().stream().anyMatch(directiveContext -> directiveContext.name().getText().equals("invoke")))
                        .forEach(fieldDefinitionContext ->
                                builder.addStatement("putFlux($S, this::$L)",
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
