package io.graphoenix.java.generator.implementer;

import com.google.common.base.CaseFormat;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
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
public class RpcObjectHandlerBuilder {

    private final IGraphQLDocumentManager manager;
    private final TypeManager typeManager;
    private GraphQLConfig graphQLConfig;

    @Inject
    public RpcObjectHandlerBuilder(IGraphQLDocumentManager manager, TypeManager typeManager) {
        this.manager = manager;
        this.typeManager = typeManager;
    }

    public RpcObjectHandlerBuilder setConfiguration(GraphQLConfig graphQLConfig) {
        this.graphQLConfig = graphQLConfig;
        this.typeManager.setGraphQLConfig(graphQLConfig);
        return this;
    }

    public void writeToFiler(Filer filer) throws IOException {
        this.buildClass().writeTo(filer);
        Logger.info("RpcObjectHandler build success");
    }

    private JavaFile buildClass() {
        TypeSpec typeSpec = buildRpcResponseHandler();
        return JavaFile.builder(graphQLConfig.getHandlerPackageName(), typeSpec).build();
    }

    private TypeSpec buildRpcResponseHandler() {
        return TypeSpec.classBuilder("RpcObjectHandler")
                .addAnnotation(ApplicationScoped.class)
                .addModifiers(Modifier.PUBLIC)
                .addMethod(buildConstructor())
                .addMethods(buildTypeMethods())
                .addMethods(buildTypeReversalMethods())
                .build();
    }

    private MethodSpec buildConstructor() {
        return MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .build();
    }

    private List<MethodSpec> buildTypeMethods() {
        return manager.getObjects()
                .filter(objectTypeDefinitionContext ->
                        !manager.isQueryOperationType(objectTypeDefinitionContext.name().getText()) &&
                                !manager.isMutationOperationType(objectTypeDefinitionContext.name().getText()) &&
                                !manager.isSubscriptionOperationType(objectTypeDefinitionContext.name().getText())
                )
                .map(this::buildTypeMethod)
                .collect(Collectors.toList());
    }

    private List<MethodSpec> buildTypeReversalMethods() {
        return manager.getObjects()
                .filter(objectTypeDefinitionContext ->
                        !manager.isQueryOperationType(objectTypeDefinitionContext.name().getText()) &&
                                !manager.isMutationOperationType(objectTypeDefinitionContext.name().getText()) &&
                                !manager.isSubscriptionOperationType(objectTypeDefinitionContext.name().getText())
                )
                .map(this::buildTypeReversalMethod)
                .collect(Collectors.toList());
    }

