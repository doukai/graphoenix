package io.graphoenix.java.generator.implementer;

import com.google.common.base.CaseFormat;
import com.squareup.javapoet.*;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.core.utils.CodecUtil;
import io.graphoenix.core.utils.DocumentUtil;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.handler.OperationHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.json.bind.Jsonb;
import org.tinylog.Logger;
import reactor.core.publisher.Mono;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.graphoenix.core.error.GraphQLErrorType.MUTATION_TYPE_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.QUERY_TYPE_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.UNSUPPORTED_FIELD_TYPE;
import static io.graphoenix.spi.constant.Hammurabi.AGGREGATE_SUFFIX;
import static io.graphoenix.spi.constant.Hammurabi.INTROSPECTION_PREFIX;

@ApplicationScoped
public class RpcServiceImplementer {

    private final IGraphQLDocumentManager manager;
    private final TypeManager typeManager;
    private GraphQLConfig graphQLConfig;

    @Inject
    public RpcServiceImplementer(IGraphQLDocumentManager manager, TypeManager typeManager) {
        this.manager = manager;
        this.typeManager = typeManager;
    }

    public RpcServiceImplementer setConfiguration(GraphQLConfig graphQLConfig) {
        this.graphQLConfig = graphQLConfig;
        this.typeManager.setGraphQLConfig(graphQLConfig);
        return this;
    }

    public void writeToFiler(Filer filer) throws IOException {
        this.buildQueryTypeServiceImplClass().writeTo(filer);
        Logger.info("QueryTypeServiceImpl build success");
    }

    private JavaFile buildQueryTypeServiceImplClass() {
        TypeSpec typeSpec = buildQueryTypeServiceImpl();
        return JavaFile.builder(graphQLConfig.getHandlerPackageName(), typeSpec).build();
    }

