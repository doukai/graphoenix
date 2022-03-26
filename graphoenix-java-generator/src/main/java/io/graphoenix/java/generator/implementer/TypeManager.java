package io.graphoenix.java.generator.implementer;

import com.google.common.base.CaseFormat;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.error.GraphQLProblem;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Collection;
import java.util.Optional;

import static io.graphoenix.core.error.GraphQLErrorType.CLASS_NAME_ARGUMENT_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.METHOD_NAME_ARGUMENT_NOT_EXIST;
import static io.graphoenix.spi.constant.Hammurabi.INTROSPECTION_PREFIX;

@ApplicationScoped
public class TypeManager {

    private final IGraphQLDocumentManager manager;

    private GraphQLConfig graphQLConfig;

    @Inject
    public TypeManager(IGraphQLDocumentManager manager) {
        this.manager = manager;
    }

    public TypeManager setGraphQLConfig(GraphQLConfig graphQLConfig) {
        this.graphQLConfig = graphQLConfig;
        return this;
    }

    public String getInvokeFieldName(String methodName) {
        if (methodName.startsWith("get")) {
            return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, methodName.replaceFirst("get", ""));
        } else if (methodName.startsWith("set")) {
            return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, methodName.replaceFirst("set", ""));
        } else {
            return methodName;
        }
    }


    public String getFieldGetterMethodName(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return getFieldGetterMethodName(fieldDefinitionContext.name().getText());
    }

    public String getFieldGetterMethodName(String fieldName) {
        if (fieldName.startsWith(INTROSPECTION_PREFIX)) {
            return "get".concat(INTROSPECTION_PREFIX).concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, fieldName.replaceFirst(INTROSPECTION_PREFIX, "")));
        } else {
            return "get".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, fieldName));
        }
    }

    public String getFieldSetterMethodName(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return getFieldSetterMethodName(fieldDefinitionContext.name().getText());
    }

    public String getFieldSetterMethodName(String fieldName) {
        if (fieldName.startsWith(INTROSPECTION_PREFIX)) {
            return "set".concat(INTROSPECTION_PREFIX).concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, fieldName.replaceFirst(INTROSPECTION_PREFIX, "")));
        } else {
            return "set".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, fieldName));
        }
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
                                        .orElseThrow(() -> new GraphQLProblem(CLASS_NAME_ARGUMENT_NOT_EXIST)),
                                directiveContext.arguments().argument().stream()
                                        .filter(argumentContext -> argumentContext.name().getText().equals("methodName"))
                                        .map(argumentContext -> argumentContext.valueWithVariable().StringValue())
                                        .map(stringValue -> stringValue.toString().substring(1, stringValue.getText().length() - 1))
                                        .findFirst()
                                        .orElseThrow(() -> new GraphQLProblem(METHOD_NAME_ARGUMENT_NOT_EXIST))
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

    public String typeToLowerCamelName(GraphqlParser.TypeContext typeContext) {
        String fieldTypeName = manager.getFieldTypeName(typeContext);
        return typeToLowerCamelName(fieldTypeName);
    }

    public String typeToLowerCamelName(String fieldTypeName) {
        if (fieldTypeName.startsWith(INTROSPECTION_PREFIX)) {
            return INTROSPECTION_PREFIX.concat(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, fieldTypeName.replaceFirst(INTROSPECTION_PREFIX, "")));
        } else {
            return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, fieldTypeName);
        }
    }
}
