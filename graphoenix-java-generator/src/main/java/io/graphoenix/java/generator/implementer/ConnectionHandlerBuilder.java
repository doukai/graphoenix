package io.graphoenix.java.generator.implementer;

import com.google.gson.JsonElement;
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
import org.tinylog.Logger;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static io.graphoenix.spi.constant.Hammurabi.*;

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
                .addStatement("this.builder = builder");
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
                .addModifiers(Modifier.PUBLIC)
                .returns(ClassName.get(JsonElement.class))
                .addParameter(ClassName.get(JsonElement.class), "jsonElement")
                .addParameter(ClassName.get(String.class), "typeName");

        if (manager.isQueryOperationType(objectTypeDefinitionContext.name().getText()) ||
                manager.isMutationOperationType(objectTypeDefinitionContext.name().getText()) ||
                manager.isSubscriptionOperationType(objectTypeDefinitionContext.name().getText())) {
            builder.addParameter(ClassName.get(GraphqlParser.OperationDefinitionContext.class), "operationDefinitionContext");
        } else {
            builder.addParameter(ClassName.get(GraphqlParser.SelectionContext.class), "selectionContext");
        }

        if (objectTypeDefinitionContext.name().getText().endsWith(CONNECTION_SUFFIX)) {
            String typeName = objectTypeDefinitionContext.name().getText().substring(0, objectTypeDefinitionContext.name().getText().length() - CONNECTION_SUFFIX.length());
            builder.beginControlFlow("if (selectionContext.field().selectionSet() != null && selectionContext.field().selectionSet().selection().size() > 0)")
                    .addStatement("jsonElement.getAsJsonObject().add(selectionContext.field().name().getText(), builder.build(jsonElement, typeName, selectionContext))")
                    .beginControlFlow("for ($T subSelectionContext : selectionContext.field().selectionSet().selection().stream().flatMap(subSelectionContext -> manager.fragmentUnzip($S, subSelectionContext)).collect($T.toList()))",
                            ClassName.get(GraphqlParser.SelectionContext.class),
                            objectTypeDefinitionContext.name().getText(),
                            ClassName.get(Collectors.class)
                    )
                    .beginControlFlow("if (subSelectionContext.field().name().getText().equals($S))", "edges")
                    .addStatement("jsonElement.getAsJsonObject().get(selectionContext.field().name().getText()).getAsJsonObject().get($S).getAsJsonArray().forEach(item -> $L(item, $S, subSelectionContext))",
                            "edges",
                            getObjectMethodName(typeName.concat(EDGE_SUFFIX)),
                            objectTypeDefinitionContext.name().getText()
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

            List<GraphqlParser.FieldDefinitionContext> objectFieldList = manager.getFields(objectTypeDefinitionContext.name().getText())
                    .filter(fieldDefinitionContext -> manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type())))
                    .collect(Collectors.toList());

            int index = 0;
            for (GraphqlParser.FieldDefinitionContext fieldDefinitionContext : objectFieldList) {
                if (index == 0) {
                    builder.beginControlFlow("if (subSelectionContext.field().name().getText().equals($S))", fieldDefinitionContext.name().getText());
                } else {
                    builder.nextControlFlow("else if (subSelectionContext.field().name().getText().equals($S))", fieldDefinitionContext.name().getText());
                }
                if (manager.isConnectionField(objectTypeDefinitionContext.name().getText(), fieldDefinitionContext.name().getText())) {
                    builder.addStatement("$L(jsonElement, $S, subSelectionContext)",
                            getObjectMethodName(manager.getFieldTypeName(fieldDefinitionContext.type())),
                            objectTypeDefinitionContext.name().getText()
                    );
                } else if (!manager.fieldTypeIsList(fieldDefinitionContext.type())) {
                    builder.addStatement("$L(jsonElement.getAsJsonObject().get($S), $S, subSelectionContext)",
                            getObjectMethodName(manager.getFieldTypeName(fieldDefinitionContext.type())),
                            fieldDefinitionContext.name().getText(),
                            objectTypeDefinitionContext.name().getText()
                    );
                } else {
                    builder.addStatement("jsonElement.getAsJsonObject().get($S).getAsJsonArray().forEach(item -> $L(item, $S, subSelectionContext))",
                            fieldDefinitionContext.name().getText(),
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
        builder.addStatement("return jsonElement");
        return builder.build();
    }

    private String getObjectMethodName(String objectName) {
        return typeManager.typeToLowerCamelName(objectName);
    }
}
