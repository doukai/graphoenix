package io.graphoenix.java.generator.implementer;

import com.google.gson.JsonElement;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.error.GraphQLProblem;
import io.graphoenix.core.handler.ConnectionBuilder;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.tinylog.Logger;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static io.graphoenix.core.error.GraphQLErrorType.TYPE_ID_FIELD_NOT_EXIST;

@ApplicationScoped
public class ConnectionHandlerBuilder {

    private final IGraphQLDocumentManager manager;
    private final TypeManager typeManager;
    private GraphQLConfig graphQLConfig;

    @Inject
    public ConnectionHandlerBuilder(IGraphQLDocumentManager manager, TypeManager typeManager) {
        this.manager = manager;
        this.typeManager = typeManager;
    }

    public ConnectionHandlerBuilder setConfiguration(GraphQLConfig graphQLConfig) {
        this.graphQLConfig = graphQLConfig;
        this.typeManager.setGraphQLConfig(graphQLConfig);
        return this;
    }

    public void writeToFiler(Filer filer) throws IOException {
        this.buildClass().writeTo(filer);
        Logger.info("ConnectionHandler build success");
    }

    private JavaFile buildClass() {
        TypeSpec typeSpec = buildInvokeHandler();
        return JavaFile.builder(graphQLConfig.getHandlerPackageName(), typeSpec).build();
    }

    private TypeSpec buildInvokeHandler() {
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
                .addMethod(buildConstructor())
                .addMethods(buildTypeInvokeMethods())
                .build();
    }

