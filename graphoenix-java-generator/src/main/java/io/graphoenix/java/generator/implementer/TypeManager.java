package io.graphoenix.java.generator.implementer;

import com.google.common.base.CaseFormat;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.core.handler.PackageManager;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.graphoenix.core.error.GraphQLErrorType.ARGUMENT_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.CLASS_NAME_ARGUMENT_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.METHOD_NAME_ARGUMENT_NOT_EXIST;
import static io.graphoenix.core.utils.DocumentUtil.DOCUMENT_UTIL;
import static io.graphoenix.core.utils.TypeNameUtil.TYPE_NAME_UTIL;
import static io.graphoenix.spi.constant.Hammurabi.*;

@ApplicationScoped
public class TypeManager {

    private final IGraphQLDocumentManager manager;
    private final PackageManager packageManager;
    private final GraphQLConfig graphQLConfig;

    @Inject
    public TypeManager(IGraphQLDocumentManager manager, PackageManager packageManager, GraphQLConfig graphQLConfig) {
        this.manager = manager;
        this.packageManager = packageManager;
        this.graphQLConfig = graphQLConfig;
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
            return "get".concat(fieldName);
        } else {
            return "get".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, fieldName));
        }
    }

    public String getFieldSetterMethodName(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return getFieldSetterMethodName(fieldDefinitionContext.name().getText());
    }

    public String getFieldSetterMethodName(String fieldName) {
        if (fieldName.startsWith(INTROSPECTION_PREFIX)) {
            return "set".concat(fieldName);
        } else {
            return "set".concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, fieldName));
        }
    }

    public Optional<Tuple2<String, String>> getInvokeDirective(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return Stream.ofNullable(fieldDefinitionContext.directives())
                .flatMap(directivesContext -> directivesContext.directive().stream())
                .filter(directiveContext -> directiveContext.name().getText().equals(INVOKE_DIRECTIVE_NAME))
                .map(directiveContext ->
                        Tuple.of(directiveContext.arguments().argument().stream()
                                        .filter(argumentContext -> argumentContext.name().getText().equals("className"))
                                        .map(argumentContext -> argumentContext.valueWithVariable().StringValue())
                                        .map(stringValue -> stringValue.toString().substring(1, stringValue.getText().length() - 1))
                                        .findFirst()
                                        .orElseThrow(() -> new GraphQLErrors(CLASS_NAME_ARGUMENT_NOT_EXIST)),
                                directiveContext.arguments().argument().stream()
                                        .filter(argumentContext -> argumentContext.name().getText().equals("methodName"))
                                        .map(argumentContext -> argumentContext.valueWithVariable().StringValue())
                                        .map(stringValue -> stringValue.toString().substring(1, stringValue.getText().length() - 1))
                                        .findFirst()
                                        .orElseThrow(() -> new GraphQLErrors(METHOD_NAME_ARGUMENT_NOT_EXIST))
                        )
                ).findFirst();
    }

    public boolean hasInputInvokes(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {
        return Stream.ofNullable(inputObjectTypeDefinitionContext.directives())
                .flatMap(directivesContext -> directivesContext.directive().stream())
                .anyMatch(directiveContext -> directiveContext.name().getText().equals(INVOKES_DIRECTIVE_NAME));
    }

    public Stream<Tuple3<String, String, String>> getInputInvokes(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {
        return Stream.ofNullable(inputObjectTypeDefinitionContext.directives())
                .flatMap(directivesContext -> directivesContext.directive().stream())
                .filter(directiveContext -> directiveContext.name().getText().equals(INVOKES_DIRECTIVE_NAME))
                .flatMap(directiveContext ->
                        directiveContext.arguments().argument().stream()
                                .filter(argumentContext -> argumentContext.name().getText().equals("list"))
                                .filter(argumentContext -> argumentContext.valueWithVariable().arrayValueWithVariable() != null)
                                .map(argumentContext -> argumentContext.valueWithVariable().arrayValueWithVariable())
                                .filter(arrayValueWithVariableContext -> arrayValueWithVariableContext.valueWithVariable() != null)
                                .flatMap(arrayValueWithVariableContext -> arrayValueWithVariableContext.valueWithVariable().stream())
                                .map(GraphqlParser.ValueWithVariableContext::objectValueWithVariable)
                                .filter(Objects::nonNull)
                                .filter(objectValueWithVariableContext -> objectValueWithVariableContext.objectFieldWithVariable() != null)
                                .map(objectValueWithVariableContext ->
                                        Tuple.of(
                                                objectValueWithVariableContext.objectFieldWithVariable().stream()
                                                        .filter(objectFieldWithVariableContext -> objectFieldWithVariableContext.name().getText().equals("className"))
                                                        .map(objectFieldWithVariableContext -> DOCUMENT_UTIL.getStringValue(objectFieldWithVariableContext.valueWithVariable().StringValue()))
                                                        .findFirst()
                                                        .orElseThrow(),
                                                objectValueWithVariableContext.objectFieldWithVariable().stream()
                                                        .filter(objectFieldWithVariableContext -> objectFieldWithVariableContext.name().getText().equals("methodName"))
                                                        .map(objectFieldWithVariableContext -> DOCUMENT_UTIL.getStringValue(objectFieldWithVariableContext.valueWithVariable().StringValue()))
                                                        .findFirst()
                                                        .orElseThrow(),
                                                objectValueWithVariableContext.objectFieldWithVariable().stream()
                                                        .filter(objectFieldWithVariableContext -> objectFieldWithVariableContext.name().getText().equals("returnClassName"))
                                                        .map(objectFieldWithVariableContext -> DOCUMENT_UTIL.getStringValue(objectFieldWithVariableContext.valueWithVariable().StringValue()))
                                                        .findFirst()
                                                        .orElseThrow()
                                        )
                                )
                );
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
                case "BigInteger":
                    return TypeName.get(BigInteger.class);
                case "BigDecimal":
                    return TypeName.get(BigDecimal.class);
                case "Date":
                    return TypeName.get(LocalDate.class);
                case "Time":
                    return TypeName.get(LocalTime.class);
                case "DateTime":
                case "Timestamp":
                    return TypeName.get(LocalDateTime.class);
                default:
                    return TYPE_NAME_UTIL.toClassName(packageManager.getClassName(typeContext));
            }
        }
    }

    public String typeToLowerCamelName(GraphqlParser.TypeContext typeContext) {
        String fieldTypeName = manager.getFieldTypeName(typeContext);
        return typeToLowerCamelName(fieldTypeName);
    }

    public String typeToLowerCamelName(String fieldTypeName) {
        if (fieldTypeName.startsWith(INTROSPECTION_PREFIX)) {
            return INTROSPECTION_PREFIX.concat(typeToLowerCamelName(fieldTypeName.replaceFirst(INTROSPECTION_PREFIX, "")));
        } else {
            return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, fieldTypeName);
        }
    }

    public Optional<GraphqlParser.DirectiveContext> getFormat(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        if (fieldDefinitionContext.directives() == null) {
            return Optional.empty();
        }
        return fieldDefinitionContext.directives().directive().stream()
                .filter(directiveContext -> directiveContext.name().getText().equals("format"))
                .findFirst();
    }

    public Optional<String> getFormatValue(GraphqlParser.DirectiveContext directiveContext) {
        if (directiveContext.arguments() == null) {
            return Optional.empty();
        }
        return directiveContext.arguments().argument().stream()
                .filter(argumentContext -> argumentContext.name().getText().equals("value"))
                .filter(argumentContext -> argumentContext.valueWithVariable().StringValue() != null)
                .findFirst()
                .map(argumentContext -> DOCUMENT_UTIL.getStringValue(argumentContext.valueWithVariable().StringValue()));
    }

    public Optional<String> getFormatLocale(GraphqlParser.DirectiveContext directiveContext) {
        if (directiveContext.arguments() == null) {
            return Optional.empty();
        }
        return directiveContext.arguments().argument().stream()
                .filter(argumentContext -> argumentContext.name().getText().equals("locale"))
                .filter(argumentContext -> argumentContext.valueWithVariable().StringValue() != null)
                .findFirst()
                .map(argumentContext -> DOCUMENT_UTIL.getStringValue(argumentContext.valueWithVariable().StringValue()));
    }

    public String getClassName(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return Optional.ofNullable(fieldDefinitionContext.directives())
                .map(this::getClassName)
                .orElseThrow(() -> new GraphQLErrors(ARGUMENT_NOT_EXIST.bind("className")));
    }

    public String getClassName(GraphqlParser.OperationDefinitionContext operationDefinitionContext) {
        return Optional.ofNullable(operationDefinitionContext.directives())
                .map(this::getClassName)
                .orElseThrow(() -> new GraphQLErrors(ARGUMENT_NOT_EXIST.bind("className")));
    }

    public String getClassName(GraphqlParser.DirectivesContext directivesContext) {
        return directivesContext.directive().stream()
                .filter(directiveContext -> directiveContext.name().getText().equals(INVOKE_DIRECTIVE_NAME))
                .flatMap(directiveContext ->
                        directiveContext.arguments().argument().stream()
                                .filter(argumentContext -> argumentContext.name().getText().equals("className"))
                                .filter(argumentContext -> argumentContext.valueWithVariable().StringValue() != null)
                                .map(argumentContext -> DOCUMENT_UTIL.getStringValue(argumentContext.valueWithVariable().StringValue()))
                )
                .findFirst()
                .orElseThrow(() -> new GraphQLErrors(ARGUMENT_NOT_EXIST.bind("className")));
    }

    public String getMethodName(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return Optional.ofNullable(fieldDefinitionContext.directives())
                .map(this::getMethodName)
                .orElseThrow(() -> new GraphQLErrors(ARGUMENT_NOT_EXIST.bind("methodName")));
    }

    public String getMethodName(GraphqlParser.OperationDefinitionContext operationDefinitionContext) {
        return Optional.ofNullable(operationDefinitionContext.directives())
                .map(this::getMethodName)
                .orElseThrow(() -> new GraphQLErrors(ARGUMENT_NOT_EXIST.bind("methodName")));
    }

    public String getMethodName(GraphqlParser.DirectivesContext directivesContext) {
        return directivesContext.directive().stream()
                .filter(directiveContext -> directiveContext.name().getText().equals(INVOKE_DIRECTIVE_NAME))
                .flatMap(directiveContext ->
                        directiveContext.arguments().argument().stream()
                                .filter(argumentContext -> argumentContext.name().getText().equals("methodName"))
                                .filter(argumentContext -> argumentContext.valueWithVariable().StringValue() != null)
                                .map(argumentContext -> DOCUMENT_UTIL.getStringValue(argumentContext.valueWithVariable().StringValue()))
                )
                .findFirst()
                .orElseThrow(() -> new GraphQLErrors(ARGUMENT_NOT_EXIST.bind("methodName")));
    }

    public List<AbstractMap.SimpleEntry<String, String>> getParameters(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return Stream.ofNullable(fieldDefinitionContext.directives())
                .flatMap(this::getParameters)
                .collect(Collectors.toList());
    }

    public List<AbstractMap.SimpleEntry<String, String>> getParameters(GraphqlParser.OperationDefinitionContext operationDefinitionContext) {
        return Stream.ofNullable(operationDefinitionContext.directives())
                .flatMap(this::getParameters)
                .collect(Collectors.toList());
    }

    public Stream<AbstractMap.SimpleEntry<String, String>> getParameters(GraphqlParser.DirectivesContext directivesContext) {
        return directivesContext.directive().stream()
                .filter(directiveContext -> directiveContext.name().getText().equals(INVOKE_DIRECTIVE_NAME))
                .flatMap(directiveContext ->
                        directiveContext.arguments().argument().stream()
                                .filter(argumentContext -> argumentContext.name().getText().equals("parameters"))
                                .filter(argumentContext -> argumentContext.valueWithVariable().arrayValueWithVariable() != null)
                                .flatMap(argumentContext -> argumentContext.valueWithVariable().arrayValueWithVariable().valueWithVariable().stream())
                )
                .filter(valueWithVariableContext -> valueWithVariableContext.objectValueWithVariable() != null)
                .map(valueWithVariableContext ->
                        new AbstractMap.SimpleEntry<>(
                                valueWithVariableContext.objectValueWithVariable().objectFieldWithVariable().stream()
                                        .filter(objectFieldWithVariableContext -> objectFieldWithVariableContext.name().getText().equals("name"))
                                        .findFirst()
                                        .map(objectFieldWithVariableContext -> DOCUMENT_UTIL.getStringValue(objectFieldWithVariableContext.valueWithVariable().StringValue()))
                                        .orElseThrow(() -> new GraphQLErrors(ARGUMENT_NOT_EXIST.bind("name"))),

                                valueWithVariableContext.objectValueWithVariable().objectFieldWithVariable().stream()
                                        .filter(objectFieldWithVariableContext -> objectFieldWithVariableContext.name().getText().equals("className"))
                                        .findFirst()
                                        .map(objectFieldWithVariableContext -> DOCUMENT_UTIL.getStringValue(objectFieldWithVariableContext.valueWithVariable().StringValue()))
                                        .orElseThrow(() -> new GraphQLErrors(ARGUMENT_NOT_EXIST.bind("className")))
                        )
                );
    }

    public String getReturnClassName(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return Optional.ofNullable(fieldDefinitionContext.directives())
                .map(this::getReturnClassName)
                .orElseThrow(() -> new GraphQLErrors(ARGUMENT_NOT_EXIST.bind("returnClassName")));
    }

    public String getReturnClassName(GraphqlParser.OperationDefinitionContext operationDefinitionContext) {
        return Optional.ofNullable(operationDefinitionContext.directives())
                .map(this::getReturnClassName)
                .orElseThrow(() -> new GraphQLErrors(ARGUMENT_NOT_EXIST.bind("returnClassName")));
    }

    public String getReturnClassName(GraphqlParser.DirectivesContext directivesContext) {
        return directivesContext.directive().stream()
                .filter(directiveContext -> directiveContext.name().getText().equals(INVOKE_DIRECTIVE_NAME))
                .flatMap(directiveContext ->
                        directiveContext.arguments().argument().stream()
                                .filter(argumentContext -> argumentContext.name().getText().equals("returnClassName"))
                                .filter(argumentContext -> argumentContext.valueWithVariable().StringValue() != null)
                                .map(argumentContext -> DOCUMENT_UTIL.getStringValue(argumentContext.valueWithVariable().StringValue()))
                )
                .findFirst()
                .orElseThrow(() -> new GraphQLErrors(ARGUMENT_NOT_EXIST.bind("returnClassName")));
    }
}
