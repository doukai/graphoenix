package io.graphoenix.java.generator.implementer;

import com.google.common.base.CaseFormat;
import com.squareup.javapoet.*;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.dto.type.OperationType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.tinylog.Logger;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.graphoenix.core.error.GraphQLErrorType.*;
import static io.graphoenix.spi.constant.Hammurabi.AGGREGATE_SUFFIX;
import static io.graphoenix.spi.constant.Hammurabi.INTROSPECTION_PREFIX;
import static io.graphoenix.spi.dto.type.OperationType.MUTATION;
import static io.graphoenix.spi.dto.type.OperationType.QUERY;

@ApplicationScoped
public class RpcRequestHandlerBuilder {

    private final IGraphQLDocumentManager manager;
    private final TypeManager typeManager;
    private GraphQLConfig graphQLConfig;

    @Inject
    public RpcRequestHandlerBuilder(IGraphQLDocumentManager manager, TypeManager typeManager) {
        this.manager = manager;
        this.typeManager = typeManager;
    }

    public RpcRequestHandlerBuilder setConfiguration(GraphQLConfig graphQLConfig) {
        this.graphQLConfig = graphQLConfig;
        this.typeManager.setGraphQLConfig(graphQLConfig);
        return this;
    }

    public void writeToFiler(Filer filer) throws IOException {
        this.buildQueryTypeServiceImplClass().writeTo(filer);
        Logger.info("QueryTypeServiceImpl build success");
        this.buildMutationTypeServiceImplClass().writeTo(filer);
        Logger.info("MutationTypeServiceImpl build success");
    }

    private JavaFile buildQueryTypeServiceImplClass() {
        TypeSpec typeSpec = buildRpcRequestHandler(QUERY);
        return JavaFile.builder(graphQLConfig.getHandlerPackageName(), typeSpec).build();
    }

    private JavaFile buildMutationTypeServiceImplClass() {
        TypeSpec typeSpec = buildRpcRequestHandler(MUTATION);
        return JavaFile.builder(graphQLConfig.getHandlerPackageName(), typeSpec).build();
    }

