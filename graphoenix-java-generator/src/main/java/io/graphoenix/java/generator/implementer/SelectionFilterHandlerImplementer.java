package io.graphoenix.java.generator.implementer;

import com.google.common.base.CaseFormat;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;

import javax.annotation.processing.Filer;
import javax.inject.Inject;
import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class SelectionFilterHandlerImplementer {

    private final IGraphQLDocumentManager manager;
    private final TypeManager typeManager;
    private GraphQLConfig graphQLConfig;

    @Inject
    public SelectionFilterHandlerImplementer(IGraphQLDocumentManager manager, TypeManager typeManager) {
        this.manager = manager;
        this.typeManager = typeManager;
    }

    public SelectionFilterHandlerImplementer setConfiguration(GraphQLConfig graphQLConfig) {
        this.graphQLConfig = graphQLConfig;
        this.typeManager.setManager(graphQLConfig);
        return this;
    }

    public void writeToFiler(Filer filer) throws IOException {
        this.buildImplementClass().writeTo(filer);
    }

    public JavaFile buildImplementClass() {
        TypeSpec typeSpec = buildSelectionFilterHandlerImpl();
        return JavaFile.builder(graphQLConfig.getPackageName(), typeSpec).build();
    }

    public TypeSpec buildSelectionFilterHandlerImpl() {
        return TypeSpec.classBuilder("SelectionFilter")
                .addMethods(buildTypeMethods())
                .addMethods(buildListTypeMethods())
                .build();
    }

    public List<MethodSpec> buildTypeMethods() {
        return manager.getObjects()
                .filter(objectTypeDefinitionContext ->
                        !manager.isQueryOperationType(objectTypeDefinitionContext.name().getText()) &&
                                !manager.isMutationOperationType(objectTypeDefinitionContext.name().getText()) &&
                                !manager.isSubscriptionOperationType(objectTypeDefinitionContext.name().getText())
                )
                .map(this::buildTypeMethod)
                .collect(Collectors.toList());
    }

    public MethodSpec buildTypeMethod(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        String typeParameterName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, objectTypeDefinitionContext.name().getText());
        MethodSpec.Builder builder = MethodSpec.methodBuilder(typeParameterName)
                .addModifiers(Modifier.PUBLIC)
                .returns(ClassName.get(JsonElement.class))
                .addParameter(ClassName.get(graphQLConfig.getObjectTypePackageName(), objectTypeDefinitionContext.name().getText()), typeParameterName)
                .addParameter(ClassName.get(GraphqlParser.SelectionSetContext.class), "selectionSet");

        builder.beginControlFlow("if (selectionSet != null && $L != null)", typeParameterName);
        builder.addStatement("$T jsonObject = new $T()", ClassName.get(JsonObject.class), ClassName.get(JsonObject.class));
        builder.beginControlFlow("for ($T selectionContext : selectionSet.selection())", ClassName.get(GraphqlParser.SelectionContext.class));
        objectTypeDefinitionContext.fieldsDefinition().fieldDefinition()
                .forEach(fieldDefinitionContext -> {
                            String fieldGetterMethodName = typeManager.getFieldGetterMethodName(fieldDefinitionContext);
                            String fieldParameterName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, manager.getFieldTypeName(fieldDefinitionContext.type()));
                            builder.beginControlFlow("if (selectionContext.field().name().getText().equals($S))", fieldDefinitionContext.name().getText());
                            if (manager.fieldTypeIsList(fieldDefinitionContext.type())) {
                                builder.addStatement("$T jsonArray = new $T()", ClassName.get(JsonArray.class), ClassName.get(JsonArray.class));
                                builder.beginControlFlow("if ($L.$L() != null)",
                                        typeParameterName,
                                        fieldGetterMethodName
                                );
                                if (manager.isScaLar(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
                                    builder.addStatement("$L.$L().forEach(item -> jsonArray.add(item))",
                                            typeParameterName,
                                            fieldGetterMethodName
                                    );
                                } else if (manager.isEnum(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
                                    builder.addStatement("$L.$L().forEach(item -> jsonArray.add(item.name()))",
                                            typeParameterName,
                                            fieldGetterMethodName
                                    );
                                } else if (manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
                                    builder.addStatement("$L.$L().forEach(item -> jsonArray.add($L(item,selectionContext.field().selectionSet())))",
                                            typeParameterName,
                                            fieldGetterMethodName,
                                            fieldParameterName
                                    );
                                }
                                builder.addStatement("jsonObject.add($S, jsonArray)", fieldDefinitionContext.name().getText());
                                builder.endControlFlow();
                            } else {
                                if (manager.isScaLar(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
                                    builder.addStatement("jsonObject.addProperty($S,$L.$L())",
                                            fieldDefinitionContext.name().getText(),
                                            typeParameterName,
                                            fieldGetterMethodName
                                    );
                                } else if (manager.isEnum(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
                                    builder.addStatement("jsonObject.addProperty($S,$L.$L().name())",
                                            fieldDefinitionContext.name().getText(),
                                            typeParameterName,
                                            fieldGetterMethodName
                                    );
                                } else if (manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
                                    builder.addStatement("jsonObject.add($S,$L($L.$L(),selectionContext.field().selectionSet()))",
                                            fieldDefinitionContext.name().getText(),
                                            fieldParameterName,
                                            typeParameterName,
                                            fieldGetterMethodName
                                    );
                                }
                            }
                            builder.endControlFlow();
                        }
                );

        builder.endControlFlow();
        builder.addStatement("return jsonObject");
        builder.endControlFlow();
        builder.addStatement("return $T.INSTANCE", ClassName.get(JsonNull.class));

        return builder.build();
    }

    public List<MethodSpec> buildListTypeMethods() {
        return manager.getObjects()
                .filter(objectTypeDefinitionContext ->
                        !manager.isQueryOperationType(objectTypeDefinitionContext.name().getText()) &&
                                !manager.isMutationOperationType(objectTypeDefinitionContext.name().getText()) &&
                                !manager.isSubscriptionOperationType(objectTypeDefinitionContext.name().getText())
                )
                .map(this::buildListTypeMethod)
                .collect(Collectors.toList());
    }

    public MethodSpec buildListTypeMethod(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        String typeParameterName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, objectTypeDefinitionContext.name().getText());
        String listTypeParameterName = typeParameterName.concat("List");

        MethodSpec.Builder builder = MethodSpec.methodBuilder(listTypeParameterName)
                .addModifiers(Modifier.PUBLIC)
                .returns(ClassName.get(JsonElement.class))
                .addParameter(ParameterizedTypeName.get(ClassName.get(Collection.class), ClassName.get(graphQLConfig.getObjectTypePackageName(), objectTypeDefinitionContext.name().getText())), listTypeParameterName)
                .addParameter(ClassName.get(GraphqlParser.SelectionSetContext.class), "selectionSet");

        builder.beginControlFlow("if (selectionSet != null && $L != null)", listTypeParameterName);
        builder.addStatement("$T jsonArray = new $T()", ClassName.get(JsonArray.class), ClassName.get(JsonArray.class));
        builder.addStatement("$L.forEach(item -> jsonArray.add($L(item, selectionSet)))",
                listTypeParameterName,
                typeParameterName
        );
        builder.addStatement("return jsonArray");
        builder.endControlFlow();
        builder.addStatement("return $T.INSTANCE", ClassName.get(JsonNull.class));
        return builder.build();
    }
}
