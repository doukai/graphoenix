package io.graphoenix.java.generator.implementer;

import com.squareup.javapoet.*;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.core.handler.MutationDataLoader;
import io.graphoenix.core.operation.Operation;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.json.spi.JsonProvider;
import jakarta.json.stream.JsonCollectors;
import org.tinylog.Logger;
import reactor.core.publisher.Mono;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static io.graphoenix.core.error.GraphQLErrorType.TYPE_ID_FIELD_NOT_EXIST;
import static io.graphoenix.spi.constant.Hammurabi.AGGREGATE_SUFFIX;
import static io.graphoenix.spi.constant.Hammurabi.PAGE_INFO_NAME;
import static io.graphoenix.spi.constant.Hammurabi.WHERE_INPUT_NAME;

@ApplicationScoped
public class MutationHandlerBuilder {

    private final IGraphQLDocumentManager manager;
    private final TypeManager typeManager;
    private final GraphQLConfig graphQLConfig;

    @Inject
    public MutationHandlerBuilder(IGraphQLDocumentManager manager, TypeManager typeManager, GraphQLConfig graphQLConfig) {
        this.manager = manager;
        this.typeManager = typeManager;
        this.graphQLConfig = graphQLConfig;
    }

    public void writeToFiler(Filer filer) throws IOException {
        this.buildClass(true).writeTo(filer);
        Logger.info("MutationBeforeHandler build success");
        this.buildClass(false).writeTo(filer);
        Logger.info("MutationAfterHandler build success");
    }

    private JavaFile buildClass(boolean before) {
        TypeSpec typeSpec = buildMutationHandler(before);
        return JavaFile.builder(graphQLConfig.getHandlerPackageName(), typeSpec).build();
    }

    private TypeSpec buildMutationHandler(boolean before) {
        TypeSpec.Builder builder = TypeSpec.classBuilder(before ? "MutationBeforeHandler" : "MutationAfterHandler")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(ApplicationScoped.class)
                .addField(
                        FieldSpec.builder(
                                ClassName.get(JsonProvider.class),
                                "jsonProvider",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).build()
                )
                .addMethod(buildConstructor())
                .addMethods(buildTypeMethods(before))
                .addMethod(buildHandleMethod(before));
        if (before) {
            builder.addMethod(buildOperationHandleMethod());
        }
        return builder.build();
    }