    private TypeSpec buildRpcRequestHandler(OperationType operationType) {
        String className;
        List<MethodSpec> methodSpecs;
        switch (operationType) {
            case QUERY:
                className = "RpcQueryRequestHandler";
                methodSpecs = buildQueryTypeMethods();
                break;
            case MUTATION:
                className = "RpcMutationRequestHandler";
                methodSpecs = buildMutationTypeMethods();
                break;
            default:
                throw new GraphQLErrors(UNSUPPORTED_OPERATION_TYPE);
        }

        return TypeSpec.classBuilder(className)
                .addAnnotation(ApplicationScoped.class)
                .addModifiers(Modifier.PUBLIC)
                .addField(
                        FieldSpec.builder(
                                ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(graphQLConfig.getHandlerPackageName(), "RpcInputObjectHandler")),
                                "inputObjectHandler",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).build()
                )
                .addField(
                        FieldSpec.builder(
                                ClassName.get(String.class),
                                "EMPTY",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).initializer("\"\"").build()
                )
                .addField(
                        FieldSpec.builder(
                                ClassName.get(String.class),
                                "QUERY",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).initializer("\"query\"").build()
                )
                .addField(
                        FieldSpec.builder(
                                ClassName.get(String.class),
                                "MUTATION",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).initializer("\"mutation\"").build()
                )
                .addField(
                        FieldSpec.builder(
                                ClassName.get(String.class),
                                "SPACE",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).initializer("\" \"").build()
                )
                .addField(
                        FieldSpec.builder(
                                ClassName.get(String.class),
                                "COLON",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).initializer("\": \"").build()
                )
                .addField(
                        FieldSpec.builder(
                                ClassName.get(String.class),
                                "COMMA",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).initializer("\", \"").build()
                )
                .addField(
                        FieldSpec.builder(
                                ClassName.get(String.class),
                                "QUOTATION",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).initializer("\"\\\"\"").build()
                )
                .addField(
                        FieldSpec.builder(
                                ClassName.get(String.class),
                                "BRACKETS_START",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).initializer("\"(\"").build()
                )
                .addField(
                        FieldSpec.builder(
                                ClassName.get(String.class),
                                "BRACKETS_END",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).initializer("\")\"").build()
                )
                .addField(
                        FieldSpec.builder(
                                ClassName.get(String.class),
                                "CURLY_BRACKETS_START",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).initializer("\"{\"").build()
                )
                .addField(
                        FieldSpec.builder(
                                ClassName.get(String.class),
                                "CURLY_BRACKETS_END",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).initializer("\"}\"").build()
                )
                .addField(
                        FieldSpec.builder(
                                ClassName.get(String.class),
                                "SQUARE_BRACKETS_START",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).initializer("\"[\"").build()
                )
                .addField(
                        FieldSpec.builder(
                                ClassName.get(String.class),
                                "SQUARE_BRACKETS_END",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).initializer("\"]\"").build()
                )
                .addMethod(buildConstructor())
                .addMethods(methodSpecs)
                .build();
    }

    private MethodSpec buildConstructor() {
        return MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(graphQLConfig.getHandlerPackageName(), "RpcInputObjectHandler")), "inputObjectHandler")
                .addStatement("this.inputObjectHandler = inputObjectHandler")
                .build();
    }

    private List<MethodSpec> buildQueryTypeMethods() {
        return manager.getQueryOperationTypeName().flatMap(manager::getObject)
                .orElseThrow(() -> new GraphQLErrors(QUERY_TYPE_NOT_EXIST))
                .fieldsDefinition().fieldDefinition().stream()
                .map(fieldDefinitionContext -> buildTypeMethod(fieldDefinitionContext, QUERY))
                .collect(Collectors.toList());
    }

    private List<MethodSpec> buildMutationTypeMethods() {
        return manager.getMutationOperationTypeName().flatMap(manager::getObject)
                .orElseThrow(() -> new GraphQLErrors(MUTATION_TYPE_NOT_EXIST))
                .fieldsDefinition().fieldDefinition().stream()
                .map(fieldDefinitionContext -> buildTypeMethod(fieldDefinitionContext, MUTATION))
                .collect(Collectors.toList());
    }

    private MethodSpec buildTypeMethod(GraphqlParser.FieldDefinitionContext fieldDefinitionContext, OperationType operationType) {
        String requestParameterName = "request";
        ClassName requestClassName = ClassName.get(graphQLConfig.getGrpcPackageName(), getRpcRequestClassName(fieldDefinitionContext, operationType));
        MethodSpec.Builder builder = MethodSpec.methodBuilder(getRpcFieldMethodName(fieldDefinitionContext))
                .addModifiers(Modifier.PUBLIC)
                .addParameter(requestClassName, requestParameterName)
                .returns(ClassName.get(String.class))
                .addStatement("$T stringBuilder = new $T()", ClassName.get(StringBuilder.class), ClassName.get(StringBuilder.class))
                .addStatement("stringBuilder.append(QUERY).append(CURLY_BRACKETS_START).append($S)", fieldDefinitionContext.name().getText());

        if (fieldDefinitionContext.argumentsDefinition() != null) {
            builder.addStatement("stringBuilder.append(BRACKETS_START)");
            List<GraphqlParser.InputValueDefinitionContext> inputValueDefinitionContexts = fieldDefinitionContext.argumentsDefinition().inputValueDefinition();
            for (GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext : inputValueDefinitionContexts) {
                CodeBlock codeBlock;
                String rpcEnumValueSuffixName = getRpcEnumValueSuffixName(inputValueDefinitionContext.type());
                String inputObjectFieldMethodName = getRpcInputObjectLowerCamelName(inputValueDefinitionContext.type());
                if (manager.fieldTypeIsList(inputValueDefinitionContext.type())) {
                    String rpcGetInputValueListName = getRpcGetInputValueListName(inputValueDefinitionContext);
                    if (manager.isScalar(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {
                        if (manager.getFieldTypeName(inputValueDefinitionContext.type()).equals("String")) {
                            codeBlock = CodeBlock.of("stringBuilder.append($S).append(COLON).append(SQUARE_BRACKETS_START).append($L.$L().stream().map(item -> QUOTATION + item + QUOTATION).collect($T.joining(COMMA))).append(SQUARE_BRACKETS_END).append(SPACE)",
                                    inputValueDefinitionContext.name().getText(),
                                    requestParameterName,
                                    rpcGetInputValueListName,
                                    ClassName.get(Collectors.class)
                            );
                        } else {
                            codeBlock = CodeBlock.of("stringBuilder.append($S).append(COLON).append(SQUARE_BRACKETS_START).append($L.$L().stream().map(item -> item + EMPTY).collect($T.joining(COMMA))).append(SQUARE_BRACKETS_END).append(SPACE)",
                                    inputValueDefinitionContext.name().getText(),
                                    requestParameterName,
                                    rpcGetInputValueListName,
                                    ClassName.get(Collectors.class)
                            );
                        }
                    } else if (manager.isEnum(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {
                        codeBlock = CodeBlock.of("stringBuilder.append($S).append(COLON).append(SQUARE_BRACKETS_START).append($L.$L().stream().map(item -> item.getValueDescriptor().getName().replaceFirst($S, EMPTY)).collect($T.joining(COMMA))).append(SQUARE_BRACKETS_END).append(SPACE)",
                                inputValueDefinitionContext.name().getText(),
                                requestParameterName,
                                rpcGetInputValueListName,
                                rpcEnumValueSuffixName,
                                ClassName.get(Collectors.class)
                        );
                    } else if (manager.isInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {
                        codeBlock = CodeBlock.of("stringBuilder.append($S).append(COLON).append(SQUARE_BRACKETS_START).append($L.$L().stream().map(item -> inputObjectHandler.get().$L(item)).collect($T.joining(COMMA))).append(SQUARE_BRACKETS_END).append(SPACE)",
                                inputValueDefinitionContext.name().getText(),
                                requestParameterName,
                                rpcGetInputValueListName,
                                inputObjectFieldMethodName,
                                ClassName.get(Collectors.class)
                        );
                    } else {
                        throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE);
                    }
                    if (inputValueDefinitionContext.type().nonNullType() == null) {
                        builder.beginControlFlow("if ($L.$L() > 0)", requestParameterName, getRpcGetInputValueCountName(inputValueDefinitionContext))
                                .addStatement(codeBlock)
                                .endControlFlow();
                    } else {
                        builder.addStatement(codeBlock);
                    }
                } else {
                    String rpcGetInputValueName = getRpcGetInputValueName(inputValueDefinitionContext);
                    if (manager.isScalar(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {
                        if (manager.getFieldTypeName(inputValueDefinitionContext.type()).equals("String")) {
                            codeBlock = CodeBlock.of("stringBuilder.append($S).append(COLON).append(QUOTATION).append($L.$L()).append(QUOTATION).append(SPACE)",
                                    inputValueDefinitionContext.name().getText(),
                                    requestParameterName,
                                    rpcGetInputValueName
                            );
                        } else {
                            codeBlock = CodeBlock.of("stringBuilder.append($S).append(COLON).append($L.$L()).append(SPACE)",
                                    inputValueDefinitionContext.name().getText(),
                                    requestParameterName,
                                    rpcGetInputValueName
                            );
                        }
                    } else if (manager.isEnum(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {
                        codeBlock = CodeBlock.of("stringBuilder.append($S).append(COLON).append($L.$L().getValueDescriptor().getName().replaceFirst($S, EMPTY)).append(SPACE)",
                                inputValueDefinitionContext.name().getText(),
                                requestParameterName,
                                rpcGetInputValueName,
                                rpcEnumValueSuffixName
                        );
                    } else if (manager.isInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {
                        codeBlock = CodeBlock.of("stringBuilder.append($S).append(COLON).append(inputObjectHandler.get().$L($L.$L())).append(SPACE)",
                                inputValueDefinitionContext.name().getText(),
                                inputObjectFieldMethodName,
                                requestParameterName,
                                rpcGetInputValueName
                        );
                    } else {
                        throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE);
                    }
                    if (inputValueDefinitionContext.type().nonNullType() == null) {
                        builder.beginControlFlow("if ($L.$L())", requestParameterName, getRpcHasInputValueName(inputValueDefinitionContext))
                                .addStatement(codeBlock)
                                .endControlFlow();
                    } else {
                        builder.addStatement(codeBlock);
                    }
                }
            }
            builder.addStatement("stringBuilder.append(BRACKETS_END).append(CURLY_BRACKETS_END)");
        }
        if (manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
            builder.beginControlFlow("if ($L.hasSelectionSet())", requestParameterName)
                    .addStatement("stringBuilder.append($L.getSelectionSet())", requestParameterName)
                    .nextControlFlow("else")
                    .addStatement("stringBuilder.append($S)", buildSelectionSet(fieldDefinitionContext.type()))
                    .endControlFlow();
        }
        builder.addStatement("return stringBuilder.toString()");
        return builder.build();
    }

    private String buildSelectionSet(GraphqlParser.TypeContext typeContext) {
        return buildSelectionSet(typeContext, 0, 0);
    }

    private String buildSelectionSet(GraphqlParser.TypeContext typeContext, int level, int layers) {
        String typeName = manager.getFieldTypeName(typeContext);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{");

        String fieldNames = manager.getFields(typeName)
                .filter(fieldDefinitionContext -> manager.isNotFunctionField(typeName, fieldDefinitionContext.name().getText()))
                .filter(fieldDefinitionContext -> manager.isNotConnectionField(typeName, fieldDefinitionContext.name().getText()))
                .filter(fieldDefinitionContext -> !fieldDefinitionContext.name().getText().endsWith(AGGREGATE_SUFFIX))
                .flatMap(fieldDefinitionContext -> {
                            if (manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
                                if (level <= layers) {
                                    return Stream.of(buildSelectionSet(fieldDefinitionContext.type(), level + 1, layers));
                                } else {
                                    return Stream.empty();
                                }
                            } else {
                                return Stream.of(fieldDefinitionContext.name().getText());
                            }
                        }
                )
                .collect(Collectors.joining(" "));

        stringBuilder.append(fieldNames);
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    private String getRpcFieldMethodName(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        String name = fieldDefinitionContext.name().getText();
        if (name.startsWith(INTROSPECTION_PREFIX)) {
            return "intro".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name.replaceFirst(INTROSPECTION_PREFIX, "")));
        }
        return name;
    }

    private String getRpcRequestClassName(GraphqlParser.FieldDefinitionContext fieldDefinitionContext, OperationType operationType) {
        String name = fieldDefinitionContext.name().getText();
        switch (operationType) {
            case QUERY:
                if (name.startsWith(INTROSPECTION_PREFIX)) {
                    return "QueryIntro".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name.replaceFirst(INTROSPECTION_PREFIX, ""))).concat("Request");
                }
                return "Query".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name.replaceFirst(INTROSPECTION_PREFIX, ""))).concat("Request");
            case MUTATION:
                if (name.startsWith(INTROSPECTION_PREFIX)) {
                    return "MutationIntro".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name.replaceFirst(INTROSPECTION_PREFIX, ""))).concat("Request");
                }
                return "Mutation".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name.replaceFirst(INTROSPECTION_PREFIX, ""))).concat("Request");
            default:
                throw new GraphQLErrors(UNSUPPORTED_OPERATION_TYPE);
        }
    }

    private String getRpcInputObjectName(GraphqlParser.TypeContext typeContext) {
        String name = manager.getFieldTypeName(typeContext);
        if (name.startsWith(INTROSPECTION_PREFIX)) {
            return "Intro".concat(name.replaceFirst(INTROSPECTION_PREFIX, ""));
        }
        return name;
    }

    private String getRpcInputObjectLowerCamelName(GraphqlParser.TypeContext typeContext) {
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, getRpcInputObjectName(typeContext));
    }

    private String getRpcEnumValueSuffixName(GraphqlParser.TypeContext typeContext) {
        return "_".concat(CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, getRpcInputObjectName(typeContext)));
    }

    private String getRpcInputValueName(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        String name = inputValueDefinitionContext.name().getText();
        if (name.startsWith(INTROSPECTION_PREFIX)) {
            return "intro".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name.replaceFirst(INTROSPECTION_PREFIX, "")));
        }
        return name;
    }

    private String getRpcGetInputValueName(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return "get".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, getRpcInputValueName(inputValueDefinitionContext)));
    }

    private String getRpcHasInputValueName(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return "has".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, getRpcInputValueName(inputValueDefinitionContext)));
    }

    private String getRpcGetInputValueListName(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return "get".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, getRpcInputValueName(inputValueDefinitionContext))).concat("List");
    }

    private String getRpcGetInputValueCountName(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return "get".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, getRpcInputValueName(inputValueDefinitionContext))).concat("Count");
    }
}
