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
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.json.bind.Jsonb;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.tinylog.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.graphoenix.core.utils.TypeNameUtil.TYPE_NAME_UTIL;
import static io.graphoenix.java.generator.utils.TypeUtil.TYPE_UTIL;

@ApplicationScoped
public class InvokeHandlerBuilder {

    private final IGraphQLDocumentManager manager;
    private final TypeManager typeManager;
    private final GraphQLConfig graphQLConfig;
    private Map<String, Map<String, List<Map.Entry<String, String>>>> invokeMethods;

    @Inject
    public InvokeHandlerBuilder(IGraphQLDocumentManager manager, TypeManager typeManager, GraphQLConfig graphQLConfig) {
        this.manager = manager;
        this.typeManager = typeManager;
        this.graphQLConfig = graphQLConfig;
    }

    public void writeToFiler(Filer filer) throws IOException {
        this.invokeMethods = manager.getObjects()
                .filter(objectTypeDefinitionContext -> !manager.isQueryOperationType(objectTypeDefinitionContext.name().getText()))
                .filter(objectTypeDefinitionContext -> !manager.isMutationOperationType(objectTypeDefinitionContext.name().getText()))
                .filter(objectTypeDefinitionContext -> !manager.isSubscriptionOperationType(objectTypeDefinitionContext.name().getText()))
                .collect(
                        Collectors.toMap(
                                objectTypeDefinitionContext -> objectTypeDefinitionContext.name().getText(),
                                objectTypeDefinitionContext ->
                                        objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                                                .filter(manager::isInvokeField)
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
                .addField(
                        FieldSpec.builder(
                                ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(Jsonb.class)),
                                "jsonb",
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
                                        FieldSpec.builder(ParameterizedTypeName.get(ClassName.get(Provider.class), TYPE_NAME_UTIL.bestGuess(className)), typeManager.typeToLowerCamelName(TYPE_NAME_UTIL.bestGuess(className).simpleName()))
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
                .addParameters(
                        classNameSet.stream()
                                .map(className ->
                                        ParameterSpec.builder(
                                                ParameterizedTypeName.get(ClassName.get(Provider.class), TYPE_NAME_UTIL.bestGuess(className)),
                                                typeManager.typeToLowerCamelName(TYPE_NAME_UTIL.bestGuess(className).simpleName())
                                        ).build()
                                )
                                .collect(Collectors.toList())
                )
                .addStatement("this.jsonb = jsonb");

        classNameSet.forEach(className ->
                builder.addStatement("this.$L = $L",
                        typeManager.typeToLowerCamelName(TYPE_NAME_UTIL.bestGuess(className).simpleName()),
                        typeManager.typeToLowerCamelName(TYPE_NAME_UTIL.bestGuess(className).simpleName())
                )
        );

        return builder.build();
    }

    private List<MethodSpec> buildTypeInvokeMethods() {
        return manager.getObjects()
                .filter(objectTypeDefinitionContext -> !manager.isQueryOperationType(objectTypeDefinitionContext.name().getText()))
                .filter(objectTypeDefinitionContext -> !manager.isMutationOperationType(objectTypeDefinitionContext.name().getText()))
                .filter(objectTypeDefinitionContext -> !manager.isSubscriptionOperationType(objectTypeDefinitionContext.name().getText()))
                .map(this::buildTypeInvokeMethod)
                .collect(Collectors.toList());
    }

    private MethodSpec buildTypeInvokeMethod(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        ClassName typeClassName = manager.getClassName(objectTypeDefinitionContext)
                .map(TYPE_NAME_UTIL::bestGuess)
                .orElseGet(() -> ClassName.get(graphQLConfig.getObjectTypePackageName(), objectTypeDefinitionContext.name().getText()));
        String typeParameterName = getParameterName(objectTypeDefinitionContext);

        return MethodSpec.methodBuilder(typeParameterName)
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(ClassName.get(Mono.class), typeClassName))
                .addParameter(typeClassName, typeParameterName)
                .addParameter(ClassName.get(GraphqlParser.SelectionSetContext.class), "selectionSetContext")
                .addStatement(
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
                                                .add(CodeBlock.builder()
                                                        .beginControlFlow("switch (fieldName)")
                                                        .indent()
                                                        .add(CodeBlock.join(
                                                                        Streams.concat(
                                                                                Stream.ofNullable(invokeMethods.get(objectTypeDefinitionContext.name().getText()))
                                                                                        .flatMap(map -> map.entrySet().stream())
                                                                                        .flatMap(entry ->
                                                                                                entry.getValue().stream()
                                                                                                        .map(methodEntry -> {
                                                                                                                    String apiVariableName = typeManager.typeToLowerCamelName(TYPE_NAME_UTIL.bestGuess(entry.getKey()).simpleName());
                                                                                                                    String invokeFieldName = typeManager.getInvokeFieldName(methodEntry.getKey());
                                                                                                                    String fieldSetterMethodName = typeManager.getFieldSetterMethodName(invokeFieldName);
                                                                                                                    CodeBlock caseCodeBlock = CodeBlock.of("case $S:\n", invokeFieldName);
                                                                                                                    CodeBlock invokeCodeBlock;
                                                                                                                    if (TYPE_UTIL.getClassName(methodEntry.getValue()).canonicalName().equals(PublisherBuilder.class.getCanonicalName())) {
                                                                                                                        invokeCodeBlock = CodeBlock.of("return $T.from($L.get().$L($L).buildRs()).doOnNext(result -> $L.$L(result));",
                                                                                                                                ClassName.get(Mono.class),
                                                                                                                                apiVariableName,
                                                                                                                                methodEntry.getKey(),
                                                                                                                                typeParameterName,
                                                                                                                                typeParameterName,
                                                                                                                                fieldSetterMethodName
                                                                                                                        );
                                                                                                                    } else if (TYPE_UTIL.getClassName(methodEntry.getValue()).canonicalName().equals(Mono.class.getCanonicalName())) {
                                                                                                                        invokeCodeBlock = CodeBlock.of("return $L.get().$L($L).doOnNext(result -> $L.$L(result));",
                                                                                                                                apiVariableName,
                                                                                                                                methodEntry.getKey(),
                                                                                                                                typeParameterName,
                                                                                                                                typeParameterName,
                                                                                                                                fieldSetterMethodName
                                                                                                                        );
                                                                                                                    } else if (TYPE_UTIL.getClassName(methodEntry.getValue()).canonicalName().equals(Flux.class.getCanonicalName())) {
                                                                                                                        invokeCodeBlock = CodeBlock.of("return $L.get().$L($L).collectList().doOnNext(result -> $L.$L(result));",
                                                                                                                                apiVariableName,
                                                                                                                                methodEntry.getKey(),
                                                                                                                                typeParameterName,
                                                                                                                                typeParameterName,
                                                                                                                                fieldSetterMethodName
                                                                                                                        );
                                                                                                                    } else {
                                                                                                                        invokeCodeBlock = CodeBlock.of("return $T.justOrEmpty($L.get().$L($L)).doOnNext(result -> $L.$L(result));",
                                                                                                                                ClassName.get(Mono.class),
                                                                                                                                apiVariableName,
                                                                                                                                methodEntry.getKey(),
                                                                                                                                typeParameterName,
                                                                                                                                typeParameterName,
                                                                                                                                fieldSetterMethodName
                                                                                                                        );
                                                                                                                    }
                                                                                                                    return CodeBlock.builder().add(caseCodeBlock).indent().add(invokeCodeBlock).unindent().build();
                                                                                                                }
                                                                                                        )
                                                                                        ),
                                                                                manager.getFields(objectTypeDefinitionContext.name().getText())
                                                                                        .filter(manager::isNotInvokeField)
                                                                                        .filter(manager::isNotFetchField)
                                                                                        .filter(manager::isNotFunctionField)
                                                                                        .filter(fieldDefinitionContext -> manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type())))
                                                                                        .filter(fieldDefinitionContext -> !manager.fieldTypeIsList(fieldDefinitionContext.type()))
                                                                                        .map(fieldDefinitionContext -> {
                                                                                                    CodeBlock caseCodeBlock = CodeBlock.of("case $S:\n", fieldDefinitionContext.name().getText());
                                                                                                    CodeBlock invokeCodeBlock = CodeBlock.of("return $L($L.$L(), selectionContext.field().selectionSet()).doOnNext($L -> $L.$L($L));",
                                                                                                            getObjectMethodName(manager.getFieldTypeName(fieldDefinitionContext.type())),
                                                                                                            typeParameterName,
                                                                                                            typeManager.getFieldGetterMethodName(fieldDefinitionContext),
                                                                                                            fieldDefinitionContext.name().getText(),
                                                                                                            typeParameterName,
                                                                                                            typeManager.getFieldSetterMethodName(fieldDefinitionContext),
                                                                                                            fieldDefinitionContext.name().getText()
                                                                                                    );
                                                                                                    return CodeBlock.builder().add(caseCodeBlock).indent().add(invokeCodeBlock).unindent().build();
                                                                                                }
                                                                                        ),
                                                                                manager.getFields(objectTypeDefinitionContext.name().getText())
                                                                                        .filter(manager::isNotInvokeField)
                                                                                        .filter(manager::isNotFetchField)
                                                                                        .filter(manager::isNotFunctionField)
                                                                                        .filter(fieldDefinitionContext -> manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type())))
                                                                                        .filter(fieldDefinitionContext -> manager.fieldTypeIsList(fieldDefinitionContext.type()))
                                                                                        .map(fieldDefinitionContext -> {
                                                                                                    CodeBlock caseCodeBlock = CodeBlock.of("case $S:\n", fieldDefinitionContext.name().getText());
                                                                                                    CodeBlock invokeCodeBlock = CodeBlock.of("return $T.from($T.justOrEmpty($L.$L())).flatMap($T::fromIterable).flatMap(item-> $L(item, selectionContext.field().selectionSet())).collectList().doOnNext($L -> $L.$L($L));",
                                                                                                            ClassName.get(Flux.class),
                                                                                                            ClassName.get(Mono.class),
                                                                                                            typeParameterName,
                                                                                                            typeManager.getFieldGetterMethodName(fieldDefinitionContext),
                                                                                                            ClassName.get(Flux.class),
                                                                                                            getObjectMethodName(manager.getFieldTypeName(fieldDefinitionContext.type())),
                                                                                                            fieldDefinitionContext.name().getText(),
                                                                                                            typeParameterName,
                                                                                                            typeManager.getFieldSetterMethodName(fieldDefinitionContext),
                                                                                                            fieldDefinitionContext.name().getText()
                                                                                                    );
                                                                                                    return CodeBlock.builder().add(caseCodeBlock).indent().add(invokeCodeBlock).unindent().build();
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
                                        CodeBlock.of(".then()"),
                                        CodeBlock.of(".thenReturn($L)", typeParameterName)
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
