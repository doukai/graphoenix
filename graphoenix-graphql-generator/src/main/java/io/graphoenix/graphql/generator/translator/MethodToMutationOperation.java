package io.graphoenix.graphql.generator.translator;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.graphql.generator.operation.Argument;
import io.graphoenix.graphql.generator.operation.Field;
import io.graphoenix.graphql.generator.operation.Operation;
import io.graphoenix.graphql.generator.operation.VariableDefinition;
import io.graphoenix.spi.annotation.TypeInput;
import io.graphoenix.spi.annotation.TypeInputs;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.config.JavaGeneratorConfig;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MethodToMutationOperation {

    private final IGraphQLDocumentManager manager;

    public MethodToMutationOperation(IGraphQLDocumentManager manager, JavaGeneratorConfig configuration) {
        this.manager = manager;
    }

    public String executableElementToMutation(String mutationFieldName, ExecutableElement executableElement, int layers) {
        Operation operation = new Operation()
                .setName(executableElement.getSimpleName().toString())
                .setOperationType("mutation");
        Field field = new Field().setName(mutationFieldName);

        Optional<? extends AnnotationMirror> expressions = getInputsAnnotation(executableElement);
        if (expressions.isPresent()) {
            field.addArguments(inputsAnnotationArguments(executableElement, expressions.get()));
            operation.addVariableDefinitions(
                    getVariableArgumentNames(expressions.get())
                            .flatMap(argumentName ->
                                    getInputsVariableNamesByArgumentName(expressions.get(), argumentName)
                                            .map(variableName -> buildVariableDefinition(mutationFieldName, argumentName.substring(1), variableName))
                            )
            );
            operation.addVariableDefinitions(
                    getRelationValueArgumentNames(expressions.get())
                            .flatMap(argumentName ->
                                    getRelationInputsVariableNamesByArgumentName(expressions.get(), argumentName)
                                            .map(variableName -> buildVariableDefinition(mutationFieldName, argumentName.substring(1), variableName))
                            )
            );
        }

        Optional<? extends AnnotationMirror> expression = getInputAnnotation(executableElement);
        if (expression.isPresent()) {
            field.addArgument(inputAnnotationToArgument(executableElement, expression.get()));
            getVariableArgumentName(expression.get())
                    .ifPresent(argumentName ->
                            operation.addVariableDefinitions(
                                    getInputVariableNamesByArgumentName(expression.get(), argumentName)
                                            .map(variableName -> buildVariableDefinition(mutationFieldName, argumentName.substring(1), variableName))
                            )
                    );
        }
        field.setFields(buildFields(getMutationTypeName(mutationFieldName), 0, layers));
        operation.addField(field);
        return operation.toString();
    }

    private List<Field> buildFields(String typeName, int level, int layers) {
        return getFields(typeName, level, layers)
                .map(fieldDefinitionContext ->
                        {
                            Field field = new Field().setName(fieldDefinitionContext.name().getText());
                            if (level <= layers) {
                                field.setFields(buildFields(manager.getFieldTypeName(fieldDefinitionContext.type()), level + 1, layers));
                            }
                            return field;
                        }
                )
                .collect(Collectors.toList());
    }

    private Optional<? extends AnnotationMirror> getInputAnnotation(ExecutableElement executableElement) {
        return executableElement.getAnnotationMirrors().stream()
                .filter(annotationMirror -> annotationMirror.getAnnotationType().asElement().getAnnotation(TypeInput.class) != null)
                .findFirst();
    }

    private Optional<? extends AnnotationMirror> getInputsAnnotation(ExecutableElement executableElement) {
        return executableElement.getAnnotationMirrors().stream()
                .filter(annotationMirror -> annotationMirror.getAnnotationType().asElement().getAnnotation(TypeInputs.class) != null)
                .findFirst();
    }

    private Argument inputAnnotationToArgument(ExecutableElement executableElement, AnnotationMirror input) {
        return new Argument()
                .setName(getValueArgumentName(input).orElseGet(() -> getVariableArgumentName(input).map(argumentName -> argumentName.substring(1)).orElseThrow()))
                .setValueWithVariable(getValue(input).orElseGet(() -> getVariable(input, executableElement).orElseThrow()));
    }

    private Stream<Argument> inputsAnnotationArguments(ExecutableElement executableElement, AnnotationMirror inputs) {

        Stream<Argument> argumentStream = getValueArguments(executableElement, inputs);
        Stream<Argument> relationValueArguments = getRelationValueArguments(executableElement, inputs);

        return Stream.concat(
                argumentStream,
                relationValueArguments
        );
    }

    private Stream<Argument> getValueArguments(ExecutableElement executableElement, AnnotationMirror inputs) {
        return inputs.getElementValues().entrySet().stream()
                .filter(entry -> entry.getKey().getSimpleName().toString().equals("value"))
                .filter(entry -> entry.getValue().getValue() instanceof Collection<?>)
                .map(entry -> (Collection<?>) entry.getValue().getValue())
                .findFirst()
                .map(collection -> collection.stream()
                        .filter(expression -> expression instanceof AnnotationValue)
                        .map(input -> (AnnotationMirror) input)
                        .map(input -> inputAnnotationToArgument(executableElement, input))
                ).orElseGet(Stream::empty);
    }

    private Stream<Argument> getRelationValueArguments(ExecutableElement executableElement, AnnotationMirror inputs) {
        return inputs.getElementValues().entrySet().stream()
                .filter(entry -> !entry.getKey().getSimpleName().toString().equals("value"))
                .map(entry -> {
                            if (entry.getValue().getValue() instanceof Collection<?>) {

                                return new Argument()
                                        .setName(entry.getKey().getSimpleName().toString())
                                        .setValueWithVariable(
                                                ((Collection<?>) entry.getValue().getValue()).stream()
                                                        .filter(expression -> expression instanceof AnnotationValue)
                                                        .map(expression -> (AnnotationMirror) expression)
                                                        .collect(Collectors.toMap(
                                                                expression -> getValueArgumentName(expression).orElseGet(() -> getVariableArgumentName(expression).orElseThrow()),
                                                                expression -> getValue(expression).orElseGet(() -> getVariable(expression, executableElement).orElseThrow())
                                                        ))
                                        );
                            }else{
                                return new Argument()
                                        .setName(entry.getKey().getSimpleName().toString())
                                        .setValueWithVariable(
                                                ((Collection<?>) entry.getValue().getValue()).stream()
                                                        .filter(expression -> expression instanceof AnnotationValue)
                                                        .map(expression -> (AnnotationMirror) expression)
                                                        .collect(Collectors.toMap(
                                                                expression -> getValueArgumentName(expression).orElseGet(() -> getVariableArgumentName(expression).orElseThrow()),
                                                                expression -> getValue(expression).orElseGet(() -> getVariable(expression, executableElement).orElseThrow())
                                                        ))
                                        );
                            }


                        }
                );
    }

    private Optional<String> getValueArgumentName(AnnotationMirror input) {
        return input.getElementValues().keySet().stream()
                .filter(annotationValue -> !annotationValue.getSimpleName().toString().startsWith("$"))
                .findFirst()
                .map(annotationValue -> annotationValue.getSimpleName().toString());
    }

    private Optional<String> getVariableArgumentName(AnnotationMirror inputs) {
        return inputs.getElementValues().keySet().stream()
                .filter(annotationValue -> annotationValue.getSimpleName().toString().startsWith("$"))
                .findFirst()
                .map(annotationValue -> annotationValue.getSimpleName().toString());
    }

    private Stream<String> getVariableArgumentNames(AnnotationMirror inputs) {
        return inputs.getElementValues().entrySet().stream()
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

    private Stream<String> getRelationValueArgumentNames(AnnotationMirror inputs) {
        return inputs.getElementValues().entrySet().stream()
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

    private Stream<String> getInputVariableNamesByArgumentName(AnnotationMirror input, String variableArgumentName) {
        return input.getElementValues().entrySet().stream()
                .filter(entry -> entry.getKey().getSimpleName().toString().equals(variableArgumentName))
                .findFirst()
                .map(entry -> entry.getValue().getValue())
                .map(value -> {
                    if (value instanceof List<?>) {
                        return ((List<?>) value).stream().map(item -> ((AnnotationValue) item).getValue().toString());
                    } else {
                        return Stream.of(value.toString());
                    }
                }).orElseGet(Stream::empty);
    }


    private Stream<String> getInputsVariableNamesByArgumentName(AnnotationMirror inputs, String variableArgumentName) {
        return inputs.getElementValues().entrySet().stream()
                .filter(entry -> entry.getKey().getSimpleName().toString().equals("value"))
                .filter(entry -> entry.getValue().getValue() instanceof Collection<?>)
                .map(entry -> (Collection<?>) entry.getValue().getValue())
                .findFirst()
                .map(collection -> collection.stream()
                        .filter(expression -> expression instanceof AnnotationValue)
                        .map(expression -> (AnnotationMirror) expression)
                        .flatMap(expression -> getInputVariableNamesByArgumentName(expression, variableArgumentName))
                ).orElseGet(Stream::empty);
    }

    private Stream<String> getRelationInputsVariableNamesByArgumentName(AnnotationMirror inputs, String variableArgumentName) {
        return inputs.getElementValues().entrySet().stream()
                .filter(entry -> !entry.getKey().getSimpleName().toString().equals("value"))
                .filter(entry -> entry.getValue().getValue() instanceof Collection<?>)
                .map(entry -> (Collection<?>) entry.getValue().getValue())
                .flatMap(collection -> collection.stream()
                        .filter(expression -> expression instanceof AnnotationValue)
                        .map(expression -> (AnnotationMirror) expression)
                        .flatMap(expression -> getInputVariableNamesByArgumentName(expression, variableArgumentName))
                );
    }

    private Optional<Object> getVariable(AnnotationMirror input, ExecutableElement parentExecutableElement) {
        return input.getElementValues().entrySet().stream()
                .filter(entry -> entry.getKey().getSimpleName().toString().startsWith("$"))
                .findFirst()
                .map(entry -> entry.getValue().getValue())
                .map(value -> {
                            if (value instanceof List<?>) {
                                return ((List<?>) value).stream()
                                        .map(item -> getParameterFromExecutableElement(parentExecutableElement, ((AnnotationValue) item).getValue().toString()).orElseThrow())
                                        .collect(Collectors.toList());
                            } else {
                                return getParameterFromExecutableElement(parentExecutableElement, value.toString()).orElseThrow();
                            }
                        }
                );
    }

    private Optional<? extends VariableElement> getParameterFromExecutableElement(ExecutableElement executableElement, String name) {
        return executableElement.getParameters().stream()
                .filter(parameter -> parameter.getSimpleName().toString().equals(name))
                .findFirst();
    }

    private Optional<Object> getValue(AnnotationMirror expression) {
        return expression.getElementValues().entrySet().stream()
                .filter(entry -> !entry.getKey().getSimpleName().toString().startsWith("$"))
                .findFirst()
                .map(entry -> entry.getValue().getValue());
    }

    private VariableDefinition buildVariableDefinition(String mutationFieldName, String argumentName, String variableName) {
        return new VariableDefinition()
                .setVariable(variableName)
                .setTypeName(getTypeName(mutationFieldName, argumentName));
    }

    private String getMutationTypeName(String mutationFieldName) {
        return manager.getMutationOperationTypeName()
                .flatMap(mutationTypeName -> manager.getField(mutationTypeName, mutationFieldName))
                .map(fieldDefinitionContext -> manager.getFieldTypeName(fieldDefinitionContext.type()))
                .orElseThrow();
    }

    private String getTypeName(String mutationFieldName, String argumentName) {
        return manager.getField(getMutationTypeName(mutationFieldName), argumentName)
                .map(fieldDefinitionContext -> manager.getFieldTypeName(fieldDefinitionContext.type()))
                .orElseThrow();
    }

    private Stream<GraphqlParser.FieldDefinitionContext> getFields(String typeName, int level, int layers) {
        return manager.getFields(typeName)
                .filter(fieldDefinitionContext -> level < layers ||
                        level == layers && (manager.isScaLar(manager.getFieldTypeName(fieldDefinitionContext.type())) || manager.isEnum(manager.getFieldTypeName(fieldDefinitionContext.type())))
                );
    }
}
