package io.graphoenix.java.generator.implementer.grpc;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.core.operation.ObjectValueWithVariable;
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
public class GrpcInputObjectHandlerBuilder {

    private final IGraphQLDocumentManager manager;
    private final TypeManager typeManager;
    private final GrpcNameUtil grpcNameUtil;
    private GraphQLConfig graphQLConfig;

    @Inject
    public GrpcInputObjectHandlerBuilder(IGraphQLDocumentManager manager, TypeManager typeManager, GrpcNameUtil grpcNameUtil) {
        this.manager = manager;
        this.typeManager = typeManager;
        this.grpcNameUtil = grpcNameUtil;
    }

    public GrpcInputObjectHandlerBuilder setConfiguration(GraphQLConfig graphQLConfig) {
        this.graphQLConfig = graphQLConfig;
        this.typeManager.setGraphQLConfig(graphQLConfig);
        return this;
    }

    public void writeToFiler(Filer filer) throws IOException {
        this.buildClass().writeTo(filer);
        Logger.info("GrpcInputObjectHandler build success");
    }

    private JavaFile buildClass() {
        TypeSpec typeSpec = buildGrpcInputObjectHandler();
        return JavaFile.builder(graphQLConfig.getHandlerPackageName(), typeSpec).build();
    }

    private TypeSpec buildGrpcInputObjectHandler() {
        return TypeSpec.classBuilder("GrpcInputObjectHandler")
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
        return manager.getInputObjects()
                .map(this::buildTypeMethod)
                .collect(Collectors.toList());
    }

    private MethodSpec buildTypeMethod(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {
        String inputObjectParameterName = grpcNameUtil.getRpcInputObjectLowerCamelName(inputObjectTypeDefinitionContext);
        ClassName typeClassName = ClassName.get(graphQLConfig.getGrpcPackageName(), grpcNameUtil.getRpcInputObjectName(inputObjectTypeDefinitionContext));
        MethodSpec.Builder builder = MethodSpec.methodBuilder(inputObjectParameterName)
                .addModifiers(Modifier.PUBLIC)
                .returns(ClassName.get(ObjectValueWithVariable.class))
                .addParameter(typeClassName, inputObjectParameterName)
                .addStatement("$T objectValueWithVariable = new $T()", ClassName.get(ObjectValueWithVariable.class), ClassName.get(ObjectValueWithVariable.class));

        List<GraphqlParser.InputValueDefinitionContext> inputValueDefinitionContexts = inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition();
        for (GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext : inputValueDefinitionContexts) {
            CodeBlock codeBlock;
            String fieldTypeName = manager.getFieldTypeName(inputValueDefinitionContext.type());
            String rpcEnumValueSuffixName = grpcNameUtil.getRpcEnumValueSuffixName(inputValueDefinitionContext.type());
            String inputObjectFieldMethodName = grpcNameUtil.getRpcInputObjectLowerCamelName(inputValueDefinitionContext.type());
            if (manager.fieldTypeIsList(inputValueDefinitionContext.type())) {
                String rpcGetInputValueListName = grpcNameUtil.getRpcGetInputValueListName(inputValueDefinitionContext);
                if (manager.isScalar(fieldTypeName)) {
                    codeBlock = CodeBlock.of("objectValueWithVariable.put($S, $L.$L())",
                            inputValueDefinitionContext.name().getText(),
                            inputObjectParameterName,
                            rpcGetInputValueListName
                    );
                } else if (manager.isEnum(fieldTypeName)) {
                    codeBlock = CodeBlock.of("objectValueWithVariable.put($S, $L.$L().stream().map(item -> item.getValueDescriptor().getName().replaceFirst($S, EMPTY)).collect($T.toList()))",
                            inputValueDefinitionContext.name().getText(),
                            inputObjectParameterName,
                            rpcGetInputValueListName,
                            rpcEnumValueSuffixName,
                            ClassName.get(Collectors.class)
                    );
                } else if (manager.isInputObject(fieldTypeName)) {
                    codeBlock = CodeBlock.of("objectValueWithVariable.put($S, $L.$L().stream().map(this::$L).collect($T.toList()))",
                            inputValueDefinitionContext.name().getText(),
                            inputObjectParameterName,
                            rpcGetInputValueListName,
                            inputObjectFieldMethodName,
                            ClassName.get(Collectors.class)
                    );
                } else {
                    throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE);
                }
                if (inputValueDefinitionContext.type().nonNullType() == null) {
                    builder.beginControlFlow("if ($L.$L() > 0)", inputObjectParameterName, grpcNameUtil.getRpcGetInputValueCountName(inputValueDefinitionContext))
                            .addStatement(codeBlock)
                            .endControlFlow();
                } else {
                    builder.addStatement(codeBlock);
                }
            } else {
                String rpcGetInputValueName = grpcNameUtil.getRpcGetInputValueName(inputValueDefinitionContext);
                if (manager.isScalar(fieldTypeName)) {
                    codeBlock = CodeBlock.of("objectValueWithVariable.put($S, $L.$L())",
                            inputValueDefinitionContext.name().getText(),
                            inputObjectParameterName,
                            rpcGetInputValueName
                    );
                } else if (manager.isEnum(fieldTypeName)) {
                    codeBlock = CodeBlock.of("objectValueWithVariable.put($S, $L.$L().getValueDescriptor().getName().replaceFirst($S, EMPTY))",
                            inputValueDefinitionContext.name().getText(),
                            inputObjectParameterName,
                            rpcGetInputValueName,
                            rpcEnumValueSuffixName
                    );
                } else if (manager.isInputObject(fieldTypeName)) {
                    codeBlock = CodeBlock.of("objectValueWithVariable.put($S, $L($L.$L()))",
                            inputValueDefinitionContext.name().getText(),
                            inputObjectFieldMethodName,
                            inputObjectParameterName,
                            rpcGetInputValueName
                    );
                } else {
                    throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE);
                }
                if (inputValueDefinitionContext.type().nonNullType() == null) {
                    builder.beginControlFlow("if ($L.$L())", inputObjectParameterName, grpcNameUtil.getRpcHasInputValueName(inputValueDefinitionContext))
                            .addStatement(codeBlock)
                            .endControlFlow();
                } else {
                    builder.addStatement(codeBlock);
                }
            }
        }
        builder.addStatement("return objectValueWithVariable");
        return builder.build();
    }
}
