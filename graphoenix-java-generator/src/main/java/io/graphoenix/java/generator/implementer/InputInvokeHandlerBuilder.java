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
import io.graphoenix.core.handler.PackageManager;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.vavr.Tuple3;
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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.graphoenix.core.utils.TypeNameUtil.TYPE_NAME_UTIL;
import static io.graphoenix.java.generator.utils.TypeUtil.TYPE_UTIL;

@ApplicationScoped
public class InputInvokeHandlerBuilder {

    private final IGraphQLDocumentManager manager;
    private final PackageManager packageManager;
    private final TypeManager typeManager;
    private final GraphQLConfig graphQLConfig;
    private Map<String, List<Tuple3<String, String, String>>> invokeMethods;

    @Inject
    public InputInvokeHandlerBuilder(IGraphQLDocumentManager manager, PackageManager packageManager, TypeManager typeManager, GraphQLConfig graphQLConfig) {
        this.manager = manager;
        this.packageManager = packageManager;
        this.typeManager = typeManager;
        this.graphQLConfig = graphQLConfig;
    }

    public void writeToFiler(Filer filer) throws IOException {
        this.invokeMethods = manager.getInputObjects()
                .filter(packageManager::isLocalPackage)
                .filter(typeManager::hasInputInvokes)
                .map(inputObjectTypeDefinitionContext -> new AbstractMap.SimpleEntry<>(inputObjectTypeDefinitionContext.name().getText(), typeManager.getInputInvokes(inputObjectTypeDefinitionContext).collect(Collectors.toList())))
                .collect(
                        Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue
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
        return TypeSpec.classBuilder("InputInvokeHandler")
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
                .flatMap(Collection::stream)
                .map(Tuple3::_1)
                .collect(Collectors.toSet())
                .stream()
                .map(className ->
                        FieldSpec.builder(ParameterizedTypeName.get(ClassName.get(Provider.class), TYPE_NAME_UTIL.toClassName(className)), typeManager.typeToLowerCamelName(TYPE_NAME_UTIL.toClassName(className).simpleName()))
                                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                                .build()
                )
                .collect(Collectors.toSet());
    }

    private MethodSpec buildConstructor() {
        Set<String> classNameSet = this.invokeMethods.values().stream()
                .flatMap(Collection::stream)
                .map(Tuple3::_1)
                .collect(Collectors.toSet());

        MethodSpec.Builder builder = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Inject.class)
                .addParameter(ParameterizedTypeName.get(ClassName.get(Provider.class), ClassName.get(Jsonb.class)), "jsonb")
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
                .addStatement("this.jsonb = jsonb");

        classNameSet.forEach(className ->
                builder.addStatement("this.$L = $L",
                        typeManager.typeToLowerCamelName(TYPE_NAME_UTIL.toClassName(className).simpleName()),
                        typeManager.typeToLowerCamelName(TYPE_NAME_UTIL.toClassName(className).simpleName())
                )
        );

        return builder.build();
    }

    private List<MethodSpec> buildTypeInvokeMethods() {
        return manager.getInputObjects()
                .map(this::buildInputTypeInvokeMethod)
                .collect(Collectors.toList());
    }