    private MethodSpec buildConstructor() {
        return MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Inject.class)
                .addParameter(ClassName.get(JsonProvider.class), "jsonProvider")
                .addStatement("this.jsonProvider = jsonProvider")
                .build();
    }

    private List<MethodSpec> buildTypeMethods(boolean before) {
        return manager.getObjects()
                .filter(manager::isNotOperationType)
                .flatMap(objectTypeDefinitionContext ->
                        Stream.of(
                                buildTypeMethod(objectTypeDefinitionContext, before),
                                buildListTypeMethod(objectTypeDefinitionContext, before)
                        )
                )
                .collect(Collectors.toList());
    }

    private MethodSpec buildTypeMethod(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext, boolean before) {
        String typeParameterName = typeManager.typeToLowerCamelName(objectTypeDefinitionContext.name().getText());
        MethodSpec.Builder builder = MethodSpec.methodBuilder(typeParameterName)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get(JsonValue.class), "valueWithVariable")
                .addParameter(ClassName.get(MutationDataLoader.class), "loader")
                .addParameter(ClassName.get(String.class), "jsonPointer");

        if (before) {
            builder.beginControlFlow("if (valueWithVariable != null && valueWithVariable.getValueType().equals($T.ValueType.OBJECT) && valueWithVariable.asJsonObject().size() > 0)", ClassName.get(JsonValue.class));
        } else {
            builder.addParameter(ClassName.get(JsonValue.class), "jsonValue")
                    .beginControlFlow("if (valueWithVariable != null && valueWithVariable.getValueType().equals($T.ValueType.OBJECT) && valueWithVariable.asJsonObject().size() > 0 && jsonValue != null && jsonValue.getValueType().equals($T.ValueType.OBJECT))", ClassName.get(JsonValue.class), ClassName.get(JsonValue.class));
        }
        builder.beginControlFlow("for ($T field : valueWithVariable.asJsonObject().entrySet())", ParameterizedTypeName.get(Map.Entry.class, String.class, JsonValue.class));

        Optional<String> idFieldName = manager.getObjectTypeIDFieldName(objectTypeDefinitionContext.name().getText());
        int index = 0;
        List<GraphqlParser.FieldDefinitionContext> fieldDefinitionContextList = objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                .filter(fieldDefinitionContext ->
                        idFieldName.isPresent() && fieldDefinitionContext.name().getText().equals(idFieldName.get()) ||
                                manager.isFetchField(fieldDefinitionContext) && manager.hasFetchWith(fieldDefinitionContext) ||
                                manager.isFetchField(fieldDefinitionContext) && !manager.hasFetchWith(fieldDefinitionContext) && manager.getFetchAnchor(fieldDefinitionContext) == before ||
                                !manager.isFetchField(fieldDefinitionContext) && manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type()))
                )
                .filter(fieldDefinitionContext -> manager.isNotContainerType(fieldDefinitionContext.type()))
                .filter(fieldDefinitionContext -> !manager.getFieldTypeName(fieldDefinitionContext.type()).equals(PAGE_INFO_NAME))
                .filter(fieldDefinitionContext -> !fieldDefinitionContext.name().getText().endsWith(AGGREGATE_SUFFIX))
                .collect(Collectors.toList());

        for (GraphqlParser.FieldDefinitionContext fieldDefinitionContext : fieldDefinitionContextList) {
            String fieldParameterName = typeManager.typeToLowerCamelName(fieldDefinitionContext.type());
            if (index == 0) {
                builder.beginControlFlow("if (field.getKey().equals($S))", fieldDefinitionContext.name().getText());
            } else {
                builder.nextControlFlow("else if (field.getKey().equals($S))", fieldDefinitionContext.name().getText());
            }
            if (manager.fieldTypeIsList(fieldDefinitionContext.type())) {
                if (manager.isFetchField(fieldDefinitionContext)) {
                    String typeName = manager.getFieldTypeName(fieldDefinitionContext.type());
                    String packageName = manager.getPackageName(typeName);
                    String protocol = manager.getProtocol(fieldDefinitionContext);
                    String from = manager.getFetchFrom(fieldDefinitionContext);
                    String to = manager.getFetchTo(fieldDefinitionContext);
                    String key = manager.getObjectTypeIDFieldName(typeName).orElseThrow(() -> new GraphQLErrors(TYPE_ID_FIELD_NOT_EXIST.bind(typeName)));
                    if (before) {
                        if (manager.hasFetchWith(fieldDefinitionContext)) {
                            String withTo = manager.getFetchWithTo(fieldDefinitionContext);
                            GraphqlParser.FieldDefinitionContext withObjectField = manager.getFetchWithObjectField(objectTypeDefinitionContext, fieldDefinitionContext);
                            builder.addStatement("loader.registerReplaceFiled(jsonPointer, $S, $S, field.getValue().asJsonArray().stream().map(item -> jsonProvider.createObjectBuilder().build()).collect($T.toJsonArray()))",
                                            fieldDefinitionContext.name().getText(),
                                            withObjectField.name().getText(),
                                            ClassName.get(JsonCollectors.class)
                                    )
                                    .addStatement("loader.registerArray($S, $S, $S, $S, $S, jsonPointer + \"/\" + $S, $S, field.getValue(), false)",
                                            packageName,
                                            protocol,
                                            typeName,
                                            to,
                                            key,
                                            withObjectField.name().getText(),
                                            withTo
                                    );
                        }
                    } else {
                        builder.beginControlFlow("if(valueWithVariable.asJsonObject().containsKey($S) && !valueWithVariable.asJsonObject().isNull($S))", from, from)
                                .addStatement("loader.registerArray($S, $S, $S, $S, field.getValue().asJsonArray().stream().map(item -> jsonProvider.createObjectBuilder(item.asJsonObject()).add($S, valueWithVariable.asJsonObject().get($S)).build()).collect($T.toJsonArray()))",
                                        packageName,
                                        protocol,
                                        typeName,
                                        key,
                                        to,
                                        from,
                                        ClassName.get(JsonCollectors.class)
                                )
                                .endControlFlow();
                    }
                } else if (manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
                    if (before) {
                        builder.addStatement("$L(field.getValue(), loader, jsonPointer + \"/\" + $S)", fieldParameterName.concat("List"), fieldDefinitionContext.name().getText());
                    } else {
                        builder.addStatement("$L(field.getValue(), loader, jsonPointer + \"/\" + $S, valueWithVariable.asJsonObject().get($S))", fieldParameterName.concat("List"), fieldDefinitionContext.name().getText(), fieldDefinitionContext.name().getText());
                    }
                }
            } else {
                if (idFieldName.isPresent() && fieldDefinitionContext.name().getText().equals(idFieldName.get())) {
                    if (before) {
                        builder.addStatement("loader.registerUpdate($S, field.getValue())",
                                objectTypeDefinitionContext.name().getText()
                        );
                    } else {
                        builder.addStatement("loader.registerCreate($S, valueWithVariable.asJsonObject().get($S))",
                                objectTypeDefinitionContext.name().getText(),
                                fieldDefinitionContext.name().getText()
                        );
                    }
                } else if (manager.isFetchField(fieldDefinitionContext)) {
                    String typeName = manager.getFieldTypeName(fieldDefinitionContext.type());
                    String packageName = manager.getPackageName(typeName);
                    String protocol = manager.getProtocol(fieldDefinitionContext);
                    String from = manager.getFetchFrom(fieldDefinitionContext);
                    String to = manager.getFetchTo(fieldDefinitionContext);
                    String key = manager.getObjectTypeIDFieldName(typeName).orElseThrow(() -> new GraphQLErrors(TYPE_ID_FIELD_NOT_EXIST.bind(typeName)));

                    if (before) {
                        if (manager.hasFetchWith(fieldDefinitionContext)) {
                            String withTo = manager.getFetchWithTo(fieldDefinitionContext);
                            GraphqlParser.FieldDefinitionContext withObjectField = manager.getFetchWithObjectField(objectTypeDefinitionContext, fieldDefinitionContext);
                            builder.addStatement("loader.registerReplaceFiled(jsonPointer, $S, $S, jsonProvider.createObjectBuilder().build())",
                                            fieldDefinitionContext.name().getText(),
                                            withObjectField.name().getText()
                                    )
                                    .addStatement("loader.register($S, $S, $S, $S, $S, jsonPointer + \"/\" + $S, $S, field.getValue(), false)",
                                            packageName,
                                            protocol,
                                            typeName,
                                            to,
                                            key,
                                            withObjectField.name().getText(),
                                            withTo
                                    );
                        } else {
                            builder.addStatement("loader.register($S, $S, $S, $S, $S, jsonPointer + \"/\" + $S, $S, field.getValue(), true)",
                                    packageName,
                                    protocol,
                                    typeName,
                                    to,
                                    key,
                                    fieldDefinitionContext.name().getText(),
                                    from
                            );
                        }
                    } else {
                        builder.beginControlFlow("if(valueWithVariable.asJsonObject().containsKey($S) && !valueWithVariable.asJsonObject().isNull($S))", from, from)
                                .addStatement("loader.register($S, $S, $S, $S, jsonProvider.createObjectBuilder(field.getValue().asJsonObject()).add($S, valueWithVariable.asJsonObject().get($S)).build())",
                                        packageName,
                                        protocol,
                                        typeName,
                                        key,
                                        to,
                                        from
                                )
                                .endControlFlow();
                    }
                } else if (manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
                    if (before) {
                        builder.addStatement("$L(field.getValue(), loader, jsonPointer + \"/\" + $S)", fieldParameterName, fieldDefinitionContext.name().getText());
                    } else {
                        builder.addStatement("$L(field.getValue(), loader, jsonPointer + \"/\" + $S, valueWithVariable.asJsonObject().get($S))", fieldParameterName, fieldDefinitionContext.name().getText(), fieldDefinitionContext.name().getText());
                    }
                }
            }
            if (index == fieldDefinitionContextList.size() - 1) {
                if (before) {
                    builder.nextControlFlow("else if (field.getKey().equals($S))", WHERE_INPUT_NAME)
                            .addStatement("loader.registerWhere($S, field.getValue())",
                                    objectTypeDefinitionContext.name().getText()
                            );
                }
                builder.endControlFlow();
            }
            index++;
        }
        builder.endControlFlow().endControlFlow();
        return builder.build();
    }

    private MethodSpec buildListTypeMethod(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext, boolean before) {
        String typeParameterName = typeManager.typeToLowerCamelName(objectTypeDefinitionContext.name().getText());
        String listTypeParameterName = typeParameterName.concat("List");
        MethodSpec.Builder builder = MethodSpec.methodBuilder(listTypeParameterName)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get(JsonValue.class), "valueWithVariable")
                .addParameter(ClassName.get(MutationDataLoader.class), "loader")
                .addParameter(ClassName.get(String.class), "jsonPointer");

        if (before) {
            builder.beginControlFlow("if (valueWithVariable != null && valueWithVariable.getValueType().equals($T.ValueType.ARRAY))", ClassName.get(JsonValue.class))
                    .addStatement("$T valueWithVariables = valueWithVariable.asJsonArray()",
                            ClassName.get(JsonArray.class)
                    )
                    .addStatement("$T.range(0, valueWithVariables.size()).forEach(index -> $L(valueWithVariables.get(index), loader, jsonPointer + \"/\" + index))",
                            ClassName.get(IntStream.class),
                            typeManager.typeToLowerCamelName(objectTypeDefinitionContext.name().getText())
                    )
                    .endControlFlow();
        } else {
            builder.addParameter(ClassName.get(JsonValue.class), "jsonValue")
                    .beginControlFlow("if (valueWithVariable != null && valueWithVariable.getValueType().equals($T.ValueType.ARRAY) && jsonValue != null && jsonValue.getValueType().equals($T.ValueType.ARRAY))", ClassName.get(JsonValue.class), ClassName.get(JsonValue.class))
                    .addStatement("$T valueWithVariables = valueWithVariable.asJsonArray()",
                            ClassName.get(JsonArray.class)
                    )
                    .addStatement("$T.range(0, valueWithVariables.size()).forEach(index -> $L(valueWithVariables.get(index), loader, jsonPointer + \"/\" + index, jsonValue.asJsonArray().get(index)))",
                            ClassName.get(IntStream.class),
                            typeManager.typeToLowerCamelName(objectTypeDefinitionContext.name().getText())
                    )
                    .endControlFlow();
        }
        return builder.build();
    }

    private MethodSpec buildOperationHandleMethod() {
        return MethodSpec.methodBuilder("handle")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get(GraphqlParser.OperationDefinitionContext.class), "operationDefinition")
                .addParameter(ClassName.get(MutationDataLoader.class), "loader")
                .returns(ParameterizedTypeName.get(ClassName.get(Mono.class), ClassName.get(Operation.class)))
                .addStatement("$T operation = new $T(operationDefinition)", ClassName.get(Operation.class), ClassName.get(Operation.class))
                .addStatement("return handle(operationDefinition, operation, loader)")
                .build();
    }

    private MethodSpec buildHandleMethod(boolean before) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("handle")
                .addParameter(ClassName.get(GraphqlParser.OperationDefinitionContext.class), "operationDefinition")
                .addModifiers(Modifier.PUBLIC);

        if (before) {
            builder.addParameter(ClassName.get(Operation.class), "operation")
                    .addParameter(ClassName.get(MutationDataLoader.class), "loader")
                    .addStatement("$T operationArguments = loader.buildOperationArguments(operation)", ClassName.get(JsonObject.class))
                    .returns(ParameterizedTypeName.get(ClassName.get(Mono.class), ClassName.get(Operation.class)));
        } else {
            builder.addParameter(ClassName.get(MutationDataLoader.class), "loader")
                    .addParameter(ClassName.get(Operation.class), "operation")
                    .addParameter(ClassName.get(JsonValue.class), "jsonValue")
                    .returns(ParameterizedTypeName.get(ClassName.get(Mono.class), ClassName.get(JsonObject.class)));
        }
        builder.beginControlFlow("for ($T selectionContext : operationDefinition.selectionSet().selection()) ", ClassName.get(GraphqlParser.SelectionContext.class))
                .addStatement("$T selectionName = selectionContext.field().alias() != null ? selectionContext.field().alias().name().getText() : selectionContext.field().name().getText()", ClassName.get(String.class));
        List<GraphqlParser.FieldDefinitionContext> fieldDefinitionContextList = manager.getMutationOperationTypeName().flatMap(manager::getObject).stream()
                .flatMap(objectTypeDefinitionContext ->
                        objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                                .filter(manager::isNotInvokeField)
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
            if (before) {
                builder.addStatement("$L(operationArguments.get(selectionName), loader, \"/\" + selectionName)", typeMethodName);
            } else {
                builder.addStatement("$L(operation.getField(selectionName).getArguments(), loader, \"/\" + selectionName, jsonValue.asJsonObject().get(selectionName))", typeMethodName);
            }
            index++;
        }
        builder.endControlFlow().endControlFlow();

        if (before) {
            if (graphQLConfig.getBackup()) {
                builder.addStatement("return loader.backup().then(loader.load(loader.replaceAll(operationArguments))).switchIfEmpty(loader.load(loader.replaceAll(operationArguments))).map(jsonValue -> loader.dispatchOperationArguments(jsonValue, operation))");
            } else {
                builder.addStatement("return loader.load(loader.replaceAll(operationArguments)).map(jsonValue -> loader.dispatchOperationArguments(jsonValue, operation))");
            }
        } else {
            builder.addStatement("return loader.load().thenReturn(jsonValue.asJsonObject())");
        }
        return builder.build();
    }
}
