package io.graphoenix.java.generator.implementer.grpc;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.java.generator.implementer.TypeManager;
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

import static io.graphoenix.core.error.GraphQLErrorType.MUTATION_TYPE_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.QUERY_TYPE_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.UNSUPPORTED_FIELD_TYPE;
import static io.graphoenix.core.error.GraphQLErrorType.UNSUPPORTED_OPERATION_TYPE;
import static io.graphoenix.spi.constant.Hammurabi.AGGREGATE_SUFFIX;
import static io.graphoenix.spi.dto.type.OperationType.MUTATION;
import static io.graphoenix.spi.dto.type.OperationType.QUERY;

@ApplicationScoped
public class GrpcRequestHandlerBuilder {

    private final IGraphQLDocumentManager manager;
    private final TypeManager typeManager;
    private final GrpcNameUtil grpcNameUtil;
    private GraphQLConfig graphQLConfig;

    @Inject
    public GrpcRequestHandlerBuilder(IGraphQLDocumentManager manager, TypeManager typeManager, GrpcNameUtil grpcNameUtil) {
        this.manager = manager;
        this.typeManager = typeManager;
        this.grpcNameUtil = grpcNameUtil;
    }

    public GrpcRequestHandlerBuilder setConfiguration(GraphQLConfig graphQLConfig) {
        this.graphQLConfig = graphQLConfig;
        this.typeManager.setGraphQLConfig(graphQLConfig);
        return this;
    }

    public void writeToFiler(Filer filer) throws IOException {
        this.buildGrpcQueryRequestHandler().writeTo(filer);
        Logger.info("GrpcQueryRequestHandler build success");
        this.buildGrpcMutationRequestHandler().writeTo(filer);
        Logger.info("GrpcMutationRequestHandler build success");
    }

    private JavaFile buildGrpcQueryRequestHandler() {
        TypeSpec typeSpec = buildGrpcRequestHandler(QUERY);
        return JavaFile.builder(graphQLConfig.getHandlerPackageName(), typeSpec).build();
    }

    private JavaFile buildGrpcMutationRequestHandler() {
        TypeSpec typeSpec = buildGrpcRequestHandler(MUTATION);
        return JavaFile.builder(graphQLConfig.getHandlerPackageName(), typeSpec).build();
    }

