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
        Logger.info("InputInvokeHandler build success");
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
                .flatMap(inputObjectTypeDefinitionContext ->
                        Stream.of(
                                buildInputTypeInvokeMethod(inputObjectTypeDefinitionContext, true),
                                buildInputTypeInvokeMethod(inputObjectTypeDefinitionContext, false)
                        )
                )
                .collect(Collectors.toList());
    }

    private MethodSpec buildInputTypeInvokeMethod(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext, boolean arguments) {
        ClassName typeClassName = TYPE_NAME_UTIL.toClassName(packageManager.getClassName(inputObjectTypeDefinitionContext));
        String typeParameterName = getParameterName(inputObjectTypeDefinitionContext);
        String resultParameterName = "result";

        MethodSpec.Builder builder = MethodSpec.methodBuilder(typeParameterName)
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(ClassName.get(Mono.class), typeClassName))
                .addParameter(typeClassName, typeParameterName);

        if (arguments) {
            builder.addParameter(ClassName.get(GraphqlParser.ArgumentsContext.class), "argumentsContext");
        } else {
            builder.addParameter(ClassName.get(GraphqlParser.ObjectValueWithVariableContext.class), "objectValueWithVariableContext");
        }

        List<Tuple3<String, String, String>> tuple3List = invokeMethods.get(inputObjectTypeDefinitionContext.name().getText());
        if (tuple3List != null && tuple3List.size() > 0) {
            int index = 0;
            for (Tuple3<String, String, String> tuple3 : tuple3List) {
                String apiVariableName = typeManager.typeToLowerCamelName(TYPE_NAME_UTIL.toClassName(tuple3._1()).simpleName());
                String methodName = tuple3._2();
                ClassName returnClassName = TYPE_UTIL.getClassName(tuple3._3());
                CodeBlock invokeCodeBlock;
                if (returnClassName.canonicalName().equals(PublisherBuilder.class.getCanonicalName())) {
                    invokeCodeBlock = CodeBlock.of("$T.from($L.get().$L($L).buildRs())",
                            ClassName.get(Mono.class),
                            apiVariableName,
                            methodName,
                            index == 0 ? typeParameterName : resultParameterName
                    );
                } else if (returnClassName.canonicalName().equals(Mono.class.getCanonicalName())) {
                    invokeCodeBlock = CodeBlock.of("$L.get().$L($L)",
                            apiVariableName,
                            methodName,
                            index == 0 ? typeParameterName : resultParameterName
                    );
                } else if (returnClassName.canonicalName().equals(Flux.class.getCanonicalName())) {
                    invokeCodeBlock = CodeBlock.of("$L.get().$L($L).last()",
                            apiVariableName,
                            methodName,
                            index == 0 ? typeParameterName : resultParameterName
                    );
                } else {
                    invokeCodeBlock = CodeBlock.of("$T.justOrEmpty($L.get().$L($L))",
                            ClassName.get(Mono.class),
                            apiVariableName,
                            methodName,
                            index == 0 ? typeParameterName : resultParameterName
                    );
                }
                if (index == 0) {
                    builder.addCode("return $L.map(result -> (($T)result))\n", invokeCodeBlock, typeClassName);
                } else {
                    builder.addCode(".flatMap(result -> $L).map(result -> (($T)result))\n", invokeCodeBlock, typeClassName);
                }
                index++;
            }
        } else {
            builder.addCode("return $T.just($L)", ClassName.get(Mono.class), typeParameterName);
        }

        builder.addCode(
                CodeBlock.join(
                        Stream.of(
                                CodeBlock.of(
                                        arguments ?
                                                ".flatMap(result -> $T.fromStream($T.ofNullable(argumentsContext).map($T.ArgumentsContext::argument).flatMap(argumentContexts -> argumentContexts.stream()))" :
                                                ".flatMap(result -> $T.fromStream($T.ofNullable(objectValueWithVariableContext).map($T.ObjectValueWithVariableContext::objectFieldWithVariable).flatMap(objectFieldWithVariableContexts -> objectFieldWithVariableContexts.stream()))"
                                        ,
                                        ClassName.get(Flux.class),
                                        ClassName.get(Stream.class),
                                        ClassName.get(GraphqlParser.class)
                                ),
                                CodeBlock.builder()
                                        .indent()
                                        .add(".flatMap(argumentContext -> {\n")
                                        .indent()
                                        .add("String fieldName = argumentContext.name().getText();\n")
                                        .add(CodeBlock.builder()
                                                .beginControlFlow("switch (fieldName)")
                                                .indent()
                                                .add(CodeBlock.join(
                                                        Streams.concat(
                                                                manager.getInputValues(inputObjectTypeDefinitionContext.name().getText())
                                                                        .filter(inputValueDefinitionContext -> manager.isInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type())))
                                                                        .filter(inputValueDefinitionContext -> !manager.fieldTypeIsList(inputValueDefinitionContext.type()))
                                                                        .map(inputValueDefinitionContext -> {
                                                                                    CodeBlock caseCodeBlock = CodeBlock.of("case $S:\n", inputValueDefinitionContext.name().getText());
                                                                                    CodeBlock invokeCodeBlock = CodeBlock.of("return $T.justOrEmpty($L.$L()).flatMap(inputValue -> $L(inputValue, argumentContext.valueWithVariable().objectValueWithVariable())).doOnNext($L -> $L.$L($L));",
                                                                                            ClassName.get(Mono.class),
                                                                                            resultParameterName,
                                                                                            typeManager.getInputValueGetterMethodName(inputValueDefinitionContext),
                                                                                            getObjectMethodName(manager.getFieldTypeName(inputValueDefinitionContext.type())),
                                                                                            typeManager.getFieldName(inputValueDefinitionContext.name().getText()),
                                                                                            resultParameterName,
                                                                                            typeManager.getInputValueSetterMethodName(inputValueDefinitionContext),
                                                                                            typeManager.getFieldName(inputValueDefinitionContext.name().getText())
                                                                                    );
                                                                                    return CodeBlock.builder().add(caseCodeBlock).indent().add(invokeCodeBlock).unindent().build();
                                                                                }
                                                                        ),
                                                                manager.getInputValues(inputObjectTypeDefinitionContext.name().getText())
                                                                        .filter(inputValueDefinitionContext -> manager.isObject(manager.getFieldTypeName(inputValueDefinitionContext.type())))
                                                                        .filter(inputValueDefinitionContext -> manager.fieldTypeIsList(inputValueDefinitionContext.type()))
                                                                        .map(inputValueDefinitionContext -> {
                                                                                    CodeBlock caseCodeBlock = CodeBlock.of("case $S:\n", inputValueDefinitionContext.name().getText());
                                                                                    CodeBlock invokeCodeBlock = CodeBlock.of("return $T.from($T.justOrEmpty($L.$L())).flatMap($T::fromIterable).flatMap(item-> $L(item, selectionContext.field().selectionSet())).collectList().doOnNext($L -> $L.$L($L));",
                                                                                            ClassName.get(Flux.class),
                                                                                            ClassName.get(Mono.class),
                                                                                            resultParameterName,
                                                                                            typeManager.getInputValueGetterMethodName(inputValueDefinitionContext),
                                                                                            ClassName.get(Flux.class),
                                                                                            getObjectMethodName(manager.getFieldTypeName(inputValueDefinitionContext.type())),
                                                                                            typeManager.getFieldName(inputValueDefinitionContext.name().getText()),
                                                                                            resultParameterName,
                                                                                            typeManager.getInputValueSetterMethodName(inputValueDefinitionContext),
                                                                                            typeManager.getFieldName(inputValueDefinitionContext.name().getText())
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
                                        .add("})\n")
                                        .add(".then()\n")
                                        .add(".thenReturn(result)\n")
                                        .unindent()
                                        .add(");")
                                        .build()
                        ).collect(Collectors.toList()),
                        System.lineSeparator()
                )
        );
        return builder.build();
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
