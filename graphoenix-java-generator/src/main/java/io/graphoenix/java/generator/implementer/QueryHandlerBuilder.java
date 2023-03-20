package io.graphoenix.java.generator.implementer;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.handler.QueryDataLoader;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.json.spi.JsonProvider;
import org.tinylog.Logger;
import reactor.core.publisher.Mono;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@ApplicationScoped
public class QueryHandlerBuilder {

    private final IGraphQLDocumentManager manager;
    private final TypeManager typeManager;
    private final GraphQLConfig graphQLConfig;

    @Inject
    public QueryHandlerBuilder(IGraphQLDocumentManager manager, TypeManager typeManager, GraphQLConfig graphQLConfig) {
        this.manager = manager;
        this.typeManager = typeManager;
        this.graphQLConfig = graphQLConfig;
    }

    public void writeToFiler(Filer filer) throws IOException {
        this.buildClass().writeTo(filer);
        Logger.info("QueryHandler build success");
    }

    private JavaFile buildClass() {
        TypeSpec typeSpec = buildQueryHandler();

        return JavaFile.builder(graphQLConfig.getHandlerPackageName(), typeSpec).build();
    }

    private TypeSpec buildQueryHandler() {
        TypeSpec.Builder builder = TypeSpec.classBuilder("QueryAfterHandler")
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
                .addField(
                        FieldSpec.builder(
                                ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(JsonProvider.class)),
                                "jsonProvider",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).build()
                )
                .addMethod(buildConstructor())
                .addMethods(buildTypeMethods())
                .addMethod(buildHandleMethod());

