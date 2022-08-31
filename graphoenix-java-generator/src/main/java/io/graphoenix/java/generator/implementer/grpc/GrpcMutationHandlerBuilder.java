package io.graphoenix.java.generator.implementer.grpc;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.operation.Argument;
import io.graphoenix.core.operation.Field;
import io.graphoenix.core.operation.ValueWithVariable;
import io.graphoenix.java.generator.implementer.TypeManager;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static io.graphoenix.spi.constant.Hammurabi.AGGREGATE_SUFFIX;
import static io.graphoenix.spi.constant.Hammurabi.PAGE_INFO_NAME;

@ApplicationScoped
public class GrpcMutationHandlerBuilder {

    private final IGraphQLDocumentManager manager;
    private final TypeManager typeManager;
    private final GrpcNameUtil grpcNameUtil;
    private GraphQLConfig graphQLConfig;

    @Inject
    public GrpcMutationHandlerBuilder(IGraphQLDocumentManager manager, TypeManager typeManager, GrpcNameUtil grpcNameUtil) {
        this.manager = manager;
        this.typeManager = typeManager;
        this.grpcNameUtil = grpcNameUtil;
    }

    public GrpcMutationHandlerBuilder setConfiguration(GraphQLConfig graphQLConfig) {
        this.graphQLConfig = graphQLConfig;
        this.typeManager.setGraphQLConfig(graphQLConfig);
        return this;
    }

    public void writeToFiler(Filer filer) throws IOException {
        this.buildClass(true).writeTo(filer);
        Logger.info("GrpcMutationBeforeHandler build success");
        this.buildClass(false).writeTo(filer);
        Logger.info("GrpcMutationAfterHandler build success");
    }

    private JavaFile buildClass(boolean anchor) {
        TypeSpec typeSpec = buildGrpcMutationHandler(anchor);
        return JavaFile.builder(graphQLConfig.getHandlerPackageName(), typeSpec).build();
    }

