package io.graphoenix.graphql.generator.translator;

import com.google.common.collect.Streams;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.error.ElementProcessException;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.graphql.generator.operation.*;
import io.graphoenix.spi.annotation.TypeExpression;
import io.graphoenix.spi.annotation.TypeExpressions;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.tinylog.Logger;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Types;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.graphoenix.core.error.ElementProcessErrorType.*;
import static io.graphoenix.core.error.GraphQLErrorType.*;
import static io.graphoenix.spi.constant.Hammurabi.*;

@ApplicationScoped
public class MethodToQueryOperation {

    private final IGraphQLDocumentManager manager;
    private final ElementManager elementManager;
    private String conditionalName;
    private String operatorName;

    @Inject
    public MethodToQueryOperation(IGraphQLDocumentManager manager, ElementManager elementManager, GraphQLConfig graphQLConfig) {
        this.manager = manager;
        this.elementManager = elementManager;
        this.conditionalName = graphQLConfig.getConditionalInputName();
        this.operatorName = graphQLConfig.getOperatorInputName();
    }

    public void setGraphQLConfig(GraphQLConfig graphQLConfig) {
        this.conditionalName = graphQLConfig.getConditionalInputName();
        this.operatorName = graphQLConfig.getOperatorInputName();
    }

    public String executableElementToQuery(String queryFieldName, ExecutableElement executableElement, String selectionSet, int layers, Types typeUtils) {
        Operation operation = new Operation()
                .setName(executableElement.getSimpleName().toString())
                .setOperationType("query");
        Field field = new Field().setName(queryFieldName);

        Optional<? extends AnnotationMirror> expressions = getExpressionsAnnotation(executableElement);
        expressions.ifPresent(annotationMirror -> field.addArguments(expressionsAnnotationArguments(executableElement, annotationMirror)));

        Optional<? extends AnnotationMirror> expression = getExpressionAnnotation(executableElement);
        expression.ifPresent(annotationMirror -> field.addArgument(expressionAnnotationToArgument(executableElement, annotationMirror)));

        operation.addVariableDefinitions(
                executableElement.getParameters().stream()
                        .map(parameter ->
                                new VariableDefinition()
                                        .setVariable(parameter.getSimpleName().toString())
                                        .setTypeName(elementManager.variableElementToInputTypeName(parameter, typeUtils))
                        )
        );

        if (selectionSet != null && !selectionSet.equals("")) {
            field.setFields(elementManager.buildFields(getQueryTypeName(queryFieldName), selectionSet));
        } else {
            field.setFields(elementManager.buildFields(getQueryTypeName(queryFieldName), 0, layers));
        }
        String query = operation.addField(field).toString();
        Logger.info("build query success:\r\n{}", query);
        return query;
    }

    private Optional<? extends AnnotationMirror> getExpressionAnnotation(ExecutableElement executableElement) {
        return executableElement.getAnnotationMirrors().stream()
                .filter(annotationMirror -> annotationMirror.getAnnotationType().asElement().getAnnotation(TypeExpression.class) != null)
                .findFirst();
    }

    private Optional<? extends AnnotationMirror> getExpressionsAnnotation(ExecutableElement executableElement) {
        return executableElement.getAnnotationMirrors().stream()
                .filter(annotationMirror -> annotationMirror.getAnnotationType().asElement().getAnnotation(TypeExpressions.class) != null)
                .findFirst();
    }

    private Argument expressionAnnotationToArgument(ExecutableElement executableElement, AnnotationMirror expression) {
        return new Argument()
                .setName(getValueArgumentName(expression)
                        .orElseGet(() ->
                                getVariableArgumentName(expression)
                                        .map(argumentName -> argumentName.substring(1))
                                        .orElseThrow(() -> new ElementProcessException(EXPRESSION_VALUE_OR_VARIABLE_FIELD_NOT_EXIST.bind(expression.getAnnotationType().asElement().getSimpleName())))
                        )
                )
                .setValueWithVariable(expressionAnnotationToMap(executableElement, expression));
    }

