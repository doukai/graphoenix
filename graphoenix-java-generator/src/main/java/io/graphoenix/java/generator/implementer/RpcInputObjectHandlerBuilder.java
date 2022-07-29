package io.graphoenix.java.generator.implementer;

import com.google.common.base.CaseFormat;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.core.utils.CodecUtil;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.tinylog.Logger;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static io.graphoenix.core.error.GraphQLErrorType.UNSUPPORTED_FIELD_TYPE;
import static io.graphoenix.spi.constant.Hammurabi.INTROSPECTION_PREFIX;

@ApplicationScoped
public class RpcInputObjectHandlerBuilder {

    private final IGraphQLDocumentManager manager;
    private final TypeManager typeManager;
    private GraphQLConfig graphQLConfig;

    @Inject
    public RpcInputObjectHandlerBuilder(IGraphQLDocumentManager manager, TypeManager typeManager) {
        this.manager = manager;
        this.typeManager = typeManager;
    }

    public RpcInputObjectHandlerBuilder setConfiguration(GraphQLConfig graphQLConfig) {
        this.graphQLConfig = graphQLConfig;
        this.typeManager.setGraphQLConfig(graphQLConfig);
        return this;
    }

    public void writeToFiler(Filer filer) throws IOException {
        this.buildClass().writeTo(filer);
        Logger.info("RpcInputObjectHandler build success");
    }

    private JavaFile buildClass() {
        TypeSpec typeSpec = buildRpcRequestHandler();
        return JavaFile.builder(graphQLConfig.getHandlerPackageName(), typeSpec).build();
    }

    private TypeSpec buildRpcRequestHandler() {
        return TypeSpec.classBuilder("RpcInputObjectHandler")
                .addAnnotation(ApplicationScoped.class)
                .addModifiers(Modifier.PUBLIC)
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
                .addMethods(buildTypeMethods())
                .addMethods(buildTypeToInputObjectMethods())
                .addMethods(buildTypeToObjectMethods())
                .build();
    }

    private MethodSpec buildConstructor() {
        return MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .build();
    }

    private List<MethodSpec> buildTypeMethods() {
        return manager.getInputObjects()
                .map(this::buildTypeMethod)
                .collect(Collectors.toList());
    }

    private List<MethodSpec> buildTypeToInputObjectMethods() {
        return manager.getInputObjects()
                .map(this::buildTypeToInputObjectMethod)
                .collect(Collectors.toList());
    }

    private List<MethodSpec> buildTypeToObjectMethods() {
        return manager.getObjects()
                .filter(objectTypeDefinitionContext ->
                        !manager.isQueryOperationType(objectTypeDefinitionContext.name().getText()) &&
                                !manager.isMutationOperationType(objectTypeDefinitionContext.name().getText()) &&
                                !manager.isSubscriptionOperationType(objectTypeDefinitionContext.name().getText())
                )
                .map(this::buildTypeToObjectMethod)
                .collect(Collectors.toList());
    }

