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
import io.graphoenix.core.operation.Operation;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
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
                .filter(objectTypeDefinitionContext -> !manager.hasClassName(objectTypeDefinitionContext))
                .filter(objectTypeDefinitionContext -> !manager.isQueryOperationType(objectTypeDefinitionContext.name().getText()))
                .filter(objectTypeDefinitionContext -> !manager.isMutationOperationType(objectTypeDefinitionContext.name().getText()))
                .filter(objectTypeDefinitionContext -> !manager.isSubscriptionOperationType(objectTypeDefinitionContext.name().getText()))
                .filter(objectTypeDefinitionContext -> !objectTypeDefinitionContext.name().getText().equals(PAGE_INFO_NAME))
                .filter(objectTypeDefinitionContext -> !objectTypeDefinitionContext.name().getText().endsWith(AGGREGATE_SUFFIX))
                .flatMap(objectTypeDefinitionContext ->
                        Stream.of(
                                buildTypeMethod(objectTypeDefinitionContext, anchor),
                                buildListTypeMethod(objectTypeDefinitionContext, anchor)
                        )
                )
                .collect(Collectors.toList());
    }

    private MethodSpec buildTypeMethod(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext, boolean anchor) {
        String typeParameterName = typeManager.typeToLowerCamelName(objectTypeDefinitionContext.name().getText());
        MethodSpec.Builder builder = MethodSpec.methodBuilder(typeParameterName)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get(JsonValue.class), "valueWithVariable")
                .addParameter(ClassName.get(MutationDataLoader.class), "loader")
                .addParameter(ClassName.get(String.class), "jsonPointer");

        if (anchor) {
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
                                manager.isFetchField(fieldDefinitionContext) && manager.getAnchor(fieldDefinitionContext) == anchor ||
                                !manager.isFetchField(fieldDefinitionContext) && manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type())) && !manager.hasClassName(fieldDefinitionContext.type())
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
                    String from = manager.getFrom(fieldDefinitionContext);
                    String to = manager.getTo(fieldDefinitionContext);
                    String key = manager.getObjectTypeIDFieldName(typeName).orElseThrow(() -> new GraphQLErrors(TYPE_ID_FIELD_NOT_EXIST.bind(typeName)));

                    if (anchor) {
                        builder.addStatement("loader.registerArray($S, $S, $S, $S, field.getValue())",
                                packageName,
                                protocol,
                                typeName,
                                key
                        );
                    } else {
                        builder.beginControlFlow("if(jsonValue.asJsonObject().containsKey($S) && !jsonValue.asJsonObject().isNull($S))", from, from)
                                .addStatement("field.getValue().asJsonArray().forEach(item -> item.asJsonObject().put($S, jsonValue.asJsonObject().get($S)))", to, from)
                                .addStatement("loader.registerArray($S, $S, $S, $S, field.getValue())",
                                        packageName,
                                        protocol,
                                        typeName,
                                        key
                                )
                                .endControlFlow();
                    }
                } else if (manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
                    if (anchor) {
                        builder.addStatement("$L(field.getValue(), loader, jsonPointer + \"/\" + $S)", fieldParameterName.concat("List"), fieldDefinitionContext.name().getText());
                    } else {
                        builder.addStatement("$L(field.getValue(), loader, jsonPointer + \"/\" + $S, jsonValue.asJsonObject().get($S))", fieldParameterName.concat("List"), fieldDefinitionContext.name().getText(), fieldDefinitionContext.name().getText());
                    }
                }
            } else {
                if (idFieldName.isPresent() && fieldDefinitionContext.name().getText().equals(idFieldName.get())) {
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
                } else if (manager.isFetchField(fieldDefinitionContext)) {
                    String typeName = manager.getFieldTypeName(fieldDefinitionContext.type());
                    String packageName = manager.getPackageName(typeName);
                    String protocol = manager.getProtocol(fieldDefinitionContext);
                    String from = manager.getFrom(fieldDefinitionContext);
                    String to = manager.getTo(fieldDefinitionContext);
                    String key = manager.getObjectTypeIDFieldName(typeName).orElseThrow(() -> new GraphQLErrors(TYPE_ID_FIELD_NOT_EXIST.bind(typeName)));

                    if (anchor) {
                        builder.addStatement("loader.register($S, $S, $S, $S, $S, jsonPointer + \"/\" + $S, $S, field.getValue())",
                                packageName,
                                protocol,
                                typeName,
                                to,
                                key,
                                fieldDefinitionContext.name().getText(),
                                from
                        );
                    } else {
                        builder.beginControlFlow("if(jsonValue.asJsonObject().containsKey($S) && !jsonValue.asJsonObject().isNull($S))", from, from)
                                .addStatement("field.getValue().asJsonObject().put($S, jsonValue.asJsonObject().get($S))", to, from)
                                .addStatement("loader.register($S, $S, $S, $S, field.getValue())",
                                        packageName,
                                        protocol,
                                        typeName,
                                        key
                                )
                                .endControlFlow();
                    }
                } else if (manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
                    if (anchor) {
                        builder.addStatement("$L(field.getValue(), loader, jsonPointer + \"/\" + $S)", fieldParameterName, fieldDefinitionContext.name().getText());
                    } else {
                        builder.addStatement("$L(field.getValue(), loader, jsonPointer + \"/\" + $S, jsonValue.asJsonObject().get($S))", fieldParameterName, fieldDefinitionContext.name().getText(), fieldDefinitionContext.name().getText());
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

    private MethodSpec buildListTypeMethod(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext, boolean anchor) {
        String typeParameterName = typeManager.typeToLowerCamelName(objectTypeDefinitionContext.name().getText());
        String listTypeParameterName = typeParameterName.concat("List");
        MethodSpec.Builder builder = MethodSpec.methodBuilder(listTypeParameterName)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get(JsonValue.class), "valueWithVariable")
                .addParameter(ClassName.get(MutationDataLoader.class), "loader")
                .addParameter(ClassName.get(String.class), "jsonPointer");

        if (anchor) {
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

    private MethodSpec buildHandleMethod(boolean anchor) {

        MethodSpec.Builder builder = MethodSpec.methodBuilder("handle")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get(GraphqlParser.OperationDefinitionContext.class), "operationDefinition")
                .addParameter(ClassName.get(MutationDataLoader.class), "loader");

        if (anchor) {
            builder.addStatement("$T operation = new $T(operationDefinition)", ClassName.get(Operation.class), ClassName.get(Operation.class))
                    .addStatement("$T operationArguments = loader.buildOperationArguments(operation)", ClassName.get(JsonObject.class))
                    .returns(ParameterizedTypeName.get(ClassName.get(Mono.class), ClassName.get(Operation.class)));
        } else {
            builder.addParameter(ClassName.get(Operation.class), "operation")
                    .addParameter(ClassName.get(JsonValue.class), "jsonValue")
                    .returns(ParameterizedTypeName.get(ClassName.get(Mono.class), ClassName.get(JsonObject.class)));
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
                builder.addStatement("$L(operationArguments.get(selectionName), loader, \"/\" + selectionName)", typeMethodName);
            } else {
                builder.addStatement("$L(operation.getField(selectionName).getArguments(), loader, \"/\" + selectionName, jsonValue.asJsonObject().get(selectionName))", typeMethodName);
            }
            index++;
        }
        builder.endControlFlow()
                .endControlFlow();

        if (anchor) {
            builder.addStatement("return loader.backup().then(loader.load(operationArguments)).map(jsonValue -> loader.dispatchOperationArguments(jsonValue, operation))");
        } else {
            builder.addStatement("return loader.load().thenReturn(jsonValue.asJsonObject())");
        }
        return builder.build();
    }
}
