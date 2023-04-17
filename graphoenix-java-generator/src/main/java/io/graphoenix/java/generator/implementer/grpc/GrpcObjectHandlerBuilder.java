package io.graphoenix.java.generator.implementer.grpc;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.core.handler.PackageManager;
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
import static io.graphoenix.core.utils.TypeNameUtil.TYPE_NAME_UTIL;

@ApplicationScoped
public class GrpcObjectHandlerBuilder {

    private final IGraphQLDocumentManager manager;
    private final PackageManager packageManager;
    private final GraphQLConfig graphQLConfig;
    private final GrpcNameUtil grpcNameUtil;

    @Inject
    public GrpcObjectHandlerBuilder(IGraphQLDocumentManager manager, PackageManager packageManager, GrpcNameUtil grpcNameUtil, GraphQLConfig graphQLConfig) {
        this.manager = manager;
        this.packageManager = packageManager;
        this.grpcNameUtil = grpcNameUtil;
        this.graphQLConfig = graphQLConfig;
    }

    public void writeToFiler(Filer filer) throws IOException {
        this.buildClass().writeTo(filer);
        Logger.info("GrpcObjectHandler build success");
    }

    private JavaFile buildClass() {
        TypeSpec typeSpec = buildGrpcObjectHandler();
        return JavaFile.builder(graphQLConfig.getGrpcHandlerPackageName(), typeSpec).build();
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
                .filter(manager::isNotOperationType)
                .map(this::buildTypeMethod)
                .collect(Collectors.toList());
    }

    private MethodSpec buildTypeMethod(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        String objectParameterName = grpcNameUtil.getLowerCamelName(objectTypeDefinitionContext.name().getText());
        ClassName className = TYPE_NAME_UTIL.toClassName(packageManager.getClassName(objectTypeDefinitionContext));
        String grpcObjectPackageName = packageManager.getGrpcClassName(objectTypeDefinitionContext);
        ClassName grpcObjectClassName = TYPE_NAME_UTIL.toClassName(grpcObjectPackageName);

        MethodSpec.Builder builder = MethodSpec.methodBuilder(grpcNameUtil.getLowerCamelName(objectTypeDefinitionContext))
                .addModifiers(Modifier.PUBLIC)
                .returns(grpcObjectClassName)
                .addParameter(className, objectParameterName)
                .addStatement("$T builder = $T.newBuilder()",
                        ClassName.get(grpcObjectPackageName, "Builder"),
                        grpcObjectClassName
                );

        List<GraphqlParser.FieldDefinitionContext> fieldDefinitionContexts = objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                .filter(manager::isNotFetchField)
                .collect(Collectors.toList());
        for (GraphqlParser.FieldDefinitionContext fieldDefinitionContext : fieldDefinitionContexts) {
            String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
            String fieldGetterName = grpcNameUtil.getGetMethodName(fieldDefinitionContext);
            String objectFieldMethodName = grpcNameUtil.getLowerCamelName(fieldDefinitionContext.type());
            CodeBlock codeBlock;
            if (manager.fieldTypeIsList(fieldDefinitionContext.type())) {
                String grpcAddAllMethodName = grpcNameUtil.getGrpcAddAllMethodName(fieldDefinitionContext);
                if (manager.isScalar(fieldTypeName)) {
                    switch (fieldTypeName) {
                        case "DateTime":
                        case "Timestamp":
                        case "Date":
                        case "Time":
                        case "BigInteger":
                        case "BigDecimal":
                            codeBlock = CodeBlock.of("builder.$L($L.$L().stream().map(item -> $T.CODEC_UTIL.encode(item)).collect($T.toList()))",
                                    grpcAddAllMethodName,
                                    objectParameterName,
                                    fieldGetterName,
                                    ClassName.get(CodecUtil.class)
                            );
                            break;
                        default:
                            codeBlock = CodeBlock.of("builder.$L($L.$L())",
                                    grpcAddAllMethodName,
                                    objectParameterName,
                                    fieldGetterName
                            );
                            break;
                    }
                } else if (manager.isEnum(fieldTypeName)) {
                    codeBlock = CodeBlock.of("builder.$L($L.$L().stream().map(item -> $T.forNumber(item.ordinal())).collect($T.toList()))",
                            grpcAddAllMethodName,
                            objectParameterName,
                            fieldGetterName,
                            TYPE_NAME_UTIL.toClassName(packageManager.getGrpcClassName(fieldDefinitionContext.type())),
                            ClassName.get(Collectors.class)
                    );
                } else if (manager.isObject(fieldTypeName)) {
                    codeBlock = CodeBlock.of("builder.$L($L.$L().stream().map(this::$L).collect($T.toList()))",
                            grpcAddAllMethodName,
                            objectParameterName,
                            fieldGetterName,
                            objectFieldMethodName,
                            ClassName.get(Collectors.class)
                    );
                } else {
                    throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE);
                }
                if (fieldDefinitionContext.type().nonNullType() != null) {
                    builder.addStatement("assert $L.$L() != null && $L.$L().size() > 0", objectParameterName, fieldGetterName, objectParameterName, fieldGetterName)
                            .addStatement(codeBlock);
                } else {
                    builder.addStatement(codeBlock);
                }
            } else {
                String grpcSetMethodName = grpcNameUtil.getGrpcSetMethodName(fieldDefinitionContext);
                if (manager.isScalar(manager.getFieldTypeName(fieldDefinitionContext.type()))) {
                    switch (fieldTypeName) {
                        case "DateTime":
                        case "Timestamp":
                        case "Date":
                        case "Time":
                        case "BigInteger":
                        case "BigDecimal":
                            codeBlock = CodeBlock.of("builder.$L($T.CODEC_UTIL.encode($L.$L()))",
                                    grpcSetMethodName,
                                    ClassName.get(CodecUtil.class),
                                    objectParameterName,
                                    fieldGetterName
                            );
                            break;
                        default:
                            codeBlock = CodeBlock.of("builder.$L($L.$L())",
                                    grpcSetMethodName,
                                    objectParameterName,
                                    fieldGetterName
                            );
                            break;
                    }
                } else if (manager.isEnum(fieldTypeName)) {
                    codeBlock = CodeBlock.of("builder.$L($T.forNumber($L.$L().ordinal()))",
                            grpcSetMethodName,
                            TYPE_NAME_UTIL.toClassName(packageManager.getGrpcClassName(fieldDefinitionContext.type())),
                            objectParameterName,
                            fieldGetterName
                    );
                } else if (manager.isObject(fieldTypeName)) {
                    codeBlock = CodeBlock.of("builder.$L($L($L.$L()))",
                            grpcSetMethodName,
                            objectFieldMethodName,
                            objectParameterName,
                            fieldGetterName
                    );
                } else {
                    throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE);
                }
                if (fieldDefinitionContext.type().nonNullType() != null) {
                    builder.addStatement("assert $L.$L() != null", objectParameterName, fieldGetterName)
                            .addStatement(codeBlock);
                } else {
                    builder.addStatement(codeBlock);
                }
            }
        }
        builder.addStatement("return builder.build()");
        return builder.build();
    }
}
