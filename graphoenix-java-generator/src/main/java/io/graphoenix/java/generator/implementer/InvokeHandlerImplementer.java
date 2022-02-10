package io.graphoenix.java.generator.implementer;

import com.google.common.base.CaseFormat;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.annotation.processing.Filer;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class InvokeHandlerImplementer {

    private final IGraphQLDocumentManager manager;
    private final TypeManager typeManager;
    private GraphQLConfig graphQLConfig;
    private Map<String, Map<TypeElement, List<ExecutableElement>>> invokeMethods;

    @Inject
    public InvokeHandlerImplementer(IGraphQLDocumentManager manager, TypeManager typeManager) {
        this.manager = manager;
        this.typeManager = typeManager;
    }

    public InvokeHandlerImplementer setConfiguration(GraphQLConfig graphQLConfig) {
        this.graphQLConfig = graphQLConfig;
        this.typeManager.setManager(graphQLConfig);
        return this;
    }

    public InvokeHandlerImplementer setInvokeMethods(Map<String, Map<TypeElement, List<ExecutableElement>>> invokeMethods) {
        this.invokeMethods = invokeMethods;
        return this;
    }

    public void writeToFiler(Filer filer) throws IOException {
        this.buildImplementClass().writeTo(filer);
    }

    private JavaFile buildImplementClass() {
        TypeSpec typeSpec = buildInvokeHandlerImpl();
        return JavaFile.builder(graphQLConfig.getHandlerPackageName(), typeSpec).build();
    }

    private TypeSpec buildInvokeHandlerImpl() {
        return TypeSpec.classBuilder("InvokeHandler")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Singleton.class)
                .addFields(buildFields())
                .addMethod(buildConstructor())
                .addMethods(buildTypeInvokeMethods())
                .build();
    }

    private Set<FieldSpec> buildFields() {
        return this.invokeMethods.values().stream()
                .flatMap(typeElementListMap ->
                        typeElementListMap.keySet().stream()
                                .map(typeElement ->
                                        FieldSpec.builder(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(typeElement)), CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, typeElement.getSimpleName().toString()))
                                                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                                                .build()
                                )
                )
                .collect(Collectors.toSet());
    }

    private MethodSpec buildConstructor() {
        MethodSpec.Builder builder = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);

        invokeMethods.values().stream()
                .flatMap(value -> value.keySet().stream())
                .collect(Collectors.toSet())
                .forEach(typeElement ->
                        builder.addStatement("this.$L = $T.getProvider($T.class)",
                                CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, typeElement.getSimpleName().toString()),
                                ClassName.get(BeanContext.class),
                                ClassName.get(typeElement)
                        )
                );

        return builder.build();
    }

    private List<MethodSpec> buildTypeInvokeMethods() {
        return manager.getObjects()
                .filter(objectTypeDefinitionContext ->
                        !manager.isQueryOperationType(objectTypeDefinitionContext.name().getText()) &&
                                !manager.isMutationOperationType(objectTypeDefinitionContext.name().getText()) &&
                                !manager.isSubscriptionOperationType(objectTypeDefinitionContext.name().getText())
                )
                .map(this::buildTypeInvokeMethod).collect(Collectors.toList());
    }

    private MethodSpec buildTypeInvokeMethod(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, objectTypeDefinitionContext.name().getText()))
                .addModifiers(Modifier.PUBLIC)
                .returns(ClassName.get(graphQLConfig.getObjectTypePackageName(), objectTypeDefinitionContext.name().getText()))
                .addParameter(ClassName.get(graphQLConfig.getObjectTypePackageName(), objectTypeDefinitionContext.name().getText()), getParameterName(objectTypeDefinitionContext));

        if (invokeMethods.get(objectTypeDefinitionContext.name().getText()) != null) {
            builder.beginControlFlow("if ($L != null)", getParameterName(objectTypeDefinitionContext));
            invokeMethods.get(objectTypeDefinitionContext.name().getText())
                    .forEach((key, value) ->
                            value.forEach(executableElement ->
                                    builder.addStatement("$L.$L($L.get().$L($L))",
                                            getParameterName(objectTypeDefinitionContext),
                                            typeManager.getInvokeFieldSetterMethodName(executableElement.getSimpleName().toString()),
                                            CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, key.getSimpleName().toString()),
                                            executableElement.getSimpleName().toString(),
                                            getParameterName(objectTypeDefinitionContext)
                                    )
                            ));

            manager.getFields(objectTypeDefinitionContext.name().getText())
                    .filter(fieldDefinitionContext -> manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type())))
                    .filter(fieldDefinitionContext -> !manager.fieldTypeIsList(fieldDefinitionContext.type()))
                    .forEach(fieldDefinitionContext ->
                            builder.addStatement("$L($L.$L())",
                                    getObjectMethodName(manager.getFieldTypeName(fieldDefinitionContext.type())),
                                    getParameterName(objectTypeDefinitionContext),
                                    typeManager.getFieldGetterMethodName(fieldDefinitionContext)
                            )
                    );

            manager.getFields(objectTypeDefinitionContext.name().getText())
                    .filter(fieldDefinitionContext -> manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type())))
                    .filter(fieldDefinitionContext -> manager.fieldTypeIsList(fieldDefinitionContext.type()))
                    .forEach(fieldDefinitionContext ->
                            builder.beginControlFlow("if ($L.$L() != null)",
                                    getParameterName(objectTypeDefinitionContext),
                                    typeManager.getFieldGetterMethodName(fieldDefinitionContext)
                            ).addStatement("$L.$L().forEach(this::$L)",
                                    getParameterName(objectTypeDefinitionContext),
                                    typeManager.getFieldGetterMethodName(fieldDefinitionContext),
                                    getObjectMethodName(manager.getFieldTypeName(fieldDefinitionContext.type()))
                            ).endControlFlow().build()
                    );

            builder.endControlFlow();
        }
        builder.addStatement("return $L", getParameterName(objectTypeDefinitionContext));

        return builder.build();
    }

    private String getParameterName(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, objectTypeDefinitionContext.name().getText());
    }

    private String getObjectMethodName(String objectName) {
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, objectName);
    }
}
