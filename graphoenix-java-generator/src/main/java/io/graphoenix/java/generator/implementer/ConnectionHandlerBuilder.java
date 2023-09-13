package io.graphoenix.java.generator.implementer;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.handler.ConnectionBuilder;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.JsonArray;
import jakarta.json.JsonPatchBuilder;
import jakarta.json.JsonValue;
import jakarta.json.spi.JsonProvider;
import org.tinylog.Logger;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static io.graphoenix.spi.constant.Hammurabi.CONNECTION_SUFFIX;
import static io.graphoenix.spi.constant.Hammurabi.EDGE_SUFFIX;
import static io.graphoenix.spi.constant.Hammurabi.PAGE_INFO_NAME;

@ApplicationScoped
public class ConnectionHandlerBuilder {

    private final IGraphQLDocumentManager manager;
    private final TypeManager typeManager;
    private final GraphQLConfig graphQLConfig;

    @Inject
    public ConnectionHandlerBuilder(IGraphQLDocumentManager manager, TypeManager typeManager, GraphQLConfig graphQLConfig) {
        this.manager = manager;
        this.typeManager = typeManager;
        this.graphQLConfig = graphQLConfig;
    }

    public void writeToFiler(Filer filer) throws IOException {
        this.buildClass().writeTo(filer);
        Logger.info("ConnectionHandler build success");
    }

    private JavaFile buildClass() {
        TypeSpec typeSpec = buildConnectionHandler();
        return JavaFile.builder(graphQLConfig.getHandlerPackageName(), typeSpec).build();
    }

