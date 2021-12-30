package io.graphoenix.graphql.generator.translator;

import io.graphoenix.graphql.generator.operation.Argument;
import io.graphoenix.graphql.generator.operation.Field;
import io.graphoenix.graphql.generator.operation.Operation;
import io.graphoenix.graphql.generator.operation.VariableDefinition;
import io.graphoenix.spi.annotation.TypeExpression;
import io.graphoenix.spi.annotation.TypeExpressions;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;

import javax.inject.Inject;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MethodToQueryOperation {

    private final String conditionalName;
    private final String operatorName;
    private final IGraphQLDocumentManager manager;
    private final ElementManager elementManager;

    @Inject
    public MethodToQueryOperation(IGraphQLDocumentManager manager, ElementManager elementManager) {
        this.conditionalName = ".Conditional";
        this.operatorName = ".Operator";
        this.manager = manager;
        this.elementManager = elementManager;
    }

    public String executableElementToQuery(String queryFieldName, ExecutableElement executableElement, int layers) {
        Operation operation = new Operation()
                .setName(executableElement.getSimpleName().toString())
                .setOperationType("query");
        Field field = new Field().setName(queryFieldName);

        Optional<? extends AnnotationMirror> expressions = getExpressionsAnnotation(executableElement);
        if (expressions.isPresent()) {
            field.addArguments(expressionsAnnotationArguments(executableElement, expressions.get()));
            operation.addVariableDefinitions(
                    getVariableArgumentNames(expressions.get())
                            .flatMap(argumentName ->
                                    getExpressionsVariableNamesByArgumentName(expressions.get(), argumentName)
                                            .map(variableName -> buildVariableDefinition(queryFieldName, argumentName.substring(1), variableName))
                            )
            );
            operation.addVariableDefinitions(
                    getRelationValueArgumentNames(expressions.get())
                            .flatMap(argumentName ->
                                    getRelationExpressionsVariableNamesByArgumentName(expressions.get(), argumentName)
                                            .map(variableName -> buildVariableDefinition(queryFieldName, argumentName.substring(1), variableName))
                            )
            );
        }

        Optional<? extends AnnotationMirror> expression = getExpressionAnnotation(executableElement);
        if (expression.isPresent()) {
            field.addArgument(expressionAnnotationToArgument(executableElement, expression.get()));
            getVariableArgumentName(expression.get())
                    .ifPresent(argumentName ->
                            operation.addVariableDefinitions(
                                    getExpressionVariableNamesByArgumentName(expression.get(), argumentName)
                                            .map(variableName -> buildVariableDefinition(queryFieldName, argumentName.substring(1), variableName))
                            )
                    );
        }
        field.setFields(elementManager.buildFields(getQueryTypeName(queryFieldName), 0, layers));
        operation.addField(field);
        return operation.toString();
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
                .setName(getValueArgumentName(expression).orElseGet(() -> getVariableArgumentName(expression).map(argumentName -> argumentName.substring(1)).orElseThrow()))
                .setValueWithVariable(
                        Map.of(
                                getOperatorArgumentName(expression),
                                getOperator(expression).orElseGet(() -> getDefaultOperator(expression).orElseThrow()),
                                getValueName(expression).orElseGet(() -> getVariableName(expression, executableElement).orElseThrow()),
                                getValue(expression).orElseGet(() -> getVariable(expression, executableElement).orElseThrow())
                        ));
    }

    private Stream<Argument> expressionsAnnotationArguments(ExecutableElement executableElement, AnnotationMirror expressions) {
        Argument conditionalArgument = new Argument()
                .setName(getConditionalArgumentName(expressions))
                .setValueWithVariable(getConditional(expressions).orElseGet(() -> getDefaultConditional(expressions).orElseThrow()));

        Stream<Argument> argumentStream = getValueArguments(executableElement, expressions);
        Stream<Argument> relationValueArguments = getRelationValueArguments(executableElement, expressions);

        return Stream.concat(
                Stream.concat(
                        Stream.of(conditionalArgument),
                        argumentStream
                ),
                relationValueArguments
        );
    }

    private Stream<Argument> getValueArguments(ExecutableElement executableElement, AnnotationMirror expressions) {
        return expressions.getElementValues().entrySet().stream()
                .filter(entry -> !entry.getKey().getReturnType().toString().endsWith(conditionalName))
                .filter(entry -> entry.getKey().getSimpleName().toString().equals("value"))
                .filter(entry -> entry.getValue().getValue() instanceof Collection<?>)
                .map(entry -> (Collection<?>) entry.getValue().getValue())
                .findFirst()
                .map(collection -> collection.stream()
                        .filter(expression -> expression instanceof AnnotationValue)
                        .map(expression -> (AnnotationMirror) expression)
                        .map(expression -> expressionAnnotationToArgument(executableElement, expression))
                ).orElseGet(Stream::empty);
    }

    private Stream<Argument> getRelationValueArguments(ExecutableElement executableElement, AnnotationMirror expressions) {
        return expressions.getElementValues().entrySet().stream()
                .filter(entry -> !entry.getKey().getReturnType().toString().endsWith(conditionalName))
                .filter(entry -> !entry.getKey().getSimpleName().toString().equals("value"))
                .filter(entry -> entry.getValue().getValue() instanceof Collection<?>)
                .map(entry ->
                        new Argument()
                                .setName(entry.getKey().getSimpleName().toString())
                                .setValueWithVariable(
                                        ((Collection<?>) entry.getValue().getValue()).stream()
                                                .filter(expression -> expression instanceof AnnotationValue)
                                                .map(expression -> (AnnotationMirror) expression)
                                                .collect(Collectors.toMap(
                                                        expression -> getValueArgumentName(expression).orElseGet(() -> getVariableArgumentName(expression).orElseThrow()),
                                                        expression ->
                                                                Map.of(
                                                                        getOperatorArgumentName(expression),
                                                                        getOperator(expression).orElseGet(() -> getDefaultOperator(expression).orElseThrow()),
                                                                        getValueName(expression).orElseGet(() -> getVariableName(expression, executableElement).orElseThrow()),
                                                                        getValue(expression).orElseGet(() -> getVariable(expression, executableElement).orElseThrow())
                                                                )
                                                ))
                                )
                );
    }

    private String getConditionalArgumentName(AnnotationMirror expressions) {
        return expressions.getAnnotationType().asElement().getEnclosedElements().stream()
                .filter(element -> ((ExecutableElement) element).getReturnType().toString().endsWith(conditionalName))
                .findFirst()
                .map(element -> element.getSimpleName().toString())
                .orElseThrow();
    }

    private Optional<AnnotationValue> getConditional(AnnotationMirror expressions) {
        return expressions.getElementValues().entrySet().stream()
                .filter(entry -> entry.getKey().getReturnType().toString().endsWith(conditionalName))
                .findFirst()
                .map(Map.Entry::getValue);
    }

    private Optional<AnnotationValue> getDefaultConditional(AnnotationMirror expressions) {
        return expressions.getAnnotationType().asElement().getEnclosedElements().stream()
                .filter(element -> ((ExecutableElement) element).getReturnType().toString().endsWith(conditionalName))
                .findFirst()
                .map(element -> ((ExecutableElement) element).getDefaultValue());
    }

    private String getOperatorArgumentName(AnnotationMirror expression) {
        return expression.getAnnotationType().asElement().getEnclosedElements().stream()
                .filter(element -> ((ExecutableElement) element).getReturnType().toString().endsWith(operatorName))
                .findFirst()
                .map(element -> element.getSimpleName().toString())
                .orElseThrow();
    }

    private Optional<AnnotationValue> getOperator(AnnotationMirror expression) {
        return expression.getElementValues().entrySet().stream()
                .filter(entry -> entry.getKey().getReturnType().toString().endsWith(operatorName))
                .findFirst()
                .map(Map.Entry::getValue);
    }

    private Optional<AnnotationValue> getDefaultOperator(AnnotationMirror expression) {
        return expression.getAnnotationType().asElement().getEnclosedElements().stream()
                .filter(element -> ((ExecutableElement) element).getReturnType().toString().endsWith(operatorName))
                .findFirst()
                .map(element -> ((ExecutableElement) element).getDefaultValue());
    }

    private Optional<String> getValueArgumentName(AnnotationMirror expression) {
        return expression.getElementValues().keySet().stream()
                .filter(annotationValue -> !annotationValue.getReturnType().toString().endsWith(operatorName))
                .filter(annotationValue -> !annotationValue.getSimpleName().toString().startsWith("$"))
                .findFirst()
                .map(annotationValue -> annotationValue.getSimpleName().toString());
    }

    private Optional<String> getVariableArgumentName(AnnotationMirror expression) {
        return expression.getElementValues().keySet().stream()
                .filter(annotationValue -> !annotationValue.getReturnType().toString().endsWith(operatorName))
                .filter(annotationValue -> annotationValue.getSimpleName().toString().startsWith("$"))
                .findFirst()
                .map(annotationValue -> annotationValue.getSimpleName().toString());
    }

    private Stream<String> getVariableArgumentNames(AnnotationMirror expressions) {
        return expressions.getElementValues().entrySet().stream()
                .filter(entry -> !entry.getKey().getReturnType().toString().endsWith(conditionalName))
                .filter(entry -> entry.getKey().getSimpleName().toString().equals("value"))
                .filter(entry -> entry.getValue().getValue() instanceof Collection<?>)
                .map(entry -> (Collection<?>) entry.getValue().getValue())
                .findFirst()
                .map(collection -> collection.stream()
                        .filter(expression -> expression instanceof AnnotationValue)
                        .map(expression -> (AnnotationMirror) expression)
                        .map(this::getVariableArgumentName)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                ).orElseGet(Stream::empty);
    }

    private Stream<String> getRelationValueArgumentNames(AnnotationMirror expressions) {
        return expressions.getElementValues().entrySet().stream()
                .filter(entry -> !entry.getKey().getReturnType().toString().endsWith(conditionalName))
                .filter(entry -> !entry.getKey().getSimpleName().toString().equals("value"))
                .filter(entry -> entry.getValue().getValue() instanceof Collection<?>)
                .map(entry -> (Collection<?>) entry.getValue().getValue())
                .flatMap(collection -> collection.stream()
                        .filter(expression -> expression instanceof AnnotationValue)
                        .map(expression -> (AnnotationMirror) expression)
                        .map(this::getVariableArgumentName)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                );
    }

    private Stream<String> getExpressionVariableNamesByArgumentName(AnnotationMirror expression, String variableArgumentName) {
        return expression.getElementValues().entrySet().stream()
                .filter(entry -> entry.getKey().getSimpleName().toString().equals(variableArgumentName))
                .filter(entry -> entry.getValue().getValue() instanceof List<?>)
                .findFirst()
                .map(entry -> (List<?>) entry.getValue().getValue())
                .map(list -> list.stream().map(value -> ((AnnotationValue) value).getValue().toString()))
                .orElseGet(Stream::empty);
    }


    private Stream<String> getExpressionsVariableNamesByArgumentName(AnnotationMirror expressions, String variableArgumentName) {
        return expressions.getElementValues().entrySet().stream()
                .filter(entry -> !entry.getKey().getReturnType().toString().endsWith(conditionalName))
                .filter(entry -> entry.getKey().getSimpleName().toString().equals("value"))
                .filter(entry -> entry.getValue().getValue() instanceof Collection<?>)
                .map(entry -> (Collection<?>) entry.getValue().getValue())
                .findFirst()
                .map(collection -> collection.stream()
                        .filter(expression -> expression instanceof AnnotationValue)
                        .map(expression -> (AnnotationMirror) expression)
                        .flatMap(expression -> getExpressionVariableNamesByArgumentName(expression, variableArgumentName))
                ).orElseGet(Stream::empty);
    }

    private Stream<String> getRelationExpressionsVariableNamesByArgumentName(AnnotationMirror expressions, String variableArgumentName) {
        return expressions.getElementValues().entrySet().stream()
                .filter(entry -> !entry.getKey().getReturnType().toString().endsWith(conditionalName))
                .filter(entry -> !entry.getKey().getSimpleName().toString().equals("value"))
                .filter(entry -> entry.getValue().getValue() instanceof Collection<?>)
                .map(entry -> (Collection<?>) entry.getValue().getValue())
                .flatMap(collection -> collection.stream()
                        .filter(expression -> expression instanceof AnnotationValue)
                        .map(expression -> (AnnotationMirror) expression)
                        .flatMap(expression -> getExpressionVariableNamesByArgumentName(expression, variableArgumentName))
                );
    }


    private Optional<List<?>> getListVariable(AnnotationMirror expression, ExecutableElement parentExecutableElement) {
        return expression.getElementValues().entrySet().stream()
                .filter(entry -> !entry.getKey().getReturnType().toString().endsWith(operatorName))
                .filter(entry -> entry.getKey().getSimpleName().toString().startsWith("$"))
                .filter(entry -> entry.getValue().getValue() instanceof List<?>)
                .findFirst()
                .map(entry -> (List<?>) entry.getValue().getValue());
    }

    private Optional<Object> getVariable(AnnotationMirror expression, ExecutableElement parentExecutableElement) {
        return getListVariable(expression, parentExecutableElement)
                .map(list -> list.stream()
                        .map(value -> elementManager.getParameterFromExecutableElement(parentExecutableElement, ((AnnotationValue) value).getValue().toString()))
                        .collect(Collectors.toList())
                )
                .map(variableElements -> variableElements.size() == 1 ? variableElements.get(0) : variableElements);
    }

    private Optional<String> getVariableName(AnnotationMirror expression, ExecutableElement parentExecutableElement) {
        return getListVariable(expression, parentExecutableElement)
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
                .filter(entry -> !entry.getKey().getReturnType().toString().endsWith(operatorName))
                .filter(entry -> !entry.getKey().getSimpleName().toString().startsWith("$"))
                .filter(entry -> entry.getValue().getValue() instanceof List<?>)
                .findFirst()
                .map(entry -> (List<?>) entry.getValue().getValue());
    }

    private Optional<Object> getValue(AnnotationMirror expression) {
        return getListValue(expression)
                .map(values -> values.size() == 1 ? values.get(0) : values);
    }

    private Optional<String> getValueName(AnnotationMirror expression) {
        return getListValue(expression)
                .map(values -> values.size() == 1 ? "val" : "in");
    }

    private VariableDefinition buildVariableDefinition(String queryFieldName, String argumentName, String variableName) {
        return new VariableDefinition()
                .setVariable(variableName)
                .setTypeName(getTypeName(queryFieldName, argumentName));
    }

    private String getTypeName(String queryFieldName, String argumentName) {
        return manager.getField(getQueryTypeName(queryFieldName), argumentName)
                .map(fieldDefinitionContext -> {
                            String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
                            if (manager.isObject(fieldTypeName)) {
                                return fieldDefinitionContext.type().getText().replace(fieldTypeName, fieldTypeName + "Input");
                            } else {
                                return fieldDefinitionContext.type().getText();
                            }
                        }
                )
                .orElseThrow();
    }

    private String getQueryTypeName(String queryFieldName) {
        return manager.getQueryOperationTypeName()
                .flatMap(queryTypeName -> manager.getField(queryTypeName, queryFieldName))
                .map(fieldDefinitionContext -> manager.getFieldTypeName(fieldDefinitionContext.type()))
                .orElseThrow();
    }
}