        return builder.build();
    }

    private MethodSpec buildConstructor() {
        return MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Inject.class)
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(IGraphQLDocumentManager.class)), "manager")
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(JsonProvider.class)), "jsonProvider")
                .addStatement("this.manager = manager")
                .addStatement("this.jsonProvider = jsonProvider")
                .build();
    }

    private List<MethodSpec> buildTypeMethods() {
        return manager.getObjects()
                .filter(objectTypeDefinitionContext -> !manager.hasClassName(objectTypeDefinitionContext))
                .filter(objectTypeDefinitionContext -> !manager.isQueryOperationType(objectTypeDefinitionContext.name().getText()))
                .filter(objectTypeDefinitionContext -> !manager.isMutationOperationType(objectTypeDefinitionContext.name().getText()))
                .filter(objectTypeDefinitionContext -> !manager.isSubscriptionOperationType(objectTypeDefinitionContext.name().getText()))
                .flatMap(objectTypeDefinitionContext ->
                        Stream.of(
                                buildTypeMethod(objectTypeDefinitionContext),
                                buildListTypeMethod(objectTypeDefinitionContext)
                        )
                )
                .collect(Collectors.toList());
    }

    private MethodSpec buildTypeMethod(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        String typeParameterName = typeManager.typeToLowerCamelName(objectTypeDefinitionContext.name().getText());
        MethodSpec.Builder builder = MethodSpec.methodBuilder(typeParameterName)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get(JsonValue.class), "jsonValue")
                .addParameter(ClassName.get(GraphqlParser.SelectionSetContext.class), "selectionSet")
                .addParameter(ClassName.get(QueryDataLoader.class), "loader")
                .addParameter(ClassName.get(String.class), "jsonPointer");

        builder.beginControlFlow("if (selectionSet != null && jsonValue != null && jsonValue.getValueType().equals($T.ValueType.OBJECT))", ClassName.get(JsonValue.class))
                .beginControlFlow("for ($T selectionContext : selectionSet.selection().stream().flatMap(selectionContext -> manager.get().fragmentUnzip($S, selectionContext)).collect($T.toList()))",
                        ClassName.get(GraphqlParser.SelectionContext.class),
                        objectTypeDefinitionContext.name().getText(),
                        ClassName.get(Collectors.class)
                )
                .addStatement("String selectionName = $T.ofNullable(selectionContext.field().alias()).map(aliasContext -> aliasContext.name().getText()).orElse(selectionContext.field().name().getText())", ClassName.get(Optional.class));

        int index = 0;
        List<GraphqlParser.FieldDefinitionContext> fieldDefinitionContextList = objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                .filter(fieldDefinitionContext ->
                        manager.isFetchField(fieldDefinitionContext) ||
                                manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type())) && !manager.hasClassName(fieldDefinitionContext.type())
                )
                .collect(Collectors.toList());

        for (GraphqlParser.FieldDefinitionContext fieldDefinitionContext : fieldDefinitionContextList) {
            String fieldParameterName = typeManager.typeToLowerCamelName(fieldDefinitionContext.type());
            if (index == 0) {
                builder.beginControlFlow("if (selectionContext.field().name().getText().equals($S))", fieldDefinitionContext.name().getText());
            } else {
                builder.nextControlFlow("else if (selectionContext.field().name().getText().equals($S))", fieldDefinitionContext.name().getText());
            }
            if (manager.fieldTypeIsList(fieldDefinitionContext.type())) {
                if (manager.isFetchField(fieldDefinitionContext)) {
                    String typeName = manager.getFieldTypeName(fieldDefinitionContext.type());
                    String packageName = manager.getPackageName(typeName);
                    String protocol = manager.getProtocol(fieldDefinitionContext);
                    String from = manager.getFrom(fieldDefinitionContext);
                    String to = manager.getTo(fieldDefinitionContext);

                    builder.beginControlFlow("if(jsonValue.asJsonObject().containsKey($S) && !jsonValue.asJsonObject().isNull($S))", from, from)
                            .addStatement("loader.registerArray($S, $S, $S, $S, jsonValue.asJsonObject().get($S), jsonPointer + \"/\" + selectionName, selectionContext.field().selectionSet())",
                                    packageName,
                                    protocol,
                                    typeName,
                                    to,
                                    from
                            )
                            .endControlFlow();
                } else if (manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
                    builder.addStatement("$L(jsonValue.asJsonObject().get(selectionName), selectionContext.field().selectionSet(), loader, jsonPointer + \"/\" + selectionName)",
                            fieldParameterName.concat("List")
                    );
                }
            } else {
                if (manager.isFetchField(fieldDefinitionContext)) {
                    String typeName = manager.getFieldTypeName(fieldDefinitionContext.type());
                    String packageName = manager.getPackageName(typeName);
                    String protocol = manager.getProtocol(fieldDefinitionContext);
                    String from = manager.getFrom(fieldDefinitionContext);
                    String to = manager.getTo(fieldDefinitionContext);

                    builder.beginControlFlow("if(jsonValue.asJsonObject().containsKey($S) && !jsonValue.asJsonObject().isNull($S))", from, from)
                            .addStatement("loader.register($S, $S, $S, $S, jsonValue.asJsonObject().get($S), jsonPointer + \"/\" + selectionName, selectionContext.field().selectionSet())",
                                    packageName,
                                    protocol,
                                    typeName,
                                    to,
                                    from
                            )
                            .endControlFlow();
                } else if (manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
                    builder.addStatement("$L(jsonValue.asJsonObject().get(selectionName), selectionContext.field().selectionSet(), loader, jsonPointer + \"/\" + selectionName)",
                            fieldParameterName
                    );
                }
            }
            if (index == fieldDefinitionContextList.size() - 1) {
                builder.endControlFlow();
            }
            index++;
        }
        builder.endControlFlow()
                .endControlFlow();
        return builder.build();
    }

    private MethodSpec buildListTypeMethod(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        String typeParameterName = typeManager.typeToLowerCamelName(objectTypeDefinitionContext.name().getText());
        String listTypeParameterName = typeParameterName.concat("List");
        MethodSpec.Builder builder = MethodSpec.methodBuilder(listTypeParameterName)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get(JsonValue.class), "jsonValue")
                .addParameter(ClassName.get(GraphqlParser.SelectionSetContext.class), "selectionSet")
                .addParameter(ClassName.get(QueryDataLoader.class), "loader")
                .addParameter(ClassName.get(String.class), "jsonPointer");

        builder.beginControlFlow("if (selectionSet != null && jsonValue != null && jsonValue.getValueType().equals($T.ValueType.ARRAY))", ClassName.get(JsonValue.class))
                .addStatement("$T.range(0, jsonValue.asJsonArray().size()).forEach(index -> $L(jsonValue.asJsonArray().get(index), selectionSet, loader, jsonPointer + \"/\" + index))",
                        ClassName.get(IntStream.class),
                        typeManager.typeToLowerCamelName(objectTypeDefinitionContext.name().getText())
                )
                .endControlFlow();
        return builder.build();
    }

    private MethodSpec buildHandleMethod() {

        MethodSpec.Builder builder = MethodSpec.methodBuilder("handle")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get(JsonObject.class), "jsonObject")
                .addParameter(ClassName.get(GraphqlParser.OperationDefinitionContext.class), "operationDefinition")
                .addParameter(ClassName.get(QueryDataLoader.class), "loader")
                .returns(ParameterizedTypeName.get(ClassName.get(Mono.class), ClassName.get(JsonValue.class)));


        builder.beginControlFlow("for ($T selectionContext : operationDefinition.selectionSet().selection()) ", ClassName.get(GraphqlParser.SelectionContext.class))
                .addStatement("String selectionName = $T.ofNullable(selectionContext.field().alias()).map(aliasContext -> aliasContext.name().getText()).orElse(selectionContext.field().name().getText())", ClassName.get(Optional.class));
        List<GraphqlParser.FieldDefinitionContext> fieldDefinitionContextList = manager.getMutationOperationTypeName().flatMap(manager::getObject).stream()
                .flatMap(objectTypeDefinitionContext ->
                        objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                                .filter(fieldDefinitionContext -> manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type())))
                )
                .collect(Collectors.toList());

        int index = 0;
        for (GraphqlParser.FieldDefinitionContext fieldDefinitionContext : fieldDefinitionContextList) {
            String typeMethodName = fieldDefinitionContext.name().getText();
            if (index == 0) {
                builder.beginControlFlow("if (selectionContext.field().name().getText().equals($S))", typeMethodName);
            } else {
                builder.nextControlFlow("else if (selectionContext.field().name().getText().equals($S))", typeMethodName);
            }
            builder.addStatement("$L(jsonObject.get(selectionName), selectionContext.field().selectionSet(), loader, \"/\" + selectionName)", typeMethodName);
            index++;
        }
        builder.endControlFlow()
                .endControlFlow()
                .addStatement("return loader.load(jsonObject)");
        return builder.build();
    }
}