    private Map<String, Object> expressionAnnotationToMap(ExecutableElement executableElement, AnnotationMirror expression) {
        if (valueIsExpressions(expression)) {
            return getExpressionsValue(executableElement, expression)
                    .orElseThrow(() -> new ElementProcessException(EXPRESSION_EXPRESSIONS_FIELD_NOT_EXIST.bind(expression.getAnnotationType().asElement().getSimpleName())));
        } else {
            return Map.of(
                    getOperatorArgumentName(expression),
                    getOperator(expression)
                            .orElseGet(() ->
                                    getDefaultOperator(expression)
                                            .orElseThrow(() -> new ElementProcessException(EXPRESSION_OPERATOR_NOT_EXIST.bind(expression.getAnnotationType().asElement().getSimpleName())))
                            ),
                    getValueName(expression)
                            .orElseGet(() ->
                                    getVariableName(expression, executableElement)
                                            .orElseThrow(() -> new ElementProcessException(EXPRESSION_VALUE_OR_VARIABLE_FIELD_NOT_EXIST.bind(expression.getAnnotationType().asElement().getSimpleName())))
                            ),
                    getValue(expression)
                            .orElseGet(() ->
                                    getVariable(expression, executableElement)
                                            .orElseThrow(() -> new ElementProcessException(EXPRESSION_VALUE_OR_VARIABLE_NOT_EXIST.bind(expression.getAnnotationType().asElement().getSimpleName())))
                            )
            );
        }
    }

    private Stream<Argument> expressionsAnnotationArguments(ExecutableElement executableElement, AnnotationMirror expressions) {
        Argument conditionalArgument = new Argument()
                .setName(getConditionalArgumentName(expressions))
                .setValueWithVariable(
                        getConditional(expressions)
                                .orElseGet(() ->
                                        getDefaultConditional(expressions)
                                                .orElseThrow(() -> new ElementProcessException(EXPRESSIONS_CONDITIONAL_NOT_EXIST.bind(expressions.getAnnotationType().asElement().getSimpleName())))
                                )
                );

        return Streams.concat(
                Stream.of(conditionalArgument),
                getValueArguments(executableElement, expressions),
                getArguments(expressions),
                getVariables(executableElement, expressions)
        );
    }

    private Map<String, Object> expressionsAnnotationMap(ExecutableElement executableElement, AnnotationMirror expressions) {
        Map<String, Object> expressionsMap = new HashMap<>();
        expressionsMap.put(
                getConditionalArgumentName(expressions),
                getConditional(expressions)
                        .orElseGet(() ->
                                getDefaultConditional(expressions)
                                        .orElseThrow(() -> new ElementProcessException(EXPRESSIONS_CONDITIONAL_NOT_EXIST.bind(expressions.getAnnotationType().asElement().getSimpleName())))
                        )
        );

        getValueExpression(expressions)
                .forEach(expression ->
                        expressionsMap.put(
                                getValueArgumentName(expression)
                                        .orElseGet(() ->
                                                getVariableArgumentName(expression)
                                                        .map(argumentName -> argumentName.substring(1))
                                                        .orElseThrow(() -> new ElementProcessException(EXPRESSION_VALUE_OR_VARIABLE_FIELD_NOT_EXIST.bind(expression.getAnnotationType().asElement().getSimpleName())))
                                        ),
                                expressionAnnotationToMap(executableElement, expression)
                        )
                );
        return expressionsMap;
    }

    private Stream<AnnotationMirror> getValueExpression(AnnotationMirror expressions) {
        return expressions.getElementValues().entrySet().stream()
                .filter(entry -> !entry.getKey().getReturnType().toString().equals(conditionalName))
                .filter(entry -> entry.getKey().getSimpleName().toString().equals("value"))
                .filter(entry -> entry.getValue().getValue() instanceof Collection<?>)
                .map(entry -> (Collection<?>) entry.getValue().getValue())
                .flatMap(collection ->
                        collection.stream()
                                .filter(expression -> expression instanceof AnnotationValue)
                                .map(expression -> (AnnotationMirror) expression)
                );
    }

    private Stream<Argument> getValueArguments(ExecutableElement executableElement, AnnotationMirror expressions) {
        return getValueExpression(expressions).map(expression -> expressionAnnotationToArgument(executableElement, expression));
    }

    private Stream<Argument> getArguments(AnnotationMirror expressions) {
        return Streams
                .concat(
                        expressions.getElementValues().entrySet().stream()
                                .filter(entry -> entry.getKey().getSimpleName().toString().equals(FIRST_INPUT_NAME)),
                        expressions.getElementValues().entrySet().stream()
                                .filter(entry -> entry.getKey().getSimpleName().toString().equals(LAST_INPUT_NAME)),
                        expressions.getElementValues().entrySet().stream()
                                .filter(entry -> entry.getKey().getSimpleName().toString().equals(OFFSET_INPUT_NAME)),
                        expressions.getElementValues().entrySet().stream()
                                .filter(entry -> entry.getKey().getSimpleName().toString().equals(AFTER_INPUT_NAME)),
                        expressions.getElementValues().entrySet().stream()
                                .filter(entry -> entry.getKey().getSimpleName().toString().equals(BEFORE_INPUT_NAME)),
                        expressions.getElementValues().entrySet().stream()
                                .filter(entry -> entry.getKey().getSimpleName().toString().equals(GROUP_BY_INPUT_NAME)),
                        expressions.getElementValues().entrySet().stream()
                                .filter(entry -> entry.getKey().getSimpleName().toString().equals(ORDER_BY_INPUT_NAME))
                )
                .map(entry -> new Argument().setName(entry.getKey().getSimpleName().toString()).setValueWithVariable(entry.getValue()));
    }

