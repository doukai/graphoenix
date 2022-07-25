package io.graphoenix.java.generator.implementer;

import com.google.common.base.CaseFormat;
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
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.tinylog.Logger;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static io.graphoenix.core.error.GraphQLErrorType.UNSUPPORTED_FIELD_TYPE;
import static io.graphoenix.spi.constant.Hammurabi.INTROSPECTION_PREFIX;

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
        this.buildClass().writeTo(filer);
        Logger.info("SelectionFilter build success");
    }

    private JavaFile buildClass() {
        TypeSpec typeSpec = buildSelectionFilter();
        return JavaFile.builder(graphQLConfig.getHandlerPackageName(), typeSpec).build();
    }

    private TypeSpec buildSelectionFilter() {
        return TypeSpec.classBuilder("RpcRequestHandler")
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
                .addMethod(buildConstructor())
                .addMethods(buildTypeMethods())
                .build();
    }

    private MethodSpec buildConstructor() {
        return MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Inject.class)
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(IGraphQLDocumentManager.class)), "manager")
                .addStatement("this.manager = manager")
                .build();
    }

    private List<MethodSpec> buildTypeMethods() {
        return manager.getInputObjects()
                .map(this::buildTypeMethod)
                .collect(Collectors.toList());
    }

    private String getRpcInputObjectName(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {
        String name = inputObjectTypeDefinitionContext.name().getText();
        if (name.startsWith(INTROSPECTION_PREFIX)) {
            return "Intro".concat(name.replaceFirst(INTROSPECTION_PREFIX, ""));
        }
        return name;
    }

    private String getRpcInputObjectLowerCamelName(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, getRpcInputObjectName(inputObjectTypeDefinitionContext));
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

    private MethodSpec buildTypeMethod(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {
        String typeParameterName = getRpcInputObjectLowerCamelName(inputObjectTypeDefinitionContext);
        ClassName typeClassName = ClassName.get(graphQLConfig.getPackageName(), getRpcInputObjectName(inputObjectTypeDefinitionContext));
        MethodSpec.Builder builder = MethodSpec.methodBuilder(typeParameterName)
                .addModifiers(Modifier.PUBLIC)
                .returns(ClassName.get(String.class))
                .addParameter(typeClassName, typeParameterName)
                .addStatement("$T stringBuilder = new $T()", ClassName.get(StringBuilder.class), ClassName.get(StringBuilder.class))
                .addStatement("stringBuilder.append(\"{\")");

        List<GraphqlParser.InputValueDefinitionContext> inputValueDefinitionContexts = inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition();
        for (GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext : inputValueDefinitionContexts) {
            if (manager.fieldTypeIsList(inputValueDefinitionContext.type())) {
                CodeBlock codeBlock;
                if (manager.isScalar(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {
                    if (manager.getFieldTypeName(inputValueDefinitionContext.type()).equals("String")) {
                        codeBlock = CodeBlock.of("stringBuilder.append($S).append(\": [\").append($L.$L().stream().map(item -> \"\\\"\" + item + \"\\\"\").collect($T.joining(\", \"))).append(\"] \")",
                                inputValueDefinitionContext.name().getText(),
                                typeParameterName,
                                getRpcGetInputValueListName(inputValueDefinitionContext),
                                ClassName.get(Collectors.class)
                        );
                    } else {
                        codeBlock = CodeBlock.of("stringBuilder.append($S).append(\": [\").append($L.$L().stream().map(item -> item + \"\").collect($T.joining(\", \"))).append(\"] \")",
                                inputValueDefinitionContext.name().getText(),
                                typeParameterName,
                                getRpcGetInputValueListName(inputValueDefinitionContext),
                                ClassName.get(Collectors.class)
                        );
                    }
                } else if (manager.isEnum(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {
                    codeBlock = CodeBlock.of("stringBuilder.append($S).append(\": [\").append($L.$L().stream().map(item -> item.getValueDescriptor().getName().replaceFirst($S, \"\")).collect($T.joining(\", \"))).append(\"] \")",
                            inputValueDefinitionContext.name().getText(),
                            typeParameterName,
                            getRpcGetInputValueListName(inputValueDefinitionContext),
                            getRpcEnumValueSuffixName(inputValueDefinitionContext.type()),
                            ClassName.get(Collectors.class)
                    );
                } else if (manager.isInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {
                    codeBlock = CodeBlock.of("stringBuilder.append($S).append(\": [\").append($L.$L().stream().map(this::$L).collect($T.joining(\", \"))).append(\"] \")",
                            inputValueDefinitionContext.name().getText(),
                            typeParameterName,
                            getRpcGetInputValueListName(inputValueDefinitionContext),
                            getRpcInputObjectLowerCamelName(inputValueDefinitionContext.type()),
                            ClassName.get(Collectors.class)
                    );
                } else {
                    throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE);
                }
                if (inputValueDefinitionContext.type().nonNullType() == null) {
                    builder.beginControlFlow("if ($L.$L() > 0)", typeParameterName, getRpcGetInputValueCountName(inputValueDefinitionContext))
                            .addStatement(codeBlock)
                            .endControlFlow();
                } else {
                    builder.addStatement(codeBlock);
                }
            } else {
                CodeBlock codeBlock;
                if (manager.isScalar(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {
                    if (manager.getFieldTypeName(inputValueDefinitionContext.type()).equals("String")) {
                        codeBlock = CodeBlock.of("stringBuilder.append($S).append(\": \").append(\"\\\"\").append($L.$L()).append(\"\\\"\").append(\" \")",
                                inputValueDefinitionContext.name().getText(),
                                typeParameterName,
                                getRpcGetInputValueName(inputValueDefinitionContext)
                        );
                    } else {
                        codeBlock = CodeBlock.of("stringBuilder.append($S).append(\": \").append($L.$L()).append(\" \")",
                                inputValueDefinitionContext.name().getText(),
                                typeParameterName,
                                getRpcGetInputValueName(inputValueDefinitionContext)
                        );
                    }
                } else if (manager.isEnum(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {
                    codeBlock = CodeBlock.of("stringBuilder.append($S).append(\": \").append($L.$L().getValueDescriptor().getName().replaceFirst($S, \"\")).append(\" \")",
                            inputValueDefinitionContext.name().getText(),
                            typeParameterName,
                            getRpcGetInputValueName(inputValueDefinitionContext),
                            getRpcEnumValueSuffixName(inputValueDefinitionContext.type())
                    );
                } else if (manager.isInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {
                    codeBlock = CodeBlock.of("stringBuilder.append($S).append(\": \").append($L($L.$L())).append(\" \")",
                            inputValueDefinitionContext.name().getText(),
                            getRpcInputObjectLowerCamelName(inputValueDefinitionContext.type()),
                            typeParameterName,
                            getRpcGetInputValueName(inputValueDefinitionContext)
                    );
                } else {
                    throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE);
                }
                if (inputValueDefinitionContext.type().nonNullType() == null) {
                    builder.beginControlFlow("if ($L.$L())", typeParameterName, getRpcHasInputValueName(inputValueDefinitionContext))
                            .addStatement(codeBlock)
                            .endControlFlow();
                } else {
                    builder.addStatement(codeBlock);
                }
            }
        }
        builder.addStatement("stringBuilder.append(\"}\")")
                .addStatement("return stringBuilder.toString()");
        return builder.build();
    }
}
