package io.graphoenix.java.generator.implementer;

import com.google.common.base.CaseFormat;
import com.squareup.javapoet.*;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.error.GraphQLErrors;
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
public class RpcResponseHandlerBuilder {

    private final IGraphQLDocumentManager manager;
    private final TypeManager typeManager;
    private GraphQLConfig graphQLConfig;

    @Inject
    public RpcResponseHandlerBuilder(IGraphQLDocumentManager manager, TypeManager typeManager) {
        this.manager = manager;
        this.typeManager = typeManager;
    }

    public RpcResponseHandlerBuilder setConfiguration(GraphQLConfig graphQLConfig) {
        this.graphQLConfig = graphQLConfig;
        this.typeManager.setGraphQLConfig(graphQLConfig);
        return this;
    }

    public void writeToFiler(Filer filer) throws IOException {
        this.buildClass().writeTo(filer);
        Logger.info("RpcResponseHandler build success");
    }

    private JavaFile buildClass() {
        TypeSpec typeSpec = buildRpcResponseHandler();
        return JavaFile.builder(graphQLConfig.getHandlerPackageName(), typeSpec).build();
    }

    private TypeSpec buildRpcResponseHandler() {
        return TypeSpec.classBuilder("RpcResponseHandler")
                .addModifiers(Modifier.PUBLIC)
                .addMethod(buildConstructor())
                .addMethods(buildTypeMethods())
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

    private MethodSpec buildTypeMethod(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        String parameterName = "object";
        MethodSpec.Builder builder = MethodSpec.methodBuilder(getRpcObjectLowerCamelName(objectTypeDefinitionContext))
                .addModifiers(Modifier.PUBLIC)
                .returns(ClassName.get(graphQLConfig.getGrpcPackageName(), getRpcObjectName(objectTypeDefinitionContext)))
                .addParameter(ClassName.get(graphQLConfig.getObjectTypePackageName(), objectTypeDefinitionContext.name().getText()), parameterName)
                .addStatement("$T builder = $T.newBuilder()",
                        ClassName.get(graphQLConfig.getGrpcPackageName(),
                                getRpcObjectName(objectTypeDefinitionContext), "Builder"),
                        ClassName.get(graphQLConfig.getGrpcPackageName(), getRpcObjectName(objectTypeDefinitionContext))
                );

        List<GraphqlParser.FieldDefinitionContext> fieldDefinitionContexts = objectTypeDefinitionContext.fieldsDefinition().fieldDefinition();
        for (GraphqlParser.FieldDefinitionContext fieldDefinitionContext : fieldDefinitionContexts) {
            if (manager.fieldTypeIsList(fieldDefinitionContext.type())) {
                if (manager.isScalar(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
                    builder.beginControlFlow("if(object.$L() != null)", getFieldGetterName(fieldDefinitionContext))
                            .addStatement("builder.$L(object.$L())", getRpcFieldAddAllName(fieldDefinitionContext), getFieldGetterName(fieldDefinitionContext))
                            .endControlFlow();
                } else if (manager.isEnum(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
                    builder.beginControlFlow("if(object.$L() != null)", getFieldGetterName(fieldDefinitionContext))
                            .addStatement("builder.$L(object.$L().stream().map(item -> $T.forNumber(item.ordinal())).collect($T.toList()))",
                                    getRpcFieldAddAllName(fieldDefinitionContext),
                                    getFieldGetterName(fieldDefinitionContext),
                                    ClassName.get(graphQLConfig.getGrpcPackageName(), getRpcObjectName(fieldDefinitionContext.type())),
                                    ClassName.get(Collectors.class)
                            )
                            .endControlFlow();
                } else if (manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
                    builder.beginControlFlow("if(object.$L() != null)", getFieldGetterName(fieldDefinitionContext))
                            .addStatement("builder.$L(object.$L().stream().map(this::$L).collect($T.toList()))",
                                    getRpcFieldAddAllName(fieldDefinitionContext),
                                    getFieldGetterName(fieldDefinitionContext),
                                    getRpcObjectLowerCamelName(fieldDefinitionContext.type()),
                                    ClassName.get(Collectors.class)
                            )
                            .endControlFlow();
                } else {
                    throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE);
                }
            } else {
                if (manager.isScalar(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
                    builder.beginControlFlow("if(object.$L() != null)", getFieldGetterName(fieldDefinitionContext))
                            .addStatement("builder.$L(object.$L())", getRpcFieldSetterName(fieldDefinitionContext), getFieldGetterName(fieldDefinitionContext))
                            .endControlFlow();
                } else if (manager.isEnum(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
                    builder.beginControlFlow("if(object.$L() != null)", getFieldGetterName(fieldDefinitionContext))
                            .addStatement("builder.$L($T.forNumber(object.$L().ordinal()))",
                                    getRpcFieldSetterName(fieldDefinitionContext),
                                    ClassName.get(graphQLConfig.getGrpcPackageName(), getRpcObjectName(fieldDefinitionContext.type())),
                                    getFieldGetterName(fieldDefinitionContext)
                            )
                            .endControlFlow();
                } else if (manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
                    builder.beginControlFlow("if(object.$L() != null)", getFieldGetterName(fieldDefinitionContext))
                            .addStatement("builder.$L($L(object.$L()))",
                                    getRpcFieldSetterName(fieldDefinitionContext),
                                    getRpcObjectLowerCamelName(fieldDefinitionContext.type()),
                                    getFieldGetterName(fieldDefinitionContext)
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

    private String getFieldGetterName(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        String name = fieldDefinitionContext.name().getText();
        if (name.startsWith(INTROSPECTION_PREFIX)) {
            return "get__".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name.replaceFirst(INTROSPECTION_PREFIX, "")));
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