    private Stream<Argument> getVariables(ExecutableElement executableElement, AnnotationMirror expressions) {
        return Streams
                .concat(
                        expressions.getElementValues().entrySet().stream()
                                .filter(entry -> entry.getKey().getSimpleName().toString().equals("$".concat(FIRST_INPUT_NAME))),
                        expressions.getElementValues().entrySet().stream()
                                .filter(entry -> entry.getKey().getSimpleName().toString().equals("$".concat(LAST_INPUT_NAME))),
                        expressions.getElementValues().entrySet().stream()
                                .filter(entry -> entry.getKey().getSimpleName().toString().equals("$".concat(OFFSET_INPUT_NAME))),
                        expressions.getElementValues().entrySet().stream()
                                .filter(entry -> entry.getKey().getSimpleName().toString().equals("$".concat(AFTER_INPUT_NAME))),
                        expressions.getElementValues().entrySet().stream()
                                .filter(entry -> entry.getKey().getSimpleName().toString().equals("$".concat(BEFORE_INPUT_NAME)))
                )
                .map(entry ->
                        new Argument()
                                .setName(entry.getKey().getSimpleName().toString().substring(1))
                                .setValueWithVariable(elementManager.getParameterFromExecutableElement(executableElement, entry.getValue().getValue().toString()))
                );
    }

    private String getConditionalArgumentName(AnnotationMirror expressions) {
        return expressions.getAnnotationType().asElement().getEnclosedElements().stream()
                .filter(element -> ((ExecutableElement) element).getReturnType().toString().equals(conditionalName))
                .findFirst()
                .map(element -> element.getSimpleName().toString())
                .orElseThrow(() -> new ElementProcessException(EXPRESSIONS_CONDITIONAL_NOT_EXIST.bind(expressions.getAnnotationType().asElement().getSimpleName())));
    }

    private Optional<AnnotationValue> getConditional(AnnotationMirror expressions) {
        return expressions.getElementValues().entrySet().stream()
                .filter(entry -> entry.getKey().getReturnType().toString().equals(conditionalName))
                .findFirst()
                .map(Map.Entry::getValue);
    }

    private Optional<AnnotationValue> getDefaultConditional(AnnotationMirror expressions) {
        return expressions.getAnnotationType().asElement().getEnclosedElements().stream()
                .filter(element -> ((ExecutableElement) element).getReturnType().toString().equals(conditionalName))
                .findFirst()
                .map(element -> ((ExecutableElement) element).getDefaultValue());
    }

    private String getOperatorArgumentName(AnnotationMirror expression) {
        return expression.getAnnotationType().asElement().getEnclosedElements().stream()
                .filter(element -> ((ExecutableElement) element).getReturnType().toString().equals(operatorName))
                .findFirst()
                .map(element -> element.getSimpleName().toString())
                .orElseThrow(() -> new ElementProcessException(EXPRESSION_OPERATOR_NOT_EXIST.bind(expression.getAnnotationType().asElement().getSimpleName())));
    }

    private Optional<AnnotationValue> getOperator(AnnotationMirror expression) {
        return expression.getElementValues().entrySet().stream()
                .filter(entry -> entry.getKey().getReturnType().toString().equals(operatorName))
                .findFirst()
                .map(Map.Entry::getValue);
    }

    private Optional<AnnotationValue> getDefaultOperator(AnnotationMirror expression) {
        return expression.getAnnotationType().asElement().getEnclosedElements().stream()
                .filter(element -> ((ExecutableElement) element).getReturnType().toString().equals(operatorName))
                .findFirst()
                .map(element -> ((ExecutableElement) element).getDefaultValue());
    }

    private Optional<String> getValueArgumentName(AnnotationMirror expression) {
        return expression.getElementValues().keySet().stream()
                .filter(annotationValue -> !annotationValue.getReturnType().toString().equals(operatorName))
                .filter(annotationValue -> !annotationValue.getSimpleName().toString().startsWith("$"))
                .findFirst()
                .map(annotationValue -> annotationValue.getSimpleName().toString());
    }

    private Optional<String> getVariableArgumentName(AnnotationMirror expression) {
        return expression.getElementValues().keySet().stream()
                .filter(annotationValue -> !annotationValue.getReturnType().toString().equals(operatorName))
                .filter(annotationValue -> annotationValue.getSimpleName().toString().startsWith("$"))
                .findFirst()
                .map(annotationValue -> annotationValue.getSimpleName().toString());
    }

