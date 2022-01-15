package io.graphoenix.java.generator.implementer;

import com.google.common.base.CaseFormat;
import com.squareup.javapoet.*;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.java.generator.config.JavaGeneratorConfig;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;

import javax.annotation.processing.Filer;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class InvokeHandlerImplementer {

    private final IGraphQLDocumentManager manager;
    private JavaGeneratorConfig configuration;
    private Map<String, Map<TypeElement, List<ExecutableElement>>> invokeMethods;

    @Inject
    public InvokeHandlerImplementer(IGraphQLDocumentManager manager) {
        this.manager = manager;
    }

    public InvokeHandlerImplementer setConfiguration(JavaGeneratorConfig configuration) {
        this.configuration = configuration;
        return this;
    }

    public InvokeHandlerImplementer setInvokeMethods(Map<String, Map<TypeElement, List<ExecutableElement>>> invokeMethods) {
        this.invokeMethods = invokeMethods;
        return this;
    }

    public void writeToFiler(Filer filer) throws IOException {
        this.buildImplementClass().writeTo(filer);
    }

    public JavaFile buildImplementClass() {
        TypeSpec typeSpec = buildInvokeHandlerImpl();
        return JavaFile.builder(configuration.getPackageName(), typeSpec).build();
    }

    public TypeSpec buildInvokeHandlerImpl() {
        return TypeSpec.classBuilder("InvokeHandlerImpl")
                .addFields(buildFields())
                .addMethod(buildConstructor())
                .addMethods(buildTypeInvokeMethods())
                .build();
    }

    public Set<FieldSpec> buildFields() {
        return this.invokeMethods.values().stream()
                .flatMap(typeElementListMap ->
                        typeElementListMap.keySet().stream()
                                .map(typeElement ->
                                        FieldSpec.builder(ClassName.get(typeElement), CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, typeElement.getSimpleName().toString())).build()
                                )
                ).collect(Collectors.toSet());
    }

    public MethodSpec buildConstructor() {
        MethodSpec.Builder builder = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);

        invokeMethods.forEach((key, value) ->
                value.keySet().forEach(
                        typeElement ->
                                builder.addStatement("this.$L = $T.get($T.class)",
                                        CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, typeElement.getSimpleName().toString()),
                                        ClassName.get(BeanContext.class),
                                        ClassName.get(typeElement)
                                )
                )
        );

        manager.getObjects().forEach(objectTypeDefinitionContext ->
                builder.addStatement("Function<$T, $T> $L = this::$L",
                        ClassName.get(configuration.getObjectTypePackageName(), objectTypeDefinitionContext.name().getText()),
                        ClassName.get(configuration.getObjectTypePackageName(), objectTypeDefinitionContext.name().getText()),
                        CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, objectTypeDefinitionContext.name().getText()),
                        CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, objectTypeDefinitionContext.name().getText())
                ).addStatement("put(User.class, user)",
                        ClassName.get(configuration.getObjectTypePackageName(), objectTypeDefinitionContext.name().getText()),
                        CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, objectTypeDefinitionContext.name().getText())
                )
        );

        return builder.build();
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
            CodeBlock.Builder nullCheckFlow = builder.beginControlFlow("if ($L != null)", getParameterName(objectTypeDefinitionContext));
            invokeMethods.get(objectTypeDefinitionContext.name().getText())
                    .forEach((key, value) ->
                            value.forEach(executableElement ->
                                    nullCheckFlow
                                            .addStatement("$L.$L($L.$L($L))",
                                                    getParameterName(objectTypeDefinitionContext),
                                                    getInvokeFieldSetterMethodName(executableElement.getSimpleName().toString()),
                                                    CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, key.getSimpleName().toString()),
                                                    executableElement.getSimpleName().toString(),
                                                    getParameterName(objectTypeDefinitionContext)
                                            )
                            ));

            manager.getFields(objectTypeDefinitionContext.name().getText())
                    .filter(fieldDefinitionContext -> manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type())))
                    .filter(fieldDefinitionContext -> !manager.fieldTypeIsList(fieldDefinitionContext.type()))
                    .forEach(fieldDefinitionContext ->
                            nullCheckFlow.addStatement("$L($L.$L)",
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
                                            .beginControlFlow("if ($L.$L != null)",
                                                    getParameterName(objectTypeDefinitionContext),
                                                    getFieldGetterMethodName(fieldDefinitionContext)
                                            )
                                            .addStatement("$L.$L.forEach(this::$L)",
                                                    getParameterName(objectTypeDefinitionContext),
                                                    getFieldGetterMethodName(fieldDefinitionContext),
                                                    getObjectMethodName(manager.getFieldTypeName(fieldDefinitionContext.type()))
                                            )
                                            .endControlFlow().build()
                            )
                    );

            nullCheckFlow.endControlFlow();
        }

        return builder/*.addStatement("return $L", getParameterName(objectTypeDefinitionContext))*/.build();
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
