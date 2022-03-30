package io.graphoenix.java.generator.implementer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.tinylog.Logger;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class SelectionFilterBuilder {

    private final IGraphQLDocumentManager manager;
    private final TypeManager typeManager;
    private GraphQLConfig graphQLConfig;

    @Inject
    public SelectionFilterBuilder(IGraphQLDocumentManager manager, TypeManager typeManager) {
        this.manager = manager;
        this.typeManager = typeManager;
    }

    public SelectionFilterBuilder setConfiguration(GraphQLConfig graphQLConfig) {
        this.graphQLConfig = graphQLConfig;
        this.typeManager.setGraphQLConfig(graphQLConfig);
        return this;
    }

    public void writeToFiler(Filer filer) throws IOException {
        this.buildClass().writeTo(filer);
        Logger.info("SelectionFilter build success");
    }

    private JavaFile buildClass() {
        TypeSpec typeSpec = buildSelectionFilter();
        return JavaFile.builder(graphQLConfig.getHandlerPackageName(), typeSpec).build();
    }

    private TypeSpec buildSelectionFilter() {
        return TypeSpec.classBuilder("SelectionFilter")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(ApplicationScoped.class)
                .addField(
                        FieldSpec.builder(
                                ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(IGraphQLDocumentManager.class)),
                                "manager",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).build()
                )
                .addMethod(buildConstructor())
                .addMethods(buildTypeMethods())
                .addMethods(buildListTypeMethods())
                .build();
    }

    private MethodSpec buildConstructor() {
        return MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Inject.class)
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(IGraphQLDocumentManager.class)), "manager")
                .addStatement("this.manager = manager")
                .build();
    }

    private List<MethodSpec> buildTypeMethods() {
        return manager.getObjects()
                .filter(objectTypeDefinitionContext ->
                        !manager.isQueryOperationType(objectTypeDefinitionContext.name().getText()) &&
                                !manager.isMutationOperationType(objectTypeDefinitionContext.name().getText()) &&
                                !manager.isSubscriptionOperationType(objectTypeDefinitionContext.name().getText())
                )
                .map(this::buildTypeMethod)
                .collect(Collectors.toList());
    }

    private MethodSpec buildTypeMethod(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        String typeParameterName = typeManager.typeToLowerCamelName(objectTypeDefinitionContext.name().getText());
        MethodSpec.Builder builder = MethodSpec.methodBuilder(typeParameterName)
                .addModifiers(Modifier.PUBLIC)
                .returns(ClassName.get(JsonElement.class))
                .addParameter(ClassName.get(graphQLConfig.getObjectTypePackageName(), objectTypeDefinitionContext.name().getText()), typeParameterName)
                .addParameter(ClassName.get(GraphqlParser.SelectionSetContext.class), "selectionSet");

        builder.beginControlFlow("if (selectionSet != null && $L != null)", typeParameterName)
                .addStatement("$T jsonObject = new $T()", ClassName.get(JsonObject.class), ClassName.get(JsonObject.class))
                .beginControlFlow("for ($T selectionContext : selectionSet.selection().stream().flatMap(selectionContext -> manager.get().fragmentUnzip($S, selectionContext)).collect($T.toList()))",
                        ClassName.get(GraphqlParser.SelectionContext.class),
                        objectTypeDefinitionContext.name().getText(),
                        ClassName.get(Collectors.class)
                )
                .addStatement("String selectionName = selectionContext.field().alias() == null ? selectionContext.field().name().getText() : selectionContext.field().alias().name().getText()");

        objectTypeDefinitionContext.fieldsDefinition().fieldDefinition()
                .forEach(fieldDefinitionContext -> {
                            String fieldGetterMethodName = typeManager.getFieldGetterMethodName(fieldDefinitionContext);
                            String fieldParameterName = typeManager.typeToLowerCamelName(fieldDefinitionContext.type());
                            builder.beginControlFlow("if (selectionContext.field().name().getText().equals($S))", fieldDefinitionContext.name().getText());

                            if (manager.fieldTypeIsList(fieldDefinitionContext.type())) {
                                builder.addStatement("$T jsonArray = new $T()", ClassName.get(JsonArray.class), ClassName.get(JsonArray.class))
                                        .beginControlFlow("if ($L.$L() != null)",
                                                typeParameterName,
                                                fieldGetterMethodName
                                        );
                                if (manager.isScalar(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
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
                                    builder.addStatement("$L.$L().forEach(item -> jsonArray.add($L(item, selectionContext.field().selectionSet())))",
                                            typeParameterName,
                                            fieldGetterMethodName,
                                            fieldParameterName
                                    );
                                }
                                builder.addStatement("jsonObject.add(selectionName, jsonArray)")
                                        .nextControlFlow("else")
                                        .addStatement("jsonObject.add(selectionName, $T.INSTANCE)", ClassName.get(JsonNull.class))
                                        .endControlFlow();
                            } else {
                                if (manager.isScalar(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
                                    builder.addStatement("jsonObject.addProperty(selectionName, $L.$L())",
                                            typeParameterName,
                                            fieldGetterMethodName
                                    );
                                } else if (manager.isEnum(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
                                    builder.addStatement("jsonObject.addProperty(selectionName, $L.$L() == null ? null : $L.$L().name())",
                                            typeParameterName,
                                            fieldGetterMethodName,
                                            typeParameterName,
                                            fieldGetterMethodName
                                    );
                                } else if (manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
                                    builder.addStatement("jsonObject.add(selectionName ,$L($L.$L(),selectionContext.field().selectionSet()))",
                                            fieldParameterName,
                                            typeParameterName,
                                            fieldGetterMethodName
                                    );
                                }
                            }
                            builder.endControlFlow();
                        }
                );
        builder.endControlFlow()
                .addStatement("return jsonObject")
                .endControlFlow()
                .addStatement("return $T.INSTANCE", ClassName.get(JsonNull.class));
        return builder.build();
    }

    private List<MethodSpec> buildListTypeMethods() {
        return manager.getObjects()
                .filter(objectTypeDefinitionContext ->
                        !manager.isQueryOperationType(objectTypeDefinitionContext.name().getText()) &&
                                !manager.isMutationOperationType(objectTypeDefinitionContext.name().getText()) &&
                                !manager.isSubscriptionOperationType(objectTypeDefinitionContext.name().getText())
                )
                .map(this::buildListTypeMethod)
                .collect(Collectors.toList());
    }

    private MethodSpec buildListTypeMethod(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        String typeParameterName = typeManager.typeToLowerCamelName(objectTypeDefinitionContext.name().getText());
        String listTypeParameterName = typeParameterName.concat("List");
        ClassName typeClassName = ClassName.get(graphQLConfig.getObjectTypePackageName(), objectTypeDefinitionContext.name().getText());

        MethodSpec.Builder builder = MethodSpec.methodBuilder(listTypeParameterName)
                .addModifiers(Modifier.PUBLIC)
                .returns(ClassName.get(JsonElement.class))
                .addParameter(ParameterizedTypeName.get(ClassName.get(Collection.class), typeClassName), listTypeParameterName)
                .addParameter(ClassName.get(GraphqlParser.SelectionSetContext.class), "selectionSet");

        builder.beginControlFlow("if (selectionSet != null && $L != null)", listTypeParameterName)
                .addStatement("$T jsonArray = new $T()", ClassName.get(JsonArray.class), ClassName.get(JsonArray.class))
                .addStatement("$L.forEach(item -> jsonArray.add($L(item, selectionSet)))",
                        listTypeParameterName,
                        typeParameterName
                )
                .addStatement("return jsonArray")
                .endControlFlow()
                .addStatement("return $T.INSTANCE", ClassName.get(JsonNull.class));
        return builder.build();
    }
}