    private TypeSpec buildConnectionHandler() {
        return TypeSpec.classBuilder("ConnectionHandler")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(ApplicationScoped.class)
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
                                ClassName.get(ConnectionBuilder.class),
                                "builder",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).build()
                )
                .addField(
                        FieldSpec.builder(
                                ClassName.get(JsonProvider.class),
                                "jsonProvider",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).build()
                )
                .addMethod(buildConstructor())
                .addMethods(buildTypeConnectionMethods())
                .build();
    }

    private MethodSpec buildConstructor() {
        MethodSpec.Builder builder = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Inject.class)
                .addParameter(ClassName.get(IGraphQLDocumentManager.class), "manager")
                .addStatement("this.manager = manager")
                .addParameter(ClassName.get(ConnectionBuilder.class), "builder")
                .addStatement("this.builder = builder")
                .addParameter(ClassName.get(JsonProvider.class), "jsonProvider")
                .addStatement("this.jsonProvider = jsonProvider");
        return builder.build();
    }

    private List<MethodSpec> buildTypeConnectionMethods() {
        return manager.getObjects()
                .filter(objectTypeDefinitionContext -> !objectTypeDefinitionContext.name().getText().equals(PAGE_INFO_NAME))
                .map(this::buildTypeConnectionMethod).collect(Collectors.toList());
    }

    private MethodSpec buildTypeConnectionMethod(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        String fieldName = typeManager.typeToLowerCamelName(objectTypeDefinitionContext.name().getText());
        MethodSpec.Builder builder = MethodSpec.methodBuilder(fieldName)
                .addModifiers(Modifier.PUBLIC);

        if (manager.isOperationType(objectTypeDefinitionContext)) {
            builder.returns(ClassName.get(JsonValue.class))
                    .addParameter(ClassName.get(JsonValue.class), "jsonValue")
                    .addParameter(ClassName.get(GraphqlParser.OperationDefinitionContext.class), "operationDefinitionContext")
                    .addStatement("$T jsonPatchBuilder = jsonProvider.createPatchBuilder()", ClassName.get(JsonPatchBuilder.class))
                    .addStatement("$T path = \"\"", ClassName.get(String.class));
        } else {
            builder.addParameter(ClassName.get(JsonPatchBuilder.class), "jsonPatchBuilder")
                    .addParameter(ClassName.get(String.class), "path")
                    .addParameter(ClassName.get(JsonValue.class), "jsonValue")
                    .addParameter(ClassName.get(String.class), "typeName")
                    .addParameter(ClassName.get(GraphqlParser.SelectionContext.class), "parentSelectionContext");
        }

        if (objectTypeDefinitionContext.name().getText().endsWith(CONNECTION_SUFFIX)) {
            String typeName = objectTypeDefinitionContext.name().getText().substring(0, objectTypeDefinitionContext.name().getText().length() - CONNECTION_SUFFIX.length());
            builder.beginControlFlow("if (jsonValue != null && !jsonValue.getValueType().equals($T.ValueType.NULL) && parentSelectionContext.field().selectionSet() != null && parentSelectionContext.field().selectionSet().selection().size() > 0)", ClassName.get(JsonValue.class))
                    .addStatement("$T connection = builder.build(jsonValue, typeName, parentSelectionContext)", ClassName.get(JsonValue.class))
                    .addStatement("jsonPatchBuilder.add(path, connection)")
//                    .beginControlFlow("for ($T selectionContext : parentSelectionContext.field().selectionSet().selection().stream().flatMap(selectionContext -> manager.fragmentUnzip($S, selectionContext)).collect($T.toList()))",
                    .beginControlFlow("for ($T selectionContext : parentSelectionContext.field().selectionSet().selection())",
                            ClassName.get(GraphqlParser.SelectionContext.class)
                    )
                    .beginControlFlow("if (selectionContext.field().name().getText().equals($S))", "edges")
                    .addStatement("String selectionName = $T.ofNullable(selectionContext.field().alias()).map(aliasContext -> aliasContext.name().getText()).orElse(selectionContext.field().name().getText())", ClassName.get(Optional.class))
                    .addStatement("$T jsonArray = connection.asJsonObject().get(selectionName).asJsonArray()", ClassName.get(JsonArray.class))
                    .addStatement("$T.range(0, jsonArray.size()).forEach(index -> $L(jsonPatchBuilder, path + \"/\" + selectionName + \"/\" + index, jsonArray.get(index), $S, selectionContext))",
                            ClassName.get(IntStream.class),
                            getObjectMethodName(typeName + EDGE_SUFFIX),
                            objectTypeDefinitionContext.name().getText()
                    )
                    .endControlFlow()
                    .endControlFlow()
                    .endControlFlow();
        } else {
            if (manager.isOperationType(objectTypeDefinitionContext)) {
                builder.beginControlFlow("if (operationDefinitionContext.selectionSet() != null && operationDefinitionContext.selectionSet().selection().size() > 0)")
//                        .beginControlFlow("for ($T selectionContext : operationDefinitionContext.selectionSet().selection().stream().flatMap(selectionContext -> manager.fragmentUnzip($S, selectionContext)).collect($T.toList()))",
                        .beginControlFlow("for ($T selectionContext : operationDefinitionContext.selectionSet().selection())",
                                ClassName.get(GraphqlParser.SelectionContext.class)
                        );
            } else {
                builder.beginControlFlow("if (jsonValue != null && !jsonValue.getValueType().equals($T.ValueType.NULL) && parentSelectionContext.field().selectionSet() != null && parentSelectionContext.field().selectionSet().selection().size() > 0)", ClassName.get(JsonValue.class))
//                        .beginControlFlow("for ($T selectionContext : parentSelectionContext.field().selectionSet().selection().stream().flatMap(selectionContext -> manager.fragmentUnzip($S, selectionContext)).collect($T.toList()))",
                        .beginControlFlow("for ($T selectionContext : parentSelectionContext.field().selectionSet().selection())",
                                ClassName.get(GraphqlParser.SelectionContext.class)
                        );
            }
            builder.addStatement("String selectionName = $T.ofNullable(selectionContext.field().alias()).map(aliasContext -> aliasContext.name().getText()).orElse(selectionContext.field().name().getText())", ClassName.get(Optional.class));
            List<GraphqlParser.FieldDefinitionContext> objectFieldList = manager.getFields(objectTypeDefinitionContext.name().getText())
                    .filter(fieldDefinitionContext -> manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type())))
                    .filter(manager::isNotInvokeField)
                    .filter(manager::isNotFetchField)
                    .collect(Collectors.toList());

            int index = 0;
            for (GraphqlParser.FieldDefinitionContext fieldDefinitionContext : objectFieldList) {
                if (index == 0) {
                    builder.beginControlFlow("if (selectionContext.field().name().getText().equals($S))", fieldDefinitionContext.name().getText());
                } else {
                    builder.nextControlFlow("else if (selectionContext.field().name().getText().equals($S))", fieldDefinitionContext.name().getText());
                }
                if (manager.isConnectionField(objectTypeDefinitionContext.name().getText(), fieldDefinitionContext.name().getText())) {
                    builder.addStatement("$L(jsonPatchBuilder, path + \"/\" + selectionName, jsonValue, $S, selectionContext)",
                            getObjectMethodName(manager.getFieldTypeName(fieldDefinitionContext.type())),
                            objectTypeDefinitionContext.name().getText()
                    );
                } else if (!manager.fieldTypeIsList(fieldDefinitionContext.type())) {
                    builder.addStatement("$L(jsonPatchBuilder, path + \"/\" + selectionName, jsonValue.asJsonObject().get(selectionName), $S, selectionContext)",
                            getObjectMethodName(manager.getFieldTypeName(fieldDefinitionContext.type())),
                            objectTypeDefinitionContext.name().getText()
                    );
                } else {
                    builder.addStatement("$T jsonArray = jsonValue.asJsonObject().get(selectionName).asJsonArray()", ClassName.get(JsonArray.class))
                            .addStatement("$T.range(0, jsonArray.size()).forEach(index -> $L(jsonPatchBuilder, path + \"/\" + selectionName + \"/\" + index, jsonArray.get(index), $S, selectionContext))",
                                    ClassName.get(IntStream.class),
                                    getObjectMethodName(manager.getFieldTypeName(fieldDefinitionContext.type())),
                                    objectTypeDefinitionContext.name().getText()
                            );
                }
                if (index == objectFieldList.size() - 1) {
                    builder.endControlFlow();
                }
                index++;
            }
            builder.endControlFlow()
                    .endControlFlow();
        }
        if (manager.isOperationType(objectTypeDefinitionContext)) {
            builder.addStatement("return jsonPatchBuilder.build().apply(jsonValue.asJsonObject())");
        }
        return builder.build();
    }

    private String getObjectMethodName(String objectName) {
        return typeManager.typeToLowerCamelName(objectName);
    }
}
