package io.graphoenix.java.generator.implementer;

import com.google.common.base.CaseFormat;
import com.google.gson.reflect.TypeToken;
import com.squareup.javapoet.ClassName;
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
import io.vavr.Tuple;
import io.vavr.Tuple2;

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
    private GraphQLConfig graphQLConfig;
    private List<Tuple2<TypeElement, ExecutableElement>> invokeMethods;

    @Inject
    public QueryHandlerImplementer(IGraphQLDocumentManager manager) {
        this.manager = manager;
    }

    public QueryHandlerImplementer setConfiguration(GraphQLConfig graphQLConfig) {
        this.graphQLConfig = graphQLConfig;
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
                .addFields(buildFields())
                .addMethod(buildConstructor())
                .addMethods(buildTypeInvokeMethods())
                .build();
    }

    public Set<FieldSpec> buildFields() {
        return manager.getFields(manager.getQueryOperationTypeName().orElseThrow()).map(this::buildField).collect(Collectors.toSet());
    }

    public MethodSpec buildMethod(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {

        MethodSpec.Builder builder = MethodSpec.methodBuilder(fieldDefinitionContext.name().getText())
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ParameterSpec.builder(ClassName.get(String.class), "graphQL").build())
                .addParameter(ParameterSpec.builder(ParameterizedTypeName.get(Map.class, String.class, Object.class), "variables").build());

        Optional<Tuple2<String, String>> invokeDirective = getInvokeDirective(fieldDefinitionContext);
        boolean fieldTypeIsList = manager.fieldTypeIsList(fieldDefinitionContext.type());
        String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
        if (invokeDirective.isPresent()) {
            builder.addStatement("return $L.$L()",
                    ClassName.get(graphQLConfig.getObjectTypePackageName(), fieldTypeName),
                    invokeMethods.stream().filter(tuple -> tuple._2().getSimpleName().toString().equals(fieldDefinitionContext.name().getText()))
                            .map(tuple -> tuple._1().getSimpleName().toString())
                            .findFirst()
                            .orElseThrow(),
                    invokeMethods.stream()
                            .filter(tuple -> tuple._2().getSimpleName().toString().equals(fieldDefinitionContext.name().getText()))
                            .map(tuple -> tuple._2().getSimpleName().toString())
                            .findFirst()
                            .orElseThrow()
            );
        } else {
            if (fieldTypeIsList) {
                builder.returns(ParameterizedTypeName.get(ClassName.get(List.class), ClassName.get(graphQLConfig.getObjectTypePackageName(), fieldTypeName)));
                builder.addStatement("$T type = new $T<$T<$T>>() {}.getType()",
                        ClassName.get(Type.class),
                        ClassName.get(TypeToken.class),
                        ClassName.get(List.class),
                        ClassName.get(graphQLConfig.getObjectTypePackageName(), fieldTypeName)
                );
                builder.addStatement("$T<$T> result = jsonBuilder.create().fromJson($L.query(graphQL, variables), type)",
                        ClassName.get(List.class),
                        ClassName.get(graphQLConfig.getObjectTypePackageName(), fieldTypeName),
                        CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, OperationHandler.class.getSimpleName()),
                        ClassName.get(graphQLConfig.getObjectTypePackageName(), fieldTypeName)
                );
                builder.addStatement("return result.stream().map(item-> invokeHandler.getInvokeMethod($T.class).apply(item)).collect($T.toList())",
                        ClassName.get(graphQLConfig.getObjectTypePackageName(), fieldTypeName),
                        ClassName.get(Collectors.class)
                );
            } else {
                builder.returns(ClassName.get(graphQLConfig.getObjectTypePackageName(), fieldTypeName));
                builder.addStatement("$T result = jsonBuilder.create().fromJson($L.query(graphQL, variables), $T.class)",
                        ClassName.get(graphQLConfig.getObjectTypePackageName(), fieldTypeName),
                        CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, OperationHandler.class.getSimpleName()),
                        ClassName.get(graphQLConfig.getObjectTypePackageName(), fieldTypeName)
                );
                builder.addStatement("return invokeHandler.getInvokeMethod($T.class).apply(result)",
                        ClassName.get(graphQLConfig.getObjectTypePackageName(), fieldTypeName)
                );
            }
        }
        return builder.build();
    }

    public MethodSpec buildConstructor() {
        return MethodSpec.constructorBuilder()
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
                )
                .build();
    }

    public List<MethodSpec> buildTypeInvokeMethods() {
        return manager.getObjects()
                .filter(objectTypeDefinitionContext ->
                        !manager.isQueryOperationType(objectTypeDefinitionContext.name().getText()) &&
                                !manager.isMutationOperationType(objectTypeDefinitionContext.name().getText()) &&
                                !manager.isSubscriptionOperationType(objectTypeDefinitionContext.name().getText())
                ).map(this::buildTypeInvokeMethod).collect(Collectors.toList());
    }

    public MethodSpec buildTypeInvokeMethod(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, objectTypeDefinitionContext.name().getText()))
                .addModifiers(Modifier.PUBLIC)
                .returns(ClassName.get(graphQLConfig.getObjectTypePackageName(), objectTypeDefinitionContext.name().getText()))
                .addParameter(ClassName.get(graphQLConfig.getObjectTypePackageName(), objectTypeDefinitionContext.name().getText()), getParameterName(objectTypeDefinitionContext));

        if (invokeMethods.get(objectTypeDefinitionContext.name().getText()) != null) {
            builder.beginControlFlow("if ($L != null)", getParameterName(objectTypeDefinitionContext));
            invokeMethods.get(objectTypeDefinitionContext.name().getText())
                    .forEach((key, value) ->
                            value.forEach(executableElement ->
                                    builder.addStatement("$L.$L($L.$L($L))",
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
                            builder.addStatement("$L($L.$L())",
                                    getObjectMethodName(manager.getFieldTypeName(fieldDefinitionContext.type())),
                                    getParameterName(objectTypeDefinitionContext),
                                    getFieldGetterMethodName(fieldDefinitionContext)
                            )
                    );

            manager.getFields(objectTypeDefinitionContext.name().getText())
                    .filter(fieldDefinitionContext -> manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type())))
                    .filter(fieldDefinitionContext -> manager.fieldTypeIsList(fieldDefinitionContext.type()))
                    .forEach(fieldDefinitionContext ->
                            builder.beginControlFlow("if ($L.$L() != null)",
                                    getParameterName(objectTypeDefinitionContext),
                                    getFieldGetterMethodName(fieldDefinitionContext)
                            ).addStatement("$L.$L().forEach(this::$L)",
                                    getParameterName(objectTypeDefinitionContext),
                                    getFieldGetterMethodName(fieldDefinitionContext),
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

    private Optional<Tuple2<String, String>> getInvokeDirective(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        if (fieldDefinitionContext.directives() == null) {
            return Optional.empty();
        }

        return fieldDefinitionContext.directives().directive().stream()
                .filter(directiveContext -> directiveContext.name().getText().equals("invoke"))
                .map(directiveContext ->
                        Tuple.of(directiveContext.arguments().argument().stream()
                                        .filter(argumentContext -> argumentContext.name().getText().equals("className"))
                                        .map(argumentContext -> argumentContext.valueWithVariable().StringValue())
                                        .map(stringValue -> stringValue.toString().substring(1, stringValue.getText().length() - 1))
                                        .findFirst()
                                        .orElseThrow(),
                                directiveContext.arguments().argument().stream()
                                        .filter(argumentContext -> argumentContext.name().getText().equals("methodName"))
                                        .map(argumentContext -> argumentContext.valueWithVariable().StringValue())
                                        .map(stringValue -> stringValue.toString().substring(1, stringValue.getText().length() - 1))
                                        .findFirst()
                                        .orElseThrow()
                        )
                ).findFirst();
    }
}
