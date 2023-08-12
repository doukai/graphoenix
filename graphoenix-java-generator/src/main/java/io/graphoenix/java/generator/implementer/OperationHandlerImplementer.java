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
import io.graphoenix.core.handler.*;
import io.graphoenix.core.operation.Operation;
import io.graphoenix.core.schema.JsonSchemaValidator;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.dto.type.OperationType;
import io.graphoenix.spi.handler.*;
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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static io.graphoenix.core.error.GraphQLErrorType.*;
import static io.graphoenix.core.utils.TypeNameUtil.TYPE_NAME_UTIL;
import static io.graphoenix.java.generator.utils.TypeUtil.TYPE_UTIL;
import static io.graphoenix.spi.dto.type.OperationType.*;

@ApplicationScoped
public class OperationHandlerImplementer {

    private final IGraphQLDocumentManager manager;
    private final PackageManager packageManager;
    private final TypeManager typeManager;
    private final GraphQLConfig graphQLConfig;

    @Inject
    public OperationHandlerImplementer(IGraphQLDocumentManager manager, PackageManager packageManager, TypeManager typeManager, GraphQLConfig graphQLConfig) {
        this.manager = manager;
        this.packageManager = packageManager;
        this.typeManager = typeManager;
        this.graphQLConfig = graphQLConfig;
    }

    public void writeToFiler(Filer filer) throws IOException {
        this.buildQueryImplementClass().writeTo(filer);
        Logger.info("QueryHandlerImpl build success");
        this.buildMutationImplementClass().writeTo(filer);
        Logger.info("MutationHandlerImpl build success");
        this.buildSubscriptionImplementClass().writeTo(filer);
        Logger.info("SubscriptionHandlerImpl build success");
    }

    private JavaFile buildQueryImplementClass() {
        TypeSpec typeSpec = buildOperationHandlerImpl(QUERY);
        return JavaFile.builder(graphQLConfig.getHandlerPackageName(), typeSpec).build();
    }

    private JavaFile buildMutationImplementClass() {
        TypeSpec typeSpec = buildOperationHandlerImpl(MUTATION);
        return JavaFile.builder(graphQLConfig.getHandlerPackageName(), typeSpec).build();
    }

