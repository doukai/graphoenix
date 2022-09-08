package io.graphoenix.java.generator.implementer.grpc;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.java.generator.implementer.TypeManager;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;
import jakarta.json.spi.JsonProvider;
import jakarta.json.stream.JsonCollectors;
import org.tinylog.Logger;
import reactor.core.publisher.Mono;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@ApplicationScoped
public class GrpcQueryDataLoaderBuilder {

    private final IGraphQLDocumentManager manager;
    private final TypeManager typeManager;
    private GraphQLConfig graphQLConfig;
    private final GrpcNameUtil grpcNameUtil;
    private final Map<String, Map<String, Set<String>>> typeMap;

    @Inject
    public GrpcQueryDataLoaderBuilder(IGraphQLDocumentManager manager, TypeManager typeManager, GrpcNameUtil grpcNameUtil) {
        this.manager = manager;
        this.typeManager = typeManager;
        this.grpcNameUtil = grpcNameUtil;
        this.typeMap = manager.getObjects()
                .flatMap(objectTypeDefinitionContext -> objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream())
                .filter(manager::isGrpcField)
                .map(fieldDefinitionContext -> new AbstractMap.SimpleEntry<>(grpcNameUtil.getPackageName(fieldDefinitionContext), new AbstractMap.SimpleEntry<>(manager.getFieldTypeName(fieldDefinitionContext.type()), grpcNameUtil.getTo(fieldDefinitionContext))))
                .collect(
                        Collectors.groupingBy(
                                AbstractMap.SimpleEntry<String, AbstractMap.SimpleEntry<String, String>>::getKey,
                                Collectors.mapping(
                                        AbstractMap.SimpleEntry<String, AbstractMap.SimpleEntry<String, String>>::getValue,
                                        Collectors.toSet()
                                )
                        )
                )
                .entrySet().stream()
                .collect(
                        Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> entry.getValue().stream()
                                        .collect(
                                                Collectors.groupingBy(
                                                        AbstractMap.SimpleEntry<String, String>::getKey,
                                                        Collectors.mapping(
                                                                AbstractMap.SimpleEntry<String, String>::getValue,
                                                                Collectors.toSet()
                                                        )
                                                )
                                        )
                        )
                );
    }

    public GrpcQueryDataLoaderBuilder setConfiguration(GraphQLConfig graphQLConfig) {
        this.graphQLConfig = graphQLConfig;
        this.typeManager.setGraphQLConfig(graphQLConfig);
        return this;
    }

    public void writeToFiler(Filer filer) throws IOException {
        this.buildClass().writeTo(filer);
        Logger.info("GrpcQueryDataLoader build success");
    }

    private JavaFile buildClass() {
        TypeSpec typeSpec = buildGrpcQueryDataLoader();
        return JavaFile.builder(graphQLConfig.getHandlerPackageName(), typeSpec).build();
    }

    private TypeSpec buildGrpcQueryDataLoader() {
        TypeSpec.Builder builder = TypeSpec.classBuilder("GrpcQueryDataLoader")
                .superclass(ClassName.get("io.graphoenix.grpc.client", "GrpcBaseQueryDataLoader"))
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(RequestScoped.class)
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
                .addField(
                        FieldSpec.builder(
                                ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get("io.graphoenix.grpc.client", "ChannelManager")),
                                "channelManager",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).build()
                )
                .addMethod(buildConstructor())
                .addMethods(buildGrpcTypeMethods())
                .addMethods(buildGrpcTypeListMethods())
                .addMethod(buildDispatchMethod())
                .addMethods(buildTypeMethods());

        typeMap.keySet().forEach(packageName ->
                builder.addField(
                        FieldSpec.builder(
                                ClassName.get(packageName, "ReactorGraphQLServiceGrpc", "ReactorGraphQLServiceStub"),
                                grpcNameUtil.getGraphQLServiceStubParameterName(packageName),
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).build()
                ).addField(
                        FieldSpec.builder(
                                ParameterizedTypeName.get(Mono.class, String.class),
                                grpcNameUtil.packageNameToUnderline(packageName).concat("_JsonMono"),
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).build()
                )
        );
        return builder.build();
    }

    private MethodSpec buildConstructor() {
        MethodSpec.Builder builder = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Inject.class)
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(IGraphQLDocumentManager.class)), "manager")
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(JsonProvider.class)), "jsonProvider")
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get("io.graphoenix.grpc.client", "ChannelManager")), "channelManager")
                .addStatement("this.manager = manager")
                .addStatement("this.jsonProvider = jsonProvider")
                .addStatement("this.channelManager = channelManager");

        this.typeMap.keySet().forEach(packageName ->
                builder.addStatement("this.$L = $T.newReactorStub(channelManager.get().getChannel($S))",
                        grpcNameUtil.getGraphQLServiceStubParameterName(packageName),
                        ClassName.get(packageName, "ReactorGraphQLServiceGrpc"),
                        packageName
                ).addStatement("this.$L = build($S).flatMap(operation -> this.$L.operation($T.newBuilder().setRequest(operation.toString()).build())).map(response -> response.getResponse())",
                        grpcNameUtil.packageNameToUnderline(packageName).concat("_JsonMono"),
                        packageName,
                        grpcNameUtil.getGraphQLServiceStubParameterName(packageName),
                        ClassName.get(packageName, "GraphQLRequest")
                )
        );
        return builder.build();
    }

    private List<MethodSpec> buildGrpcTypeMethods() {
        return this.typeMap.entrySet().stream()
                .flatMap(packageNameEntry ->
                        packageNameEntry.getValue().entrySet().stream()
                                .flatMap(typeNameEntry ->
                                        typeNameEntry.getValue().stream()
                                                .map(fieldName ->
                                                        buildGrpcTypeMethod(packageNameEntry.getKey(), typeNameEntry.getKey(), fieldName)
                                                )
                                )
                )
                .collect(Collectors.toList());
    }

    private List<MethodSpec> buildGrpcTypeListMethods() {
        return this.typeMap.entrySet().stream()
                .flatMap(packageNameEntry ->
                        packageNameEntry.getValue().entrySet().stream()
                                .flatMap(typeNameEntry ->
                                        typeNameEntry.getValue().stream()
                                                .map(fieldName ->
                                                        buildGrpcTypeListMethod(packageNameEntry.getKey(), typeNameEntry.getKey(), fieldName)
                                                )
                                )
                )
                .collect(Collectors.toList());
    }

    private MethodSpec buildGrpcTypeMethod(String packageName, String typeName, String fieldName) {
        return MethodSpec.methodBuilder(grpcNameUtil.getTypeMethodName(packageName, typeName, fieldName))
                .addModifiers(Modifier.PUBLIC)
                .addParameter(String.class, "key")
                .addParameter(GraphqlParser.SelectionSetContext.class, "selectionSetContext")
                .addStatement("addSelection($S, $S, $S, $S)", packageName, typeName, fieldName, fieldName)
                .addStatement("mergeSelection($S, $S, $S, selectionSetContext)", packageName, typeName, fieldName)
                .addStatement("addCondition($S, $S, $S, key)", packageName, typeName, fieldName)
                .build();
    }

    private MethodSpec buildGrpcTypeListMethod(String packageName, String typeName, String fieldName) {
        return MethodSpec.methodBuilder(grpcNameUtil.getTypeListMethodName(packageName, typeName, fieldName))
                .addModifiers(Modifier.PUBLIC)
                .addParameter(String.class, "key")
                .addParameter(GraphqlParser.SelectionSetContext.class, "selectionSetContext")
                .addStatement("addSelection($S, $S, $S, $S)", packageName, typeName, fieldName, fieldName)
                .addStatement("mergeSelection($S, $S, $S, selectionSetContext)", packageName, typeName, fieldName)
                .addStatement("addCondition($S, $S, $S, key)", packageName, typeName, fieldName)
                .build();
    }

    private MethodSpec buildDispatchMethod() {
        List<CodeBlock> monoList = new ArrayList<>();
        int index = 0;
        for (String packageName : this.typeMap.keySet()) {
            if (index == 0) {
                monoList.add(CodeBlock.of("return this.$L.flatMap(response -> $T.fromRunnable(() -> addResult($S, response)))",
                        grpcNameUtil.packageNameToUnderline(packageName).concat("_JsonMono"),
                        ClassName.get(Mono.class),
                        packageName));
            } else {
                monoList.add(CodeBlock.of(".then(this.$L.flatMap(response -> $T.fromRunnable(() -> addResult($S, response))))",
                        grpcNameUtil.packageNameToUnderline(packageName).concat("_JsonMono"),
                        ClassName.get(Mono.class),
                        packageName));
            }
            index++;
        }
        CodeBlock codeBlock;
        if (monoList.size() > 0) {
            monoList.add(
                    CodeBlock.of(".then($T.fromSupplier(() -> queryType(jsonValue, operationDefinitionContext.selectionSet()).map(builder -> builder.build()).orElse($T.EMPTY_JSON_OBJECT)))",
                            ClassName.get(Mono.class),
                            ClassName.get(JsonValue.class)
                    )
            );
            codeBlock = CodeBlock.join(monoList, System.lineSeparator());
        } else {
            codeBlock = CodeBlock.of("return Mono.empty()");
        }
        return MethodSpec.methodBuilder("dispatch")
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(ClassName.get(Mono.class), ClassName.get(JsonValue.class)))
                .addParameter(ParameterSpec.builder(ClassName.get(JsonValue.class), "jsonValue").build())
                .addParameter(ParameterSpec.builder(ClassName.get(GraphqlParser.OperationDefinitionContext.class), "operationDefinitionContext").build())
                .addStatement(codeBlock)
                .build();
    }

    private List<MethodSpec> buildTypeMethods() {
        return manager.getObjects()
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
                .returns(ParameterizedTypeName.get(Optional.class, JsonObjectBuilder.class))
                .addParameter(ClassName.get(JsonValue.class), "jsonValue")
                .addParameter(ClassName.get(GraphqlParser.SelectionSetContext.class), "selectionSet");

        builder.beginControlFlow("if (selectionSet != null && jsonValue != null && jsonValue.getValueType().equals($T.ValueType.OBJECT))", ClassName.get(JsonValue.class))
                .addStatement("$T objectBuilder = jsonProvider.get().createObjectBuilder(jsonValue.asJsonObject())", ClassName.get(JsonObjectBuilder.class))
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

                    builder.beginControlFlow("if(jsonValue.asJsonObject().isNull($S))", from)
                            .addStatement("objectBuilder.add(selectionName, $T.NULL)", ClassName.get(JsonValue.class))
                            .nextControlFlow("else")
                            .addStatement("$T data = getResult($S)", ClassName.get(JsonValue.class), packageName)
                            .beginControlFlow("if (data == null || data.getValueType().equals(JsonValue.ValueType.NULL))")
                            .addStatement("objectBuilder.add(selectionName, $T.NULL)", ClassName.get(JsonValue.class))
                            .nextControlFlow("else")
                            .addStatement("$T fieldValue = data.asJsonObject().get(getQueryFieldAlias($S, $S))", ClassName.get(JsonValue.class), typeName, to)
                            .beginControlFlow("if (fieldValue == null || fieldValue.getValueType().equals($T.ValueType.NULL))", ClassName.get(JsonValue.class))
                            .addStatement("objectBuilder.add(selectionName, $T.NULL)", ClassName.get(JsonValue.class))
                            .nextControlFlow("else")
                            .addStatement("objectBuilder.add(selectionName, fieldValue.asJsonArray().stream().filter(item -> item.asJsonObject().get($S).toString().equals(jsonValue.asJsonObject().get($S).toString())).map(result -> jsonValueFilter(result, selectionContext.field().selectionSet())).collect($T.toJsonArray()))",
                                    to,
                                    from,
                                    ClassName.get(JsonCollectors.class)
                            )
                            .endControlFlow()
                            .endControlFlow()
                            .endControlFlow();
                } else if (manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
                    builder.addStatement("$L(jsonValue.asJsonObject().get(selectionName), selectionContext.field().selectionSet()).ifPresent(builder -> objectBuilder.add(selectionName, builder))",
                            fieldParameterName.concat("List")
                    );
                }
            } else {
                if (manager.isGrpcField(fieldDefinitionContext)) {
                    String typeName = manager.getFieldTypeName(fieldDefinitionContext.type());
                    String packageName = grpcNameUtil.getPackageName(fieldDefinitionContext);
                    String from = grpcNameUtil.getFrom(fieldDefinitionContext);
                    String to = grpcNameUtil.getTo(fieldDefinitionContext);

                    builder.beginControlFlow("if(jsonValue.asJsonObject().isNull($S))", from)
                            .addStatement("objectBuilder.add(selectionName, $T.NULL)", ClassName.get(JsonValue.class))
                            .nextControlFlow("else")
                            .addStatement("$T data = getResult($S)", ClassName.get(JsonValue.class), packageName)
                            .beginControlFlow("if (data == null || data.getValueType().equals(JsonValue.ValueType.NULL))")
                            .addStatement("objectBuilder.add(selectionName, $T.NULL)", ClassName.get(JsonValue.class))
                            .nextControlFlow("else")
                            .addStatement("$T fieldValue = data.asJsonObject().get(getQueryFieldAlias($S, $S))", ClassName.get(JsonValue.class), typeName, to)
                            .beginControlFlow("if (fieldValue == null || fieldValue.getValueType().equals($T.ValueType.NULL))", ClassName.get(JsonValue.class))
                            .addStatement("objectBuilder.add(selectionName, $T.NULL)", ClassName.get(JsonValue.class))
                            .nextControlFlow("else")
                            .addStatement("objectBuilder.add(selectionName, fieldValue.asJsonArray().stream().filter(item -> item.asJsonObject().get($S).toString().equals(jsonValue.asJsonObject().get($S).toString())).map(result -> jsonValueFilter(result, selectionContext.field().selectionSet())).findFirst().orElse($T.NULL))",
                                    to,
                                    from,
                                    ClassName.get(JsonValue.class))
                            .endControlFlow()
                            .endControlFlow()
                            .endControlFlow();
                } else if (manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
                    builder.addStatement("$L(jsonValue.asJsonObject().get(selectionName), selectionContext.field().selectionSet()).ifPresent(builder -> objectBuilder.add(selectionName, builder))",
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
                .addStatement("return $T.of(objectBuilder)", ClassName.get(Optional.class))
                .endControlFlow()
                .addStatement("return Optional.empty()");
        return builder.build();
    }

    private MethodSpec buildListTypeMethod(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        String typeParameterName = typeManager.typeToLowerCamelName(objectTypeDefinitionContext.name().getText());
        String listTypeParameterName = typeParameterName.concat("List");
        MethodSpec.Builder builder = MethodSpec.methodBuilder(listTypeParameterName)
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(Optional.class, JsonArrayBuilder.class))
                .addParameter(ClassName.get(JsonValue.class), "jsonValue")
                .addParameter(ClassName.get(GraphqlParser.SelectionSetContext.class), "selectionSet");

        builder.beginControlFlow("if (selectionSet != null && jsonValue != null && jsonValue.getValueType().equals($T.ValueType.ARRAY))", ClassName.get(JsonValue.class))
                .addStatement("$T arrayBuilder = jsonProvider.get().createArrayBuilder(jsonValue.asJsonArray())", ClassName.get(JsonArrayBuilder.class))
                .addStatement("$T.range(0, jsonValue.asJsonArray().size()).forEach(index -> $L(jsonValue.asJsonArray().get(index), selectionSet).ifPresent(builder -> arrayBuilder.set(index, builder)))",
                        ClassName.get(IntStream.class),
                        typeManager.typeToLowerCamelName(objectTypeDefinitionContext.name().getText())
                )
                .addStatement("return $T.of(arrayBuilder)", ClassName.get(Optional.class))
                .endControlFlow()
                .addStatement("return Optional.empty()");
        return builder.build();
    }
}