    private MethodSpec buildTypeMethod(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {
        String typeParameterName = getRpcInputObjectLowerCamelName(inputObjectTypeDefinitionContext);
        ClassName typeClassName = ClassName.get(graphQLConfig.getGrpcPackageName(), getRpcInputObjectName(inputObjectTypeDefinitionContext));
        MethodSpec.Builder builder = MethodSpec.methodBuilder(typeParameterName)
                .addModifiers(Modifier.PUBLIC)
                .returns(ClassName.get(String.class))
                .addParameter(typeClassName, typeParameterName)
                .addStatement("$T stringBuilder = new $T()", ClassName.get(StringBuilder.class), ClassName.get(StringBuilder.class))
                .addStatement("stringBuilder.append(CURLY_BRACKETS_START)");

        List<GraphqlParser.InputValueDefinitionContext> inputValueDefinitionContexts = inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition();
        for (GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext : inputValueDefinitionContexts) {
            if (manager.fieldTypeIsList(inputValueDefinitionContext.type())) {
                CodeBlock codeBlock;
                String fieldTypeName = manager.getFieldTypeName(inputValueDefinitionContext.type());
                if (manager.isScalar(fieldTypeName)) {
                    if (fieldTypeName.equals("String") || fieldTypeName.equals("DateTime") || fieldTypeName.equals("Timestamp") || fieldTypeName.equals("Date") || fieldTypeName.equals("Time")) {
                        codeBlock = CodeBlock.of("stringBuilder.append($S).append(COLON).append(SQUARE_BRACKETS_START).append($L.$L().stream().map(item -> QUOTATION + item + QUOTATION).collect($T.joining(COMMA))).append(SQUARE_BRACKETS_END).append(SPACE)",
                                inputValueDefinitionContext.name().getText(),
                                typeParameterName,
                                getRpcGetInputValueListName(inputValueDefinitionContext),
                                ClassName.get(Collectors.class)
                        );
                    } else {
                        codeBlock = CodeBlock.of("stringBuilder.append($S).append(COLON).append(SQUARE_BRACKETS_START).append($L.$L().stream().map(item -> item + EMPTY).collect($T.joining(COMMA))).append(SQUARE_BRACKETS_END).append(SPACE)",
                                inputValueDefinitionContext.name().getText(),
                                typeParameterName,
                                getRpcGetInputValueListName(inputValueDefinitionContext),
                                ClassName.get(Collectors.class)
                        );
                    }
                } else if (manager.isEnum(fieldTypeName)) {
                    codeBlock = CodeBlock.of("stringBuilder.append($S).append(COLON).append(SQUARE_BRACKETS_START).append($L.$L().stream().map(item -> item.getValueDescriptor().getName().replaceFirst($S, EMPTY)).collect($T.joining(COMMA))).append(SQUARE_BRACKETS_END).append(SPACE)",
                            inputValueDefinitionContext.name().getText(),
                            typeParameterName,
                            getRpcGetInputValueListName(inputValueDefinitionContext),
                            getRpcEnumValueSuffixName(inputValueDefinitionContext.type()),
                            ClassName.get(Collectors.class)
                    );
                } else if (manager.isInputObject(fieldTypeName)) {
                    codeBlock = CodeBlock.of("stringBuilder.append($S).append(COLON).append(SQUARE_BRACKETS_START).append($L.$L().stream().map(this::$L).collect($T.joining(COMMA))).append(SQUARE_BRACKETS_END).append(SPACE)",
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
                String fieldTypeName = manager.getFieldTypeName(inputValueDefinitionContext.type());
                if (manager.isScalar(fieldTypeName)) {
                    if (fieldTypeName.equals("String") || fieldTypeName.equals("DateTime") || fieldTypeName.equals("Timestamp") || fieldTypeName.equals("Date") || fieldTypeName.equals("Time")) {
                        codeBlock = CodeBlock.of("stringBuilder.append($S).append(COLON).append(QUOTATION).append($L.$L()).append(QUOTATION).append(SPACE)",
                                inputValueDefinitionContext.name().getText(),
                                typeParameterName,
                                getRpcGetInputValueName(inputValueDefinitionContext)
                        );
                    } else {
                        codeBlock = CodeBlock.of("stringBuilder.append($S).append(COLON).append($L.$L()).append(SPACE)",
                                inputValueDefinitionContext.name().getText(),
                                typeParameterName,
                                getRpcGetInputValueName(inputValueDefinitionContext)
                        );
                    }
                } else if (manager.isEnum(fieldTypeName)) {
                    codeBlock = CodeBlock.of("stringBuilder.append($S).append(COLON).append($L.$L().getValueDescriptor().getName().replaceFirst($S, EMPTY)).append(SPACE)",
                            inputValueDefinitionContext.name().getText(),
                            typeParameterName,
                            getRpcGetInputValueName(inputValueDefinitionContext),
                            getRpcEnumValueSuffixName(inputValueDefinitionContext.type())
                    );
                } else if (manager.isInputObject(fieldTypeName)) {
                    codeBlock = CodeBlock.of("stringBuilder.append($S).append(COLON).append($L($L.$L())).append(SPACE)",
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
        builder.addStatement("stringBuilder.append(CURLY_BRACKETS_END)")
                .addStatement("return stringBuilder.toString()");
        return builder.build();
    }

    private MethodSpec buildTypeToInputObjectMethod(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {
        String typeParameterName = getRpcInputObjectLowerCamelName(inputObjectTypeDefinitionContext);
        ClassName inputObjectClassName = ClassName.get(graphQLConfig.getInputObjectTypePackageName(), inputObjectTypeDefinitionContext.name().getText());
        ClassName typeClassName = ClassName.get(graphQLConfig.getGrpcPackageName(), getRpcInputObjectName(inputObjectTypeDefinitionContext));
        MethodSpec.Builder builder = MethodSpec.methodBuilder(typeParameterName.concat("ToInputObject"))
                .addModifiers(Modifier.PUBLIC)
                .returns(inputObjectClassName)
                .addParameter(typeClassName, typeParameterName)
                .addStatement("$T inputObject = new $T()", inputObjectClassName, inputObjectClassName);

        List<GraphqlParser.InputValueDefinitionContext> inputValueDefinitionContexts = inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition();
        for (GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext : inputValueDefinitionContexts) {
            if (manager.fieldTypeIsList(inputValueDefinitionContext.type())) {
                CodeBlock codeBlock;
                String fieldTypeName = manager.getFieldTypeName(inputValueDefinitionContext.type());
                if (manager.isScalar(fieldTypeName)) {

                    if (fieldTypeName.equals("DateTime") || fieldTypeName.equals("Timestamp") || fieldTypeName.equals("Date") || fieldTypeName.equals("Time")) {
                        codeBlock = CodeBlock.of("inputObject.$L($L.$L().stream().map(item -> $T.CODEC_UTIL.$L(item)).collect($T.toList()))",
                                getInputValueSetterName(inputValueDefinitionContext),
                                typeParameterName,
                                getRpcGetInputValueListName(inputValueDefinitionContext),
                                ClassName.get(CodecUtil.class),
                                getDecodeName(fieldTypeName),
                                ClassName.get(Collectors.class)
                        );
                    } else {
                        codeBlock = CodeBlock.of("inputObject.$L($L.$L().stream().map(item -> item).collect($T.toList()))",
                                getInputValueSetterName(inputValueDefinitionContext),
                                typeParameterName,
                                getRpcGetInputValueListName(inputValueDefinitionContext),
                                ClassName.get(Collectors.class)
                        );
                    }
                } else if (manager.isEnum(fieldTypeName)) {
                    codeBlock = CodeBlock.of("inputObject.$L($L.$L().stream().map(item -> $T.valueOf(item.name())).collect($T.toList()))",
                            getInputValueSetterName(inputValueDefinitionContext),
                            typeParameterName,
                            getRpcGetInputValueListName(inputValueDefinitionContext),
                            ClassName.get(graphQLConfig.getEnumTypePackageName(), fieldTypeName),
                            ClassName.get(Collectors.class)
                    );
                } else if (manager.isInputObject(fieldTypeName)) {
                    codeBlock = CodeBlock.of("inputObject.$L($L.$L().stream().map(this::$L).collect($T.toList()))",
                            getInputValueSetterName(inputValueDefinitionContext),
                            typeParameterName,
                            getRpcGetInputValueListName(inputValueDefinitionContext),
                            getRpcInputObjectLowerCamelName(inputValueDefinitionContext.type()).concat("ToInputObject"),
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
                String fieldTypeName = manager.getFieldTypeName(inputValueDefinitionContext.type());
                if (manager.isScalar(fieldTypeName)) {
                    if (fieldTypeName.equals("DateTime") || fieldTypeName.equals("Timestamp") || fieldTypeName.equals("Date") || fieldTypeName.equals("Time")) {
                        codeBlock = CodeBlock.of("inputObject.$L($T.CODEC_UTIL.$L($L.$L()))",
                                getInputValueSetterName(inputValueDefinitionContext),
                                ClassName.get(CodecUtil.class),
                                getDecodeName(fieldTypeName),
                                typeParameterName,
                                getRpcGetInputValueName(inputValueDefinitionContext)
                        );
                    } else {
                        codeBlock = CodeBlock.of("inputObject.$L($L.$L())",
                                getInputValueSetterName(inputValueDefinitionContext),
                                typeParameterName,
                                getRpcGetInputValueName(inputValueDefinitionContext)
                        );
                    }
                } else if (manager.isEnum(fieldTypeName)) {
                    codeBlock = CodeBlock.of("inputObject.$L($T.valueOf($L.$L().name()))",
                            getInputValueSetterName(inputValueDefinitionContext),
                            ClassName.get(graphQLConfig.getEnumTypePackageName(), fieldTypeName),
                            typeParameterName,
                            getRpcGetInputValueName(inputValueDefinitionContext)
                    );
                } else if (manager.isInputObject(fieldTypeName)) {
                    codeBlock = CodeBlock.of("inputObject.$L($L($L.$L()))",
                            getInputValueSetterName(inputValueDefinitionContext),
                            getRpcInputObjectLowerCamelName(inputValueDefinitionContext.type()).concat("ToInputObject"),
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
        builder.addStatement("return inputObject");
        return builder.build();
    }

    private MethodSpec buildTypeToObjectMethod(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        String typeParameterName = getRpcObjectLowerCamelName(objectTypeDefinitionContext);
        ClassName inputObjectClassName = ClassName.get(graphQLConfig.getObjectTypePackageName(), objectTypeDefinitionContext.name().getText());
        ClassName typeClassName = ClassName.get(graphQLConfig.getGrpcPackageName(), getRpcObjectName(objectTypeDefinitionContext).concat("Input"));
        MethodSpec.Builder builder = MethodSpec.methodBuilder(typeParameterName.concat("InputToObject"))
                .addModifiers(Modifier.PUBLIC)
                .returns(inputObjectClassName)
                .addParameter(typeClassName, typeParameterName)
                .addStatement("$T object = new $T()", inputObjectClassName, inputObjectClassName);

        List<GraphqlParser.FieldDefinitionContext> fieldDefinitionContexts = objectTypeDefinitionContext.fieldsDefinition().fieldDefinition();
        for (GraphqlParser.FieldDefinitionContext fieldDefinitionContext : fieldDefinitionContexts) {
            if (manager.fieldTypeIsList(fieldDefinitionContext.type())) {
                CodeBlock codeBlock;
                String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
                if (manager.isScalar(fieldTypeName)) {

                    if (fieldTypeName.equals("DateTime") || fieldTypeName.equals("Timestamp") || fieldTypeName.equals("Date") || fieldTypeName.equals("Time")) {
                        codeBlock = CodeBlock.of("object.$L($L.$L().stream().map(item -> $T.CODEC_UTIL.$L(item)).collect($T.toList()))",
                                getFieldSetterName(fieldDefinitionContext),
                                typeParameterName,
                                getRpcGetFieldListName(fieldDefinitionContext),
                                ClassName.get(CodecUtil.class),
                                getDecodeName(fieldTypeName),
                                ClassName.get(Collectors.class)
                        );
                    } else {
                        codeBlock = CodeBlock.of("object.$L($L.$L().stream().map(item -> item).collect($T.toList()))",
                                getFieldSetterName(fieldDefinitionContext),
                                typeParameterName,
                                getRpcGetFieldListName(fieldDefinitionContext),
                                ClassName.get(Collectors.class)
                        );
                    }
                } else if (manager.isEnum(fieldTypeName)) {
                    codeBlock = CodeBlock.of("object.$L($L.$L().stream().map(item -> $T.valueOf(item.name())).collect($T.toList()))",
                            getFieldSetterName(fieldDefinitionContext),
                            typeParameterName,
                            getRpcGetFieldListName(fieldDefinitionContext),
                            ClassName.get(graphQLConfig.getEnumTypePackageName(), fieldTypeName),
                            ClassName.get(Collectors.class)
                    );
                } else if (manager.isObject(fieldTypeName)) {
                    codeBlock = CodeBlock.of("object.$L($L.$L().stream().map(this::$L).collect($T.toList()))",
                            getFieldSetterName(fieldDefinitionContext),
                            typeParameterName,
                            getRpcGetFieldListName(fieldDefinitionContext),
                            getRpcInputObjectLowerCamelName(fieldDefinitionContext.type()).concat("InputToObject"),
                            ClassName.get(Collectors.class)
                    );
                } else {
                    throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE);
                }
                if (fieldDefinitionContext.type().nonNullType() == null) {
                    builder.beginControlFlow("if ($L.$L() > 0)", typeParameterName, getRpcGetFieldCountName(fieldDefinitionContext))
                            .addStatement(codeBlock)
                            .endControlFlow();
                } else {
                    builder.addStatement(codeBlock);
                }
            } else {
                CodeBlock codeBlock;
                String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
                if (manager.isScalar(fieldTypeName)) {
                    if (fieldTypeName.equals("DateTime") || fieldTypeName.equals("Timestamp") || fieldTypeName.equals("Date") || fieldTypeName.equals("Time")) {
                        codeBlock = CodeBlock.of("object.$L($T.CODEC_UTIL.$L($L.$L()))",
                                getFieldSetterName(fieldDefinitionContext),
                                ClassName.get(CodecUtil.class),
                                getDecodeName(fieldTypeName),
                                typeParameterName,
                                getRpcGetFieldName(fieldDefinitionContext)
                        );
                    } else {
                        codeBlock = CodeBlock.of("object.$L($L.$L())",
                                getFieldSetterName(fieldDefinitionContext),
                                typeParameterName,
                                getRpcGetFieldName(fieldDefinitionContext)
                        );
                    }
                } else if (manager.isEnum(fieldTypeName)) {
                    codeBlock = CodeBlock.of("object.$L($T.valueOf($L.$L().name()))",
                            getFieldSetterName(fieldDefinitionContext),
                            ClassName.get(graphQLConfig.getEnumTypePackageName(), fieldTypeName),
                            typeParameterName,
                            getRpcGetFieldName(fieldDefinitionContext)
                    );
                } else if (manager.isObject(fieldTypeName)) {
                    codeBlock = CodeBlock.of("object.$L($L($L.$L()))",
                            getFieldSetterName(fieldDefinitionContext),
                            getRpcInputObjectLowerCamelName(fieldDefinitionContext.type()).concat("InputToObject"),
                            typeParameterName,
                            getRpcGetFieldName(fieldDefinitionContext)
                    );
                } else {
                    throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE);
                }
                if (fieldDefinitionContext.type().nonNullType() == null) {
                    builder.beginControlFlow("if ($L.$L())", typeParameterName, getRpcHasFieldName(fieldDefinitionContext))
                            .addStatement(codeBlock)
                            .endControlFlow();
                } else {
                    builder.addStatement(codeBlock);
                }
            }
        }
        builder.addStatement("return object");
        return builder.build();
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

    private String getRpcInputObjectName(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {
        String name = inputObjectTypeDefinitionContext.name().getText();
        if (name.startsWith(INTROSPECTION_PREFIX)) {
            return "Intro".concat(name.replaceFirst(INTROSPECTION_PREFIX, ""));
        }
        return name;
    }

    private String getRpcObjectName(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        String name = objectTypeDefinitionContext.name().getText();
        if (name.startsWith(INTROSPECTION_PREFIX)) {
            return "Intro".concat(name.replaceFirst(INTROSPECTION_PREFIX, ""));
        }
        return name;
    }

    private String getRpcInputObjectLowerCamelName(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, getRpcInputObjectName(inputObjectTypeDefinitionContext));
    }

    private String getRpcObjectLowerCamelName(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, getRpcObjectName(objectTypeDefinitionContext));
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

    private String getRpcFieldName(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        String name = fieldDefinitionContext.name().getText();
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

    private String getRpcGetFieldName(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return "get".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, getRpcFieldName(fieldDefinitionContext)));
    }

    private String getRpcHasFieldName(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return "has".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, getRpcFieldName(fieldDefinitionContext)));
    }

    private String getRpcGetFieldListName(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return "get".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, getRpcFieldName(fieldDefinitionContext))).concat("List");
    }

    private String getRpcGetFieldCountName(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return "get".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, getRpcFieldName(fieldDefinitionContext))).concat("Count");
    }

    private String getFieldSetterName(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        String name = fieldDefinitionContext.name().getText();
        if (name.startsWith(INTROSPECTION_PREFIX)) {
            return "set__".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name.replaceFirst(INTROSPECTION_PREFIX, "")));
        }
        return "set".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name));
    }

    private String getInputValueSetterName(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        String name = inputValueDefinitionContext.name().getText();
        if (name.startsWith(INTROSPECTION_PREFIX)) {
            return "set__".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name.replaceFirst(INTROSPECTION_PREFIX, "")));
        }
        return "set".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name));
    }
}