    private JavaFile buildSubscriptionImplementClass() {
        TypeSpec typeSpec = buildOperationHandlerImpl(SUBSCRIPTION);
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
            case SUBSCRIPTION:
                className = "SubscriptionHandlerImpl";
                superinterface = ClassName.get(SubscriptionHandler.class);
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
                                ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(GraphQLFetchFieldProcessor.class)),
                                "fetchFieldProcessor",
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
                .addField(
                        FieldSpec.builder(
                                ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(JsonSchemaValidator.class)),
                                "validator",
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
                builder
                        .addField(
                                FieldSpec.builder(
                                        ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(graphQLConfig.getHandlerPackageName(), "MutationBeforeHandler")),
                                        "mutationBeforeHandler",
                                        Modifier.PRIVATE,
                                        Modifier.FINAL
                                ).build()
                        )
                        .addField(
                                FieldSpec.builder(
                                        ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(graphQLConfig.getHandlerPackageName(), "MutationAfterHandler")),
                                        "mutationAfterHandler",
                                        Modifier.PRIVATE,
                                        Modifier.FINAL
                                ).build()
                        )
                        .addField(
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
                                        ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(OperationSubscriber.class)),
                                        "operationSubscriber",
                                        Modifier.PRIVATE,
                                        Modifier.FINAL
                                ).build()
                        );
                break;
            case SUBSCRIPTION:
                builder.addFields(buildSubscriptionFields())
                        .addField(
                                FieldSpec.builder(
                                        ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(OperationSubscriber.class)),
                                        "operationSubscriber",
                                        Modifier.PRIVATE,
                                        Modifier.FINAL
                                ).build()
                        )
                        .addMethod(buildInvokeMethod());
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
            case SUBSCRIPTION:
                return manager.getFields(manager.getSubscriptionOperationTypeName().orElseThrow(() -> new GraphQLErrors(QUERY_TYPE_NOT_EXIST)))
                        .map(this::buildMethod)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
            default:
                throw new GraphQLErrors(GraphQLErrorType.UNSUPPORTED_OPERATION_TYPE);
        }
    }

    private Set<FieldSpec> buildQueryFields() {
        return manager.getFields(manager.getQueryOperationTypeName().orElseThrow(() -> new GraphQLErrors(QUERY_TYPE_NOT_EXIST)))
                .filter(packageManager::isLocalPackage)
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
                .filter(packageManager::isLocalPackage)
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

    private Set<FieldSpec> buildSubscriptionFields() {
        return manager.getFields(manager.getSubscriptionOperationTypeName().orElseThrow(() -> new GraphQLErrors(SUBSCRIBE_TYPE_NOT_EXIST)))
                .filter(packageManager::isLocalPackage)
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
        Class<?> publisherClass;
        switch (type) {
            case QUERY:
                operationName = "query";
                publisherClass = Mono.class;
                break;
            case MUTATION:
                operationName = "mutation";
                publisherClass = Mono.class;
                break;
            case SUBSCRIPTION:
                operationName = "subscription";
                publisherClass = Mono.class;
                break;
            default:
                throw new GraphQLErrors(GraphQLErrorType.UNSUPPORTED_OPERATION_TYPE);
        }

        MethodSpec.Builder builder = MethodSpec.methodBuilder(operationName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(ParameterizedTypeName.get(ClassName.get(publisherClass), ClassName.get(JsonValue.class)));
        switch (type) {
            case QUERY:
            case MUTATION:
            case SUBSCRIPTION:
                builder.addParameter(ParameterSpec.builder(ClassName.get(GraphqlParser.OperationDefinitionContext.class), "operationDefinitionContext").build())
                        .addStatement("return $L(defaultOperationHandler.get(), operationDefinitionContext)", operationName);
                break;
            default:
                throw new GraphQLErrors(GraphQLErrorType.UNSUPPORTED_OPERATION_TYPE);
        }
        return builder.build();
    }

    private MethodSpec buildInvokeMethod() {
        return MethodSpec.methodBuilder("invoke")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(ParameterSpec.builder(ClassName.get(GraphqlParser.OperationDefinitionContext.class), "operationDefinitionContext").build())
                .addParameter(ParameterSpec.builder(ClassName.get(JsonValue.class), "jsonValue").build())
                .returns(ParameterizedTypeName.get(ClassName.get(Mono.class), ClassName.get(JsonValue.class)))
                .addStatement("return invoke(jsonValue, operationDefinitionContext)")
                .build();
    }

    private MethodSpec buildOperationMethod(OperationType type) {
        String operationName;
        String operationMethodName;
        Class<?> publisherClass;
        String operationTypeName;
        switch (type) {
            case QUERY:
                operationName = "query";
                operationMethodName = "query";
                publisherClass = Mono.class;
                operationTypeName = manager.getQueryOperationTypeName().orElseThrow(() -> new GraphQLErrors(QUERY_TYPE_NOT_EXIST));
                break;
            case MUTATION:
                operationName = "mutation";
                operationMethodName = "mutation";
                publisherClass = Mono.class;
                operationTypeName = manager.getMutationOperationTypeName().orElseThrow(() -> new GraphQLErrors(MUTATION_TYPE_NOT_EXIST));
                break;
            case SUBSCRIPTION:
                operationName = "subscription";
                operationMethodName = "query";
                publisherClass = Mono.class;
                operationTypeName = manager.getSubscriptionOperationTypeName().orElseThrow(() -> new GraphQLErrors(SUBSCRIBE_TYPE_NOT_EXIST));
                break;
            default:
                throw new GraphQLErrors(GraphQLErrorType.UNSUPPORTED_OPERATION_TYPE);
        }

        MethodSpec.Builder builder = MethodSpec.methodBuilder(operationName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(ParameterSpec.builder(ClassName.get(OperationHandler.class), "operationHandler").build())
                .addParameter(ParameterSpec.builder(ClassName.get(GraphqlParser.OperationDefinitionContext.class), "operationDefinitionContext").build())
                .returns(ParameterizedTypeName.get(ClassName.get(publisherClass), ClassName.get(JsonValue.class)))
                .addStatement("validator.get().validateOperation(operationDefinitionContext)");

        switch (type) {
            case QUERY:
                builder.addStatement("$T operationWithFetchFieldDefinitionContext = fetchFieldProcessor.get().buildFetchFields(operationDefinitionContext)", ClassName.get(GraphqlParser.OperationDefinitionContext.class))
                        .addStatement("$T queryLoader = queryDataLoader.get()", ClassName.get(QueryDataLoader.class))
                        .addStatement(
                                CodeBlock.join(
                                        List.of(
                                                CodeBlock.of("return operationHandler.$L(operationWithFetchFieldDefinitionContext).map(jsonString -> jsonProvider.get().createReader(new $T(jsonString)).readObject())",
                                                        operationMethodName,
                                                        ClassName.get(StringReader.class)
                                                ),
                                                CodeBlock.of(".flatMap(jsonObject -> queryHandler.get().handle(jsonObject, operationWithFetchFieldDefinitionContext, queryLoader))"),
                                                CodeBlock.of(".map(jsonValue -> connectionHandler.get().$L(jsonValue, operationDefinitionContext))", typeManager.typeToLowerCamelName(operationTypeName)),
                                                CodeBlock.of(".flatMap(jsonValue -> invoke(jsonValue, operationDefinitionContext))")
                                        ),
                                        System.lineSeparator()
                                )
                        );
                break;
            case MUTATION:
                builder.addAnnotation(Transactional.class)
                        .addStatement("$T mutationLoader = mutationDataLoader.get()", ClassName.get(MutationDataLoader.class))
                        .addStatement("$T queryLoader = queryDataLoader.get()", ClassName.get(QueryDataLoader.class))
                        .addStatement(
                                CodeBlock.join(
                                        List.of(
                                                CodeBlock.of("return mutationBeforeHandler.get().handle(operationDefinitionContext, mutationLoader)"),
                                                CodeBlock.builder()
                                                        .add(".map(operation -> operationSubscriber.get().buildSubscriptionFilterSelection(operation))\n")
                                                        .add(".flatMap(operation ->\n")
                                                        .indent()
                                                        .add("$T.just(fetchFieldProcessor.get().buildFetchFields(operation))\n", ClassName.get(Mono.class))
                                                        .add(".flatMap(operationWithFetchFieldDefinitionContext ->\n")
                                                        .indent()
                                                        .add("operationHandler.$L(operationWithFetchFieldDefinitionContext)\n", operationMethodName)
                                                        .add(".map(jsonString -> jsonProvider.get().createReader(new $T(jsonString)).readObject())\n", ClassName.get(StringReader.class))
                                                        .add(".flatMap(jsonObject -> mutationAfterHandler.get().handle(operationDefinitionContext, mutationLoader.then(), operation, jsonObject))\n")
                                                        .add(".flatMap(jsonObject -> queryHandler.get().handle(jsonObject, operationWithFetchFieldDefinitionContext, queryLoader))\n")
                                                        .add(".flatMap(jsonValue -> operationSubscriber.get().sendMutation(operationWithFetchFieldDefinitionContext, operation, jsonValue))\n")
                                                        .unindent()
                                                        .add(")\n")
                                                        .unindent()
                                                        .add(")")
                                                        .build(),
                                                CodeBlock.of(".map(jsonValue -> connectionHandler.get().$L(jsonValue, operationDefinitionContext))", typeManager.typeToLowerCamelName(operationTypeName)),
                                                CodeBlock.of(".flatMap(jsonValue -> invoke(jsonValue, operationDefinitionContext))"),
                                                graphQLConfig.getCompensating() ?
                                                        CodeBlock.of(".onErrorResume(throwable -> mutationLoader.compensating(throwable).then($T.error(new $T(throwable))))", ClassName.get(Mono.class), ClassName.get(GraphQLErrors.class)) :
                                                        CodeBlock.of(".onErrorResume(throwable -> $T.error(new $T(throwable)))", ClassName.get(Mono.class), ClassName.get(GraphQLErrors.class))
                                        ),
                                        System.lineSeparator()
                                )
                        );
                break;
            case SUBSCRIPTION:
                builder.addStatement("$T operation = operationSubscriber.get().buildIDSelection(operationDefinitionContext)", ClassName.get(Operation.class))
                        .addStatement("$T operationWithFetchFieldDefinitionContext = fetchFieldProcessor.get().buildFetchFields(operation)", ClassName.get(GraphqlParser.OperationDefinitionContext.class))
                        .addStatement("$T queryLoader = queryDataLoader.get()", ClassName.get(QueryDataLoader.class))
                        .addStatement(
                                CodeBlock.join(
                                        List.of(
                                                CodeBlock.of("return operationHandler.$L(operationWithFetchFieldDefinitionContext).map(jsonString -> jsonProvider.get().createReader(new $T(jsonString)).readObject())",
                                                        operationMethodName,
                                                        ClassName.get(StringReader.class)
                                                ),
                                                CodeBlock.of(".flatMap(jsonObject -> queryHandler.get().handle(jsonObject, operationWithFetchFieldDefinitionContext, queryLoader))"),
                                                CodeBlock.of(".map(jsonValue -> connectionHandler.get().$L(jsonValue, operationDefinitionContext))", typeManager.typeToLowerCamelName(operationTypeName))
                                        ),
                                        System.lineSeparator()
                                )
                        );
                break;
            default:
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

        if (packageManager.isLocalPackage(fieldDefinitionContext)) {
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
                                                TYPE_UTIL.getTypeName(parameter.getValue())
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
                        invokeCodeBlock = CodeBlock.of("return $T.from($L.buildRs()).map(invoked-> selectionFilter.get().$L(invoked, selectionContext.field().selectionSet()))", ClassName.get(Mono.class), fieldDefinitionContext.name().getText(), filterMethodName);
                    } else if (TYPE_UTIL.getClassName(returnClassName).canonicalName().equals(Mono.class.getName())) {
                        invokeCodeBlock = CodeBlock.of("return $L.map(invoked-> selectionFilter.get().$L(invoked, selectionContext.field().selectionSet()))", fieldDefinitionContext.name().getText(), filterMethodName);
                    } else if (TYPE_UTIL.getClassName(returnClassName).canonicalName().equals(Flux.class.getName())) {
                        invokeCodeBlock = CodeBlock.of("return $L.collectList().map(invoked-> selectionFilter.get().$L(invoked, selectionContext.field().selectionSet()))", fieldDefinitionContext.name().getText(), filterMethodName);
                    } else {
                        invokeCodeBlock = CodeBlock.of("return $T.just($L).map(invoked-> selectionFilter.get().$L(invoked, selectionContext.field().selectionSet()))", ClassName.get(Mono.class), fieldDefinitionContext.name().getText(), filterMethodName);
                    }
                } else {
                    String filterMethodName;
                    if (fieldTypeIsList) {
                        filterMethodName = "toJsonValueList";
                    } else {
                        filterMethodName = "toJsonValue";
                    }
                    if (TYPE_UTIL.getClassName(returnClassName).canonicalName().equals(PublisherBuilder.class.getName())) {
                        invokeCodeBlock = CodeBlock.of("return $T.from($L.buildRs()).map(this::$L)", ClassName.get(Mono.class), fieldDefinitionContext.name().getText(), filterMethodName);
                    } else if (TYPE_UTIL.getClassName(returnClassName).canonicalName().equals(Mono.class.getName())) {
                        invokeCodeBlock = CodeBlock.of("return $L.map(this::$L)", fieldDefinitionContext.name().getText(), filterMethodName);
                    } else if (TYPE_UTIL.getClassName(returnClassName).canonicalName().equals(Flux.class.getName())) {
                        invokeCodeBlock = CodeBlock.of("return $L.collectList().map(this::$L)", fieldDefinitionContext.name().getText(), filterMethodName);
                    } else {
                        invokeCodeBlock = CodeBlock.of("return $T.just($L).map(this::$L)", ClassName.get(Mono.class), fieldDefinitionContext.name().getText(), filterMethodName);
                    }
                }
                builder.addStatement(invokeCodeBlock);
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
                                )
                                .beginControlFlow("if($L == null)", fieldTypeParameterName)
                                .addStatement("return $T.just($T.NULL)", ClassName.get(Mono.class), ClassName.get(JsonValue.class))
                                .endControlFlow()
                                .addStatement(
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
        } else {
            builder.addStatement("return $T.just(jsonValue)", ClassName.get(Mono.class));
        }
        return builder.build();
    }

    private MethodSpec buildConstructor(OperationType type) {
        MethodSpec.Builder builder = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Inject.class)
                .addParameter(ClassName.get(GraphQLConfig.class), "graphQLConfig")
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(IGraphQLDocumentManager.class)), "manager")
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(GraphQLFetchFieldProcessor.class)), "fetchFieldProcessor")
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(graphQLConfig.getHandlerPackageName(), "InvokeHandler")), "invokeHandler")
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(graphQLConfig.getHandlerPackageName(), "ConnectionHandler")), "connectionHandler")
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(graphQLConfig.getHandlerPackageName(), "SelectionFilter")), "selectionFilter")
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(JsonProvider.class)), "jsonProvider")
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(Jsonb.class)), "jsonb")
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(ArgumentBuilder.class)), "argumentBuilder")
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(QueryDataLoader.class)), "queryDataLoader")
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(graphQLConfig.getHandlerPackageName(), "QueryAfterHandler")), "queryHandler")
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(JsonSchemaValidator.class)), "validator")
                .addStatement("this.graphQLConfig = graphQLConfig")
                .addStatement("this.manager = manager")
                .addStatement("this.fetchFieldProcessor = fetchFieldProcessor")
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
                .addStatement("this.queryHandler = queryHandler")
                .addStatement("this.validator = validator");

        switch (type) {
            case QUERY:
                manager.getFields(manager.getQueryOperationTypeName().orElseThrow(() -> new GraphQLErrors(QUERY_TYPE_NOT_EXIST)))
                        .filter(packageManager::isLocalPackage)
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
                        .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(OperationSubscriber.class)), "operationSubscriber")
                        .addStatement("this.operationSubscriber = operationSubscriber");
                manager.getFields(manager.getMutationOperationTypeName().orElseThrow(() -> new GraphQLErrors(MUTATION_TYPE_NOT_EXIST)))
                        .filter(packageManager::isLocalPackage)
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
            case SUBSCRIPTION:
                builder.addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(OperationSubscriber.class)), "operationSubscriber")
                        .addStatement("this.operationSubscriber = operationSubscriber");
                manager.getFields(manager.getSubscriptionOperationTypeName().orElseThrow(() -> new GraphQLErrors(QUERY_TYPE_NOT_EXIST)))
                        .filter(packageManager::isLocalPackage)
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
                manager.getFields(manager.getSubscriptionOperationTypeName().orElseThrow(() -> new GraphQLErrors(QUERY_TYPE_NOT_EXIST)))
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
