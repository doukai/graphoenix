package io.graphoenix.java.generator.implementer.grpc;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.handler.GraphQLFieldFormatter;
import io.graphoenix.core.operation.Argument;
import io.graphoenix.core.operation.Field;
import io.graphoenix.core.operation.ValueWithVariable;
import io.graphoenix.java.generator.implementer.TypeManager;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.constant.Hammurabi;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.json.spi.JsonProvider;
import org.tinylog.Logger;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
                                ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(GraphQLFieldFormatter.class)),
                                "formatter",
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
                .addMethods(buildTypeMethods(anchor));

        return builder.build();
    }

    private MethodSpec buildConstructor() {
        return MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Inject.class)
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(IGraphQLDocumentManager.class)), "manager")
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(GraphQLFieldFormatter.class)), "formatter")
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(JsonProvider.class)), "jsonProvider")
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get("io.graphoenix.grpc.client", "ChannelManager")), "channelManager")
                .addStatement("this.manager = manager")
                .addStatement("this.formatter = formatter")
                .addStatement("this.jsonProvider = jsonProvider")
                .addStatement("this.channelManager = channelManager")
                .build();
    }

    private List<MethodSpec> buildTypeMethods(boolean anchor) {
        return manager.getObjects()
                .filter(objectTypeDefinitionContext ->
                        !manager.isQueryOperationType(objectTypeDefinitionContext.name().getText()) &&
                                !manager.isMutationOperationType(objectTypeDefinitionContext.name().getText()) &&
                                !manager.isSubscriptionOperationType(objectTypeDefinitionContext.name().getText())
                )
                .flatMap(objectTypeDefinitionContext ->
                        Stream.of(
                                buildTypeFieldMethod(objectTypeDefinitionContext, anchor),
                                buildTypeMethod(objectTypeDefinitionContext, anchor),
                                buildListTypeFieldMethod(objectTypeDefinitionContext),
                                buildListTypeMethod(objectTypeDefinitionContext)
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

        builder.beginControlFlow("if (field != null && field.getArguments() != null && field.getArguments().size() > 0)")
                .beginControlFlow("for ($T argument : field.getArguments())", ClassName.get(Argument.class));

        int index = 0;
        List<GraphqlParser.FieldDefinitionContext> fieldDefinitionContextList = objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                .filter(fieldDefinitionContext -> manager.isGrpcField(fieldDefinitionContext) || manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type())))
                .filter(fieldDefinitionContext -> grpcNameUtil.getAnchor(fieldDefinitionContext) == anchor)
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
                        builder.addStatement("loader.$L(argument.getValueWithVariable())", grpcNameUtil.getTypeListMethodName(packageName, typeName));
                    } else {
                        builder.addStatement("argument.getValueWithVariable().asArray().forEach(item -> item.asObject().put($S, field.getValueWithVariableOrEmpty($S)))", to, from)
                                .addStatement("loader.$L(argument.getValueWithVariable())", grpcNameUtil.getTypeListMethodName(packageName, typeName));
                    }
                } else if (manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
                    builder.addStatement("$L(argument.getValueWithVariable(), loader)", fieldParameterName.concat("List"));
                }
            } else {
                if (manager.isGrpcField(fieldDefinitionContext)) {
                    String typeName = manager.getFieldTypeName(fieldDefinitionContext.type());
                    String packageName = grpcNameUtil.getPackageName(fieldDefinitionContext);
                    String from = grpcNameUtil.getFrom(fieldDefinitionContext);
                    String to = grpcNameUtil.getTo(fieldDefinitionContext);

                    if (anchor) {
                        builder.addStatement("loader.$L(argument.getValueWithVariable()).subscribe(result -> field.getOrCreateArgument($S).setValueWithVariable(result.asJsonObject().getString($S)))",
                                grpcNameUtil.getTypeMethodName(packageName, typeName),
                                from,
                                to
                        );
                    } else {
                        builder.addStatement("argument.getValueWithVariable().asObject().put($S, field.getValueWithVariableOrEmpty($S))", to, from)
                                .addStatement("loader.$L(argument.getValueWithVariable())", grpcNameUtil.getTypeMethodName(packageName, typeName));
                    }
                } else if (manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
                    builder.addStatement("$L(argument.getValueWithVariable(), loader)", fieldParameterName);
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

        builder.beginControlFlow("if (valueWithVariable != null && valueWithVariable.isObject() && valueWithVariable.asObject().size() > 0)")
                .beginControlFlow("for ($T field : valueWithVariable.asObject().entrySet())", ParameterizedTypeName.get(Map.Entry.class, String.class, ValueWithVariable.class));

        int index = 0;
        List<GraphqlParser.FieldDefinitionContext> fieldDefinitionContextList = objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                .filter(fieldDefinitionContext -> manager.isGrpcField(fieldDefinitionContext) || manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type())))
                .filter(fieldDefinitionContext -> grpcNameUtil.getAnchor(fieldDefinitionContext) == anchor)
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
                        builder.addStatement("loader.$L(field.getValue())", grpcNameUtil.getTypeListMethodName(packageName, typeName));
                    } else {
                        builder.addStatement("field.getValue().asArray().forEach(item -> item.asObject().put($S, valueWithVariable.asObject().get($S)))", to, from)
                                .addStatement("loader.$L(field.getValue())", grpcNameUtil.getTypeListMethodName(packageName, typeName));
                    }
                } else if (manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
                    builder.addStatement("$L(field.getValue(), loader)", fieldParameterName.concat("List"));
                }
            } else {
                if (manager.isGrpcField(fieldDefinitionContext)) {
                    String typeName = manager.getFieldTypeName(fieldDefinitionContext.type());
                    String packageName = grpcNameUtil.getPackageName(fieldDefinitionContext);
                    String from = grpcNameUtil.getFrom(fieldDefinitionContext);
                    String to = grpcNameUtil.getTo(fieldDefinitionContext);

                    if (anchor) {
                        builder.addStatement("loader.$L(field.getValue()).subscribe(result -> valueWithVariable.asObject().put($S, new ValueWithVariable(result.asJsonObject().getString($S))))",
                                grpcNameUtil.getTypeMethodName(packageName, typeName),
                                from,
                                to
                        );
                    } else {
                        builder.addStatement("field.getValue().asObject().put($S, valueWithVariable.asObject().get($S))", to, from)
                                .addStatement("loader.$L(field.getValue())", grpcNameUtil.getTypeMethodName(packageName, typeName));
                    }
                } else if (manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
                    builder.addStatement("$L(field.getValue(), loader)", fieldParameterName);
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

    private MethodSpec buildListTypeFieldMethod(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        String typeParameterName = typeManager.typeToLowerCamelName(objectTypeDefinitionContext.name().getText());
        String listTypeParameterName = typeParameterName.concat("List");
        MethodSpec.Builder builder = MethodSpec.methodBuilder(listTypeParameterName)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get(Field.class), "field")
                .addParameter(ClassName.get(graphQLConfig.getHandlerPackageName(), "GrpcMutationDataLoader"), "loader");

        builder.beginControlFlow(
                "if (field != null && field.getArgument($T.LIST_INPUT_NAME).isPresent() && field.getArgument($T.LIST_INPUT_NAME).get().getValueWithVariable().isArray())",
                ClassName.get(Hammurabi.class),
                ClassName.get(Hammurabi.class)
        ).addStatement("field.getArgument($T.LIST_INPUT_NAME).get().getValueWithVariable().asArray().forEach(item -> $L(item, loader))",
                ClassName.get(Hammurabi.class),
                typeManager.typeToLowerCamelName(objectTypeDefinitionContext.name().getText())
        ).endControlFlow();
        return builder.build();
    }

    private MethodSpec buildListTypeMethod(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        String typeParameterName = typeManager.typeToLowerCamelName(objectTypeDefinitionContext.name().getText());
        String listTypeParameterName = typeParameterName.concat("List");
        MethodSpec.Builder builder = MethodSpec.methodBuilder(listTypeParameterName)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get(ValueWithVariable.class), "valueWithVariable")
                .addParameter(ClassName.get(graphQLConfig.getHandlerPackageName(), "GrpcMutationDataLoader"), "loader");

        builder.beginControlFlow("if (valueWithVariable != null && valueWithVariable.isArray())")
                .addStatement("valueWithVariable.asArray().forEach(item -> $L(item, loader))",
                        typeManager.typeToLowerCamelName(objectTypeDefinitionContext.name().getText())
                )
                .endControlFlow();
        return builder.build();
    }
}
