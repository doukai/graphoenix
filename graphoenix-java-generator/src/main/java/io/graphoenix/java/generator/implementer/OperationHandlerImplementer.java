package io.graphoenix.java.generator.implementer;

import com.google.common.base.CaseFormat;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
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
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.core.handler.BaseOperationHandler;
import io.graphoenix.core.handler.GraphQLVariablesProcessor;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.dto.type.OperationType;
import io.graphoenix.spi.handler.MutationHandler;
import io.graphoenix.spi.handler.OperationHandler;
import io.graphoenix.spi.handler.QueryHandler;
import io.graphoenix.spi.module.Module;
import io.graphoenix.spi.module.Provides;
import io.vavr.Tuple2;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Query;
import reactor.core.publisher.Mono;

import javax.annotation.processing.Filer;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

import static io.graphoenix.spi.dto.type.OperationType.MUTATION;
import static io.graphoenix.spi.dto.type.OperationType.QUERY;

public class OperationHandlerImplementer {

    private final IGraphQLDocumentManager manager;
    private final TypeManager typeManager;
    private GraphQLConfig graphQLConfig;
    private List<Tuple2<TypeElement, ExecutableElement>> invokeMethods;

    @Inject
    public OperationHandlerImplementer(IGraphQLDocumentManager manager, TypeManager typeManager) {
        this.manager = manager;
        this.typeManager = typeManager;
    }

    public OperationHandlerImplementer setConfiguration(GraphQLConfig graphQLConfig) {
        this.graphQLConfig = graphQLConfig;
        this.typeManager.setManager(graphQLConfig);
        return this;
    }

    public OperationHandlerImplementer setInvokeMethods(List<Tuple2<TypeElement, ExecutableElement>> invokeMethods) {
        this.invokeMethods = invokeMethods;
        return this;
    }

    public void writeToFiler(Filer filer) throws IOException {
        this.buildQueryImplementClass().writeTo(filer);
        this.buildMutationImplementClass().writeTo(filer);
        this.buildOperationContextImplementClass().writeTo(filer);
    }

    private JavaFile buildQueryImplementClass() {
        TypeSpec typeSpec = buildOperationHandlerImpl(QUERY);
        return JavaFile.builder(graphQLConfig.getHandlerPackageName(), typeSpec).build();
    }

    private JavaFile buildMutationImplementClass() {
        TypeSpec typeSpec = buildOperationHandlerImpl(MUTATION);
        return JavaFile.builder(graphQLConfig.getHandlerPackageName(), typeSpec).build();
    }

    private JavaFile buildOperationContextImplementClass() {
        TypeSpec typeSpec = buildOperationContext();
        return JavaFile.builder(graphQLConfig.getModulePackageName(), typeSpec).build();
    }