    private MethodSpec buildInputTypeInvokeMethod(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {
        ClassName typeClassName = TYPE_NAME_UTIL.toClassName(packageManager.getClassName(inputObjectTypeDefinitionContext));
        String typeParameterName = getParameterName(inputObjectTypeDefinitionContext);

        MethodSpec.Builder builder = MethodSpec.methodBuilder(typeParameterName)
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(ClassName.get(Mono.class), typeClassName))
                .addParameter(typeClassName, typeParameterName);

        List<Tuple3<String, String, String>> tuple3List = invokeMethods.get(inputObjectTypeDefinitionContext.name().getText());

        if (tuple3List != null && tuple3List.size() > 0) {
            int index = 0;
            for (Tuple3<String, String, String> tuple3 : tuple3List) {
                if (index == 0) {
                    String apiVariableName = typeManager.typeToLowerCamelName(TYPE_NAME_UTIL.toClassName(tuple3._1()).simpleName());
                    String methodName = tuple3._2();
                    ClassName returnClassName = TYPE_UTIL.getClassName(tuple3._3());
                    CodeBlock invokeCodeBlock;
                    if (returnClassName.canonicalName().equals(PublisherBuilder.class.getCanonicalName())) {
                        invokeCodeBlock = CodeBlock.of("$T.from($L.get().$L($L).buildRs())",
                                ClassName.get(Mono.class),
                                apiVariableName,
                                methodName,
                                typeParameterName
                        );
                    } else if (returnClassName.canonicalName().equals(Mono.class.getCanonicalName())) {
                        invokeCodeBlock = CodeBlock.of("return $L.get().$L($L)",
                                apiVariableName,
                                methodName,
                                typeParameterName
                        );
                    } else {
                        invokeCodeBlock = CodeBlock.of("return $T.justOrEmpty($L.get().$L($L))",
                                ClassName.get(Mono.class),
                                apiVariableName,
                                methodName,
                                typeParameterName
                        );
                    }
                }
                index++;
            }
        }

                .addStatement(
                CodeBlock.join(
                        Stream.of(
                                CodeBlock.of(
                                        "return $T.fromStream($T.ofNullable(argumentsDefinitionContext).map($T.SelectionSetContext::selection).flatMap(selectionContexts -> selectionContexts.stream()))",
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
                                                                Stream.ofNullable(invokeMethods.get(inputObjectTypeDefinitionContext.name().getText()))
                                                                        .flatMap(list ->
                                                                                list.stream()
                                                                                        .map(tuple3 -> {
                                                                                                    String apiVariableName = typeManager.typeToLowerCamelName(TYPE_NAME_UTIL.toClassName(tuple3._1()).simpleName());
                                                                                                    ClassName returnClassName = TYPE_UTIL.getClassName(tuple3._3());
                                                                                                    CodeBlock caseCodeBlock = CodeBlock.of("case $S:\n", invokeFieldName);
                                                                                                    CodeBlock invokeCodeBlock;
                                                                                                    if (returnClassName.canonicalName().equals(PublisherBuilder.class.getCanonicalName())) {
                                                                                                        invokeCodeBlock = CodeBlock.of("return $T.from($L.get().$L($L).buildRs()).doOnNext(result -> $L.$L(result));",
                                                                                                                ClassName.get(Mono.class),
                                                                                                                apiVariableName,
                                                                                                                methodEntry.getKey(),
                                                                                                                typeParameterName,
                                                                                                                typeParameterName,
                                                                                                                fieldSetterMethodName
                                                                                                        );
                                                                                                    } else if (returnClassName.canonicalName().equals(Mono.class.getCanonicalName())) {
                                                                                                        invokeCodeBlock = CodeBlock.of("return $L.get().$L($L).doOnNext(result -> $L.$L(result));",
                                                                                                                apiVariableName,
                                                                                                                methodEntry.getKey(),
                                                                                                                typeParameterName,
                                                                                                                typeParameterName,
                                                                                                                fieldSetterMethodName
                                                                                                        );
                                                                                                    } else if (returnClassName.canonicalName().equals(Flux.class.getCanonicalName())) {
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
                                                                                    CodeBlock invokeCodeBlock = CodeBlock.of("return $T.justOrEmpty($L.$L()).flatMap(field -> $L(field, selectionContext.field().selectionSet())).doOnNext($L -> $L.$L($L));",
                                                                                            ClassName.get(Mono.class),
                                                                                            typeParameterName,
                                                                                            typeManager.getFieldGetterMethodName(fieldDefinitionContext),
                                                                                            getObjectMethodName(manager.getFieldTypeName(fieldDefinitionContext.type())),
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

    private String getParameterName(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {
        return typeManager.typeToLowerCamelName(inputObjectTypeDefinitionContext.name().getText());
    }

    private String getObjectMethodName(String objectName) {
        return typeManager.typeToLowerCamelName(objectName);
    }
}