    private TypeSpec buildGrpcMutationHandler(boolean anchor) {
        TypeSpec.Builder builder = TypeSpec.classBuilder(anchor ? "GrpcMutationBeforeHandler" : "GrpcMutationAfterHandler")
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
                .addParameter(ClassName.get(graphQLConfig.getHandlerPackageName(), "GrpcMutationDataLoader"), "loader");

        if (anchor) {
            builder.beginControlFlow("if (field != null && field.getArguments() != null && field.getArguments().size() > 0)");
        } else {
            builder.addParameter(ClassName.get(JsonValue.class), "jsonValue")
                    .beginControlFlow("if (field != null && field.getArguments() != null && field.getArguments().size() > 0 && jsonValue != null && jsonValue.getValueType().equals($T.ValueType.OBJECT))", ClassName.get(JsonValue.class));
        }
        builder.beginControlFlow("for ($T argument : field.getArguments())", ClassName.get(Argument.class));

        int index = 0;
        List<GraphqlParser.FieldDefinitionContext> fieldDefinitionContextList = objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                .filter(fieldDefinitionContext -> manager.isGrpcField(fieldDefinitionContext) && grpcNameUtil.getAnchor(fieldDefinitionContext) || !manager.isGrpcField(fieldDefinitionContext) && manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type())))
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

                    if (anchor) {
                        builder.addStatement("loader.$L(argument.getValueWithVariable(), $S)", grpcNameUtil.getTypeListMethodName(packageName, typeName), to);
                    } else {
                        builder.addStatement("argument.getValueWithVariable().asArray().forEach(item -> item.asObject().put($S, jsonValue.asJsonObject().get($S)))", to, from)
                                .addStatement("loader.$L(argument.getValueWithVariable(), $S)", grpcNameUtil.getTypeListMethodName(packageName, typeName), from);
                    }
                } else if (manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
                    if (anchor) {
                        builder.addStatement("$L(argument.getValueWithVariable(), loader)", fieldParameterName.concat("List"));
                    } else {
                        builder.addStatement("$L(argument.getValueWithVariable(), loader, jsonValue.asJsonObject().get($S))", fieldParameterName.concat("List"), fieldDefinitionContext.name().getText());
                    }
                }
            } else {
                if (manager.isGrpcField(fieldDefinitionContext)) {
                    String typeName = manager.getFieldTypeName(fieldDefinitionContext.type());
                    String packageName = grpcNameUtil.getPackageName(fieldDefinitionContext);
                    String from = grpcNameUtil.getFrom(fieldDefinitionContext);
                    String to = grpcNameUtil.getTo(fieldDefinitionContext);
                    if (anchor) {
                        builder.addStatement("loader.$L(argument.getValueWithVariable(), $S).subscribe(result -> field.getOrCreateArgument($S).setValueWithVariable(result))",
                                grpcNameUtil.getTypeMethodName(packageName, typeName),
                                to,
                                from
                        );
                    } else {
                        builder.addStatement("argument.getValueWithVariable().asObject().put($S, jsonValue.asJsonObject().get($S))", to, from)
                                .addStatement("loader.$L(argument.getValueWithVariable(), $S)", grpcNameUtil.getTypeMethodName(packageName, typeName), from);
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
                .addParameter(ClassName.get(graphQLConfig.getHandlerPackageName(), "GrpcMutationDataLoader"), "loader");

        if (anchor) {
            builder.beginControlFlow("if (valueWithVariable != null && valueWithVariable.isObject() && valueWithVariable.asObject().size() > 0)");
        } else {
            builder.addParameter(ClassName.get(JsonValue.class), "jsonValue")
                    .beginControlFlow("if (valueWithVariable != null && valueWithVariable.isObject() && valueWithVariable.asObject().size() > 0 && jsonValue != null && jsonValue.getValueType().equals($T.ValueType.OBJECT))", ClassName.get(JsonValue.class));
        }
        builder.beginControlFlow("for ($T field : valueWithVariable.asObject().entrySet())", ParameterizedTypeName.get(Map.Entry.class, String.class, ValueWithVariable.class));

        int index = 0;
        List<GraphqlParser.FieldDefinitionContext> fieldDefinitionContextList = objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                .filter(fieldDefinitionContext -> manager.isGrpcField(fieldDefinitionContext) && grpcNameUtil.getAnchor(fieldDefinitionContext) == anchor || !manager.isGrpcField(fieldDefinitionContext) && manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type())))
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

                    if (anchor) {
                        builder.addStatement("loader.$L(field.getValue(), $S)", grpcNameUtil.getTypeListMethodName(packageName, typeName), to);
                    } else {
                        builder.addStatement("field.getValue().asArray().forEach(item -> item.asObject().put($S, jsonValue.asJsonObject().get($S)))", to, from)
                                .addStatement("loader.$L(field.getValue(), $S)", grpcNameUtil.getTypeListMethodName(packageName, typeName), from);
                    }
                } else if (manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
                    if (anchor) {
                        builder.addStatement("$L(field.getValue(), loader)", fieldParameterName.concat("List"));
                    } else {
                        builder.addStatement("$L(field.getValue(), loader, jsonValue.asJsonObject().get($S))", fieldParameterName.concat("List"), fieldDefinitionContext.name().getText());
                    }
                }
            } else {
                if (manager.isGrpcField(fieldDefinitionContext)) {
                    String typeName = manager.getFieldTypeName(fieldDefinitionContext.type());
                    String packageName = grpcNameUtil.getPackageName(fieldDefinitionContext);
                    String from = grpcNameUtil.getFrom(fieldDefinitionContext);
                    String to = grpcNameUtil.getTo(fieldDefinitionContext);

                    if (anchor) {
                        builder.addStatement("loader.$L(field.getValue(), $S).subscribe(result -> valueWithVariable.asObject().put($S, new ValueWithVariable(result)))",
                                grpcNameUtil.getTypeMethodName(packageName, typeName),
                                to,
                                from
                        );
                    } else {
                        builder.addStatement("field.getValue().asObject().put($S, jsonValue.asJsonObject().get($S))", to, from)
                                .addStatement("loader.$L(field.getValue(), $S)", grpcNameUtil.getTypeMethodName(packageName, typeName), from);
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
                .addParameter(ClassName.get(graphQLConfig.getHandlerPackageName(), "GrpcMutationDataLoader"), "loader");

        if (anchor) {
            builder.beginControlFlow(
                    "if (field != null && field.getArgument($T.LIST_INPUT_NAME).isPresent() && field.getArgument($T.LIST_INPUT_NAME).get().getValueWithVariable().isArray())",
                    ClassName.get(Hammurabi.class),
                    ClassName.get(Hammurabi.class)
            ).addStatement("field.getArgument($T.LIST_INPUT_NAME).get().getValueWithVariable().asArray().forEach(item -> $L(item, loader))",
                    ClassName.get(Hammurabi.class),
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
                    .addStatement("$T.range(0, field.getArgument($T.LIST_INPUT_NAME).get().getValueWithVariable().asArray().size()).forEach(index -> $L(new $T<>(field.getArgument($T.LIST_INPUT_NAME).get().getValueWithVariable().asArray()).get(index), loader, jsonValue.asJsonArray().get(index)))",
                            ClassName.get(IntStream.class),
                            ClassName.get(Hammurabi.class),
                            typeManager.typeToLowerCamelName(objectTypeDefinitionContext.name().getText()),
                            ClassName.get(ArrayList.class),
                            ClassName.get(Hammurabi.class)
                    ).endControlFlow();
        }
        return builder.build();
    }

    private MethodSpec buildListTypeMethod(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext, boolean anchor) {
        String typeParameterName = typeManager.typeToLowerCamelName(objectTypeDefinitionContext.name().getText());
        String listTypeParameterName = typeParameterName.concat("List");
        MethodSpec.Builder builder = MethodSpec.methodBuilder(listTypeParameterName)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get(ValueWithVariable.class), "valueWithVariable")
                .addParameter(ClassName.get(graphQLConfig.getHandlerPackageName(), "GrpcMutationDataLoader"), "loader");

        if (anchor) {
            builder.beginControlFlow("if (valueWithVariable != null && valueWithVariable.isArray())")
                    .addStatement("valueWithVariable.asArray().forEach(item -> $L(item, loader))",
                            typeManager.typeToLowerCamelName(objectTypeDefinitionContext.name().getText())
                    )
                    .endControlFlow();
        } else {
            builder.addParameter(ClassName.get(JsonValue.class), "jsonValue")
                    .beginControlFlow("if (valueWithVariable != null && valueWithVariable.isArray() && jsonValue != null && jsonValue.getValueType().equals($T.ValueType.ARRAY))", ClassName.get(JsonValue.class))
                    .addStatement("$T.range(0, valueWithVariable.asArray().size()).forEach(index -> $L(new $T<>(valueWithVariable.asArray()).get(index), loader, jsonValue.asJsonArray().get(index)))",
                            ClassName.get(IntStream.class),
                            typeManager.typeToLowerCamelName(objectTypeDefinitionContext.name().getText()),
                            ClassName.get(ArrayList.class)
                    )
                    .endControlFlow();
        }
        return builder.build();
    }

    private MethodSpec buildHandleMethod(boolean anchor) {

        MethodSpec.Builder builder = MethodSpec.methodBuilder("handle")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get(GraphqlParser.OperationDefinitionContext.class), "operationDefinition")
                .addParameter(ClassName.get(graphQLConfig.getHandlerPackageName(), "GrpcMutationDataLoader"), "loader")
                .returns(ParameterizedTypeName.get(ClassName.get(Mono.class), ClassName.get(Void.class)))
                .beginControlFlow("for ($T selectionContext : operationDefinition.selectionSet().selection()) ", ClassName.get(GraphqlParser.SelectionContext.class));

        if (!anchor) {
            builder.addParameter(ClassName.get(JsonValue.class), "jsonValue")
                    .addStatement("$T selectionName = selectionContext.field().alias() != null ? selectionContext.field().alias().name().getText() : selectionContext.field().name().getText()", ClassName.get(String.class));
        }
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
                builder.addStatement("$L(new $T(selectionContext.field()), loader)", typeMethodName, ClassName.get(Field.class));
            } else {
                builder.addStatement("$L(new $T(selectionContext.field()), loader, jsonValue.asJsonObject().get(selectionName))", typeMethodName, ClassName.get(Field.class));
            }
            index++;
        }
        builder.endControlFlow()
                .endControlFlow()
                .addStatement("return loader.dispatch()");
        return builder.build();
    }
}
