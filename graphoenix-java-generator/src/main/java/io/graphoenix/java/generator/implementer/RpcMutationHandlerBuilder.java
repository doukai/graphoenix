package io.graphoenix.java.generator.implementer;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.core.handler.GraphQLFieldFormatter;
import io.graphoenix.core.operation.Argument;
import io.graphoenix.core.operation.Field;
import io.graphoenix.core.operation.ValueWithVariable;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
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

import static io.graphoenix.core.error.GraphQLErrorType.ARGUMENT_NOT_EXIST;
import static io.graphoenix.core.utils.DocumentUtil.DOCUMENT_UTIL;
import static io.graphoenix.spi.constant.Hammurabi.GRPC_DIRECTIVE_NAME;

@ApplicationScoped
public class RpcMutationHandlerBuilder {

    private final IGraphQLDocumentManager manager;
    private final TypeManager typeManager;
    private GraphQLConfig graphQLConfig;

    @Inject
    public RpcMutationHandlerBuilder(IGraphQLDocumentManager manager, TypeManager typeManager) {
        this.manager = manager;
        this.typeManager = typeManager;
    }

    public RpcMutationHandlerBuilder setConfiguration(GraphQLConfig graphQLConfig) {
        this.graphQLConfig = graphQLConfig;
        this.typeManager.setGraphQLConfig(graphQLConfig);
        return this;
    }

    public void writeToFiler(Filer filer) throws IOException {
        this.buildClass().writeTo(filer);
        Logger.info("RpcMutationHandler build success");
    }

    private JavaFile buildClass() {
        TypeSpec typeSpec = buildRpcMutationHandler();

        return JavaFile.builder(graphQLConfig.getHandlerPackageName(), typeSpec).build();
    }

