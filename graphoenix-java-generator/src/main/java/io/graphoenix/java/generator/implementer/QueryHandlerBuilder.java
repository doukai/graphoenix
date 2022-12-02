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
import io.graphoenix.java.generator.implementer.grpc.GrpcNameUtil;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.json.JsonValue;
import jakarta.json.spi.JsonProvider;
import org.tinylog.Logger;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@ApplicationScoped
public class QueryHandlerBuilder {

    private final IGraphQLDocumentManager manager;
    private final TypeManager typeManager;
    private final GrpcNameUtil grpcNameUtil;
    private GraphQLConfig graphQLConfig;

    @Inject
    public QueryHandlerBuilder(IGraphQLDocumentManager manager, TypeManager typeManager, GrpcNameUtil grpcNameUtil) {
        this.manager = manager;
        this.typeManager = typeManager;
        this.grpcNameUtil = grpcNameUtil;
    }

    public QueryHandlerBuilder setConfiguration(GraphQLConfig graphQLConfig) {
        this.graphQLConfig = graphQLConfig;
        this.typeManager.setGraphQLConfig(graphQLConfig);
        return this;
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
                .addMethods(buildTypeMethods());

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
                .filter(objectTypeDefinitionContext ->
                        !manager.isQueryOperationType(objectTypeDefinitionContext.name().getText()) &&
                                !manager.isMutationOperationType(objectTypeDefinitionContext.name().getText()) &&
                                !manager.isSubscriptionOperationType(objectTypeDefinitionContext.name().getText())
                )
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
                .addStatement("String selectionName = selectionContext.field().alias() == null ? selectionContext.field().name().getText() : selectionContext.field().alias().name().getText()");

        int index = 0;
        List<GraphqlParser.FieldDefinitionContext> fieldDefinitionContextList = objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                .filter(fieldDefinitionContext -> manager.isGrpcField(fieldDefinitionContext) || manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type())))
                .collect(Collectors.toList());

        for (GraphqlParser.FieldDefinitionContext fieldDefinitionContext : fieldDefinitionContextList) {
            String fieldParameterName = typeManager.typeToLowerCamelName(fieldDefinitionContext.type());
            if (index == 0) {
                builder.beginControlFlow("if (selectionContext.field().name().getText().equals($S))", fieldDefinitionContext.name().getText());
            } else {
                builder.nextControlFlow("else if (selectionContext.field().name().getText().equals($S))", fieldDefinitionContext.name().getText());
            }
            if (manager.fieldTypeIsList(fieldDefinitionContext.type())) {
                if (manager.isGrpcField(fieldDefinitionContext)) {
                    String typeName = manager.getFieldTypeName(fieldDefinitionContext.type());
                    String packageName = grpcNameUtil.getPackageName(fieldDefinitionContext);
                    String from = grpcNameUtil.getFrom(fieldDefinitionContext);
                    String to = grpcNameUtil.getTo(fieldDefinitionContext);

                    builder.beginControlFlow("if(jsonValue.asJsonObject().containsKey($S) && !jsonValue.asJsonObject().isNull($S))", from, from)
                            .addStatement("loader.registerArray($S, $S, $S, jsonValue.asJsonObject().get($S), jsonPointer + \"/\" + selectionName, selectionContext.field().selectionSet())",
                                    packageName,
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
                if (manager.isGrpcField(fieldDefinitionContext)) {
                    String typeName = manager.getFieldTypeName(fieldDefinitionContext.type());
                    String packageName = grpcNameUtil.getPackageName(fieldDefinitionContext);
                    String from = grpcNameUtil.getFrom(fieldDefinitionContext);
                    String to = grpcNameUtil.getTo(fieldDefinitionContext);

                    builder.beginControlFlow("if(jsonValue.asJsonObject().containsKey($S) && !jsonValue.asJsonObject().isNull($S))", from, from)
                            .addStatement("loader.register($S, $S, $S, jsonValue.asJsonObject().get($S), jsonPointer + \"/\" + selectionName, selectionContext.field().selectionSet())",
                                    packageName,
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
}