    private Stream<String> getExpressionVariableNamesByArgumentName(AnnotationMirror expression, String variableArgumentName) {
        return expression.getElementValues().entrySet().stream()
                .filter(entry -> entry.getKey().getSimpleName().toString().equals(variableArgumentName))
                .filter(entry -> entry.getValue().getValue() instanceof List<?>)
                .map(entry -> (List<?>) entry.getValue().getValue())
                .flatMap(list -> list.stream().map(value -> ((AnnotationValue) value).getValue().toString()));
    }

    private Optional<List<?>> getListVariable(AnnotationMirror expression) {
        return expression.getElementValues().entrySet().stream()
                .filter(entry -> !entry.getKey().getReturnType().toString().equals(operatorName))
                .filter(entry -> entry.getKey().getSimpleName().toString().startsWith("$"))
                .filter(entry -> entry.getValue().getValue() instanceof List<?>)
                .findFirst()
                .map(entry -> (List<?>) entry.getValue().getValue());
    }

    private Optional<Object> getVariable(AnnotationMirror expression, ExecutableElement parentExecutableElement) {
        return getListVariable(expression)
                .map(list -> list.stream()
                        .map(value -> elementManager.getParameterFromExecutableElement(parentExecutableElement, ((AnnotationValue) value).getValue().toString()))
                        .collect(Collectors.toList())
                )
                .map(variableElements -> variableElements.size() == 1 ? variableElements.get(0) : variableElements);
    }

    private Optional<String> getVariableName(AnnotationMirror expression, ExecutableElement parentExecutableElement) {
        return getListVariable(expression)
                .flatMap(list ->
                        list.size() == 1 ?
                                getVariableName(parentExecutableElement, ((AnnotationValue) list.get(0)).getValue().toString()) :
                                Optional.of("in")
                );
    }

    public Optional<String> getVariableName(ExecutableElement executableElement, String name) {
        return executableElement.getParameters().stream()
                .filter(parameter -> parameter.getSimpleName().toString().equals(name))
                .findFirst()
                .map(parameter -> parameter.asType().getKind().equals(TypeKind.ARRAY) ? "in" : "val");
    }

    private Optional<List<?>> getListValue(AnnotationMirror expression) {
        return expression.getElementValues().entrySet().stream()
                .filter(entry -> !entry.getKey().getReturnType().toString().equals(operatorName))
                .filter(entry -> !entry.getKey().getSimpleName().toString().startsWith("$"))
                .filter(entry -> entry.getValue().getValue() instanceof List<?>)
                .findFirst()
                .map(entry -> (List<?>) entry.getValue().getValue());
    }

    private boolean valueIsExpressions(AnnotationMirror expression) {
        return getListValue(expression).stream().flatMap(Collection::stream).anyMatch(value -> value instanceof AnnotationMirror);
    }

    private Optional<Object> getValue(AnnotationMirror expression) {
        return getListValue(expression)
                .map(values -> values.size() == 1 ? values.get(0) : values);
    }

    private Optional<Map<String, Object>> getExpressionsValue(ExecutableElement executableElement, AnnotationMirror expression) {
        return getValue(expression).map(expressions -> expressionsAnnotationMap(executableElement, (AnnotationMirror) expressions));
    }

    private Optional<String> getValueName(AnnotationMirror expression) {
        return getListValue(expression)
                .map(values -> values.size() == 1 ? "val" : "in");
    }

    private String getTypeName(String queryFieldName, String argumentName) {
        String queryTypeName = getQueryTypeName(queryFieldName);
        return manager.getField(queryTypeName, argumentName)
                .map(fieldDefinitionContext -> {
                            String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
                            if (manager.isObject(fieldTypeName)) {
                                return fieldDefinitionContext.type().getText().replace(fieldTypeName, fieldTypeName.concat(INPUT_SUFFIX));
                            } else {
                                return fieldDefinitionContext.type().getText();
                            }
                        }
                )
                .orElseThrow(() -> new GraphQLErrors(FIELD_NOT_EXIST.bind(queryTypeName, argumentName)));
    }

    private String getQueryTypeName(String queryFieldName) {
        return manager.getQueryOperationTypeName()
                .flatMap(queryTypeName -> manager.getField(queryTypeName, queryFieldName))
                .map(fieldDefinitionContext -> manager.getFieldTypeName(fieldDefinitionContext.type()))
                .orElseThrow(() -> new GraphQLErrors(QUERY_TYPE_NOT_EXIST));
    }
}
