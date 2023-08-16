package io.graphoenix.java.generator.implementer;

import com.google.common.collect.Streams;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.core.handler.PackageManager;
import io.graphoenix.core.operation.Field;
import io.graphoenix.core.operation.Operation;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.json.bind.Jsonb;
import org.tinylog.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.graphoenix.core.error.GraphQLErrorType.INPUT_OBJECT_NOT_EXIST;
import static io.graphoenix.core.utils.TypeNameUtil.TYPE_NAME_UTIL;
import static io.graphoenix.java.generator.utils.TypeUtil.TYPE_UTIL;
import static io.graphoenix.spi.constant.Hammurabi.QUERY_TYPE_NAME;

@ApplicationScoped
public class ArgumentsInvokeHandlerBuilder {

    private final IGraphQLDocumentManager manager;
    private final PackageManager packageManager;
    private final TypeManager typeManager;
    private final GraphQLConfig graphQLConfig;
    private Map<String, Map<String, List<Map.Entry<String, String>>>> invokeMethods;

    @Inject
    public ArgumentsInvokeHandlerBuilder(IGraphQLDocumentManager manager, PackageManager packageManager, TypeManager typeManager, GraphQLConfig graphQLConfig) {
        this.manager = manager;
        this.packageManager = packageManager;
        this.typeManager = typeManager;
        this.graphQLConfig = graphQLConfig;
    }

    public void writeToFiler(Filer filer) throws IOException {
        this.invokeMethods = manager.getObjects()
                .filter(manager::isNotOperationType)
                .collect(
                        Collectors.toMap(
                                objectTypeDefinitionContext -> objectTypeDefinitionContext.name().getText(),
                                objectTypeDefinitionContext ->
                                        objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                                                .filter(manager::isInvokeField)
                                                .filter(packageManager::isLocalPackage)
                                                .map(fieldDefinitionContext ->
                                                        new AbstractMap.SimpleEntry<>(
                                                                typeManager.getClassName(fieldDefinitionContext),
                                                                new AbstractMap.SimpleEntry<>(
                                                                        typeManager.getMethodName(fieldDefinitionContext),
                                                                        typeManager.getReturnClassName(fieldDefinitionContext)
                                                                )
                                                        )
                                                )
                                                .collect(
                                                        Collectors.groupingBy(
                                                                AbstractMap.SimpleEntry<String, AbstractMap.SimpleEntry<String, String>>::getKey,
                                                                Collectors.mapping(
                                                                        AbstractMap.SimpleEntry<String, AbstractMap.SimpleEntry<String, String>>::getValue,
                                                                        Collectors.toList()
                                                                )
                                                        )
                                                )
                        )
                );
        this.buildClass().writeTo(filer);
        Logger.info("ArgumentsInvokeHandler build success");
    }

    private JavaFile buildClass() {
        TypeSpec typeSpec = buildInvokeHandler();
        return JavaFile.builder(graphQLConfig.getHandlerPackageName(), typeSpec).build();
    }

