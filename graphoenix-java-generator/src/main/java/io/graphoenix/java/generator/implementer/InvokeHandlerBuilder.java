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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.graphoenix.core.utils.TypeNameUtil.TYPE_NAME_UTIL;
import static io.graphoenix.java.generator.utils.TypeUtil.TYPE_UTIL;

@ApplicationScoped
public class InvokeHandlerBuilder {

    private final IGraphQLDocumentManager manager;
    private final TypeManager typeManager;
    private GraphQLConfig graphQLConfig;
    private Map<String, Map<String, List<Map.Entry<String, String>>>> invokeMethods;

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
                .addParameters(
                        classNameSet.stream()
                                .map(className ->
                                        ParameterSpec.builder(
                                                ParameterizedTypeName.get(ClassName.get(Provider.class), TYPE_NAME_UTIL.bestGuess(className)),
                                                typeManager.typeToLowerCamelName(TYPE_NAME_UTIL.bestGuess(className).simpleName())
                                        ).build()
                                )
                                .collect(Collectors.toList())
                );

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
                .filter(objectTypeDefinitionContext ->
                        !manager.isQueryOperationType(objectTypeDefinitionContext.name().getText()) &&
                                !manager.isMutationOperationType(objectTypeDefinitionContext.name().getText()) &&
                                !manager.isSubscriptionOperationType(objectTypeDefinitionContext.name().getText())
                )
                .map(this::buildTypeInvokeMethod)
                .collect(Collectors.toList());
    }

    private MethodSpec buildTypeInvokeMethod(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        ClassName typeClassName;
        Optional<String> className = typeManager.getClassName(objectTypeDefinitionContext);
        if (className.isPresent()) {
            typeClassName = TYPE_NAME_UTIL.bestGuess(className.get());
        } else {
            typeClassName = ClassName.get(graphQLConfig.getObjectTypePackageName(), objectTypeDefinitionContext.name().getText());
        }
        return MethodSpec.methodBuilder(typeManager.typeToLowerCamelName(objectTypeDefinitionContext.name().getText()))
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(ClassName.get(Mono.class), typeClassName))
                .addParameter(typeClassName, getParameterName(objectTypeDefinitionContext))
                .addStatement(
                        CodeBlock.join(
                                Streams.concat(
                                        Stream.of(CodeBlock.of("return $T.justOrEmpty($L)", ClassName.get(Mono.class), getParameterName(objectTypeDefinitionContext))),
                                        Stream.ofNullable(invokeMethods.get(objectTypeDefinitionContext.name().getText()))
                                                .flatMap(map -> map.entrySet().stream())
                                                .flatMap(entry ->
                                                        entry.getValue().stream()
                                                                .map(methodEntry -> {
                                                                            if (TYPE_UTIL.getClassName(methodEntry.getValue()).canonicalName().equals(PublisherBuilder.class.getCanonicalName())) {
                                                                                return CodeBlock.of(".flatMap(next -> $T.from($L.get().$L(next).buildRs()).map(result -> {next.$L(result); return next;}).switchIfEmpty($T.just(next)))",
                                                                                        ClassName.get(Mono.class),
                                                                                        typeManager.typeToLowerCamelName(TYPE_NAME_UTIL.bestGuess(entry.getKey()).simpleName()),
                                                                                        methodEntry.getKey(),
                                                                                        typeManager.getFieldSetterMethodName(typeManager.getInvokeFieldName(methodEntry.getKey())),
                                                                                        ClassName.get(Mono.class)
                                                                                );
                                                                            } else if (TYPE_UTIL.getClassName(methodEntry.getValue()).canonicalName().equals(Mono.class.getCanonicalName())) {
                                                                                return CodeBlock.of(".flatMap(next -> $L.get().$L(next).map(result -> {next.$L(result); return next;}).switchIfEmpty($T.just(next)))",
                                                                                        typeManager.typeToLowerCamelName(TYPE_NAME_UTIL.bestGuess(entry.getKey()).simpleName()),
                                                                                        methodEntry.getKey(),
                                                                                        typeManager.getFieldSetterMethodName(typeManager.getInvokeFieldName(methodEntry.getKey())),
                                                                                        ClassName.get(Mono.class)
                                                                                );
                                                                            } else if (TYPE_UTIL.getClassName(methodEntry.getValue()).canonicalName().equals(Flux.class.getCanonicalName())) {
                                                                                return CodeBlock.of(".flatMap(next -> $L.get().$L(next).collectList().map(result -> {next.$L(result); return next;}).switchIfEmpty($T.just(next)))",
                                                                                        typeManager.typeToLowerCamelName(TYPE_NAME_UTIL.bestGuess(entry.getKey()).simpleName()),
                                                                                        methodEntry.getKey(),
                                                                                        typeManager.getFieldSetterMethodName(typeManager.getInvokeFieldName(methodEntry.getKey())),
                                                                                        ClassName.get(Mono.class)
                                                                                );
                                                                            } else {
                                                                                return CodeBlock.of(".flatMap(next -> $T.justOrEmpty($L.get().$L(next)).map(result -> {next.$L(result); return next;}).switchIfEmpty($T.just(next)))",
                                                                                        ClassName.get(Mono.class),
                                                                                        typeManager.typeToLowerCamelName(TYPE_NAME_UTIL.bestGuess(entry.getKey()).simpleName()),
                                                                                        methodEntry.getKey(),
                                                                                        typeManager.getFieldSetterMethodName(typeManager.getInvokeFieldName(methodEntry.getKey())),
                                                                                        ClassName.get(Mono.class)
                                                                                );
                                                                            }
                                                                        }
                                                                )
                                                ),
                                        manager.getFields(objectTypeDefinitionContext.name().getText())
                                                .filter(manager::isNotFetchField)
                                                .filter(fieldDefinitionContext -> manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type())))
                                                .filter(fieldDefinitionContext -> !manager.fieldTypeIsList(fieldDefinitionContext.type()))
                                                .map(fieldDefinitionContext ->
                                                        CodeBlock.of(".flatMap(next -> $L(next.$L()).map(result -> {next.$L(result); return next;}).switchIfEmpty($T.just(next)))",
                                                                getObjectMethodName(manager.getFieldTypeName(fieldDefinitionContext.type())),
                                                                typeManager.getFieldGetterMethodName(fieldDefinitionContext),
                                                                typeManager.getFieldSetterMethodName(fieldDefinitionContext),
                                                                ClassName.get(Mono.class)
                                                        )
                                                ),
                                        manager.getFields(objectTypeDefinitionContext.name().getText())
                                                .filter(manager::isNotFetchField)
                                                .filter(fieldDefinitionContext -> manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type())))
                                                .filter(fieldDefinitionContext -> manager.fieldTypeIsList(fieldDefinitionContext.type()))
                                                .map(fieldDefinitionContext ->
                                                        CodeBlock.of(".flatMap(next -> $T.from($T.justOrEmpty(next.$L())).flatMap($T::fromIterable).flatMap(this::$L).collectList().map(result -> {next.$L(result); return next;}).switchIfEmpty($T.just(next)))",
                                                                ClassName.get(Flux.class),
                                                                ClassName.get(Mono.class),
                                                                typeManager.getFieldGetterMethodName(fieldDefinitionContext),
                                                                ClassName.get(Flux.class),
                                                                getObjectMethodName(manager.getFieldTypeName(fieldDefinitionContext.type())),
                                                                typeManager.getFieldSetterMethodName(fieldDefinitionContext),
                                                                ClassName.get(Mono.class)
                                                        )
                                                )
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
