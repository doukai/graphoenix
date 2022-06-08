package io.graphoenix.graphql.generator.translator;

import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.graphql.generator.operation.Argument;
import io.graphoenix.graphql.generator.operation.Field;
import io.graphoenix.graphql.generator.operation.Operation;
import io.graphoenix.graphql.generator.operation.VariableDefinition;
import io.graphoenix.spi.annotation.TypeInput;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.tinylog.Logger;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.graphoenix.core.error.GraphQLErrorType.FIELD_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.MUTATION_TYPE_NOT_EXIST;
import static io.graphoenix.spi.constant.Hammurabi.INPUT_SUFFIX;

@ApplicationScoped
public class MethodToMutationOperation {

    private final IGraphQLDocumentManager manager;
    private final ElementManager elementManager;

    @Inject
    public MethodToMutationOperation(IGraphQLDocumentManager manager, ElementManager elementManager) {
        this.manager = manager;
        this.elementManager = elementManager;
    }

    public String executableElementToMutation(String mutationFieldName, ExecutableElement executableElement, String selectionSet, int layers) {
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
        if (selectionSet != null && !selectionSet.equals("")) {
            field.setFields(elementManager.buildFields(getMutationTypeName(mutationFieldName), selectionSet));
        } else {
            field.setFields(elementManager.buildFields(getMutationTypeName(mutationFieldName), 0, layers));
        }
        String mutation = operation.addField(field).toString();
        Logger.info("build mutation success:\r\n{}", mutation);
        return mutation;
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
                            if (fieldName.startsWith("$")) {
                                argument.setName(fieldName.substring(1));
                                argument.setValueWithVariable(rebuildVariable(executableElement, value));
                            } else {
                                argument.setName(fieldName);
                                argument.setValueWithVariable(rebuildValue(executableElement, value));
                            }
                            return argument;
                        }
                );
    }

    private Object rebuildValue(ExecutableElement executableElement, Object value) {
        if (value instanceof Collection<?>) {
            return ((Collection<?>) value).stream()
                    .map(item -> rebuildValue(executableElement, ((AnnotationValue) item).getValue()))
                    .collect(Collectors.toList());
        } else {
            if (value instanceof AnnotationMirror) {
                return rebuildAnnotationMirror(executableElement, (AnnotationMirror) value);
            } else {
                return value;
            }
        }
    }

    private Object rebuildVariable(ExecutableElement executableElement, Object value) {
        if (value instanceof Collection<?>) {
            return ((Collection<?>) value).stream()
                    .map(item -> rebuildVariable(executableElement, ((AnnotationValue) item).getValue()))
                    .collect(Collectors.toList());
        } else {
            if (value instanceof AnnotationMirror) {
                return rebuildAnnotationMirror(executableElement, (AnnotationMirror) value);
            } else {
                return elementManager.getParameterFromExecutableElement(executableElement, value.toString());
            }
        }
    }

    private Object rebuildAnnotationMirror(ExecutableElement executableElement, AnnotationMirror value) {
        return value.getElementValues().entrySet().stream()
                .collect(Collectors
                        .toMap(entry -> {
                                    String fieldName = entry.getKey().getSimpleName().toString();
                                    if (fieldName.startsWith("$")) {
                                        return fieldName.substring(1);
                                    } else {
                                        return fieldName;
                                    }
                                },
                                entry -> {
                                    String fieldName = entry.getKey().getSimpleName().toString();
                                    if (fieldName.startsWith("$")) {
                                        return rebuildVariable(executableElement, entry.getValue().getValue());
                                    } else {
                                        return rebuildValue(executableElement, entry.getValue().getValue());
                                    }
                                }
                        )
                );
    }

    private Stream<String> getVariableArgumentNames(AnnotationMirror inputs) {
        return inputs.getElementValues().keySet().stream()
                .filter(annotationValue -> annotationValue.getSimpleName().toString().startsWith("$"))
                .map(annotationValue -> annotationValue.getSimpleName().toString());
    }

    private Stream<String> getInputVariableNamesByArgumentName(AnnotationMirror input, String variableArgumentName) {
        return input.getElementValues().entrySet().stream()
                .filter(entry -> entry.getKey().getSimpleName().toString().equals(variableArgumentName))
                .map(entry -> entry.getValue().getValue())
                .flatMap(value -> {
                            if (value instanceof List<?>) {
                                return ((List<?>) value).stream().map(item -> ((AnnotationValue) item).getValue().toString());
                            } else {
                                return Stream.of(value.toString());
                            }
                        }
                );
    }

    private VariableDefinition buildVariableDefinition(String mutationFieldName, String argumentName, String variableName) {
        return new VariableDefinition()
                .setVariable(variableName)
                .setTypeName(getTypeName(mutationFieldName, argumentName));
    }

    private String getTypeName(String mutationFieldName, String argumentName) {
        String mutationTypeName = getMutationTypeName(mutationFieldName);
        return manager.getField(mutationTypeName, argumentName)
                .map(fieldDefinitionContext -> {
                            String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
                            if (manager.isObject(fieldTypeName)) {
                                return fieldDefinitionContext.type().getText().replace(fieldTypeName, fieldTypeName.concat(INPUT_SUFFIX));
                            } else {
                                return fieldDefinitionContext.type().getText();
                            }
                        }
                )
                .orElseThrow(() -> new GraphQLErrors(FIELD_NOT_EXIST.bind(mutationTypeName, argumentName)));
    }

    private String getMutationTypeName(String mutationFieldName) {
        return manager.getMutationOperationTypeName()
                .flatMap(mutationTypeName -> manager.getField(mutationTypeName, mutationFieldName))
                .map(fieldDefinitionContext -> manager.getFieldTypeName(fieldDefinitionContext.type()))
                .orElseThrow(() -> new GraphQLErrors(MUTATION_TYPE_NOT_EXIST));
    }
}
