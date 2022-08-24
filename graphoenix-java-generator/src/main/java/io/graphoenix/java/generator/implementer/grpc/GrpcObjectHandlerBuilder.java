package io.graphoenix.java.generator.implementer.grpc;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.core.utils.CodecUtil;
import io.graphoenix.java.generator.implementer.TypeManager;
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

@ApplicationScoped
public class GrpcObjectHandlerBuilder {

    private final IGraphQLDocumentManager manager;
    private final TypeManager typeManager;
    private GraphQLConfig graphQLConfig;
    private final GrpcNameUtil grpcNameUtil;

    @Inject
    public GrpcObjectHandlerBuilder(IGraphQLDocumentManager manager, TypeManager typeManager, GrpcNameUtil grpcNameUtil) {
        this.manager = manager;
        this.typeManager = typeManager;
        this.grpcNameUtil = grpcNameUtil;
    }

    public GrpcObjectHandlerBuilder setConfiguration(GraphQLConfig graphQLConfig) {
        this.graphQLConfig = graphQLConfig;
        this.typeManager.setGraphQLConfig(graphQLConfig);
        return this;
    }

    public void writeToFiler(Filer filer) throws IOException {
        this.buildClass().writeTo(filer);
        Logger.info("GrpcObjectHandler build success");
    }

    private JavaFile buildClass() {
        TypeSpec typeSpec = buildGrpcObjectHandler();
        return JavaFile.builder(graphQLConfig.getHandlerPackageName(), typeSpec).build();
    }

    private TypeSpec buildGrpcObjectHandler() {
        return TypeSpec.classBuilder("GrpcObjectHandler")
                .addAnnotation(ApplicationScoped.class)
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
        String objectParameterName = grpcNameUtil.getRpcObjectLowerCamelName(objectTypeDefinitionContext);
        String rpcObjectName = grpcNameUtil.getRpcObjectName(objectTypeDefinitionContext);
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
            String fieldGetterName = grpcNameUtil.getFieldGetterName(fieldDefinitionContext);
            String fieldRpcObjectName = grpcNameUtil.getRpcObjectName(fieldDefinitionContext.type());
            String objectFieldMethodName = grpcNameUtil.getRpcObjectLowerCamelName(fieldDefinitionContext.type());
            if (manager.fieldTypeIsList(fieldDefinitionContext.type())) {
                String rpcFieldAddAllName = grpcNameUtil.getRpcFieldAddAllName(fieldDefinitionContext);
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
                String rpcFieldSetterName = grpcNameUtil.getRpcFieldSetterName(fieldDefinitionContext);
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
}