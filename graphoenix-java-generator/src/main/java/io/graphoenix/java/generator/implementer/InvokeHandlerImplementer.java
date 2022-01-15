package io.graphoenix.java.generator.implementer;

import com.google.common.base.CaseFormat;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.java.generator.config.JavaGeneratorConfig;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;

import javax.inject.Inject;
import javax.lang.model.element.Modifier;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class InvokeHandlerImplementer {

    private final IGraphQLDocumentManager manager;
    private JavaGeneratorConfig configuration;
    private Map<String, Map<String, List<String>>> invokeMethods;

    @Inject
    public InvokeHandlerImplementer(IGraphQLDocumentManager manager) {
        this.manager = manager;
    }

    public InvokeHandlerImplementer setConfiguration(JavaGeneratorConfig configuration, Map<String, Map<String, List<String>>> invokeMethods) {
        this.configuration = configuration;
        this.invokeMethods = invokeMethods;
        return this;
    }

    public TypeSpec buildInvokeHandlerImpl() {
        return TypeSpec.classBuilder("InvokeHandlerImpl")
                .addMethods(buildTypeInvokeMethods())
                .build();
    }

    public List<MethodSpec> buildTypeInvokeMethods() {
        return manager.getObjects().map(this::buildTypeInvokeMethod).collect(Collectors.toList());
    }

    public MethodSpec buildTypeInvokeMethod(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        return MethodSpec.methodBuilder(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, objectTypeDefinitionContext.name().getText()))
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get(configuration.getObjectTypePackageName(), objectTypeDefinitionContext.name().getText()), getParameterName(objectTypeDefinitionContext))
                .addStatement(buildTypeInvokeMethodStatement(objectTypeDefinitionContext))
                .build();
    }

    public CodeBlock buildTypeInvokeMethodStatement(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        CodeBlock.Builder builder = CodeBlock.builder();

        if (invokeMethods.get(objectTypeDefinitionContext.name().getText()) != null) {
            CodeBlock.Builder nullCheckFlow = builder.beginControlFlow("if ($S != null)");
            invokeMethods.get(objectTypeDefinitionContext.name().getText())
                    .forEach((key, value) ->
                            value.forEach(methodName ->
                                    nullCheckFlow
                                            .addStatement("$S.$S($S.$S($S))",
                                                    getParameterName(objectTypeDefinitionContext),
                                                    getInvokeFieldSetterMethodName(methodName),
                                                    CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, key),
                                                    methodName,
                                                    getParameterName(objectTypeDefinitionContext)
                                            )
                            ));

            manager.getFields(objectTypeDefinitionContext.name().getText())
                    .filter(fieldDefinitionContext -> manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type())))
                    .filter(fieldDefinitionContext -> !manager.fieldTypeIsList(fieldDefinitionContext.type()))
                    .forEach(fieldDefinitionContext ->
                            nullCheckFlow.addStatement("$S($S.$S)",
                                    getObjectMethodName(manager.getFieldTypeName(fieldDefinitionContext.type())),
                                    getParameterName(objectTypeDefinitionContext),
                                    getFieldGetterMethodName(fieldDefinitionContext)
                            )
                    );

            manager.getFields(objectTypeDefinitionContext.name().getText())
                    .filter(fieldDefinitionContext -> manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type())))
                    .filter(fieldDefinitionContext -> manager.fieldTypeIsList(fieldDefinitionContext.type()))
                    .forEach(fieldDefinitionContext ->
                            nullCheckFlow.addStatement(
                                    CodeBlock.builder()
                                            .beginControlFlow("if ($S.$S != null)",
                                                    getParameterName(objectTypeDefinitionContext),
                                                    getFieldGetterMethodName(fieldDefinitionContext)
                                            )
                                            .addStatement("$S.$S.forEach(this::$S)",
                                                    getParameterName(objectTypeDefinitionContext),
                                                    getFieldGetterMethodName(fieldDefinitionContext),
                                                    getObjectMethodName(manager.getFieldTypeName(fieldDefinitionContext.type()))
                                            )
                                            .endControlFlow().build()
                            )
                    );

            nullCheckFlow.endControlFlow();
        }

        return builder.addStatement("return $S", getParameterName(objectTypeDefinitionContext)).build();
    }

    private String getParameterName(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, objectTypeDefinitionContext.name().getText());
    }

    private String getObjectMethodName(String objectName) {
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, objectName);
    }

    private String getInvokeFieldName(String methodName) {
        if (methodName.startsWith("get")) {
            return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, methodName.replaceFirst("get", ""));
        } else if (methodName.startsWith("set")) {
            return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, methodName.replaceFirst("set", ""));
        } else if (methodName.startsWith("is")) {
            return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, methodName.replaceFirst("is", ""));
        } else {
            return methodName;
        }
    }

    private String getInvokeFieldGetterMethodName(String methodName) {
        return "get".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, getInvokeFieldName(methodName)));
    }

    private String getInvokeFieldSetterMethodName(String methodName) {
        return "set".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, getInvokeFieldName(methodName)));
    }


    private String getFieldGetterMethodName(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return "get".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, getInvokeFieldName(fieldDefinitionContext.name().getText())));
    }

    private String getFieldSetterMethodName(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return "set".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, getInvokeFieldName(fieldDefinitionContext.name().getText())));
    }
}
