package io.graphoenix.java.generator.implementer;

import com.google.common.base.CaseFormat;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.squareup.javapoet.*;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.handler.BaseQueryHandler;
import io.graphoenix.spi.handler.InvokeHandler;
import io.graphoenix.spi.handler.OperationHandler;
import io.vavr.Tuple;
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
                .addField(FieldSpec.builder(ClassName.get(GsonBuilder.class), CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, GsonBuilder.class.getSimpleName())).initializer("new $T()", ClassName.get(GsonBuilder.class)).build())
                .addField(FieldSpec.builder(ClassName.get(OperationHandler.class), CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, OperationHandler.class.getSimpleName())).build())
                .addField(FieldSpec.builder(ClassName.get(InvokeHandler.class), CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, InvokeHandler.class.getSimpleName())).build())
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
                ).collect(Collectors.toSet());
    }

    public MethodSpec buildMethod(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {

        MethodSpec.Builder builder = MethodSpec.methodBuilder(fieldDefinitionContext.name().getText())
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ParameterSpec.builder(ClassName.get(String.class), "graphQL").build())
                .addParameter(ParameterSpec.builder(ParameterizedTypeName.get(Map.class, String.class, String.class), "variables").build());

        Optional<Tuple2<String, String>> invokeDirective = getInvokeDirective(fieldDefinitionContext);
        boolean fieldTypeIsList = manager.fieldTypeIsList(fieldDefinitionContext.type());
        String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
        if (invokeDirective.isPresent()) {

            Tuple2<TypeElement, ExecutableElement> method = invokeMethods.stream()
                    .filter(tuple -> getInvokeFieldName(tuple._2().getSimpleName().toString()).equals(fieldDefinitionContext.name().getText()))
                    .findFirst()
                    .orElseThrow();

            builder.addStatement("return $L.$L()",
//                    ClassName.get(graphQLConfig.getObjectTypePackageName(), fieldTypeName),
                    method._1().getSimpleName().toString(),
                    method._2().getSimpleName().toString()
            );
        } else {
            if (fieldTypeIsList) {
                builder.returns(ParameterizedTypeName.get(ClassName.get(Mono.class), ParameterizedTypeName.get(ClassName.get(List.class), ClassName.get(graphQLConfig.getObjectTypePackageName(), fieldTypeName))));
                builder.addStatement("$T type = new $T<$T<$T>>() {}.getType()",
                        ClassName.get(Type.class),
                        ClassName.get(TypeToken.class),
                        ClassName.get(List.class),
                        ClassName.get(graphQLConfig.getObjectTypePackageName(), fieldTypeName)
                );
                builder.addStatement("$T<$T<$T>> result = $L.query(graphQL, variables).map(jsonString ->  $L.create().fromJson(jsonString, type))",
                        ClassName.get(Mono.class),
                        ClassName.get(List.class),
                        ClassName.get(graphQLConfig.getObjectTypePackageName(), fieldTypeName),
                        CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, OperationHandler.class.getSimpleName()),
                        CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, GsonBuilder.class.getSimpleName())
                );
                builder.addStatement("return result.map(list-> list.stream().map(item -> invokeHandler.getInvokeMethod($T.class).apply(item)).collect($T.toList()))",
                        ClassName.get(graphQLConfig.getObjectTypePackageName(), fieldTypeName),
                        ClassName.get(Collectors.class)
                );
            } else {
                builder.returns(ParameterizedTypeName.get(ClassName.get(Mono.class), ClassName.get(graphQLConfig.getObjectTypePackageName(), fieldTypeName)));
                builder.addStatement("$T<$T> result = $L.query(graphQL, variables).map(jsonString ->  $L.create().fromJson(jsonString, $T.class))",
                        ClassName.get(Mono.class),
                        ClassName.get(graphQLConfig.getObjectTypePackageName(), fieldTypeName),
                        CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, OperationHandler.class.getSimpleName()),
                        CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, GsonBuilder.class.getSimpleName()),
                        ClassName.get(graphQLConfig.getObjectTypePackageName(), fieldTypeName)
                );
                builder.addStatement("return result.map(object-> invokeHandler.getInvokeMethod($T.class).apply(object))",
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
