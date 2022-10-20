package io.graphoenix.java.generator.implementer;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.core.handler.MutationDataLoader;
import io.graphoenix.core.operation.Argument;
import io.graphoenix.core.operation.ArrayValueWithVariable;
import io.graphoenix.core.operation.Field;
import io.graphoenix.core.operation.Operation;
import io.graphoenix.core.operation.ValueWithVariable;
import io.graphoenix.java.generator.implementer.grpc.GrpcNameUtil;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.constant.Hammurabi;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.JsonValue;
import org.tinylog.Logger;
import reactor.core.publisher.Mono;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static io.graphoenix.core.error.GraphQLErrorType.TYPE_ID_FIELD_NOT_EXIST;
import static io.graphoenix.spi.constant.Hammurabi.AGGREGATE_SUFFIX;
import static io.graphoenix.spi.constant.Hammurabi.PAGE_INFO_NAME;

@ApplicationScoped
public class MutationHandlerBuilder {

    private final IGraphQLDocumentManager manager;
    private final TypeManager typeManager;
    private final GrpcNameUtil grpcNameUtil;
    private GraphQLConfig graphQLConfig;

    @Inject
    public MutationHandlerBuilder(IGraphQLDocumentManager manager, TypeManager typeManager, GrpcNameUtil grpcNameUtil) {
        this.manager = manager;
        this.typeManager = typeManager;
        this.grpcNameUtil = grpcNameUtil;
    }

    public MutationHandlerBuilder setConfiguration(GraphQLConfig graphQLConfig) {
        this.graphQLConfig = graphQLConfig;
        this.typeManager.setGraphQLConfig(graphQLConfig);
        return this;
    }

    public void writeToFiler(Filer filer) throws IOException {
        this.buildClass(true).writeTo(filer);
        Logger.info("MutationBeforeHandler build success");
        this.buildClass(false).writeTo(filer);
        Logger.info("MutationAfterHandler build success");
    }

    private JavaFile buildClass(boolean anchor) {
        TypeSpec typeSpec = buildMutationHandler(anchor);
        return JavaFile.builder(graphQLConfig.getHandlerPackageName(), typeSpec).build();
    }

    private TypeSpec buildMutationHandler(boolean anchor) {
        TypeSpec.Builder builder = TypeSpec.classBuilder(anchor ? "MutationBeforeHandler" : "MutationAfterHandler")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(ApplicationScoped.class)
                .addMethod(buildConstructor())
                .addMethods(buildTypeMethods(anchor))
                .addMethod(buildHandleMethod(anchor));
        return builder.build();
    }

