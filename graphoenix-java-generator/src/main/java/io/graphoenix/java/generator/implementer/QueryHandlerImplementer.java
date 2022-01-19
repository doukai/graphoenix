package io.graphoenix.java.generator.implementer;

import com.google.common.base.CaseFormat;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
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
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.handler.BaseQueryHandler;
import io.graphoenix.spi.handler.InvokeHandler;
import io.graphoenix.spi.handler.OperationHandler;
import io.vavr.Tuple2;
import reactor.core.publisher.Mono;

import javax.annotation.processing.Filer;
import javax.inject.Inject;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class QueryHandlerImplementer {

    private final IGraphQLDocumentManager manager;
    private final TypeManager typeManager;
    private GraphQLConfig graphQLConfig;
    private List<Tuple2<TypeElement, ExecutableElement>> invokeMethods;

    @Inject
    public QueryHandlerImplementer(IGraphQLDocumentManager manager, TypeManager typeManager) {
        this.manager = manager;
        this.typeManager = typeManager;
    }

    public QueryHandlerImplementer setConfiguration(GraphQLConfig graphQLConfig) {
        this.graphQLConfig = graphQLConfig;
        this.typeManager.setManager(graphQLConfig);
        return this;
    }

    public QueryHandlerImplementer setInvokeMethods(List<Tuple2<TypeElement, ExecutableElement>> invokeMethods) {
        this.invokeMethods = invokeMethods;
        return this;
    }

    public void writeToFiler(Filer filer) throws IOException {
        this.buildImplementClass().writeTo(filer);
    }

    public JavaFile buildImplementClass() {
        TypeSpec typeSpec = buildInvokeHandlerImpl();
        return JavaFile.builder(graphQLConfig.getPackageName(), typeSpec).build();
    }

    public TypeSpec buildInvokeHandlerImpl() {
        return TypeSpec.classBuilder("QueryHandlerImpl")
                .superclass(BaseQueryHandler.class)
                .addField(
                        FieldSpec.builder(
                                ClassName.get(GsonBuilder.class),
                                CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, GsonBuilder.class.getSimpleName()),
                                Modifier.PRIVATE,
                                Modifier.FINAL
                        ).initializer("new $T()", ClassName.get(GsonBuilder.class)).build()
                )
                .addField(
                        FieldSpec.builder(
                                ClassName.get(OperationHandler.class), CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, OperationHandler.class.getSimpleName()),
                                Modifier.PRIVATE,
                                Modifier.FINAL).build()
                )
                .addField(
                        FieldSpec.builder(ClassName.get(InvokeHandler.class), CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, InvokeHandler.class.getSimpleName()),
                                Modifier.PRIVATE,
                                Modifier.FINAL).build()
                )
                .addFields(buildFields())
                .addMethod(buildConstructor())
                .addMethods(buildMethods())
                .build();
    }

    public Set<MethodSpec> buildMethods() {
        return manager.getFields(manager.getQueryOperationTypeName().orElseThrow()).map(this::buildMethod).collect(Collectors.toSet());
    }

    public Set<FieldSpec> buildFields() {
        return this.invokeMethods.stream()
                .map(Tuple2::_1)
                .collect(Collectors.toSet())
                .stream()
                .map(typeElement ->
                        FieldSpec.builder(ClassName.get(typeElement), CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, typeElement.getSimpleName().toString()))
                                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                                .build()
                )
                .collect(Collectors.toSet());
    }

    public MethodSpec buildMethod(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {

        MethodSpec.Builder builder = MethodSpec.methodBuilder(fieldDefinitionContext.name().getText())
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ParameterSpec.builder(ClassName.get(String.class), "graphQL").build())
                .addParameter(ParameterSpec.builder(ParameterizedTypeName.get(Map.class, String.class, String.class), "variables").build())
                .returns(ParameterizedTypeName.get(ClassName.get(Mono.class), typeManager.typeContextToTypeName(fieldDefinitionContext.type())));

        Optional<Tuple2<String, String>> invokeDirective = typeManager.getInvokeDirective(fieldDefinitionContext);
        boolean fieldTypeIsList = manager.fieldTypeIsList(fieldDefinitionContext.type());
        String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());

        builder.addStatement("$T type = new $T<$T>() {}.getType()",
                ClassName.get(Type.class),
                ClassName.get(TypeToken.class),
                typeManager.typeContextToTypeName(fieldDefinitionContext.type())
        );

        if (invokeDirective.isPresent()) {

            Tuple2<TypeElement, ExecutableElement> method = invokeMethods.stream()
                    .filter(tuple -> typeManager.getInvokeFieldName(tuple._2().getSimpleName().toString()).equals(fieldDefinitionContext.name().getText()))
                    .findFirst()
                    .orElseThrow();

            if (method._2().getReturnType().toString().equals(Mono.class.getSimpleName())) {
                builder.addStatement("return $L.$L($L)",
                        CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, method._1().getSimpleName().toString()),
                        method._2().getSimpleName().toString(),
                        CodeBlock.join(method._2().getParameters().stream()
                                .map(variableElement ->
                                        CodeBlock.of("$L.create().fromJson(variables.get($S), $T.class)",
                                                CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, GsonBuilder.class.getSimpleName()),
                                                variableElement.getSimpleName().toString(),
                                                ClassName.get(variableElement.asType())
                                        )
                                )
                                .collect(Collectors.toList()), ",")
                );
            } else {
                builder.addStatement("return $T.just($L.$L($L))",
                        ClassName.get(Mono.class),
                        CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, method._1().getSimpleName().toString()),
                        method._2().getSimpleName().toString(),
                        CodeBlock.join(method._2().getParameters().stream()
                                .map(variableElement ->
                                        CodeBlock.of("$L.create().fromJson(variables.get($S), $T.class)",
                                                CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, GsonBuilder.class.getSimpleName()),
                                                variableElement.getSimpleName().toString(),
                                                ClassName.get(variableElement.asType())
                                        )
                                )
                                .collect(Collectors.toList()), ",")
                );
            }
        } else {
            if (manager.isObject(fieldTypeName)) {
                builder.addStatement("$T<$T> result = $L.query(graphQL, variables).map(jsonString-> $L.create().fromJson(jsonString, type))",
                        ClassName.get(Mono.class),
                        typeManager.typeContextToTypeName(fieldDefinitionContext.type()),
                        CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, OperationHandler.class.getSimpleName()),
                        CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, GsonBuilder.class.getSimpleName())
                );

                if (fieldTypeIsList) {
                    builder.addStatement("return result.map(list-> list.stream().map(item -> invokeHandler.getInvokeMethod($T.class).apply(item)).collect($T.toList()))",
                            ClassName.get(graphQLConfig.getObjectTypePackageName(), fieldTypeName),
                            ClassName.get(Collectors.class)
                    );
                } else {
                    builder.addStatement("return result.map(object-> invokeHandler.getInvokeMethod($T.class).apply(object))",
                            ClassName.get(graphQLConfig.getObjectTypePackageName(), fieldTypeName)
                    );
                }
            } else {
                builder.addStatement("return $L.query(graphQL, variables).map(jsonString-> $L.create().fromJson(jsonString, type))",
                        CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, OperationHandler.class.getSimpleName()),
                        CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, GsonBuilder.class.getSimpleName())
                );
            }
        }
        return builder.build();
    }

    public MethodSpec buildConstructor() {
        MethodSpec.Builder builder = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addStatement("this.$L = $T.get($T.class)",
                        CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, OperationHandler.class.getSimpleName()),
                        ClassName.get(BeanContext.class),
                        ClassName.get(OperationHandler.class)
                )
                .addStatement("this.$L = $T.get($T.class)",
                        CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, InvokeHandler.class.getSimpleName()),
                        ClassName.get(BeanContext.class),
                        ClassName.get(InvokeHandler.class)
                );

        this.invokeMethods.stream()
                .map(Tuple2::_1)
                .collect(Collectors.toSet())
                .forEach(typeElement ->
                        builder.addStatement("this.$L = $T.get($T.class)",
                                CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, typeElement.getSimpleName().toString()),
                                ClassName.get(BeanContext.class),
                                ClassName.get(typeElement)
                        )
                );

        return builder.build();
    }
}