    private TypeSpec buildQueryTypeServiceImpl() {
        return TypeSpec.classBuilder("QueryTypeServiceImpl")
                .superclass(ClassName.get(graphQLConfig.getGrpcPackageName(), "ReactorQueryTypeServiceGrpc", "QueryTypeServiceImplBase"))
                .addModifiers(Modifier.PUBLIC)
                .addFields(buildQueryFields())
                .addField(
                        FieldSpec.builder(
                                ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(graphQLConfig.getHandlerPackageName(), "RpcRequestHandler")),
                                "requestHandler",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).build()
                )
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
                                ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(graphQLConfig.getHandlerPackageName(), "RpcObjectHandler")),
                                "rpcObjectHandler",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).build()
                )
                .addField(
                        FieldSpec.builder(
                                ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(OperationHandler.class)),
                                "operationHandler",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).build()
                )
                .addField(
                        FieldSpec.builder(
                                ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(graphQLConfig.getHandlerPackageName(), "InvokeHandler")),
                                "invokeHandler",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).build()
                )
                .addField(
                        FieldSpec.builder(
                                ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(Jsonb.class)),
                                "jsonb",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).build()
                )
                .addMethod(buildConstructor())
                .addMethods(buildQueryTypeMethods())
                .build();
    }

    private MethodSpec buildConstructor() {
        return MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(graphQLConfig.getHandlerPackageName(), "RpcRequestHandler")), "requestHandler")
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(graphQLConfig.getHandlerPackageName(), "RpcInputObjectHandler")), "inputObjectHandler")
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(graphQLConfig.getHandlerPackageName(), "RpcObjectHandler")), "rpcObjectHandler")
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(OperationHandler.class)), "operationHandler")
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(graphQLConfig.getHandlerPackageName(), "InvokeHandler")), "invokeHandler")
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(Jsonb.class)), "jsonb")
                .addStatement("this.requestHandler = requestHandler")
                .addStatement("this.inputObjectHandler = inputObjectHandler")
                .addStatement("this.rpcObjectHandler = rpcObjectHandler")
                .addStatement("this.operationHandler = operationHandler")
                .addStatement("this.invokeHandler = invokeHandler")
                .addStatement("this.jsonb = jsonb")
                .build();
    }

    private Set<FieldSpec> buildQueryFields() {
        return manager.getFields(manager.getQueryOperationTypeName().orElseThrow(() -> new GraphQLErrors(QUERY_TYPE_NOT_EXIST)))
                .filter(fieldDefinitionContext -> fieldDefinitionContext.directives() != null)
                .filter(fieldDefinitionContext -> fieldDefinitionContext.directives().directive().stream().anyMatch(directiveContext -> directiveContext.name().getText().equals("invoke")))
                .map(typeManager::getClassName)
                .distinct()
                .collect(Collectors.toList()).stream()
                .map(className ->
                        FieldSpec.builder(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.bestGuess(className)), typeManager.typeToLowerCamelName(ClassName.bestGuess(className).simpleName()))
                                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                                .build()
                )
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<FieldSpec> buildMutationFields() {
        return manager.getFields(manager.getMutationOperationTypeName().orElseThrow(() -> new GraphQLErrors(MUTATION_TYPE_NOT_EXIST)))
                .filter(fieldDefinitionContext -> fieldDefinitionContext.directives() != null)
                .filter(fieldDefinitionContext -> fieldDefinitionContext.directives().directive().stream().anyMatch(directiveContext -> directiveContext.name().getText().equals("invoke")))
                .map(typeManager::getClassName)
                .distinct()
                .collect(Collectors.toList()).stream()
                .map(className ->
                        FieldSpec.builder(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.bestGuess(className)), typeManager.typeToLowerCamelName(ClassName.bestGuess(className).simpleName()))
                                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                                .build()
                )
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<MethodSpec> buildQueryTypeMethods() {
        return manager.getQueryOperationTypeName().flatMap(manager::getObject)
                .orElseThrow(() -> new GraphQLErrors(QUERY_TYPE_NOT_EXIST))
                .fieldsDefinition().fieldDefinition().stream()
                .map(this::buildTypeMethod)
                .collect(Collectors.toList());
    }

    private MethodSpec buildTypeMethod(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        String methodName = getRpcName(fieldDefinitionContext.type());
        String requestParameterName = "request";
        ParameterizedTypeName requestClassName = ParameterizedTypeName.get(ClassName.get(Mono.class), ClassName.get(graphQLConfig.getGrpcPackageName(), getRpcQueryRequestClassName(fieldDefinitionContext)));
        ParameterizedTypeName responseClassName = ParameterizedTypeName.get(ClassName.get(Mono.class), ClassName.get(graphQLConfig.getGrpcPackageName(), getRpcQueryResponseClassName(fieldDefinitionContext)));
        MethodSpec.Builder builder = MethodSpec.methodBuilder(getRpcName(fieldDefinitionContext))
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(requestClassName, requestParameterName)
                .returns(responseClassName);

        CodeBlock codeBlock;
        if (manager.isInvokeField(fieldDefinitionContext)) {

            String className = typeManager.getClassName(fieldDefinitionContext);
            String methodName2 = typeManager.getMethodName(fieldDefinitionContext);
            List<AbstractMap.SimpleEntry<String, String>> parameters = typeManager.getParameters(fieldDefinitionContext);
            String returnClassName = typeManager.getReturnClassName(fieldDefinitionContext);

            CodeBlock invokeCodeBlock = CodeBlock.of("return $L.get().$L($L)",
                    typeManager.getTypeNameByString(returnClassName),
                    typeManager.typeToLowerCamelName(ClassName.bestGuess(className).simpleName()),
                    methodName2,
                    CodeBlock.join(
                            parameters.stream()
                                    .flatMap(parameter ->
                                            fieldDefinitionContext.argumentsDefinition().inputValueDefinition().stream()
                                                    .filter(inputValueDefinitionContext -> inputValueDefinitionContext.name().getText().equals(parameter.getKey()))
                                    )
                                    .map(inputValueDefinitionContext -> {
                                                String fieldTypeName = manager.getFieldTypeName(inputValueDefinitionContext.type());
                                                if (manager.isScalar(fieldTypeName)) {
                                                    if (fieldTypeName.equals("DateTime") || fieldTypeName.equals("Timestamp") || fieldTypeName.equals("Date") || fieldTypeName.equals("Time")) {
                                                        return CodeBlock.of("$T.CODEC_UTIL.$L($L.$L())",
                                                                ClassName.get(CodecUtil.class),
                                                                getDecodeName(fieldTypeName),
                                                                requestParameterName,
                                                                getRpcInputValueGetterName(inputValueDefinitionContext)
                                                        );
                                                    } else {
                                                        return CodeBlock.of("$L.$L()", requestParameterName, getRpcInputValueGetterName(inputValueDefinitionContext));
                                                    }
                                                } else if (manager.isEnum(fieldTypeName)) {
                                                    return CodeBlock.of("$T.valueOf($L.$L().name())",
                                                            ClassName.get(graphQLConfig.getEnumTypePackageName(), fieldTypeName),
                                                            requestParameterName,
                                                            getRpcInputValueGetterName(inputValueDefinitionContext)
                                                    );
                                                } else if (manager.isInputObject(fieldTypeName)) {
                                                    return CodeBlock.of("rpcInputObjectHandler.get().$L($L.$L())",
                                                            getRpcInputObjectLowerCamelName(inputValueDefinitionContext.type()).concat("ToInputObject"),
                                                            requestParameterName,
                                                            getRpcInputValueGetterName(inputValueDefinitionContext)
                                                    );
                                                } else {
                                                    throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE);
                                                }
                                            }
                                    )
                                    .collect(Collectors.toList()), ", ")
            );

            if (manager.fieldTypeIsList(fieldDefinitionContext.type())) {
                codeBlock = CodeBlock.join(
                        List.of(
                                invokeCodeBlock,
                                CodeBlock.of(".map(queryType -> queryType.$L().stream().map(item -> rpcObjectHandler.get().$L(item)).collect($T.toList()))",
                                        getFieldGetterName(fieldDefinitionContext),
                                        methodName,
                                        ClassName.get(Collectors.class)
                                ),
                                CodeBlock.of(".map(result -> $T.newBuilder().$L(result).build())",
                                        ClassName.get(graphQLConfig.getGrpcPackageName(), getRpcQueryResponseClassName(fieldDefinitionContext)),
                                        getRpcFieldAddAllName(fieldDefinitionContext)
                                )
                        ),
                        System.lineSeparator()
                );
            } else {
                codeBlock = CodeBlock.join(
                        List.of(
                                invokeCodeBlock,
                                CodeBlock.of(".map(queryType -> rpcObjectHandler.get().$L(queryType.$L()))", methodName, getFieldGetterName(fieldDefinitionContext)),
                                CodeBlock.of(".map(result -> $T.newBuilder().$L(result).build())",
                                        ClassName.get(graphQLConfig.getGrpcPackageName(), getRpcQueryResponseClassName(fieldDefinitionContext)),
                                        getRpcFieldSetterName(fieldDefinitionContext)
                                )
                        ),
                        System.lineSeparator()
                );
            }

        } else {
            if (manager.fieldTypeIsList(fieldDefinitionContext.type())) {
                codeBlock = CodeBlock.join(
                        List.of(
                                CodeBlock.of("return $L.map(requestHandler.get()::$L)", requestParameterName, getRpcName(fieldDefinitionContext)),
                                CodeBlock.of(".map($T.DOCUMENT_UTIL::graphqlToOperation)", ClassName.get(DocumentUtil.class)),
                                CodeBlock.of(".flatMap(operationHandler.get()::query)"),
                                CodeBlock.of(".map(jsonSting -> jsonb.get().fromJson(jsonSting, $T.class))",
                                        ClassName.get(
                                                graphQLConfig.getObjectTypePackageName(),
                                                manager.getQueryOperationTypeName().orElseThrow(() -> new GraphQLErrors(QUERY_TYPE_NOT_EXIST))
                                        )
                                ),
                                CodeBlock.of(".map(queryType -> queryType.$L().stream().map(item -> rpcObjectHandler.get().$L(item)).collect($T.toList()))",
                                        getFieldGetterName(fieldDefinitionContext),
                                        methodName,
                                        ClassName.get(Collectors.class)
                                ),
                                CodeBlock.of(".map(result -> $T.newBuilder().$L(result).build())",
                                        ClassName.get(graphQLConfig.getGrpcPackageName(), getRpcQueryResponseClassName(fieldDefinitionContext)),
                                        getRpcFieldAddAllName(fieldDefinitionContext)
                                )
                        ),
                        System.lineSeparator()
                );
            } else {
                codeBlock = CodeBlock.join(
                        List.of(
                                CodeBlock.of("return $L.map(requestHandler.get()::$L)", requestParameterName, getRpcName(fieldDefinitionContext)),
                                CodeBlock.of(".map($T.DOCUMENT_UTIL::graphqlToOperation)", ClassName.get(DocumentUtil.class)),
                                CodeBlock.of(".flatMap(operationHandler.get()::query)"),
                                CodeBlock.of(".map(jsonSting -> jsonb.get().fromJson(jsonSting, $T.class))",
                                        ClassName.get(
                                                graphQLConfig.getObjectTypePackageName(),
                                                manager.getQueryOperationTypeName().orElseThrow(() -> new GraphQLErrors(QUERY_TYPE_NOT_EXIST))
                                        )
                                ),
                                CodeBlock.of(".map(queryType -> rpcObjectHandler.get().$L(queryType.$L()))", methodName, getFieldGetterName(fieldDefinitionContext)),
                                CodeBlock.of(".map(result -> $T.newBuilder().$L(result).build())",
                                        ClassName.get(graphQLConfig.getGrpcPackageName(), getRpcQueryResponseClassName(fieldDefinitionContext)),
                                        getRpcFieldSetterName(fieldDefinitionContext)
                                )
                        ),
                        System.lineSeparator()
                );
            }
        }
        return builder.addStatement(codeBlock).build();
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

    private String getRpcName(GraphqlParser.TypeContext typeContext) {
        String name = manager.getFieldTypeName(typeContext);
        if (name.startsWith(INTROSPECTION_PREFIX)) {
            return "intro".concat(name.replaceFirst(INTROSPECTION_PREFIX, ""));
        }
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, name);
    }

    private String getRpcName(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        String name = fieldDefinitionContext.name().getText();
        if (name.startsWith(INTROSPECTION_PREFIX)) {
            return "intro".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name.replaceFirst(INTROSPECTION_PREFIX, "")));
        }
        return name;
    }

    private String getRpcQueryRequestClassName(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        String name = fieldDefinitionContext.name().getText();
        if (name.startsWith(INTROSPECTION_PREFIX)) {
            return "QueryIntro".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name.replaceFirst(INTROSPECTION_PREFIX, ""))).concat("Request");
        }
        return "Query".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name.replaceFirst(INTROSPECTION_PREFIX, ""))).concat("Request");
    }

    private String getRpcMutationRequestClassName(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        String name = fieldDefinitionContext.name().getText();
        if (name.startsWith(INTROSPECTION_PREFIX)) {
            return "MutationIntro".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name.replaceFirst(INTROSPECTION_PREFIX, ""))).concat("Request");
        }
        return "Mutation".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name.replaceFirst(INTROSPECTION_PREFIX, ""))).concat("Request");
    }

    private String getRpcQueryResponseClassName(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        String name = fieldDefinitionContext.name().getText();
        if (name.startsWith(INTROSPECTION_PREFIX)) {
            return "QueryIntro".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name.replaceFirst(INTROSPECTION_PREFIX, ""))).concat("Response");
        }
        return "Query".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name.replaceFirst(INTROSPECTION_PREFIX, ""))).concat("Response");
    }

    private String getRpcMutationResponseClassName(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        String name = fieldDefinitionContext.name().getText();
        if (name.startsWith(INTROSPECTION_PREFIX)) {
            return "MutationIntro".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name.replaceFirst(INTROSPECTION_PREFIX, ""))).concat("Response");
        }
        return "Mutation".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name.replaceFirst(INTROSPECTION_PREFIX, ""))).concat("Response");
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

    private String getFieldGetterName(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        String name = fieldDefinitionContext.name().getText();
        if (name.startsWith(INTROSPECTION_PREFIX)) {
            return "get__".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name.replaceFirst(INTROSPECTION_PREFIX, "")));
        }
        return "get".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name));
    }

    private String getRpcFieldGetterName(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        String name = fieldDefinitionContext.name().getText();
        if (name.startsWith(INTROSPECTION_PREFIX)) {
            return "getIntro".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name.replaceFirst(INTROSPECTION_PREFIX, "")));
        }
        return "get".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name));
    }

    private String getRpcInputValueGetterName(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        String name = inputValueDefinitionContext.name().getText();
        if (name.startsWith(INTROSPECTION_PREFIX)) {
            return "getIntro".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name.replaceFirst(INTROSPECTION_PREFIX, "")));
        }
        return "get".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name));
    }

    private String getRpcFieldSetterName(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        String name = fieldDefinitionContext.name().getText();
        if (name.startsWith(INTROSPECTION_PREFIX)) {
            return "setIntro".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name.replaceFirst(INTROSPECTION_PREFIX, "")));
        }
        return "set".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name));
    }

    private String getRpcFieldAddAllName(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        String name = fieldDefinitionContext.name().getText();
        if (name.startsWith(INTROSPECTION_PREFIX)) {
            return "addAllIntro".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name.replaceFirst(INTROSPECTION_PREFIX, "")));
        }
        return "addAll".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name));
    }

    private String getDecodeName(String fieldTypeName) {
        if (fieldTypeName.equals("DateTime")) {
            return "decodeLocalDateTime";
        } else if (fieldTypeName.equals("Timestamp")) {
            return "decodeLocalDateTime";
        } else if (fieldTypeName.equals("Date")) {
            return "decodeLocalDate";
        } else if (fieldTypeName.equals("Time")) {
            return "decodeLocalTime";
        }
        throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE);
    }
}