    private MethodSpec buildConstructor() {
        return MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Inject.class)
                .build();
    }

    private List<MethodSpec> buildTypeMethods(boolean anchor) {
        return manager.getObjects()
                .filter(objectTypeDefinitionContext ->
                        !manager.isQueryOperationType(objectTypeDefinitionContext.name().getText()) &&
                                !manager.isMutationOperationType(objectTypeDefinitionContext.name().getText()) &&
                                !manager.isSubscriptionOperationType(objectTypeDefinitionContext.name().getText())
                )
                .filter(objectTypeDefinitionContext -> manager.isNotContainerType(objectTypeDefinitionContext.name().getText()))
                .filter(objectTypeDefinitionContext -> !objectTypeDefinitionContext.name().getText().equals(PAGE_INFO_NAME))
                .filter(objectTypeDefinitionContext -> !objectTypeDefinitionContext.name().getText().endsWith(AGGREGATE_SUFFIX))
                .flatMap(objectTypeDefinitionContext ->
                        Stream.of(
                                buildTypeFieldMethod(objectTypeDefinitionContext, anchor),
                                buildTypeMethod(objectTypeDefinitionContext, anchor),
                                buildListTypeFieldMethod(objectTypeDefinitionContext, anchor),
                                buildListTypeMethod(objectTypeDefinitionContext, anchor)
                        )
                )
                .collect(Collectors.toList());
    }

    private MethodSpec buildTypeFieldMethod(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext, boolean anchor) {
        String typeParameterName = typeManager.typeToLowerCamelName(objectTypeDefinitionContext.name().getText());
        MethodSpec.Builder builder = MethodSpec.methodBuilder(typeParameterName)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get(Field.class), "field")
                .addParameter(ClassName.get(MutationDataLoader.class), "loader");

        if (anchor) {
            builder.beginControlFlow("if (field != null && field.getArguments() != null && field.getArguments().size() > 0)");
        } else {
            builder.addParameter(ClassName.get(JsonValue.class), "jsonValue")
                    .beginControlFlow("if (field != null && field.getArguments() != null && field.getArguments().size() > 0 && jsonValue != null && jsonValue.getValueType().equals($T.ValueType.OBJECT))", ClassName.get(JsonValue.class));
        }
        builder.beginControlFlow("for ($T argument : field.getArguments())", ClassName.get(Argument.class));

        String idFieldName = manager.getObjectTypeIDFieldName(objectTypeDefinitionContext.name().getText()).orElseThrow(() -> new GraphQLErrors(TYPE_ID_FIELD_NOT_EXIST));
        int index = 0;
        List<GraphqlParser.FieldDefinitionContext> fieldDefinitionContextList = objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                .filter(fieldDefinitionContext -> fieldDefinitionContext.name().getText().equals(idFieldName) || manager.isGrpcField(fieldDefinitionContext) && grpcNameUtil.getAnchor(fieldDefinitionContext) == anchor || !manager.isGrpcField(fieldDefinitionContext) && manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type())))
                .filter(fieldDefinitionContext -> manager.isNotContainerType(manager.getFieldTypeName(fieldDefinitionContext.type())))
                .filter(fieldDefinitionContext -> !manager.getFieldTypeName(fieldDefinitionContext.type()).equals(PAGE_INFO_NAME))
                .filter(fieldDefinitionContext -> !fieldDefinitionContext.name().getText().endsWith(AGGREGATE_SUFFIX))
                .collect(Collectors.toList());

        for (GraphqlParser.FieldDefinitionContext fieldDefinitionContext : fieldDefinitionContextList) {
            String fieldParameterName = typeManager.typeToLowerCamelName(fieldDefinitionContext.type());
            if (index == 0) {
                builder.beginControlFlow("if (argument.getName().equals($S))", fieldDefinitionContext.name().getText());
            } else {
                builder.nextControlFlow("else if (argument.getName().equals($S))", fieldDefinitionContext.name().getText());
            }
            if (manager.fieldTypeIsList(fieldDefinitionContext.type())) {
                if (manager.isGrpcField(fieldDefinitionContext)) {
                    String typeName = manager.getFieldTypeName(fieldDefinitionContext.type());
                    String packageName = grpcNameUtil.getPackageName(fieldDefinitionContext);
                    String from = grpcNameUtil.getFrom(fieldDefinitionContext);
                    String to = grpcNameUtil.getTo(fieldDefinitionContext);
                    String key = grpcNameUtil.getKey(fieldDefinitionContext);

                    if (anchor) {
                        builder.addStatement("loader.registerArray($S, $S, $S, argument.getValueWithVariable())",
                                packageName,
                                typeName,
                                key
                        );
                    } else {
                        builder.beginControlFlow("if(jsonValue.asJsonObject().containsKey($S) && !jsonValue.asJsonObject().isNull($S))", from, from)
                                .addStatement("argument.getValueWithVariable().asArray().forEach(item -> item.asObject().put($S, jsonValue.asJsonObject().get($S)))", to, from)
                                .addStatement("loader.registerArray($S, $S, $S, argument.getValueWithVariable())",
                                        packageName,
                                        typeName,
                                        key
                                )
                                .endControlFlow();
                    }
                } else if (manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
                    if (anchor) {
                        builder.addStatement("$L(argument.getValueWithVariable(), loader)", fieldParameterName.concat("List"));
                    } else {
                        builder.addStatement("$L(argument.getValueWithVariable(), loader, jsonValue.asJsonObject().get($S))",
                                fieldParameterName.concat("List"),
                                fieldDefinitionContext.name().getText()
                        );
                    }
                }
            } else {
                if (fieldDefinitionContext.name().getText().equals(idFieldName)) {
                    if (anchor) {
                        builder.addStatement("loader.registerUpdate($S, argument.getValueWithVariable())",
                                objectTypeDefinitionContext.name().getText()
                        );
                    } else {
                        builder.addStatement("loader.registerCreate($S, jsonValue.asJsonObject().get($S))",
                                objectTypeDefinitionContext.name().getText(),
                                fieldDefinitionContext.name().getText()
                        );
                    }
                } else if (manager.isGrpcField(fieldDefinitionContext)) {
                    String typeName = manager.getFieldTypeName(fieldDefinitionContext.type());
                    String packageName = grpcNameUtil.getPackageName(fieldDefinitionContext);
                    String from = grpcNameUtil.getFrom(fieldDefinitionContext);
                    String to = grpcNameUtil.getTo(fieldDefinitionContext);
                    String key = grpcNameUtil.getKey(fieldDefinitionContext);
                    if (anchor) {
                        builder.addStatement("loader.register($S, $S, $S, $S, (jsonObject) -> field.addArgument($S, jsonObject.get($S)), argument.getValueWithVariable())",
                                packageName,
                                typeName,
                                to,
                                key,
                                from,
                                to
                        );
                    } else {
                        builder.beginControlFlow("if(jsonValue.asJsonObject().containsKey($S) && !jsonValue.asJsonObject().isNull($S))", from, from)
                                .addStatement("argument.getValueWithVariable().asObject().put($S, jsonValue.asJsonObject().get($S))", to, from)
                                .addStatement("loader.register($S, $S, $S, argument.getValueWithVariable())",
                                        packageName,
                                        typeName,
                                        key
                                )
                                .endControlFlow();
                    }
                } else if (manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
                    if (anchor) {
                        builder.addStatement("$L(argument.getValueWithVariable(), loader)", fieldParameterName);
                    } else {
                        builder.addStatement("$L(argument.getValueWithVariable(), loader, jsonValue.asJsonObject().get($S))", fieldParameterName, fieldDefinitionContext.name().getText());
                    }
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

    private MethodSpec buildTypeMethod(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext, boolean anchor) {
        String typeParameterName = typeManager.typeToLowerCamelName(objectTypeDefinitionContext.name().getText());
        MethodSpec.Builder builder = MethodSpec.methodBuilder(typeParameterName)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get(ValueWithVariable.class), "valueWithVariable")
                .addParameter(ClassName.get(MutationDataLoader.class), "loader");

        if (anchor) {
            builder.beginControlFlow("if (valueWithVariable != null && valueWithVariable.isObject() && valueWithVariable.asObject().size() > 0)");
        } else {
            builder.addParameter(ClassName.get(JsonValue.class), "jsonValue")
                    .beginControlFlow("if (valueWithVariable != null && valueWithVariable.isObject() && valueWithVariable.asObject().size() > 0 && jsonValue != null && jsonValue.getValueType().equals($T.ValueType.OBJECT))", ClassName.get(JsonValue.class));
        }
        builder.beginControlFlow("for ($T field : valueWithVariable.asObject().entrySet())", ParameterizedTypeName.get(Map.Entry.class, String.class, ValueWithVariable.class));

        String idFieldName = manager.getObjectTypeIDFieldName(objectTypeDefinitionContext.name().getText()).orElseThrow(() -> new GraphQLErrors(TYPE_ID_FIELD_NOT_EXIST));
        int index = 0;
        List<GraphqlParser.FieldDefinitionContext> fieldDefinitionContextList = objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                .filter(fieldDefinitionContext -> fieldDefinitionContext.name().getText().equals(idFieldName) || manager.isGrpcField(fieldDefinitionContext) && grpcNameUtil.getAnchor(fieldDefinitionContext) == anchor || !manager.isGrpcField(fieldDefinitionContext) && manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type())))
                .filter(fieldDefinitionContext -> manager.isNotContainerType(manager.getFieldTypeName(fieldDefinitionContext.type())))
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
                if (manager.isGrpcField(fieldDefinitionContext)) {
                    String typeName = manager.getFieldTypeName(fieldDefinitionContext.type());
                    String packageName = grpcNameUtil.getPackageName(fieldDefinitionContext);
                    String from = grpcNameUtil.getFrom(fieldDefinitionContext);
                    String to = grpcNameUtil.getTo(fieldDefinitionContext);
                    String key = grpcNameUtil.getKey(fieldDefinitionContext);

                    if (anchor) {
                        builder.addStatement("loader.registerArray($S, $S, $S, field.getValue())",
                                packageName,
                                typeName,
                                key
                        );
                    } else {
                        builder.beginControlFlow("if(jsonValue.asJsonObject().containsKey($S) && !jsonValue.asJsonObject().isNull($S))", from, from)
                                .addStatement("field.getValue().asArray().forEach(item -> item.asObject().put($S, jsonValue.asJsonObject().get($S)))", to, from)
                                .addStatement("loader.registerArray($S, $S, $S, field.getValue())",
                                        packageName,
                                        typeName,
                                        key
                                )
                                .endControlFlow();
                    }
                } else if (manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
                    if (anchor) {
                        builder.addStatement("$L(field.getValue(), loader)", fieldParameterName.concat("List"));
                    } else {
                        builder.addStatement("$L(field.getValue(), loader, jsonValue.asJsonObject().get($S))", fieldParameterName.concat("List"), fieldDefinitionContext.name().getText());
                    }
                }
            } else {
                if (fieldDefinitionContext.name().getText().equals(idFieldName)) {
                    if (anchor) {
                        builder.addStatement("loader.registerUpdate($S, field.getValue())",
                                objectTypeDefinitionContext.name().getText()
                        );
                    } else {
                        builder.addStatement("loader.registerCreate($S, jsonValue.asJsonObject().get($S))",
                                objectTypeDefinitionContext.name().getText(),
                                fieldDefinitionContext.name().getText()
                        );
                    }
                } else if (manager.isGrpcField(fieldDefinitionContext)) {
                    String typeName = manager.getFieldTypeName(fieldDefinitionContext.type());
                    String packageName = grpcNameUtil.getPackageName(fieldDefinitionContext);
                    String from = grpcNameUtil.getFrom(fieldDefinitionContext);
                    String to = grpcNameUtil.getTo(fieldDefinitionContext);
                    String key = grpcNameUtil.getKey(fieldDefinitionContext);

                    if (anchor) {
                        builder.addStatement("loader.register($S, $S, $S, $S, (jsonObject) -> valueWithVariable.asObject().put($S, jsonObject.get($S)), field.getValue())",
                                packageName,
                                typeName,
                                to,
                                key,
                                from,
                                to
                        );
                    } else {
                        builder.beginControlFlow("if(jsonValue.asJsonObject().containsKey($S) && !jsonValue.asJsonObject().isNull($S))", from, from)
                                .addStatement("field.getValue().asObject().put($S, jsonValue.asJsonObject().get($S))", to, from)
                                .addStatement("loader.register($S, $S, $S, field.getValue())",
                                        packageName,
                                        typeName,
                                        key
                                )
                                .endControlFlow();
                    }
                } else if (manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
                    if (anchor) {
                        builder.addStatement("$L(field.getValue(), loader)", fieldParameterName);
                    } else {
                        builder.addStatement("$L(field.getValue(), loader, jsonValue.asJsonObject().get($S))", fieldParameterName, fieldDefinitionContext.name().getText());
                    }
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

    private MethodSpec buildListTypeFieldMethod(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext, boolean anchor) {
        String typeParameterName = typeManager.typeToLowerCamelName(objectTypeDefinitionContext.name().getText());
        String listTypeParameterName = typeParameterName.concat("List");
        MethodSpec.Builder builder = MethodSpec.methodBuilder(listTypeParameterName)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get(Field.class), "field")
                .addParameter(ClassName.get(MutationDataLoader.class), "loader");

        if (anchor) {
            builder.beginControlFlow(
                    "if (field != null && field.getArgument($T.LIST_INPUT_NAME).isPresent() && field.getArgument($T.LIST_INPUT_NAME).get().getValueWithVariable().isArray())",
                    ClassName.get(Hammurabi.class),
                    ClassName.get(Hammurabi.class)
            ).addStatement("$T valueWithVariables = field.getArgument($T.LIST_INPUT_NAME).get().getValueWithVariable().asArray()",
                    ClassName.get(ArrayValueWithVariable.class),
                    ClassName.get(Hammurabi.class)
            ).addStatement("$T.range(0, valueWithVariables.size()).forEach(index -> $L(valueWithVariables.get(index), loader))",
                    ClassName.get(IntStream.class),
                    typeManager.typeToLowerCamelName(objectTypeDefinitionContext.name().getText())
            ).endControlFlow();
        } else {
            builder.addParameter(ClassName.get(JsonValue.class), "jsonValue")
                    .beginControlFlow(
                            "if (field != null && field.getArgument($T.LIST_INPUT_NAME).isPresent() && field.getArgument($T.LIST_INPUT_NAME).get().getValueWithVariable().isArray() && jsonValue != null && jsonValue.getValueType().equals($T.ValueType.ARRAY))",
                            ClassName.get(Hammurabi.class),
                            ClassName.get(Hammurabi.class),
                            ClassName.get(JsonValue.class)
                    )
                    .addStatement("$T valueWithVariables = field.getArgument($T.LIST_INPUT_NAME).get().getValueWithVariable().asArray()",
                            ClassName.get(ArrayValueWithVariable.class),
                            ClassName.get(Hammurabi.class)
                    )
                    .addStatement("$T.range(0, valueWithVariables.size()).forEach(index -> $L(valueWithVariables.get(index), loader, jsonValue.asJsonArray().get(index)))",
                            ClassName.get(IntStream.class),
                            typeManager.typeToLowerCamelName(objectTypeDefinitionContext.name().getText())
                    )
                    .endControlFlow();
        }
        return builder.build();
    }

    private MethodSpec buildListTypeMethod(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext, boolean anchor) {
        String typeParameterName = typeManager.typeToLowerCamelName(objectTypeDefinitionContext.name().getText());
        String listTypeParameterName = typeParameterName.concat("List");
        MethodSpec.Builder builder = MethodSpec.methodBuilder(listTypeParameterName)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get(ValueWithVariable.class), "valueWithVariable")
                .addParameter(ClassName.get(MutationDataLoader.class), "loader");

        if (anchor) {
            builder.beginControlFlow("if (valueWithVariable != null && valueWithVariable.isArray())")
                    .addStatement("$T valueWithVariables = valueWithVariable.asArray()",
                            ClassName.get(ArrayValueWithVariable.class)
                    )
                    .addStatement("$T.range(0, valueWithVariables.size()).forEach(index -> $L(valueWithVariables.get(index), loader))",
                            ClassName.get(IntStream.class),
                            typeManager.typeToLowerCamelName(objectTypeDefinitionContext.name().getText())
                    )
                    .endControlFlow();
        } else {
            builder.addParameter(ClassName.get(JsonValue.class), "jsonValue")
                    .beginControlFlow("if (valueWithVariable != null && valueWithVariable.isArray() && jsonValue != null && jsonValue.getValueType().equals($T.ValueType.ARRAY))", ClassName.get(JsonValue.class))
                    .addStatement("$T valueWithVariables = valueWithVariable.asArray()",
                            ClassName.get(ArrayValueWithVariable.class)
                    )
                    .addStatement("$T.range(0, valueWithVariables.size()).forEach(index -> $L(valueWithVariables.get(index), loader, jsonValue.asJsonArray().get(index)))",
                            ClassName.get(IntStream.class),
                            typeManager.typeToLowerCamelName(objectTypeDefinitionContext.name().getText())
                    )
                    .endControlFlow();
        }
        return builder.build();
    }

    private MethodSpec buildHandleMethod(boolean anchor) {

        MethodSpec.Builder builder = MethodSpec.methodBuilder("handle")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get(GraphqlParser.OperationDefinitionContext.class), "operationDefinition")
                .addParameter(ClassName.get(MutationDataLoader.class), "loader")
                .returns(ParameterizedTypeName.get(ClassName.get(Mono.class), ClassName.get(Operation.class)));

        if (anchor) {
            builder.addStatement("$T operation = new $T(operationDefinition)", ClassName.get(Operation.class), ClassName.get(Operation.class));
        } else {
            builder.addParameter(ClassName.get(Operation.class), "operation")
                    .addParameter(ClassName.get(JsonValue.class), "jsonValue");
        }
        builder.beginControlFlow("for ($T selectionContext : operationDefinition.selectionSet().selection()) ", ClassName.get(GraphqlParser.SelectionContext.class))
                .addStatement("$T selectionName = selectionContext.field().alias() != null ? selectionContext.field().alias().name().getText() : selectionContext.field().name().getText()", ClassName.get(String.class));
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
            if (anchor) {
                builder.addStatement("$L(operation.getField(selectionName), loader)", typeMethodName);
            } else {
                builder.addStatement("$L(operation.getField(selectionName), loader, jsonValue.asJsonObject().get(selectionName))", typeMethodName);
            }
            index++;
        }
        builder.endControlFlow()
                .endControlFlow();

        if (anchor) {
            builder.addStatement("return loader.backup().then(loader.load()).thenReturn(operation)");
        } else {
            builder.addStatement("return loader.load().thenReturn(operation)");
        }
        return builder.build();
    }
}
