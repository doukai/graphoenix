package io.graphoenix.java.generator.implementer;

import com.google.common.base.CaseFormat;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.core.handler.ArgumentBuilder;
import io.graphoenix.core.utils.CodecUtil;
import io.graphoenix.core.utils.DocumentUtil;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.handler.OperationHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.json.bind.Jsonb;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.tinylog.Logger;
import reactor.core.publisher.Flux;
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
    private List<String> invokeClassNames;

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
        invokeClassNames = manager.getFields(manager.getQueryOperationTypeName().orElseThrow(() -> new GraphQLErrors(QUERY_TYPE_NOT_EXIST)))
                .filter(fieldDefinitionContext -> fieldDefinitionContext.directives() != null)
                .filter(fieldDefinitionContext -> fieldDefinitionContext.directives().directive().stream().anyMatch(directiveContext -> directiveContext.name().getText().equals("invoke")))
                .map(typeManager::getClassName)
                .distinct()
                .collect(Collectors.toList());
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
                .addField(
                        FieldSpec.builder(
                                ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(ArgumentBuilder.class)),
                                "argumentBuilder",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).build()
                )
                .addMethod(buildConstructor())
                .addMethods(buildQueryTypeMethods())
                .build();
    }

    private MethodSpec buildConstructor() {
        MethodSpec.Builder builder = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(graphQLConfig.getHandlerPackageName(), "RpcRequestHandler")), "requestHandler")
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(graphQLConfig.getHandlerPackageName(), "RpcInputObjectHandler")), "inputObjectHandler")
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(graphQLConfig.getHandlerPackageName(), "RpcObjectHandler")), "rpcObjectHandler")
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(OperationHandler.class)), "operationHandler")
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(graphQLConfig.getHandlerPackageName(), "InvokeHandler")), "invokeHandler")
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(Jsonb.class)), "jsonb")
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(ArgumentBuilder.class)), "argumentBuilder")
                .addParameters(
                        invokeClassNames.stream()
                                .map(className ->
                                        ParameterSpec.builder(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.bestGuess(className)), typeManager.typeToLowerCamelName(ClassName.bestGuess(className).simpleName())).build()
                                )
                                .collect(Collectors.toList())
                )
                .addStatement("this.requestHandler = requestHandler")
                .addStatement("this.inputObjectHandler = inputObjectHandler")
                .addStatement("this.rpcObjectHandler = rpcObjectHandler")
                .addStatement("this.operationHandler = operationHandler")
                .addStatement("this.invokeHandler = invokeHandler")
                .addStatement("this.jsonb = jsonb")
                .addStatement("this.argumentBuilder = argumentBuilder");
        invokeClassNames.forEach(className -> builder.addStatement("this.$L = $L", typeManager.typeToLowerCamelName(ClassName.bestGuess(className).simpleName()), typeManager.typeToLowerCamelName(ClassName.bestGuess(className).simpleName())));
        return builder.build();
    }

    private Set<FieldSpec> buildQueryFields() {
        return invokeClassNames.stream()
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
        CodeBlock wrapperCodeBlock;
        String fieldTypeName1 = manager.getFieldTypeName(fieldDefinitionContext.type());


        if (manager.isInvokeField(fieldDefinitionContext)) {
            String className = typeManager.getClassName(fieldDefinitionContext);
            String methodName2 = typeManager.getMethodName(fieldDefinitionContext);
            List<AbstractMap.SimpleEntry<String, String>> parameters = typeManager.getParameters(fieldDefinitionContext);
            String returnClassName = typeManager.getReturnClassName(fieldDefinitionContext);

            CodeBlock invokeCodeBlock = CodeBlock.of(".map(selectionContext -> $L.get().$L($L))",
                    typeManager.typeToLowerCamelName(ClassName.bestGuess(className).simpleName()),
                    methodName2,
                    CodeBlock.join(parameters.stream()
                            .map(parameter ->
                                    CodeBlock.of("argumentBuilder.get().getArgument(selectionContext, $S, $T.class)",
                                            parameter.getKey(),
                                            typeManager.getClassNameByString(parameter.getValue())
                                    )
                            )
                            .collect(Collectors.toList()), ", ")
            );

            CodeBlock mapBlock;
            if (typeManager.getClassNameByString(returnClassName).canonicalName().equals(PublisherBuilder.class.getName())) {
                mapBlock = CodeBlock.of(".flatMap(result -> $T.from(result.buildRs()))", ClassName.get(Mono.class));
            } else if (typeManager.getClassNameByString(returnClassName).canonicalName().equals(Mono.class.getName())) {
                mapBlock = CodeBlock.of(".flatMap(result -> result)");
            } else if (typeManager.getClassNameByString(returnClassName).canonicalName().equals(Flux.class.getName())) {
                mapBlock = CodeBlock.of(".flatMap(result -> result.collectList())");
            } else {
                mapBlock = CodeBlock.of(".flatMap(result -> $T.just(result))", ClassName.get(Mono.class));
            }

            if (manager.fieldTypeIsList(fieldDefinitionContext.type())) {
                if (manager.isScalar(fieldTypeName1)) {
                    if (fieldTypeName1.equals("DateTime") || fieldTypeName1.equals("Timestamp") || fieldTypeName1.equals("Date") || fieldTypeName1.equals("Time")) {
                        wrapperCodeBlock = CodeBlock.of(".map(item -> item.stream().map($T.CODEC_UTIL::encode).collect($T.toList()))", ClassName.get(CodecUtil.class));
                    } else {
                        wrapperCodeBlock = CodeBlock.of(".map(item -> item)");
                    }
                } else if (manager.isEnum(fieldTypeName1)) {
                    wrapperCodeBlock = CodeBlock.of(".map(item -> item.stream().map(item::ordinal).map($T::forNumber).collect($T.toList()))",
                            ClassName.get(graphQLConfig.getGrpcPackageName(), getRpcObjectName(fieldDefinitionContext.type())),
                            getFieldGetterName(fieldDefinitionContext),
                            ClassName.get(Collectors.class)
                    );
                } else if (manager.isObject(fieldTypeName1)) {
                    wrapperCodeBlock = CodeBlock.of(".map(item -> item.stream().map(rpcObjectHandler.get()::$L).collect($T.toList()))", methodName, ClassName.get(Collectors.class));
                } else {
                    throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE);
                }

                codeBlock = CodeBlock.join(
                        List.of(
                                CodeBlock.of("return $L.map(requestHandler.get()::$L)", requestParameterName, getRpcName(fieldDefinitionContext)),
                                CodeBlock.of(".map($T.DOCUMENT_UTIL::graphqlToOperation)", ClassName.get(DocumentUtil.class)),
                                CodeBlock.of(".map(operationDefinitionContext -> operationDefinitionContext.selectionSet().selection(0))"),
                                invokeCodeBlock,
                                mapBlock,
                                wrapperCodeBlock,
                                CodeBlock.of(".map(item -> $T.newBuilder().$L(item).build())",
                                        ClassName.get(graphQLConfig.getGrpcPackageName(), getRpcQueryResponseClassName(fieldDefinitionContext)),
                                        getRpcFieldAddAllName(fieldDefinitionContext)
                                )
                        ),
                        System.lineSeparator()
                );
            } else {
                if (manager.isScalar(fieldTypeName1)) {
                    if (fieldTypeName1.equals("DateTime") || fieldTypeName1.equals("Timestamp") || fieldTypeName1.equals("Date") || fieldTypeName1.equals("Time")) {
                        wrapperCodeBlock = CodeBlock.of(".map(item -> $T.CODEC_UTIL.encode(item))", ClassName.get(CodecUtil.class));
                    } else {
                        wrapperCodeBlock = CodeBlock.of(".map(item -> item)");
                    }
                } else if (manager.isEnum(fieldTypeName1)) {
                    wrapperCodeBlock = CodeBlock.of(".map(item -> $T.forNumber(item.ordinal()))",
                            ClassName.get(graphQLConfig.getGrpcPackageName(), getRpcObjectName(fieldDefinitionContext.type())),
                            getFieldGetterName(fieldDefinitionContext)
                    );
                } else if (manager.isObject(fieldTypeName1)) {
                    wrapperCodeBlock = CodeBlock.of(".map(item -> rpcObjectHandler.get().$L(item))", methodName);
                } else {
                    throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE);
                }
                codeBlock = CodeBlock.join(
                        List.of(
                                CodeBlock.of("return $L.map(requestHandler.get()::$L)", requestParameterName, getRpcName(fieldDefinitionContext)),
                                CodeBlock.of(".map($T.DOCUMENT_UTIL::graphqlToOperation)", ClassName.get(DocumentUtil.class)),
                                CodeBlock.of(".map(operationDefinitionContext -> operationDefinitionContext.selectionSet().selection(0))"),
                                invokeCodeBlock,
                                mapBlock,
                                wrapperCodeBlock,
                                CodeBlock.of(".map(item -> $T.newBuilder().$L(item).build())",
                                        ClassName.get(graphQLConfig.getGrpcPackageName(), getRpcQueryResponseClassName(fieldDefinitionContext)),
                                        getRpcFieldSetterName(fieldDefinitionContext)
                                )
                        ),
                        System.lineSeparator()
                );
            }
        } else {
            if (manager.fieldTypeIsList(fieldDefinitionContext.type())) {
                if (manager.isScalar(fieldTypeName1)) {
                    if (fieldTypeName1.equals("DateTime") || fieldTypeName1.equals("Timestamp") || fieldTypeName1.equals("Date") || fieldTypeName1.equals("Time")) {
                        wrapperCodeBlock = CodeBlock.of(".map(queryType -> queryType.$L().stream().map(item -> $T.CODEC_UTIL.encode(item)).collect($T.toList()))", ClassName.get(CodecUtil.class), getFieldGetterName(fieldDefinitionContext), ClassName.get(Collectors.class));
                    } else {
                        wrapperCodeBlock = CodeBlock.of(".map(queryType -> queryType.$L())", getFieldGetterName(fieldDefinitionContext));
                    }
                } else if (manager.isEnum(fieldTypeName1)) {
                    wrapperCodeBlock = CodeBlock.of(".map(queryType -> queryType.$L().stream().map(item -> $T.forNumber(item.ordinal())).collect($T.toList()))",
                            ClassName.get(graphQLConfig.getGrpcPackageName(), getRpcObjectName(fieldDefinitionContext.type())),
                            getFieldGetterName(fieldDefinitionContext),
                            ClassName.get(Collectors.class)
                    );
                } else if (manager.isObject(fieldTypeName1)) {
                    wrapperCodeBlock = CodeBlock.of(".map(queryType -> queryType.$L().stream().map(item -> rpcObjectHandler.get().$L(item)).collect($T.toList()))", getFieldGetterName(fieldDefinitionContext), methodName, ClassName.get(Collectors.class));
                } else {
                    throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE);
                }
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
                                wrapperCodeBlock,
                                CodeBlock.of(".map(result -> $T.newBuilder().$L(result).build())",
                                        ClassName.get(graphQLConfig.getGrpcPackageName(), getRpcQueryResponseClassName(fieldDefinitionContext)),
                                        getRpcFieldAddAllName(fieldDefinitionContext)
                                )
                        ),
                        System.lineSeparator()
                );
            } else {
                if (manager.isScalar(fieldTypeName1)) {
                    if (fieldTypeName1.equals("DateTime") || fieldTypeName1.equals("Timestamp") || fieldTypeName1.equals("Date") || fieldTypeName1.equals("Time")) {
                        wrapperCodeBlock = CodeBlock.of(".map(queryType -> $T.CODEC_UTIL.encode(queryType.$L()))", ClassName.get(CodecUtil.class), getFieldGetterName(fieldDefinitionContext));
                    } else {
                        wrapperCodeBlock = CodeBlock.of(".map(queryType -> queryType.$L())", getFieldGetterName(fieldDefinitionContext));
                    }
                } else if (manager.isEnum(fieldTypeName1)) {
                    wrapperCodeBlock = CodeBlock.of(".map(queryType -> $T.forNumber(queryType.$L().ordinal()))",
                            ClassName.get(graphQLConfig.getGrpcPackageName(), getRpcObjectName(fieldDefinitionContext.type())),
                            getFieldGetterName(fieldDefinitionContext)
                    );
                } else if (manager.isObject(fieldTypeName1)) {
                    wrapperCodeBlock = CodeBlock.of(".map(queryType -> rpcObjectHandler.get().$L(queryType.$L()))", methodName, getFieldGetterName(fieldDefinitionContext));
                } else {
                    throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE);
                }
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
                                wrapperCodeBlock,
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

    private String getRpcObjectName(GraphqlParser.TypeContext typeContext) {
        String name = manager.getFieldTypeName(typeContext);
        if (name.startsWith(INTROSPECTION_PREFIX)) {
            return "Intro".concat(name.replaceFirst(INTROSPECTION_PREFIX, ""));
        }
        return name;
    }

}