    private TypeSpec buildInvokeHandler() {
        return TypeSpec.classBuilder("ArgumentsInvokeHandler")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(ApplicationScoped.class)
                .addField(
                        FieldSpec.builder(
                                ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(Jsonb.class)),
                                "jsonb",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).build()
                )
                .addField(
                        FieldSpec.builder(
                                ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(graphQLConfig.getHandlerPackageName(), "InputInvokeHandler")),
                                "inputInvokeHandlerProvider",
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).build()
                )
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
                                        FieldSpec.builder(ParameterizedTypeName.get(ClassName.get(Provider.class), TYPE_NAME_UTIL.toClassName(className)), typeManager.typeToLowerCamelName(TYPE_NAME_UTIL.toClassName(className).simpleName()))
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
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(Jsonb.class)), "jsonb")
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(graphQLConfig.getHandlerPackageName(), "InputInvokeHandler")), "inputInvokeHandlerProvider")
                .addParameters(
                        classNameSet.stream()
                                .map(className ->
                                        ParameterSpec.builder(
                                                ParameterizedTypeName.get(ClassName.get(Provider.class), TYPE_NAME_UTIL.toClassName(className)),
                                                typeManager.typeToLowerCamelName(TYPE_NAME_UTIL.toClassName(className).simpleName())
                                        ).build()
                                )
                                .collect(Collectors.toList())
                )
                .addStatement("this.jsonb = jsonb")
                .addStatement("this.inputInvokeHandlerProvider = inputInvokeHandlerProvider");

        classNameSet.forEach(className ->
                builder.addStatement("this.$L = $L",
                        typeManager.typeToLowerCamelName(TYPE_NAME_UTIL.toClassName(className).simpleName()),
                        typeManager.typeToLowerCamelName(TYPE_NAME_UTIL.toClassName(className).simpleName())
                )
        );

        return builder.build();
    }

    private List<MethodSpec> buildTypeInvokeMethods() {
        return manager.getObjects()
                .map(this::buildTypeInvokeMethod)
                .collect(Collectors.toList());
    }

    private MethodSpec buildTypeInvokeMethod(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        String typeParameterName = getParameterName(objectTypeDefinitionContext);

        MethodSpec.Builder builder = MethodSpec.methodBuilder(typeParameterName)
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(ClassName.get(Mono.class), ClassName.get(Void.class)))
                .addParameter(ClassName.get(GraphqlParser.SelectionSetContext.class), "selectionSetContext");

        if (manager.isOperationType(objectTypeDefinitionContext)) {
            builder.addParameter(ClassName.get(GraphqlParser.OperationDefinitionContext.class), "operationDefinitionContext")
                    .addStatement("$T operation = new Operation(operationDefinitionContext)", ClassName.get(Operation.class));
        } else {
            builder.addParameter(ClassName.get(Field.class), "field");
        }
        String operationTypeName;
        if (manager.getMutationOperationTypeName().isPresent() && manager.getMutationOperationTypeName().get().equals(objectTypeDefinitionContext.name().getText())) {
            operationTypeName = manager.getMutationOperationTypeName().get();
        } else {
            operationTypeName = manager.getQueryOperationTypeName().orElse(QUERY_TYPE_NAME);
        }

        return builder.addStatement(
                        CodeBlock.join(
                                Stream.of(
                                        CodeBlock.of(
                                                "return $T.fromStream($T.ofNullable(selectionSetContext).map($T.SelectionSetContext::selection).flatMap(selectionContexts -> selectionContexts.stream()))",
                                                ClassName.get(Flux.class),
                                                ClassName.get(Stream.class),
                                                ClassName.get(GraphqlParser.class)
                                        ),
                                        CodeBlock.of(".filter(selectionContext -> selectionContext.field() != null)"),
                                        CodeBlock.builder()
                                                .add(".flatMap(selectionContext -> {\n")
                                                .indent()
                                                .add("String fieldName = selectionContext.field().name().getText();\n")
                                                .add("String selectionName = $T.ofNullable(selectionContext.field().alias()).map(aliasContext -> aliasContext.name().getText()).orElse(selectionContext.field().name().getText());\n", ClassName.get(Optional.class))
                                                .add(manager.isOperationType(objectTypeDefinitionContext) ? CodeBlock.of("$T field = operation.getField(selectionName);\n", ClassName.get(Field.class)) : CodeBlock.of(""))
                                                .add(CodeBlock.builder()
                                                        .beginControlFlow("switch (fieldName)")
                                                        .indent()
                                                        .add(CodeBlock.join(
                                                                        Streams.concat(
                                                                                manager.getFields(objectTypeDefinitionContext.name().getText())
                                                                                        .filter(manager::isNotInvokeField)
                                                                                        .filter(manager::isNotFetchField)
                                                                                        .filter(manager::isNotFunctionField)
                                                                                        .filter(fieldDefinitionContext -> fieldDefinitionContext.argumentsDefinition() != null)
                                                                                        .filter(fieldDefinitionContext -> manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type())))
                                                                                        .filter(fieldDefinitionContext -> !manager.fieldTypeIsList(fieldDefinitionContext.type()))
                                                                                        .map(fieldDefinitionContext -> {
                                                                                                    CodeBlock.Builder codeBlock = CodeBlock.builder().add("case $S:\n", fieldDefinitionContext.name().getText())
                                                                                                            .indent();
                                                                                                    String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
                                                                                                    String methodName = typeManager.typeToLowerCamelName(fieldTypeName) + operationTypeName + "Arguments";
                                                                                                    String argumentInputName = fieldTypeName + operationTypeName + "Arguments";
                                                                                                    codeBlock.add("inputInvokeHandlerProvider.get().$L(jsonb.get().fromJson(field.getArguments().toString(), $T.class), selectionContext.field().arguments())\n",
                                                                                                            methodName,
                                                                                                            TYPE_UTIL.getClassName(packageManager.getClassName(manager.getInputObject(argumentInputName).orElseThrow(() -> new GraphQLErrors(INPUT_OBJECT_NOT_EXIST.bind(argumentInputName)))))
                                                                                                    );
                                                                                                    codeBlock.add(".then($L(selectionContext.field().selectionSet(), field.getField(selectionName)));\n",
                                                                                                            getObjectMethodName(fieldTypeName)
                                                                                                    ).unindent();
                                                                                                    return codeBlock.build();
                                                                                                }
                                                                                        ),
                                                                                manager.getFields(objectTypeDefinitionContext.name().getText())
                                                                                        .filter(manager::isNotInvokeField)
                                                                                        .filter(manager::isNotFetchField)
                                                                                        .filter(manager::isNotFunctionField)
                                                                                        .filter(fieldDefinitionContext -> fieldDefinitionContext.argumentsDefinition() != null)
                                                                                        .filter(fieldDefinitionContext -> manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type())))
                                                                                        .filter(fieldDefinitionContext -> manager.fieldTypeIsList(fieldDefinitionContext.type()))
                                                                                        .map(fieldDefinitionContext -> {
                                                                                                    CodeBlock.Builder codeBlock = CodeBlock.builder().add("case $S:\n", fieldDefinitionContext.name().getText())
                                                                                                            .indent();
                                                                                                    String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
                                                                                                    String methodName = typeManager.typeToLowerCamelName(fieldTypeName) + "List" + operationTypeName + "Arguments";
                                                                                                    String argumentInputName = fieldTypeName + "List" + operationTypeName + "Arguments";
                                                                                                    codeBlock.add("inputInvokeHandlerProvider.get().$L(jsonb.get().fromJson(field.getArguments().toString(), $T.class), selectionContext.field().arguments())\n",
                                                                                                            methodName,
                                                                                                            TYPE_UTIL.getClassName(packageManager.getClassName(manager.getInputObject(argumentInputName).orElseThrow(() -> new GraphQLErrors(INPUT_OBJECT_NOT_EXIST.bind(argumentInputName)))))
                                                                                                    );
                                                                                                    codeBlock.add(".then($L(selectionContext.field().selectionSet(), field.getField(selectionName)));\n",
                                                                                                            getObjectMethodName(fieldTypeName)
                                                                                                    ).unindent();
                                                                                                    return codeBlock.build();
                                                                                                }
                                                                                        ),
                                                                                Stream.of(CodeBlock.builder().add(CodeBlock.of("default:\n")).indent().add(CodeBlock.of("return $T.empty();\n", ClassName.get(Flux.class))).unindent().build())
                                                                        ).collect(Collectors.toList()),
                                                                        System.lineSeparator()
                                                                )
                                                        )
                                                        .unindent()
                                                        .endControlFlow()
                                                        .build()
                                                )
                                                .unindent()
                                                .add("})")
                                                .build(),
                                        CodeBlock.of(".then()")
                                ).collect(Collectors.toList()),
                                System.lineSeparator()
                        )
                )
                .build();
    }

    private String getParameterName(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        return typeManager.typeToLowerCamelName(objectTypeDefinitionContext.name().getText());
    }

    private String getObjectMethodName(String objectName) {
        return typeManager.typeToLowerCamelName(objectName);
    }
}
