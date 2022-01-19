package io.graphoenix.java.generator.implementer;

import com.google.common.base.CaseFormat;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.vavr.Tuple;
import io.vavr.Tuple2;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Optional;

public class TypeManager {

    private final IGraphQLDocumentManager manager;

    private GraphQLConfig graphQLConfig;

    @Inject
    public TypeManager(IGraphQLDocumentManager manager) {
        this.manager = manager;
    }

    public TypeManager setManager(GraphQLConfig graphQLConfig) {
        this.graphQLConfig = graphQLConfig;
        return this;
    }

    public String getInvokeFieldName(String methodName) {
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

    public String getInvokeFieldGetterMethodName(String methodName) {
        return "get".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, getInvokeFieldName(methodName)));
    }

    public String getInvokeFieldSetterMethodName(String methodName) {
        return "set".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, getInvokeFieldName(methodName)));
    }

    public String getFieldGetterMethodName(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return "get".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, getInvokeFieldName(fieldDefinitionContext.name().getText())));
    }

    public String getFieldSetterMethodName(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return "set".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, getInvokeFieldName(fieldDefinitionContext.name().getText())));
    }

    public Optional<Tuple2<String, String>> getInvokeDirective(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
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

    public TypeName typeContextToTypeName(GraphqlParser.TypeContext typeContext) {
        String fieldTypeName = manager.getFieldTypeName(typeContext);
        boolean typeIsList = manager.fieldTypeIsList(typeContext);
        if (typeIsList) {
            return ParameterizedTypeName.get(ClassName.get(Collection.class), typeContextToTypeName(typeContext.listType().type()));
        } else {
            switch (fieldTypeName) {
                case "ID":
                case "String":
                    return ClassName.get(String.class);
                case "Int":
                    return ClassName.get(Integer.class);
                case "Float":
                    return ClassName.get(Float.class);
                case "Boolean":
                    return ClassName.get(Boolean.class);
                default:
                    return ClassName.get(graphQLConfig.getObjectTypePackageName(), fieldTypeName);
            }
        }
    }
}