    private MethodSpec buildConstructor() {
        MethodSpec.Builder builder = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Inject.class)
                .addParameter(ClassName.get(IGraphQLDocumentManager.class), "manager")
                .addStatement("this.manager = manager")
                .addParameter(ClassName.get(ConnectionBuilder.class), "builder")
                .addStatement("this.builder = builder");
        return builder.build();
    }

    private List<MethodSpec> buildTypeInvokeMethods() {
        return manager.getObjects()
                .filter(objectTypeDefinitionContext -> !objectTypeDefinitionContext.name().getText().equals("PageInfo"))
                .map(this::buildTypeInvokeMethod).collect(Collectors.toList());
    }

    private MethodSpec buildTypeInvokeMethod(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        String fieldName = typeManager.typeToLowerCamelName(objectTypeDefinitionContext.name().getText());
        MethodSpec.Builder builder = MethodSpec.methodBuilder(fieldName)
                .addModifiers(Modifier.PUBLIC)
                .returns(ClassName.get(JsonElement.class))
                .addParameter(ClassName.get(JsonElement.class), "jsonElement");

        if (manager.isQueryOperationType(objectTypeDefinitionContext.name().getText()) ||
                manager.isMutationOperationType(objectTypeDefinitionContext.name().getText()) ||
                manager.isSubscriptionOperationType(objectTypeDefinitionContext.name().getText())) {
            builder.addParameter(ClassName.get(GraphqlParser.OperationDefinitionContext.class), "operationDefinitionContext");
        } else {
            builder.addParameter(ClassName.get(GraphqlParser.SelectionContext.class), "selectionContext");
        }

        if (objectTypeDefinitionContext.name().getText().endsWith("Connection")) {
            String typeName = objectTypeDefinitionContext.name().getText().substring(0, objectTypeDefinitionContext.name().getText().length() - "Connection".length());
            String idFieldName = manager.getObjectTypeIDFieldName(typeName)
                    .orElseThrow(() -> new GraphQLProblem(TYPE_ID_FIELD_NOT_EXIST.bind(typeName)));
            String cursorFieldName = manager.getFieldByDirective(typeName, "cursor")
                    .findFirst()
                    .or(() -> manager.getObjectTypeIDFieldDefinition(typeName))
                    .orElseThrow(() -> new GraphQLProblem(TYPE_ID_FIELD_NOT_EXIST.bind(typeName)))
                    .name()
                    .getText();

            builder.addStatement("jsonElement.getAsJsonObject().add($S, builder.build(jsonElement, $S, $S, $S, selectionContext))",
                            fieldName,
                            typeName,
                            idFieldName,
                            cursorFieldName
                    )
                    .beginControlFlow("if (selectionContext.field().selectionSet() != null && selectionContext.field().selectionSet().selection().size() > 0)")
                    .beginControlFlow("for ($T subSelectionContext : selectionContext.field().selectionSet().selection().stream().flatMap(subSelectionContext -> manager.fragmentUnzip($S, subSelectionContext)).collect($T.toList()))",
                            ClassName.get(GraphqlParser.SelectionContext.class),
                            objectTypeDefinitionContext.name().getText(),
                            ClassName.get(Collectors.class)
                    )
                    .beginControlFlow("if (subSelectionContext.field().name().getText().equals($S))", "edges")
                    .addStatement("jsonElement.getAsJsonObject().get($S).getAsJsonObject().get($S).getAsJsonArray().forEach(item -> $L(item, subSelectionContext))",
                            fieldName,
                            "edges",
                            getObjectMethodName(typeName.concat("Edge"))
                    )
                    .endControlFlow()
                    .endControlFlow()
                    .endControlFlow();
        } else {

            if (manager.isQueryOperationType(objectTypeDefinitionContext.name().getText()) ||
                    manager.isMutationOperationType(objectTypeDefinitionContext.name().getText()) ||
                    manager.isSubscriptionOperationType(objectTypeDefinitionContext.name().getText())) {
                builder.beginControlFlow("if (operationDefinitionContext.selectionSet() != null && operationDefinitionContext.selectionSet().selection().size() > 0)")
                        .beginControlFlow("for ($T subSelectionContext : operationDefinitionContext.selectionSet().selection().stream().flatMap(subSelectionContext -> manager.fragmentUnzip($S, subSelectionContext)).collect($T.toList()))",
                                ClassName.get(GraphqlParser.SelectionContext.class),
                                objectTypeDefinitionContext.name().getText(),
                                ClassName.get(Collectors.class)
                        );
            } else {
                builder.beginControlFlow("if (selectionContext.field().selectionSet() != null && selectionContext.field().selectionSet().selection().size() > 0)")
                        .beginControlFlow("for ($T subSelectionContext : selectionContext.field().selectionSet().selection().stream().flatMap(subSelectionContext -> manager.fragmentUnzip($S, subSelectionContext)).collect($T.toList()))",
                                ClassName.get(GraphqlParser.SelectionContext.class),
                                objectTypeDefinitionContext.name().getText(),
                                ClassName.get(Collectors.class)
                        );
            }

            manager.getFields(objectTypeDefinitionContext.name().getText())
                    .filter(fieldDefinitionContext -> manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type())))
                    .filter(fieldDefinitionContext -> !manager.fieldTypeIsList(fieldDefinitionContext.type()))
                    .forEach(fieldDefinitionContext -> {
                                builder.beginControlFlow("if (subSelectionContext.field().name().getText().equals($S))", fieldDefinitionContext.name().getText());
                                if (manager.isConnectionField(objectTypeDefinitionContext.name().getText(), fieldDefinitionContext.name().getText())) {
                                    builder.addStatement("$L(jsonElement, subSelectionContext)",
                                            getObjectMethodName(manager.getFieldTypeName(fieldDefinitionContext.type()))
                                    );
                                } else {
                                    builder.addStatement("$L(jsonElement.getAsJsonObject().get($S), subSelectionContext)",
                                            getObjectMethodName(manager.getFieldTypeName(fieldDefinitionContext.type())),
                                            fieldDefinitionContext.name().getText()
                                    );
                                }
                                builder.endControlFlow();
                            }
                    );

            manager.getFields(objectTypeDefinitionContext.name().getText())
                    .filter(fieldDefinitionContext -> manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type())))
                    .filter(fieldDefinitionContext -> manager.fieldTypeIsList(fieldDefinitionContext.type()))
                    .forEach(fieldDefinitionContext ->
                            builder.beginControlFlow("if (subSelectionContext.field().name().getText().equals($S))", fieldDefinitionContext.name().getText())
                                    .addStatement("jsonElement.getAsJsonObject().get($S).getAsJsonArray().forEach(item -> $L(item, subSelectionContext))",
                                            fieldDefinitionContext.name().getText(),
                                            getObjectMethodName(manager.getFieldTypeName(fieldDefinitionContext.type()))
                                    )
                                    .endControlFlow().build()
                    );
            builder.endControlFlow()
                    .endControlFlow();
        }
        builder.addStatement("return jsonElement");
        return builder.build();
    }

    private String getObjectMethodName(String objectName) {
        return typeManager.typeToLowerCamelName(objectName);
    }
}