    private TypeSpec buildRpcMutationHandler() {
        TypeSpec.Builder builder = TypeSpec.classBuilder("RpcMutationHandler")
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
                .addMethods(buildTypeMethods())
                .addMethods(buildListTypeMethods());

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

    private List<MethodSpec> buildTypeMethods() {
        return manager.getObjects()
                .filter(objectTypeDefinitionContext ->
                        !manager.isQueryOperationType(objectTypeDefinitionContext.name().getText()) &&
                                !manager.isMutationOperationType(objectTypeDefinitionContext.name().getText()) &&
                                !manager.isSubscriptionOperationType(objectTypeDefinitionContext.name().getText())
                )
                .flatMap(objectTypeDefinitionContext ->
                        Stream.of(
                                buildTypeFieldMethod(objectTypeDefinitionContext, true),
                                buildTypeMethod(objectTypeDefinitionContext, true)
                        )
                )
                .collect(Collectors.toList());
    }

    private List<MethodSpec> buildListTypeMethods() {
        return manager.getObjects()
                .filter(objectTypeDefinitionContext ->
                        !manager.isQueryOperationType(objectTypeDefinitionContext.name().getText()) &&
                                !manager.isMutationOperationType(objectTypeDefinitionContext.name().getText()) &&
                                !manager.isSubscriptionOperationType(objectTypeDefinitionContext.name().getText())
                )
                .map(this::buildListTypeMethod)
                .collect(Collectors.toList());
    }

    private MethodSpec buildTypeFieldMethod(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext, boolean anchor) {
        String typeParameterName = typeManager.typeToLowerCamelName(objectTypeDefinitionContext.name().getText());
        MethodSpec.Builder builder = MethodSpec.methodBuilder(typeParameterName)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get(Field.class), "field")
                .addParameter(ClassName.get(graphQLConfig.getHandlerPackageName(), "RpcMutationDataLoader"), "loader");

        builder.beginControlFlow("if (field != null && field.getArguments() != null && field.getArguments().size() > 0)")
                .beginControlFlow("for ($T argument : field.getArguments())", ClassName.get(Argument.class));

        int index = 0;
        List<GraphqlParser.FieldDefinitionContext> fieldDefinitionContextList = objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                .filter(fieldDefinitionContext -> manager.isGrpcField(fieldDefinitionContext) || manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type())))
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
                    String packageName = getPackageName(fieldDefinitionContext);
                    String from = getFrom(fieldDefinitionContext);
                    String to = getTo(fieldDefinitionContext);

                    if (anchor) {
                        builder.addStatement("loader.$L(argument.getValueWithVariable()).subscribe(result -> field.getOrCreateArgument($S).setValueWithVariable((result.asJsonObject().getString($S))))",
                                getTypeListMethodName(packageName, typeName),
                                from,
                                to
                        );
                    } else {
                        builder.addStatement("field.getOrCreateArgument($S).setValueWithVariable((result.asJsonObject().getString($S)))", to, from)
                                .addStatement("loader.$L(argument.getValueWithVariable())", getTypeListMethodName(packageName, typeName));
                    }
                } else if (manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
                    builder.addStatement("$L(argument.getValueWithVariable(), loader)", fieldParameterName.concat("List"));
                }
            } else {
                if (manager.isGrpcField(fieldDefinitionContext)) {
                    String typeName = manager.getFieldTypeName(fieldDefinitionContext.type());
                    String packageName = getPackageName(fieldDefinitionContext);
                    String from = getFrom(fieldDefinitionContext);
                    String to = getTo(fieldDefinitionContext);

                    if (anchor) {
                        builder.addStatement("loader.$L(argument.getValueWithVariable()).subscribe(result -> field.getOrCreateArgument($S).setValueWithVariable((result.asJsonObject().getString($S))))",
                                getTypeMethodName(packageName, typeName),
                                from,
                                to
                        );
                    } else {
                        builder.addStatement("field.getOrCreateArgument($S).setValueWithVariable((result.asJsonObject().getString($S)))", to, from)
                                .addStatement("loader.$L(argument.getValueWithVariable())", getTypeMethodName(packageName, typeName));
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
                .addParameter(ClassName.get(graphQLConfig.getHandlerPackageName(), "RpcMutationDataLoader"), "loader");

        builder.beginControlFlow("if (valueWithVariable != null && valueWithVariable.isObject() && valueWithVariable.asObject().size() > 0)")
                .beginControlFlow("for ($T field : valueWithVariable.asObject().entrySet())", ParameterizedTypeName.get(Map.Entry.class, String.class, ValueWithVariable.class));

        int index = 0;
        List<GraphqlParser.FieldDefinitionContext> fieldDefinitionContextList = objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                .filter(fieldDefinitionContext -> manager.isGrpcField(fieldDefinitionContext) || manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type())))
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
                    String packageName = getPackageName(fieldDefinitionContext);
                    String from = getFrom(fieldDefinitionContext);
                    String to = getTo(fieldDefinitionContext);

                    if (anchor) {
                        builder.addStatement("loader.$L(field.getValue()).subscribe(result -> field.getOrCreateArgument($S).setValueWithVariable((result.asJsonObject().getString($S))))",
                                getTypeListMethodName(packageName, typeName),
                                from,
                                to
                        );
                    } else {
                        builder.addStatement("field.getOrCreateArgument($S).setValueWithVariable((result.asJsonObject().getString($S)))", to, from)
                                .addStatement("loader.$L(field.getValue())", getTypeListMethodName(packageName, typeName));
                    }
                } else if (manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
                    builder.addStatement("$L(field.getValue(), loader)", fieldParameterName.concat("List"));
                }
            } else {
                if (manager.isGrpcField(fieldDefinitionContext)) {
                    String typeName = manager.getFieldTypeName(fieldDefinitionContext.type());
                    String packageName = getPackageName(fieldDefinitionContext);
                    String from = getFrom(fieldDefinitionContext);
                    String to = getTo(fieldDefinitionContext);

                    if (anchor) {
                        builder.addStatement("loader.$L(field.getValue()).subscribe(result -> field.getOrCreateArgument($S).setValueWithVariable((result.asJsonObject().getString($S))))",
                                getTypeMethodName(packageName, typeName),
                                from,
                                to
                        );
                    } else {
                        builder.addStatement("field.getOrCreateArgument($S).setValueWithVariable((result.asJsonObject().getString($S)))", to, from)
                                .addStatement("loader.$L(field.getValue())", getTypeMethodName(packageName, typeName));
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

    private MethodSpec buildListTypeMethod(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        String typeParameterName = typeManager.typeToLowerCamelName(objectTypeDefinitionContext.name().getText());
        String listTypeParameterName = typeParameterName.concat("List");
        MethodSpec.Builder builder = MethodSpec.methodBuilder(listTypeParameterName)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get(ValueWithVariable.class), "valueWithVariable")
                .addParameter(ClassName.get(graphQLConfig.getHandlerPackageName(), "RpcMutationDataLoader"), "loader");

        builder.beginControlFlow("if (valueWithVariable != null && valueWithVariable.isArray())")
                .addStatement("valueWithVariable.asArray().getValueWithVariables().forEach(item -> $L(item, loader))",
                        typeManager.typeToLowerCamelName(objectTypeDefinitionContext.name().getText())
                )
                .endControlFlow();
        return builder.build();
    }

    private String packageNameToUnderline(String packageName) {
        return String.join("_", packageName.split("\\."));
    }

    private String getPackageName(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return fieldDefinitionContext.directives().directive().stream()
                .filter(directiveContext -> directiveContext.name().getText().equals(GRPC_DIRECTIVE_NAME))
                .flatMap(directiveContext -> directiveContext.arguments().argument().stream())
                .filter(argumentContext -> argumentContext.name().getText().equals("packageName"))
                .filter(argumentContext -> argumentContext.valueWithVariable().StringValue() != null)
                .map(argumentContext -> DOCUMENT_UTIL.getStringValue(argumentContext.valueWithVariable().StringValue()))
                .findFirst()
                .orElseThrow(() -> new GraphQLErrors(ARGUMENT_NOT_EXIST.bind("packageName")));
    }

    private String getFrom(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return fieldDefinitionContext.directives().directive().stream()
                .filter(directiveContext -> directiveContext.name().getText().equals(GRPC_DIRECTIVE_NAME))
                .flatMap(directiveContext -> directiveContext.arguments().argument().stream())
                .filter(argumentContext -> argumentContext.name().getText().equals("from"))
                .filter(argumentContext -> argumentContext.valueWithVariable().StringValue() != null)
                .map(argumentContext -> DOCUMENT_UTIL.getStringValue(argumentContext.valueWithVariable().StringValue()))
                .findFirst()
                .orElseThrow(() -> new GraphQLErrors(ARGUMENT_NOT_EXIST.bind("from")));
    }

    private String getTo(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return fieldDefinitionContext.directives().directive().stream()
                .filter(directiveContext -> directiveContext.name().getText().equals(GRPC_DIRECTIVE_NAME))
                .flatMap(directiveContext -> directiveContext.arguments().argument().stream())
                .filter(argumentContext -> argumentContext.name().getText().equals("to"))
                .filter(argumentContext -> argumentContext.valueWithVariable().StringValue() != null)
                .map(argumentContext -> DOCUMENT_UTIL.getStringValue(argumentContext.valueWithVariable().StringValue()))
                .findFirst()
                .orElseThrow(() -> new GraphQLErrors(ARGUMENT_NOT_EXIST.bind("to")));
    }

    private String getTypeMethodName(String packageName, String typeName) {
        return packageNameToUnderline(packageName).concat("_").concat(typeName);
    }

    private String getTypeListMethodName(String packageName, String typeName) {
        return getTypeMethodName(packageName, typeName).concat("List");
    }
}
