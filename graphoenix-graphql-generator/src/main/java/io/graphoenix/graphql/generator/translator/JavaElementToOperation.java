package io.graphoenix.graphql.generator.translator;

import io.graphoenix.graphql.generator.operation.*;
import io.graphoenix.spi.annotation.QueryOperation;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.config.JavaGeneratorConfig;

import javax.lang.model.element.*;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class JavaElementToOperation {

    private final String conditionalName;
    private final String operatorName;

    private final IGraphQLDocumentManager manager;
    private final JavaGeneratorConfig configuration;

    public JavaElementToOperation(IGraphQLDocumentManager manager, JavaGeneratorConfig configuration) {
        this.manager = manager;
        this.configuration = configuration;
        this.conditionalName = configuration.getEnumTypePackageName().concat(".Conditional");
        this.operatorName = configuration.getEnumTypePackageName().concat(".Operator");
    }

    public Stream<String> buildOperationResources(PackageElement packageElement, TypeElement typeElement) {
        return typeElement.getEnclosedElements().stream()
                .filter(element -> element.getKind().equals(ElementKind.METHOD))
                .map(element -> executableElementToOperation((ExecutableElement) element));
    }

    private String executableElementToOperation(ExecutableElement executableElement) {
        QueryOperation queryOperation = executableElement.getAnnotation(QueryOperation.class);
        if (queryOperation != null) {
            return executableElementToQuery(queryOperation.value(), executableElement);
        }
        return null;
    }

    private String executableElementToQuery(String queryFieldName, ExecutableElement executableElement) {
        Operation operation = new Operation()
                .setName(executableElement.getSimpleName().toString())
                .setOperationType("query");
        Field field = new Field().setName(queryFieldName);

        Optional<? extends AnnotationMirror> expressions = getExpressionsAnnotation(executableElement);
        if (expressions.isPresent()) {
            field.addArguments(expressionsAnnotationArguments(executableElement, expressions.get()));
            operation.addVariableDefinitions(getValueArgumentNames(expressions.get()).map(argumentName -> buildVariableDefinition(queryFieldName, argumentName)));
        }

        Optional<? extends AnnotationMirror> expression = getExpressionAnnotation(executableElement);
        if (expression.isPresent()) {
            field.addArgument(expressionAnnotationToArgument(executableElement, expression.get()));
            operation.addVariableDefinition(buildVariableDefinition(queryFieldName, getValueArgumentName(expression.get())));
        }
        operation.addField(field);
        return operation.toString();
    }

    private Optional<? extends AnnotationMirror> getExpressionAnnotation(ExecutableElement executableElement) {
        return executableElement.getAnnotationMirrors().stream()
                .filter(annotationMirror ->
                        annotationMirror.getAnnotationType().asElement().getEnclosedElements().stream()
                                .map(element -> (ExecutableElement) element)
                                .anyMatch(filedElement -> filedElement.getReturnType().toString().equals(operatorName))
                )
                .findFirst();
    }

    private Optional<? extends AnnotationMirror> getExpressionsAnnotation(ExecutableElement executableElement) {
        return executableElement.getAnnotationMirrors().stream()
                .filter(annotationMirror ->
                        annotationMirror.getAnnotationType().asElement().getEnclosedElements().stream()
                                .map(element -> (ExecutableElement) element)
                                .anyMatch(filedElement -> filedElement.getReturnType().toString().equals(conditionalName))
                )
                .findFirst();
    }

    private Argument expressionAnnotationToArgument(ExecutableElement executableElement, AnnotationMirror expression) {
        return new Argument()
                .setName(getValueArgumentName(expression))
                .setValueWithVariable(
                        Map.of(
                                getOperatorArgumentName(expression),
                                getOperator(expression).orElseGet(() -> getDefaultOperator(expression).orElseThrow()),
                                "val",
                                getValueVariable(expression, executableElement)
                        ));
    }

    private Stream<Argument> expressionsAnnotationArguments(ExecutableElement executableElement, AnnotationMirror expressions) {
        Argument conditionalArgument = new Argument()
                .setName(getConditionalArgumentName(expressions))
                .setValueWithVariable(getConditional(expressions).orElseGet(() -> getDefaultConditional(expressions).orElseThrow()));

        Stream<Argument> argumentStream = getValueArguments(executableElement, expressions);

        return Stream.concat(Stream.of(conditionalArgument), argumentStream);
    }


    private Stream<Argument> getValueArguments(ExecutableElement executableElement, AnnotationMirror expressions) {
        return expressions.getElementValues().entrySet().stream()
                .filter(entry -> !entry.getKey().getReturnType().toString().equals(conditionalName))
                .filter(entry -> entry.getKey().getSimpleName().toString().equals("value"))
                .filter(entry -> entry.getValue().getValue() instanceof Collection<?>)
                .map(entry -> (Collection<?>) entry.getValue().getValue())
                .findFirst()
                .map(collection ->
                        collection.stream()
                                .filter(expression -> expression instanceof AnnotationValue)
                                .map(expression -> (AnnotationMirror) expression)
                                .map(expression -> expressionAnnotationToArgument(executableElement, expression))
                ).orElseGet(Stream::empty);
    }


    private String getConditionalArgumentName(AnnotationMirror expressions) {
        return expressions.getAnnotationType().asElement().getEnclosedElements().stream()
                .filter(element -> ((ExecutableElement) element).getReturnType().toString().equals(conditionalName))
                .findFirst()
                .map(element -> element.getSimpleName().toString())
                .orElseThrow();
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
                .orElseThrow();
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

    private String getValueArgumentName(AnnotationMirror expression) {
        return expression.getElementValues().keySet().stream()
                .filter(annotationValue -> !annotationValue.getReturnType().toString().equals(operatorName))
                .findFirst()
                .map(annotationValue -> annotationValue.getSimpleName().toString())
                .orElseThrow();
    }

    private Stream<String> getValueArgumentNames(AnnotationMirror expressions) {
        return expressions.getElementValues().entrySet().stream()
                .filter(entry -> !entry.getKey().getReturnType().toString().equals(conditionalName))
                .filter(entry -> entry.getKey().getSimpleName().toString().equals("value"))
                .filter(entry -> entry.getValue().getValue() instanceof Collection<?>)
                .map(entry -> (Collection<?>) entry.getValue().getValue())
                .findFirst()
                .map(collection ->
                        collection.stream()
                                .filter(expression -> expression instanceof AnnotationValue)
                                .map(expression -> (AnnotationMirror) expression)
                                .map(this::getValueArgumentName)
                ).orElseGet(Stream::empty);
    }

    private VariableElement getValueVariable(AnnotationMirror expression, ExecutableElement parentExecutableElement) {
        return expression.getElementValues().entrySet().stream()
                .filter(entry -> !entry.getKey().getReturnType().toString().equals(operatorName))
                .findFirst()
                .flatMap(entry ->
                        parentExecutableElement.getParameters().stream()
                                .filter(variableElement -> variableElement.getSimpleName().toString().equals(entry.getValue().getValue().toString()))
                                .findFirst()
                ).orElseThrow();
    }

    private VariableDefinition buildVariableDefinition(String queryFieldName, String argumentName) {
        return new VariableDefinition()
                .setVariable(argumentName)
                .setTypeName(getTypeName(queryFieldName, argumentName));
    }

    private String getQueryTypeName(String queryFieldName) {
        return manager.getQueryOperationTypeName()
                .flatMap(queryTypeName -> manager.getField(queryTypeName, queryFieldName))
                .map(fieldDefinitionContext -> manager.getFieldTypeName(fieldDefinitionContext.type()))
                .orElseThrow();
    }

    private String getTypeName(String queryFieldName, String argumentName) {
        return manager.getField(getQueryTypeName(queryFieldName), argumentName)
                .map(fieldDefinitionContext -> manager.getFieldTypeName(fieldDefinitionContext.type()))
                .orElseThrow();
    }
}