    private TypeSpec buildGrpcRequestHandler(OperationType operationType) {
        String className;
        List<MethodSpec> methodSpecs;
        switch (operationType) {
            case QUERY:
                className = "GrpcQueryRequestHandler";
                methodSpecs = buildQueryTypeMethods();
                break;
            case MUTATION:
                className = "GrpcMutationRequestHandler";
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
                                ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(graphQLConfig.getHandlerPackageName(), "GrpcInputObjectHandler")),
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
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(graphQLConfig.getHandlerPackageName(), "GrpcInputObjectHandler")), "inputObjectHandler")
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
        String operationName;
        switch (operationType) {
            case QUERY:
                operationName = "QUERY";
                break;
            case MUTATION:
                operationName = "MUTATION";
                break;
            default:
                throw new GraphQLErrors(UNSUPPORTED_OPERATION_TYPE);
        }
        ClassName requestClassName = ClassName.get(graphQLConfig.getGrpcPackageName(), grpcNameUtil.getRpcRequestClassName(fieldDefinitionContext, operationType));
        MethodSpec.Builder builder = MethodSpec.methodBuilder(grpcNameUtil.getRpcFieldMethodName(fieldDefinitionContext))
                .addModifiers(Modifier.PUBLIC)
                .addParameter(requestClassName, requestParameterName)
                .returns(ClassName.get(String.class))
                .addStatement("$T operationBuilder = new $T()", ClassName.get(StringBuilder.class), ClassName.get(StringBuilder.class))
                .addStatement("operationBuilder.append($L).append(CURLY_BRACKETS_START).append($S)", operationName, fieldDefinitionContext.name().getText());

        if (fieldDefinitionContext.argumentsDefinition() != null) {
            builder.addStatement("$T argumentsBuilder = new $T()", ClassName.get(StringBuilder.class), ClassName.get(StringBuilder.class));
            List<GraphqlParser.InputValueDefinitionContext> inputValueDefinitionContexts = fieldDefinitionContext.argumentsDefinition().inputValueDefinition();
            for (GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext : inputValueDefinitionContexts) {
                CodeBlock codeBlock;
                String fieldTypeName = manager.getFieldTypeName(inputValueDefinitionContext.type());
                String rpcEnumValueSuffixName = grpcNameUtil.getRpcEnumValueSuffixName(inputValueDefinitionContext.type());
                String inputObjectFieldMethodName = grpcNameUtil.getRpcInputObjectLowerCamelName(inputValueDefinitionContext.type());
                if (manager.fieldTypeIsList(inputValueDefinitionContext.type())) {
                    String rpcGetInputValueListName = grpcNameUtil.getRpcGetInputValueListName(inputValueDefinitionContext);
                    if (manager.isScalar(fieldTypeName)) {
                        if (fieldTypeName.equals("String") || fieldTypeName.equals("DateTime") || fieldTypeName.equals("Timestamp") || fieldTypeName.equals("Date") || fieldTypeName.equals("Time")) {
                            codeBlock = CodeBlock.of("argumentsBuilder.append($S).append(COLON).append(SQUARE_BRACKETS_START).append($L.$L().stream().map(item -> QUOTATION + item + QUOTATION).collect($T.joining(COMMA))).append(SQUARE_BRACKETS_END).append(SPACE)",
                                    inputValueDefinitionContext.name().getText(),
                                    requestParameterName,
                                    rpcGetInputValueListName,
                                    ClassName.get(Collectors.class)
                            );
                        } else {
                            codeBlock = CodeBlock.of("argumentsBuilder.append($S).append(COLON).append(SQUARE_BRACKETS_START).append($L.$L().stream().map(item -> item + EMPTY).collect($T.joining(COMMA))).append(SQUARE_BRACKETS_END).append(SPACE)",
                                    inputValueDefinitionContext.name().getText(),
                                    requestParameterName,
                                    rpcGetInputValueListName,
                                    ClassName.get(Collectors.class)
                            );
                        }
                    } else if (manager.isEnum(fieldTypeName)) {
                        codeBlock = CodeBlock.of("argumentsBuilder.append($S).append(COLON).append(SQUARE_BRACKETS_START).append($L.$L().stream().map(item -> item.getValueDescriptor().getName().replaceFirst($S, EMPTY)).collect($T.joining(COMMA))).append(SQUARE_BRACKETS_END).append(SPACE)",
                                inputValueDefinitionContext.name().getText(),
                                requestParameterName,
                                rpcGetInputValueListName,
                                rpcEnumValueSuffixName,
                                ClassName.get(Collectors.class)
                        );
                    } else if (manager.isInputObject(fieldTypeName)) {
                        codeBlock = CodeBlock.of("argumentsBuilder.append($S).append(COLON).append(SQUARE_BRACKETS_START).append($L.$L().stream().map(item -> inputObjectHandler.get().$L(item)).collect($T.joining(COMMA))).append(SQUARE_BRACKETS_END).append(SPACE)",
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
                        builder.beginControlFlow("if ($L.$L() > 0)", requestParameterName, grpcNameUtil.getRpcGetInputValueCountName(inputValueDefinitionContext))
                                .addStatement(codeBlock)
                                .endControlFlow();
                    } else {
                        builder.addStatement(codeBlock);
                    }
                } else {
                    String rpcGetInputValueName = grpcNameUtil.getRpcGetInputValueName(inputValueDefinitionContext);
                    if (manager.isScalar(fieldTypeName)) {
                        if (fieldTypeName.equals("String") || fieldTypeName.equals("DateTime") || fieldTypeName.equals("Timestamp") || fieldTypeName.equals("Date") || fieldTypeName.equals("Time")) {
                            codeBlock = CodeBlock.of("argumentsBuilder.append($S).append(COLON).append(QUOTATION).append($L.$L()).append(QUOTATION).append(SPACE)",
                                    inputValueDefinitionContext.name().getText(),
                                    requestParameterName,
                                    rpcGetInputValueName
                            );
                        } else {
                            codeBlock = CodeBlock.of("argumentsBuilder.append($S).append(COLON).append($L.$L()).append(SPACE)",
                                    inputValueDefinitionContext.name().getText(),
                                    requestParameterName,
                                    rpcGetInputValueName
                            );
                        }
                    } else if (manager.isEnum(fieldTypeName)) {
                        codeBlock = CodeBlock.of("argumentsBuilder.append($S).append(COLON).append($L.$L().getValueDescriptor().getName().replaceFirst($S, EMPTY)).append(SPACE)",
                                inputValueDefinitionContext.name().getText(),
                                requestParameterName,
                                rpcGetInputValueName,
                                rpcEnumValueSuffixName
                        );
                    } else if (manager.isInputObject(fieldTypeName)) {
                        codeBlock = CodeBlock.of("argumentsBuilder.append($S).append(COLON).append(inputObjectHandler.get().$L($L.$L())).append(SPACE)",
                                inputValueDefinitionContext.name().getText(),
                                inputObjectFieldMethodName,
                                requestParameterName,
                                rpcGetInputValueName
                        );
                    } else {
                        throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE);
                    }
                    if (inputValueDefinitionContext.type().nonNullType() == null) {
                        builder.beginControlFlow("if ($L.$L())", requestParameterName, grpcNameUtil.getRpcHasInputValueName(inputValueDefinitionContext))
                                .addStatement(codeBlock)
                                .endControlFlow();
                    } else {
                        builder.addStatement(codeBlock);
                    }
                }
            }
            builder.beginControlFlow("if ($L.hasArguments())", requestParameterName)
                    .addStatement("argumentsBuilder.append(SPACE).append($L.getArguments())", requestParameterName)
                    .endControlFlow()
                    .beginControlFlow("if(argumentsBuilder.length() > 0)")
                    .addStatement("operationBuilder.append(BRACKETS_START).append(argumentsBuilder).append(BRACKETS_END)")
                    .endControlFlow();
        }
        if (manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
            builder.beginControlFlow("if ($L.hasSelectionSet())", requestParameterName)
                    .addStatement("operationBuilder.append($L.getSelectionSet())", requestParameterName)
                    .nextControlFlow("else")
                    .addStatement("operationBuilder.append($S)", buildSelectionSet(fieldDefinitionContext.type()))
                    .endControlFlow();
        }
        builder.addStatement("operationBuilder.append(CURLY_BRACKETS_END)")
                .addStatement("return operationBuilder.toString()");
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
                                    return Stream.of(fieldDefinitionContext.name().getText().concat(buildSelectionSet(fieldDefinitionContext.type(), level + 1, layers)));
                                } else {
                                    return Stream.empty();
                                }
                            } else {
                                return Stream.of(fieldDefinitionContext.name().getText());
                            }
                        }
                )
                .collect(Collectors.joining(" "));

        stringBuilder.append(fieldNames).append("}");
        return stringBuilder.toString();
    }
}
