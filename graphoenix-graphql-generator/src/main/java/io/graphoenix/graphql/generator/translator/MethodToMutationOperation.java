package io.graphoenix.graphql.generator.translator;

import io.graphoenix.graphql.generator.operation.Argument;
import io.graphoenix.graphql.generator.operation.Field;
import io.graphoenix.graphql.generator.operation.Operation;
import io.graphoenix.graphql.generator.operation.VariableDefinition;
import io.graphoenix.spi.annotation.TypeInput;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.config.JavaGeneratorConfig;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MethodToMutationOperation {

    private final IGraphQLDocumentManager manager;
    private final ElementManager elementManager;

    public MethodToMutationOperation(IGraphQLDocumentManager manager, JavaGeneratorConfig configuration) {
        this.manager = manager;
        this.elementManager = new ElementManager(manager);
    }

    public String executableElementToMutation(String mutationFieldName, ExecutableElement executableElement, int layers) {
        Operation operation = new Operation()
                .setName(executableElement.getSimpleName().toString())
                .setOperationType("mutation");
        Field field = new Field().setName(mutationFieldName);

        Optional<? extends AnnotationMirror> expression = getInputAnnotation(executableElement);
        if (expression.isPresent()) {
            field.addArguments(inputAnnotationToArgument(executableElement, expression.get()));
            operation.addVariableDefinitions(
                    getVariableArgumentNames(expression.get())
                            .flatMap(argumentName ->
                                    getInputVariableNamesByArgumentName(expression.get(), argumentName)
                                            .map(variableName -> buildVariableDefinition(mutationFieldName, argumentName.substring(1), variableName))
                            )
            );
        }
        field.setFields(elementManager.buildFields(getMutationTypeName(mutationFieldName), 0, layers));
        operation.addField(field);
        return operation.toString();
    }

    private Optional<? extends AnnotationMirror> getInputAnnotation(ExecutableElement executableElement) {
        return executableElement.getAnnotationMirrors().stream()
                .filter(annotationMirror -> annotationMirror.getAnnotationType().asElement().getAnnotation(TypeInput.class) != null)
                .findFirst();
    }

    private Stream<Argument> inputAnnotationToArgument(ExecutableElement executableElement, AnnotationMirror input) {
        return input.getElementValues().entrySet().stream()
                .map(entry -> {
                    String fieldName = entry.getKey().getSimpleName().toString();
                    Object value = entry.getValue().getValue();
                    Argument argument = new Argument();
                    if (entry.getKey().getSimpleName().toString().startsWith("$")) {
                        argument.setName(fieldName.substring(1));
                        if (value instanceof Collection<?>) {
                            argument.setValueWithVariable(
                                    ((Collection<?>) value).stream()
                                            .map(item -> elementManager.getParameterFromExecutableElement(executableElement, ((AnnotationValue) item).getValue().toString()).orElseThrow())
                                            .collect(Collectors.toList())
                            );
                        } else {
                            argument.setValueWithVariable(elementManager.getParameterFromExecutableElement(executableElement, value.toString()).orElseThrow());
                        }
                    } else {
                        argument.setName(fieldName);
                        if (value instanceof Collection<?>) {
                            argument.setValueWithVariable(
                                    ((Collection<?>) value).stream()
                                            .map(item -> ((AnnotationValue) item).getValue())
                                            .collect(Collectors.toList())
                            );
                        } else {
                            argument.setValueWithVariable(value);
                        }
                    }
                    return argument;
                });
    }

    private Stream<String> getVariableArgumentNames(AnnotationMirror inputs) {
        return inputs.getElementValues().keySet().stream()
                .filter(annotationValue -> annotationValue.getSimpleName().toString().startsWith("$"))
                .map(annotationValue -> annotationValue.getSimpleName().toString());
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

    private VariableDefinition buildVariableDefinition(String mutationFieldName, String argumentName, String variableName) {
        return new VariableDefinition()
                .setVariable(variableName)
                .setTypeName(getTypeName(mutationFieldName, argumentName));
    }

    private String getTypeName(String mutationFieldName, String argumentName) {
        return manager.getField(getMutationTypeName(mutationFieldName), argumentName)
                .map(fieldDefinitionContext -> manager.getFieldTypeName(fieldDefinitionContext.type()))
                .orElseThrow();
    }

    private String getMutationTypeName(String mutationFieldName) {
        return manager.getMutationOperationTypeName()
                .flatMap(mutationTypeName -> manager.getField(mutationTypeName, mutationFieldName))
                .map(fieldDefinitionContext -> manager.getFieldTypeName(fieldDefinitionContext.type()))
                .orElseThrow();
    }
}
