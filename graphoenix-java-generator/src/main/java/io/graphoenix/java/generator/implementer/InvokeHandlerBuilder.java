package io.graphoenix.java.generator.implementer;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.tinylog.Logger;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class InvokeHandlerBuilder {

    private final IGraphQLDocumentManager manager;
    private final TypeManager typeManager;
    private GraphQLConfig graphQLConfig;
    private Map<String, Map<String, List<String>>> invokeMethods;

    @Inject
    public InvokeHandlerBuilder(IGraphQLDocumentManager manager, TypeManager typeManager) {
        this.manager = manager;
        this.typeManager = typeManager;
    }

    public InvokeHandlerBuilder setConfiguration(GraphQLConfig graphQLConfig) {
        this.graphQLConfig = graphQLConfig;
        this.typeManager.setGraphQLConfig(graphQLConfig);
        return this;
    }

    public void writeToFiler(Filer filer) throws IOException {
        this.invokeMethods = manager.getObjects()
                .filter(objectTypeDefinitionContext ->
                        !manager.isQueryOperationType(objectTypeDefinitionContext.name().getText()) &&
                                !manager.isMutationOperationType(objectTypeDefinitionContext.name().getText()) &&
                                !manager.isSubscriptionOperationType(objectTypeDefinitionContext.name().getText())
                )
                .collect(Collectors.toMap(
                                objectTypeDefinitionContext -> objectTypeDefinitionContext.name().getText(),
                                objectTypeDefinitionContext ->
                                        objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                                                .filter(fieldDefinitionContext -> fieldDefinitionContext.directives() != null)
                                                .filter(fieldDefinitionContext -> fieldDefinitionContext.directives().directive().stream().anyMatch(directiveContext -> directiveContext.name().getText().equals("invoke")))
                                                .map(fieldDefinitionContext ->
                                                        new AbstractMap.SimpleEntry<>(
                                                                typeManager.getClassName(fieldDefinitionContext),
                                                                typeManager.getMethodName(fieldDefinitionContext)
                                                        )
                                                )
                                                .collect(
                                                        Collectors.groupingBy(
                                                                AbstractMap.SimpleEntry<String, String>::getKey,
                                                                Collectors.mapping(
                                                                        AbstractMap.SimpleEntry<String, String>::getValue,
                                                                        Collectors.toList()
                                                                )
                                                        )
                                                )
                        )
                );
        this.buildClass().writeTo(filer);
        Logger.info("InvokeHandler build success");
    }

    private JavaFile buildClass() {
        TypeSpec typeSpec = buildInvokeHandler();
        return JavaFile.builder(graphQLConfig.getHandlerPackageName(), typeSpec).build();
    }

    private TypeSpec buildInvokeHandler() {
        return TypeSpec.classBuilder("InvokeHandler")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(ApplicationScoped.class)
                .addFields(buildFields())
                .addMethod(buildConstructor())
                .addMethods(buildTypeInvokeMethods())
                .build();
    }

    private Set<FieldSpec> buildFields() {
        return this.invokeMethods.values().stream()
                .flatMap(classMap ->
                        classMap.keySet().stream()
                                .map(className ->
                                        FieldSpec.builder(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.bestGuess(className)), typeManager.typeToLowerCamelName(ClassName.bestGuess(className).simpleName()))
                                                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                                                .build()
                                )
                )
                .collect(Collectors.toSet());
    }

    private MethodSpec buildConstructor() {
        Set<String> classNameSet = invokeMethods.values().stream()
                .flatMap(value -> value.keySet().stream())
                .collect(Collectors.toSet());

        MethodSpec.Builder builder = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Inject.class)
                .addParameters(classNameSet.stream()
                        .map(className ->
                                ParameterSpec.builder(
                                        ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.bestGuess(className)),
                                        typeManager.typeToLowerCamelName(ClassName.bestGuess(className).simpleName())
                                ).build()
                        )
                        .collect(Collectors.toList())
                );

        classNameSet.forEach(className ->
                builder.addStatement("this.$L = $L",
                        typeManager.typeToLowerCamelName(ClassName.bestGuess(className).simpleName()),
                        typeManager.typeToLowerCamelName(ClassName.bestGuess(className).simpleName())
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
        ClassName typeClassName;
        Optional<String> className = typeManager.getClassName(objectTypeDefinitionContext);
        if (className.isPresent()) {
            typeClassName = ClassName.bestGuess(className.get());
        } else {
            typeClassName = ClassName.get(graphQLConfig.getObjectTypePackageName(), objectTypeDefinitionContext.name().getText());
        }
        MethodSpec.Builder builder = MethodSpec.methodBuilder(typeManager.typeToLowerCamelName(objectTypeDefinitionContext.name().getText()))
                .addModifiers(Modifier.PUBLIC)
                .returns(typeClassName)
                .addParameter(typeClassName, getParameterName(objectTypeDefinitionContext));

        if (invokeMethods.get(objectTypeDefinitionContext.name().getText()) != null) {
            builder.beginControlFlow("if ($L != null)", getParameterName(objectTypeDefinitionContext));
            invokeMethods.get(objectTypeDefinitionContext.name().getText())
                    .forEach((returnClassName, methodNames) ->
                            methodNames.forEach(methodName ->
                                    builder.addStatement("$L.$L($L.get().$L($L))",
                                            getParameterName(objectTypeDefinitionContext),
                                            typeManager.getFieldSetterMethodName(typeManager.getInvokeFieldName(methodName)),
                                            typeManager.typeToLowerCamelName(ClassName.bestGuess(returnClassName).simpleName()),
                                            methodName,
                                            getParameterName(objectTypeDefinitionContext)
                                    )
                            )
                    );

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
        return typeManager.typeToLowerCamelName(objectTypeDefinitionContext.name().getText());
    }

    private String getObjectMethodName(String objectName) {
        return typeManager.typeToLowerCamelName(objectName);
    }
}
