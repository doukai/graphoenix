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
import io.graphoenix.core.operation.EnumValue;
import io.graphoenix.core.operation.Field;
import io.graphoenix.core.operation.Operation;
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
    private final GrpcNameUtil grpcNameUtil;
    private final GraphQLConfig graphQLConfig;

    @Inject
    public GrpcRequestHandlerBuilder(IGraphQLDocumentManager manager, GrpcNameUtil grpcNameUtil, GraphQLConfig graphQLConfig) {
        this.manager = manager;
        this.grpcNameUtil = grpcNameUtil;
        this.graphQLConfig = graphQLConfig;
    }

    public void writeToFiler(Filer filer) throws IOException {
        this.buildGrpcQueryRequestHandler().writeTo(filer);
        Logger.info("GrpcQueryRequestHandler build success");
        this.buildGrpcMutationRequestHandler().writeTo(filer);
        Logger.info("GrpcMutationRequestHandler build success");
    }

    private JavaFile buildGrpcQueryRequestHandler() {
        TypeSpec typeSpec = buildGrpcRequestHandler(QUERY);
        return JavaFile.builder(graphQLConfig.getGrpcHandlerPackageName(), typeSpec).build();
    }

    private JavaFile buildGrpcMutationRequestHandler() {
        TypeSpec typeSpec = buildGrpcRequestHandler(MUTATION);
        return JavaFile.builder(graphQLConfig.getGrpcHandlerPackageName(), typeSpec).build();
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
                                ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(graphQLConfig.getGrpcHandlerPackageName(), "GrpcInputObjectHandler")),
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
                .addMethod(buildConstructor())
                .addMethods(methodSpecs)
                .build();
    }

    private MethodSpec buildConstructor() {
        return MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(graphQLConfig.getGrpcHandlerPackageName(), "GrpcInputObjectHandler")), "inputObjectHandler")
                .addStatement("this.inputObjectHandler = inputObjectHandler")
                .build();
    }

    private List<MethodSpec> buildQueryTypeMethods() {
        return manager.getQueryOperationTypeName()
                .flatMap(manager::getObject)
                .orElseThrow(() -> new GraphQLErrors(QUERY_TYPE_NOT_EXIST))
                .fieldsDefinition().fieldDefinition().stream()
                .map(fieldDefinitionContext -> buildTypeMethod(fieldDefinitionContext, QUERY))
                .collect(Collectors.toList());
    }

    private List<MethodSpec> buildMutationTypeMethods() {
        return manager.getMutationOperationTypeName()
                .flatMap(manager::getObject)
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
        ClassName requestClassName = ClassName.get(manager.getGrpcPackageName(fieldDefinitionContext).orElseGet(graphQLConfig::getGrpcPackageName), grpcNameUtil.getGrpcRequestClassName(fieldDefinitionContext, operationType));
        MethodSpec.Builder builder = MethodSpec.methodBuilder(grpcNameUtil.getGrpcFieldName(fieldDefinitionContext))
                .addModifiers(Modifier.PUBLIC)
                .addParameter(requestClassName, requestParameterName)
                .returns(ClassName.get(String.class))
                .addStatement("$T field = new $T()", ClassName.get(Field.class), ClassName.get(Field.class))
                .addStatement("field.setName($S)", fieldDefinitionContext.name().getText());

        if (fieldDefinitionContext.argumentsDefinition() != null) {
            List<GraphqlParser.InputValueDefinitionContext> inputValueDefinitionContexts = fieldDefinitionContext.argumentsDefinition().inputValueDefinition();
            for (GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext : inputValueDefinitionContexts) {
                CodeBlock codeBlock;
                String fieldTypeName = manager.getFieldTypeName(inputValueDefinitionContext.type());
                String grpcEnumValueSuffixName = grpcNameUtil.getGrpcEnumValueSuffixName(inputValueDefinitionContext.type());
                String inputObjectFieldMethodName = grpcNameUtil.getLowerCamelName(inputValueDefinitionContext.type());
                String grpcGetMethodName = grpcNameUtil.getGrpcGetMethodName(inputValueDefinitionContext);
                if (manager.fieldTypeIsList(inputValueDefinitionContext.type())) {
                    String grpcGetListMethodName = grpcNameUtil.getGrpcGetListMethodName(inputValueDefinitionContext);
                    if (manager.isScalar(fieldTypeName)) {
                        codeBlock = CodeBlock.of("field.addArgument($S, $L.$L())",
                                inputValueDefinitionContext.name().getText(),
                                requestParameterName,
                                grpcGetListMethodName
                        );
                    } else if (manager.isEnum(fieldTypeName)) {
                        codeBlock = CodeBlock.of("field.addArgument($S, $L.$L().stream().map(item -> item.getValueDescriptor().getName().replaceFirst($S, EMPTY)).collect($T.toList()))",
                                inputValueDefinitionContext.name().getText(),
                                requestParameterName,
                                grpcGetListMethodName,
                                grpcEnumValueSuffixName,
                                ClassName.get(Collectors.class)
                        );
                    } else if (manager.isInputObject(fieldTypeName)) {
                        codeBlock = CodeBlock.of("field.addArgument($S, $L.$L().stream().map(item -> inputObjectHandler.get().$L(item)).collect($T.toList()))",
                                inputValueDefinitionContext.name().getText(),
                                requestParameterName,
                                grpcGetListMethodName,
                                inputObjectFieldMethodName,
                                ClassName.get(Collectors.class)
                        );
                    } else {
                        throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE);
                    }
                    if (inputValueDefinitionContext.type().nonNullType() != null) {
                        builder.addStatement(codeBlock);
                    } else {
                        builder.beginControlFlow("if ($L.$L() > 0)", requestParameterName, grpcNameUtil.getGrpcGetCountMethodName(inputValueDefinitionContext))
                                .addStatement(codeBlock)
                                .endControlFlow();
                    }
                } else {
                    if (manager.isScalar(fieldTypeName)) {
                        codeBlock = CodeBlock.of("field.addArgument($S, $L.$L())",
                                inputValueDefinitionContext.name().getText(),
                                requestParameterName,
                                grpcGetMethodName
                        );
                    } else if (manager.isEnum(fieldTypeName)) {
                        codeBlock = CodeBlock.of("field.addArgument($S, new $T($L.$L().getValueDescriptor().getName().replaceFirst($S, EMPTY)))",
                                inputValueDefinitionContext.name().getText(),
                                ClassName.get(EnumValue.class),
                                requestParameterName,
                                grpcGetMethodName,
                                grpcEnumValueSuffixName
                        );
                    } else if (manager.isInputObject(fieldTypeName)) {
                        codeBlock = CodeBlock.of("field.addArgument($S, inputObjectHandler.get().$L($L.$L()))",
                                inputValueDefinitionContext.name().getText(),
                                inputObjectFieldMethodName,
                                requestParameterName,
                                grpcGetMethodName
                        );
                    } else {
                        throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE);
                    }
                    if (inputValueDefinitionContext.type().nonNullType() != null) {
                        builder.addStatement(codeBlock);
                    } else {
                        builder.beginControlFlow("if ($L.$L())", requestParameterName, grpcNameUtil.getGrpcHasMethodName(inputValueDefinitionContext))
                                .addStatement(codeBlock)
                                .endControlFlow();
                    }
                }
            }
        }
        if (manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
            builder.beginControlFlow("if ($L.hasSelectionSet())", requestParameterName)
                    .addStatement("field.setSelections($L.getSelectionSet())", requestParameterName)
                    .nextControlFlow("else")
                    .addStatement("field.setSelections($S)", buildSelectionSet(fieldDefinitionContext.type()))
                    .endControlFlow();
        }
        builder.addStatement("return new $T().setOperationType($L).addField(field).toString()", ClassName.get(Operation.class), operationName);
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
                                    return Stream.of(fieldDefinitionContext.name().getText() + buildSelectionSet(fieldDefinitionContext.type(), level + 1, layers));
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