    private MethodSpec buildTypeMethod(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        String objectParameterName = getRpcObjectLowerCamelName(objectTypeDefinitionContext);
        String rpcObjectName = getRpcObjectName(objectTypeDefinitionContext);
        MethodSpec.Builder builder = MethodSpec.methodBuilder(objectParameterName)
                .addModifiers(Modifier.PUBLIC)
                .returns(ClassName.get(graphQLConfig.getGrpcPackageName(), rpcObjectName))
                .addParameter(ClassName.get(graphQLConfig.getObjectTypePackageName(), objectTypeDefinitionContext.name().getText()), objectParameterName)
                .addStatement("$T builder = $T.newBuilder()",
                        ClassName.get(graphQLConfig.getGrpcPackageName(), rpcObjectName, "Builder"),
                        ClassName.get(graphQLConfig.getGrpcPackageName(), rpcObjectName)
                );

        List<GraphqlParser.FieldDefinitionContext> fieldDefinitionContexts = objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                .filter(manager::isNotGrpcField)
                .collect(Collectors.toList());
        for (GraphqlParser.FieldDefinitionContext fieldDefinitionContext : fieldDefinitionContexts) {
            String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
            String fieldGetterName = getFieldGetterName(fieldDefinitionContext);
            String fieldRpcObjectName = getRpcObjectName(fieldDefinitionContext.type());
            String objectFieldMethodName = getRpcObjectLowerCamelName(fieldDefinitionContext.type());
            if (manager.fieldTypeIsList(fieldDefinitionContext.type())) {
                String rpcFieldAddAllName = getRpcFieldAddAllName(fieldDefinitionContext);
                if (manager.isScalar(fieldTypeName)) {
                    if (fieldTypeName.equals("DateTime") || fieldTypeName.equals("Timestamp") || fieldTypeName.equals("Date") || fieldTypeName.equals("Time")) {
                        builder.beginControlFlow("if($L.$L() != null)", objectParameterName, fieldGetterName)
                                .addStatement("builder.$L($L.$L().stream().map(item -> $T.CODEC_UTIL.encode(item)).collect($T.toList()))",
                                        rpcFieldAddAllName,
                                        objectParameterName,
                                        fieldGetterName,
                                        ClassName.get(CodecUtil.class)
                                )
                                .endControlFlow();
                    } else {
                        builder.beginControlFlow("if($L.$L() != null)", objectParameterName, fieldGetterName)
                                .addStatement("builder.$L($L.$L())",
                                        rpcFieldAddAllName,
                                        objectParameterName,
                                        fieldGetterName
                                )
                                .endControlFlow();
                    }
                } else if (manager.isEnum(fieldTypeName)) {
                    builder.beginControlFlow("if($L.$L() != null)", objectParameterName, fieldGetterName)
                            .addStatement("builder.$L($L.$L().stream().map(item -> $T.forNumber(item.ordinal())).collect($T.toList()))",
                                    rpcFieldAddAllName,
                                    objectParameterName,
                                    fieldGetterName,
                                    ClassName.get(graphQLConfig.getGrpcPackageName(), fieldRpcObjectName),
                                    ClassName.get(Collectors.class)
                            )
                            .endControlFlow();
                } else if (manager.isObject(fieldTypeName)) {
                    builder.beginControlFlow("if($L.$L() != null)", objectParameterName, fieldGetterName)
                            .addStatement("builder.$L($L.$L().stream().map(this::$L).collect($T.toList()))",
                                    rpcFieldAddAllName,
                                    objectParameterName,
                                    fieldGetterName,
                                    objectFieldMethodName,
                                    ClassName.get(Collectors.class)
                            )
                            .endControlFlow();
                } else {
                    throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE);
                }
            } else {
                String rpcFieldSetterName = getRpcFieldSetterName(fieldDefinitionContext);
                if (manager.isScalar(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
                    if (fieldTypeName.equals("DateTime") || fieldTypeName.equals("Timestamp") || fieldTypeName.equals("Date") || fieldTypeName.equals("Time")) {
                        builder.beginControlFlow("if($L.$L() != null)", objectParameterName, fieldGetterName)
                                .addStatement("builder.$L($T.CODEC_UTIL.encode($L.$L()))",
                                        rpcFieldSetterName,
                                        ClassName.get(CodecUtil.class),
                                        objectParameterName,
                                        fieldGetterName
                                )
                                .endControlFlow();
                    } else {
                        builder.beginControlFlow("if($L.$L() != null)", objectParameterName, fieldGetterName)
                                .addStatement("builder.$L($L.$L())",
                                        rpcFieldSetterName,
                                        objectParameterName,
                                        fieldGetterName
                                )
                                .endControlFlow();
                    }
                } else if (manager.isEnum(fieldTypeName)) {
                    builder.beginControlFlow("if($L.$L() != null)", objectParameterName, fieldGetterName)
                            .addStatement("builder.$L($T.forNumber($L.$L().ordinal()))",
                                    rpcFieldSetterName,
                                    ClassName.get(graphQLConfig.getGrpcPackageName(), fieldRpcObjectName),
                                    objectParameterName,
                                    fieldGetterName
                            )
                            .endControlFlow();
                } else if (manager.isObject(fieldTypeName)) {
                    builder.beginControlFlow("if($L.$L() != null)", objectParameterName, fieldGetterName)
                            .addStatement("builder.$L($L($L.$L()))",
                                    rpcFieldSetterName,
                                    objectFieldMethodName,
                                    objectParameterName,
                                    fieldGetterName
                            )
                            .endControlFlow();
                } else {
                    throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE);
                }
            }
        }
        builder.addStatement("return builder.build()");
        return builder.build();
    }

    private MethodSpec buildTypeReversalMethod(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        String objectParameterName = typeManager.typeToLowerCamelName(objectTypeDefinitionContext.name().getText());
        String rpcObjectParameterName = getRpcObjectLowerCamelName(objectTypeDefinitionContext);
        String objectName = objectTypeDefinitionContext.name().getText();
        MethodSpec.Builder builder = MethodSpec.methodBuilder(objectParameterName)
                .addModifiers(Modifier.PUBLIC)
                .returns(ClassName.get(graphQLConfig.getObjectTypePackageName(), objectName))
                .addParameter(ClassName.get(graphQLConfig.getGrpcPackageName(), getRpcObjectName(objectTypeDefinitionContext)), rpcObjectParameterName)
                .addStatement("$T object = new $T()",
                        ClassName.get(graphQLConfig.getObjectTypePackageName(), objectName),
                        ClassName.get(graphQLConfig.getObjectTypePackageName(), objectName)
                );

        List<GraphqlParser.FieldDefinitionContext> fieldDefinitionContexts = objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                .filter(manager::isNotGrpcField)
                .collect(Collectors.toList());
        for (GraphqlParser.FieldDefinitionContext fieldDefinitionContext : fieldDefinitionContexts) {
            String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
            String fieldGetterName = getRpcFieldGetterName(fieldDefinitionContext);
            String fieldRpcObjectName = getRpcObjectName(fieldDefinitionContext.type());
            String objectFieldMethodName = typeManager.typeToLowerCamelName(fieldDefinitionContext.type());
            CodeBlock codeBlock;
            if (manager.fieldTypeIsList(fieldDefinitionContext.type())) {
                String rpcFieldAddAllName = getRpcFieldAddAllName(fieldDefinitionContext);
                if (manager.isScalar(fieldTypeName)) {
                    switch (fieldTypeName) {
                        case "DateTime":
                        case "Timestamp":
                            codeBlock = CodeBlock.of("object.$L($L.$L().stream().map(item -> $T.CODEC_UTIL.decodeLocalDateTime(item)).collect($T.toList()))",
                                    rpcFieldAddAllName,
                                    rpcObjectParameterName,
                                    fieldGetterName,
                                    ClassName.get(CodecUtil.class)
                            );
                            break;
                        case "Date":
                            codeBlock = CodeBlock.of("object.$L($L.$L().stream().map(item -> $T.CODEC_UTIL.decodeLocalDate(item)).collect($T.toList()))",
                                    rpcFieldAddAllName,
                                    rpcObjectParameterName,
                                    fieldGetterName,
                                    ClassName.get(CodecUtil.class)
                            );
                            break;
                        case "Time":
                            codeBlock = CodeBlock.of("object.$L($L.$L().stream().map(item -> $T.CODEC_UTIL.decodeLocalTime(item)).collect($T.toList()))",
                                    rpcFieldAddAllName,
                                    rpcObjectParameterName,
                                    fieldGetterName,
                                    ClassName.get(CodecUtil.class)
                            );
                            break;
                        default:
                            codeBlock = CodeBlock.of("object.$L($L.$L())",
                                    rpcFieldAddAllName,
                                    rpcObjectParameterName,
                                    fieldGetterName);
                            break;
                    }
                } else if (manager.isEnum(fieldTypeName)) {
                    codeBlock = CodeBlock.of("object.$L($L.$L().stream().map(item -> $T.forNumber(item.ordinal())).collect($T.toList()))",
                            rpcFieldAddAllName,
                            rpcObjectParameterName,
                            fieldGetterName,
                            ClassName.get(graphQLConfig.getGrpcPackageName(), fieldRpcObjectName),
                            ClassName.get(Collectors.class)
                    );
                } else if (manager.isObject(fieldTypeName)) {
                    codeBlock = CodeBlock.of("object.$L($L.$L().stream().map(this::$L).collect($T.toList()))",
                            rpcFieldAddAllName,
                            rpcObjectParameterName,
                            fieldGetterName,
                            objectFieldMethodName,
                            ClassName.get(Collectors.class)
                    );
                } else {
                    throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE);
                }

                String getFieldCountName = getRpcGetFieldCountName(fieldDefinitionContext);
                if (fieldDefinitionContext.type().nonNullType() == null) {
                    builder.beginControlFlow("if($L.$L() > 0)", rpcObjectParameterName, getFieldCountName)
                            .addStatement(codeBlock)
                            .endControlFlow();
                } else {
                    builder.addStatement(codeBlock);
                }
            } else {
                String rpcFieldSetterName = getRpcFieldSetterName(fieldDefinitionContext);
                if (manager.isScalar(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
                    if (fieldTypeName.equals("DateTime") || fieldTypeName.equals("Timestamp")) {
                        codeBlock = CodeBlock.of("object.$L($T.CODEC_UTIL.decodeLocalDateTime($L.$L()))",
                                rpcFieldSetterName,
                                ClassName.get(CodecUtil.class),
                                rpcObjectParameterName,
                                fieldGetterName
                        );
                    } else if (fieldTypeName.equals("Date")) {
                        codeBlock = CodeBlock.of("object.$L($T.CODEC_UTIL.decodeLocalDate($L.$L()))",
                                rpcFieldSetterName,
                                ClassName.get(CodecUtil.class),
                                rpcObjectParameterName,
                                fieldGetterName
                        );
                    } else if (fieldTypeName.equals("Time")) {
                        codeBlock = CodeBlock.of("object.$L($T.CODEC_UTIL.decodeLocalTime($L.$L()))",
                                rpcFieldSetterName,
                                ClassName.get(CodecUtil.class),
                                rpcObjectParameterName,
                                fieldGetterName
                        );
                    } else {
                        codeBlock = CodeBlock.of("object.$L($L.$L())",
                                rpcFieldSetterName,
                                rpcObjectParameterName,
                                fieldGetterName
                        );
                    }
                } else if (manager.isEnum(fieldTypeName)) {
                    codeBlock = CodeBlock.of("object.$L($T.forNumber($L.$L().ordinal()))",
                            rpcFieldSetterName,
                            ClassName.get(graphQLConfig.getGrpcPackageName(), fieldRpcObjectName),
                            rpcObjectParameterName,
                            fieldGetterName
                    );
                } else if (manager.isObject(fieldTypeName)) {
                    codeBlock = CodeBlock.of("object.$L($L($L.$L()))",
                            rpcFieldSetterName,
                            objectFieldMethodName,
                            rpcObjectParameterName,
                            fieldGetterName
                    );
                } else {
                    throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE);
                }

                String hasFieldName = getRpcHasFieldName(fieldDefinitionContext);
                if (fieldDefinitionContext.type().nonNullType() == null) {
                    builder.beginControlFlow("if($L.$L())", rpcObjectParameterName, hasFieldName)
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

    private String getFieldGetterName(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        String name = fieldDefinitionContext.name().getText();
        if (name.startsWith(INTROSPECTION_PREFIX)) {
            return "get".concat(name);
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

    private String getRpcFieldGetterName(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        String name = fieldDefinitionContext.name().getText();
        if (name.startsWith(INTROSPECTION_PREFIX)) {
            return "getIntro".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name.replaceFirst(INTROSPECTION_PREFIX, "")));
        }
        return "get".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name));
    }


    private String getRpcHasFieldName(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        String name = fieldDefinitionContext.name().getText();
        if (name.startsWith(INTROSPECTION_PREFIX)) {
            return "hasIntro".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name.replaceFirst(INTROSPECTION_PREFIX, "")));
        }
        return "has".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name));
    }

    private String getRpcGetFieldListName(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return "get".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, getRpcFieldName(fieldDefinitionContext))).concat("List");
    }

    private String getRpcGetFieldCountName(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return "get".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, getRpcFieldName(fieldDefinitionContext))).concat("Count");
    }

    private String getRpcFieldName(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        String name = fieldDefinitionContext.name().getText();
        if (name.startsWith(INTROSPECTION_PREFIX)) {
            return "intro".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name.replaceFirst(INTROSPECTION_PREFIX, "")));
        }
        return name;
    }

    private String getRpcFieldAddAllName(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        String name = fieldDefinitionContext.name().getText();
        if (name.startsWith(INTROSPECTION_PREFIX)) {
            return "addAllIntro".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name.replaceFirst(INTROSPECTION_PREFIX, "")));
        }
        return "addAll".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name));
    }

    private String getRpcObjectName(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        String name = objectTypeDefinitionContext.name().getText();
        if (name.startsWith(INTROSPECTION_PREFIX)) {
            return "Intro".concat(name.replaceFirst(INTROSPECTION_PREFIX, ""));
        }
        return name;
    }

    private String getRpcObjectLowerCamelName(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        String name = objectTypeDefinitionContext.name().getText();
        if (name.startsWith(INTROSPECTION_PREFIX)) {
            return "intro".concat(name.replaceFirst(INTROSPECTION_PREFIX, ""));
        }
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, name);
    }

    private String getRpcObjectName(GraphqlParser.TypeContext typeContext) {
        String name = manager.getFieldTypeName(typeContext);
        if (name.startsWith(INTROSPECTION_PREFIX)) {
            return "Intro".concat(name.replaceFirst(INTROSPECTION_PREFIX, ""));
        }
        return name;
    }

    private String getRpcObjectLowerCamelName(GraphqlParser.TypeContext typeContext) {
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, getRpcObjectName(typeContext));
    }
}