    private TypeSpec buildOperationHandlerImpl(OperationType type) {
        String className;
        TypeName typeName;
        switch (type) {
            case QUERY:
                className = "QueryHandlerImpl";
                typeName = ClassName.get(QueryHandler.class);
                break;
            case MUTATION:
                className = "MutationHandlerImpl";
                typeName = ClassName.get(MutationHandler.class);
                break;
            default:
                throw new RuntimeException();
        }

        return TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Singleton.class)
                .superclass(BaseOperationHandler.class)
                .addSuperinterface(typeName)
                .addField(
                        FieldSpec.builder(
                                ClassName.get(IGraphQLDocumentManager.class),
                                "manager",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).build()
                )
                .addField(
                        FieldSpec.builder(
                                ClassName.get(GraphQLVariablesProcessor.class),
                                "variablesProcessor",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).build()
                )
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
                                ClassName.get(OperationHandler.class),
                                "operationHandler",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).build()
                )
                .addField(
                        FieldSpec.builder(
                                ClassName.get(graphQLConfig.getHandlerPackageName(), "InvokeHandler"),
                                "invokeHandler",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).build()
                )
                .addField(
                        FieldSpec.builder(
                                ClassName.get(graphQLConfig.getHandlerPackageName(), "SelectionFilter"),
                                "selectionFilter",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).build()
                )
                .addMethod(buildOperationMethod(type))
                .addFields(buildFields(type))
                .addMethod(buildConstructor(type))
                .addMethods(buildMethods(type))
                .build();
    }

    private TypeSpec buildOperationContext() {
        return TypeSpec.classBuilder("OperationHandlerContext")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Module.class)
                .addMethod(MethodSpec.methodBuilder("invokeHandler")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Provides.class)
                        .addAnnotation(Singleton.class)
                        .returns(ClassName.get(graphQLConfig.getHandlerPackageName(), "InvokeHandler"))
                        .addStatement("return new $T()", ClassName.get(graphQLConfig.getHandlerPackageName(), "InvokeHandler"))
                        .build()
                )
                .addMethod(MethodSpec.methodBuilder("selectionFilter")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Provides.class)
                        .addAnnotation(Singleton.class)
                        .returns(ClassName.get(graphQLConfig.getHandlerPackageName(), "SelectionFilter"))
                        .addStatement("return new $T()", ClassName.get(graphQLConfig.getHandlerPackageName(), "SelectionFilter"))
                        .build()
                )
                .addMethod(MethodSpec.methodBuilder("queryHandlerImpl")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Provides.class)
                        .addAnnotation(Singleton.class)
                        .returns(ClassName.get(QueryHandler.class))
                        .addStatement("return new $T()", ClassName.get(graphQLConfig.getHandlerPackageName(), "QueryHandlerImpl"))
                        .build()
                )
                .addMethod(MethodSpec.methodBuilder("mutationHandlerImpl")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Provides.class)
                        .addAnnotation(Singleton.class)
                        .returns(ClassName.get(MutationHandler.class))
                        .addStatement("return new $T()", ClassName.get(graphQLConfig.getHandlerPackageName(), "MutationHandlerImpl"))
                        .build()
                )
                .build();
    }

    private Set<MethodSpec> buildMethods(OperationType type) {
        switch (type) {
            case QUERY:
                return manager.getFields(manager.getQueryOperationTypeName().orElseThrow())
                        .map(fieldDefinitionContext -> buildMethod(fieldDefinitionContext, type))
                        .collect(Collectors.toCollection(LinkedHashSet::new));
            case MUTATION:
                return manager.getFields(manager.getMutationOperationTypeName().orElseThrow())
                        .map(fieldDefinitionContext -> buildMethod(fieldDefinitionContext, type))
                        .collect(Collectors.toCollection(LinkedHashSet::new));
        }
        throw new RuntimeException();
    }

    private Set<FieldSpec> buildFields(OperationType type) {
        return this.invokeMethods.stream()
                .filter(tuple2 -> tuple2._2().getAnnotation(getAnnotationByType(type)) != null)
                .map(Tuple2::_1)
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .map(typeElement ->
                        FieldSpec.builder(ClassName.get(typeElement), CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, typeElement.getSimpleName().toString()))
                                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                                .build()
                )
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private MethodSpec buildOperationMethod(OperationType type) {

        String operationName;
        switch (type) {
            case QUERY:
                operationName = "query";
                break;
            case MUTATION:
                operationName = "mutation";
                break;
            default:
                throw new RuntimeException();
        }

        return MethodSpec.methodBuilder(operationName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(ParameterSpec.builder(ClassName.get(String.class), "graphQL").build())
                .addParameter(ParameterSpec.builder(ParameterizedTypeName.get(Map.class, String.class, String.class), "variables").build())
                .returns(ParameterizedTypeName.get(ClassName.get(Mono.class), ClassName.get(JsonElement.class)))
                .addStatement("manager.registerFragment(graphQL)")
                .addStatement("$T operationDefinitionContext = variablesProcessor.buildVariables(graphQL, variables)", ClassName.get(GraphqlParser.OperationDefinitionContext.class))
                .addStatement("$T result = operationHandler.$L(operationDefinitionContext).map(jsonString -> gsonBuilder.create().fromJson(jsonString, $T.class))",
                        ParameterizedTypeName.get(Mono.class, JsonElement.class),
                        operationName,
                        ClassName.get(JsonElement.class)
                )
                .addStatement("return result.map(jsonElement -> invoke(jsonElement, operationDefinitionContext))")
                .build();
    }

    private MethodSpec buildMethod(GraphqlParser.FieldDefinitionContext fieldDefinitionContext, OperationType type) {

        MethodSpec.Builder builder = MethodSpec.methodBuilder(fieldDefinitionContext.name().getText())
                .addModifiers(Modifier.PRIVATE)
                .addParameter(ClassName.get(JsonElement.class), "jsonElement")
                .addParameter(ClassName.get(GraphqlParser.SelectionContext.class), "selectionContext")
                .returns(ClassName.get(JsonElement.class));

        Optional<Tuple2<String, String>> invokeDirective = typeManager.getInvokeDirective(fieldDefinitionContext);
        boolean fieldTypeIsList = manager.fieldTypeIsList(fieldDefinitionContext.type());
        String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
        String fieldTypeParameterName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, manager.getFieldTypeName(fieldDefinitionContext.type()));

        if (invokeDirective.isPresent()) {

            Tuple2<TypeElement, ExecutableElement> method = invokeMethods.stream()
                    .filter(tuple2 -> tuple2._2().getAnnotation(getAnnotationByType(type)) != null)
                    .filter(tuple2 -> typeManager.getInvokeFieldName(tuple2._2().getSimpleName().toString()).equals(fieldDefinitionContext.name().getText()))
                    .findFirst()
                    .orElseThrow();

            builder.addStatement("$T result = $L.$L($L)",
                    typeManager.typeContextToTypeName(fieldDefinitionContext.type()),
                    CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, method._1().getSimpleName().toString()),
                    method._2().getSimpleName().toString(),
                    CodeBlock.join(method._2().getParameters().stream()
                            .map(variableElement ->
                                    CodeBlock.of("getArgument(selectionContext, $S, $T.class)",
                                            variableElement.getSimpleName().toString(),
                                            ClassName.get(variableElement.asType())
                                    )
                            )
                            .collect(Collectors.toList()), ",")
            );

            if (manager.isObject(fieldTypeName)) {
                if (fieldTypeIsList) {
                    builder.addStatement("return selectionFilter.$L(result, selectionContext.field().selectionSet())", fieldTypeParameterName.concat("List"));
                } else {
                    builder.addStatement("return selectionFilter.$L(result, selectionContext.field().selectionSet())", fieldTypeParameterName);
                }
            } else {
                if (fieldTypeIsList) {
                    builder.addStatement("$T jsonArray = new $T()", ClassName.get(JsonArray.class), ClassName.get(JsonArray.class));
                    builder.addStatement("result.stream().forEach(item -> jsonArray.add(new $T(item)))", ClassName.get(JsonPrimitive.class));
                    builder.addStatement("return jsonArray");
                } else {
                    builder.addStatement("return new $T(result)", ClassName.get(JsonPrimitive.class));
                }
            }
        } else {
            if (manager.isObject(fieldTypeName)) {
                if (fieldTypeIsList) {
                    builder.addStatement("$T type = new $T<$T>() {}.getType()",
                            ClassName.get(Type.class),
                            ClassName.get(TypeToken.class),
                            typeManager.typeContextToTypeName(fieldDefinitionContext.type())
                    );
                    builder.addStatement("$T result = $L.create().fromJson(jsonElement, type)",
                            typeManager.typeContextToTypeName(fieldDefinitionContext.type()),
                            "gsonBuilder"
                    );
                    builder.addStatement("return selectionFilter.$L(result.stream().map(item -> invokeHandler.$L(item)).collect($T.toList()), selectionContext.field().selectionSet())",
                            fieldTypeParameterName.concat("List"),
                            fieldTypeParameterName,
                            ClassName.get(Collectors.class)
                    );
                } else {
                    builder.addStatement("$T result = $L.create().fromJson(jsonElement, $T.class)",
                            typeManager.typeContextToTypeName(fieldDefinitionContext.type()),
                            "gsonBuilder",
                            typeManager.typeContextToTypeName(fieldDefinitionContext.type())
                    );
                    builder.addStatement("return selectionFilter.$L(invokeHandler.$L(result), selectionContext.field().selectionSet())",
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
                .addStatement("this.$L = $T.get($T.class)",
                        "manager",
                        ClassName.get(BeanContext.class),
                        ClassName.get(IGraphQLDocumentManager.class)
                )
                .addStatement("this.$L = $T.get($T.class)",
                        "variablesProcessor",
                        ClassName.get(BeanContext.class),
                        ClassName.get(GraphQLVariablesProcessor.class)
                )
                .addStatement("this.$L = new $T()",
                        "gsonBuilder",
                        ClassName.get(GsonBuilder.class)
                )
                .addStatement("this.$L = $T.get($T.class)",
                        "operationHandler",
                        ClassName.get(BeanContext.class),
                        ClassName.get(OperationHandler.class)
                )
                .addStatement("this.$L = $T.get($T.class)",
                        "invokeHandler",
                        ClassName.get(BeanContext.class),
                        ClassName.get(graphQLConfig.getHandlerPackageName(), "InvokeHandler")
                )
                .addStatement("this.$L = $T.get($T.class)",
                        "selectionFilter",
                        ClassName.get(BeanContext.class),
                        ClassName.get(graphQLConfig.getHandlerPackageName(), "SelectionFilter")
                );

        invokeMethods.stream()
                .filter(tuple2 -> tuple2._2().getAnnotation(getAnnotationByType(type)) != null)
                .map(Tuple2::_1)
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .forEach(typeElement ->
                        builder.addStatement("this.$L = $T.get($T.class)",
                                CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, typeElement.getSimpleName().toString()),
                                ClassName.get(BeanContext.class),
                                ClassName.get(typeElement)
                        )
                );

        switch (type) {
            case QUERY:
                manager.getFields(manager.getQueryOperationTypeName().orElseThrow())
                        .forEach(fieldDefinitionContext ->
                                builder.addStatement("put($S, this::$L)",
                                        fieldDefinitionContext.name().getText(),
                                        fieldDefinitionContext.name().getText()
                                )
                        );
                break;
            case MUTATION:
                manager.getFields(manager.getMutationOperationTypeName().orElseThrow())
                        .forEach(fieldDefinitionContext ->
                                builder.addStatement("put($S, this::$L)",
                                        fieldDefinitionContext.name().getText(),
                                        fieldDefinitionContext.name().getText()
                                )
                        );
                break;
        }
        return builder.build();
    }

    private Class<? extends Annotation> getAnnotationByType(OperationType type) {
        switch (type) {
            case QUERY:
                return Query.class;
            case MUTATION:
                return Mutation.class;
            default:
                throw new RuntimeException();
        }
    }
}
