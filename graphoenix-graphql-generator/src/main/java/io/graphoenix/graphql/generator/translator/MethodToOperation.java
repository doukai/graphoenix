package io.graphoenix.graphql.generator.translator;

import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.graphql.generator.operation.Argument;
import io.graphoenix.graphql.generator.operation.Field;
import io.graphoenix.graphql.generator.operation.Operation;
import io.graphoenix.graphql.generator.operation.VariableDefinition;
import io.graphoenix.spi.annotation.Arguments;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.dto.type.OperationType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.tinylog.Logger;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.util.Types;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.graphoenix.core.error.GraphQLErrorType.MUTATION_TYPE_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.QUERY_TYPE_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.UNSUPPORTED_OPERATION_TYPE;

@ApplicationScoped
public class MethodToOperation {

    private final IGraphQLDocumentManager manager;
    private final ElementManager elementManager;

    @Inject
    public MethodToOperation(IGraphQLDocumentManager manager, ElementManager elementManager) {
        this.manager = manager;
        this.elementManager = elementManager;
    }

    public String executableElementToOperation(OperationType operationType, String fieldName, ExecutableElement executableElement, String selectionSet, int layers, Types typeUtils) {

        String operationName;
        String typeName;
        switch (operationType) {
            case QUERY:
                operationName = "query";
                typeName = getQueryTypeName(fieldName);
                break;
            case MUTATION:
                operationName = "mutation";
                typeName = getMutationTypeName(fieldName);
                break;
            default:
                throw new GraphQLErrors(UNSUPPORTED_OPERATION_TYPE);
        }

        Operation operation = new Operation()
                .setName(executableElement.getSimpleName().toString())
                .setOperationType(operationName);
        Field field = new Field().setName(fieldName);

        Optional<? extends AnnotationMirror> expression = getInputAnnotation(executableElement);
        expression.ifPresent(annotationMirror -> field.addArguments(inputAnnotationToArgument(executableElement, annotationMirror)));

        operation.addVariableDefinitions(
                executableElement.getParameters().stream()
                        .map(parameter ->
                                new VariableDefinition()
                                        .setVariable(parameter.getSimpleName().toString())
                                        .setTypeName(elementManager.variableElementToInputTypeName(parameter, typeUtils))
                        )
        );

        if (selectionSet != null && !selectionSet.equals("")) {
            field.setFields(elementManager.buildFields(selectionSet));
        } else {
            field.setFields(elementManager.buildFields(typeName, 0, layers));
        }
        String operationString = operation.addField(field).toString();
        Logger.info("build operation success:\r\n{}", operationString);
        return operationString;
    }

    private Optional<? extends AnnotationMirror> getInputAnnotation(ExecutableElement executableElement) {
        return executableElement.getAnnotationMirrors().stream()
                .filter(annotationMirror -> annotationMirror.getAnnotationType().asElement().getAnnotation(Arguments.class) != null)
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

    private String getMutationTypeName(String mutationFieldName) {
        return manager.getMutationOperationTypeName()
                .flatMap(mutationTypeName -> manager.getField(mutationTypeName, mutationFieldName))
                .map(fieldDefinitionContext -> manager.getFieldTypeName(fieldDefinitionContext.type()))
                .orElseThrow(() -> new GraphQLErrors(MUTATION_TYPE_NOT_EXIST));
    }

    private String getQueryTypeName(String queryFieldName) {
        return manager.getQueryOperationTypeName()
                .flatMap(queryTypeName -> manager.getField(queryTypeName, queryFieldName))
                .map(fieldDefinitionContext -> manager.getFieldTypeName(fieldDefinitionContext.type()))
                .orElseThrow(() -> new GraphQLErrors(QUERY_TYPE_NOT_EXIST));
    }
}
