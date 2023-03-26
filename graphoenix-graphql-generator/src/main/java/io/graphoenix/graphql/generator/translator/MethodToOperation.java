package io.graphoenix.graphql.generator.translator;

import com.google.common.base.CaseFormat;
import io.graphoenix.core.document.Directive;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.core.operation.*;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.dto.type.OperationType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.JsonValue;
import jakarta.json.stream.JsonCollectors;
import org.tinylog.Logger;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.util.Types;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.graphoenix.core.error.GraphQLErrorType.MUTATION_TYPE_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.QUERY_TYPE_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.UNSUPPORTED_OPERATION_TYPE;
import static io.graphoenix.spi.constant.Hammurabi.*;

@ApplicationScoped
public class MethodToOperation {

    private final IGraphQLDocumentManager manager;
    private final ElementManager elementManager;

    @Inject
    public MethodToOperation(IGraphQLDocumentManager manager, ElementManager elementManager) {
        this.manager = manager;
        this.elementManager = elementManager;
    }

    public String executableElementToOperation(ExecutableElement executableElement, int index, Types typeUtils) {
        OperationType operationType = elementManager.getOperationTypeFromExecutableElement(executableElement);
        String selectionSet = elementManager.getSelectionSetFromExecutableElement(executableElement);
        int layers = elementManager.getLayersFromExecutableElement(executableElement);
        String fieldName = elementManager.getOperationFieldNameFromExecutableElement(executableElement);
        String operationTypeNameName;
        String typeName;
        String typeInputName;
        switch (operationType) {
            case QUERY:
                operationTypeNameName = "query";
                typeName = getQueryTypeName(fieldName);
                typeInputName = typeName.concat("Expression");
                break;
            case MUTATION:
                operationTypeNameName = "mutation";
                typeName = getMutationTypeName(fieldName);
                typeInputName = typeName.concat("Input");
                break;
            default:
                throw new GraphQLErrors(UNSUPPORTED_OPERATION_TYPE);
        }

        Operation operation = new Operation()
                .setName(elementManager.getOperationNameFromExecutableElement(executableElement, index))
                .setOperationType(operationTypeNameName)
                .addDirective(
                        new Directive()
                                .setName(INVOKE_DIRECTIVE_NAME)
                                .addArgument("className", executableElement.getEnclosingElement().toString())
                                .addArgument("methodName", executableElement.getSimpleName().toString())
                                .addArgument(
                                        "parameters",
                                        new ArrayValueWithVariable(
                                                executableElement.getParameters().stream()
                                                        .map(parameter -> Map.of("name", parameter.getSimpleName().toString(), "className", parameter.asType().toString()))
                                                        .collect(Collectors.toList())
                                        )
                                )
                                .addArgument("returnClassName", executableElement.getReturnType().toString())
                );
        Field field = new Field().setName(fieldName);

        Optional<? extends AnnotationMirror> expression = getInputAnnotation(executableElement, typeInputName);
        expression.ifPresent(annotationMirror -> field.addArguments(inputAnnotationToArgument(executableElement, annotationMirror)));

        operation.addVariableDefinitions(
                executableElement.getParameters().stream()
                        .map(parameter ->
                                new VariableDefinition()
                                        .setVariable(parameter.getSimpleName().toString())
                                        .setTypeName(elementManager.variableElementToInputTypeName(parameter, typeUtils))
                        )
        );

        if (!selectionSet.equals("")) {
            field.setFields(elementManager.buildFields(selectionSet));
        } else {
            field.setFields(elementManager.buildFields(typeName, 0, layers));
        }
        String operationString = operation.addField(field).toString();
        Logger.info("build operation success:\r\n{}", operationString);
        return operationString;
    }

    private Optional<? extends AnnotationMirror> getInputAnnotation(ExecutableElement executableElement, String typeInputName) {
        return executableElement.getAnnotationMirrors().stream()
                .filter(annotationMirror -> annotationMirror.getAnnotationType().asElement().getSimpleName().toString().startsWith(typeInputName))
                .findFirst();
    }

    private Arguments inputAnnotationToArgument(ExecutableElement executableElement, AnnotationMirror input) {
        return new Arguments(
                input.getElementValues().entrySet().stream()
                        .map(entry -> {
                                    String fieldName = entry.getKey().getSimpleName().toString();
                                    Object value = entry.getValue().getValue();
                                    if (fieldName.startsWith("$")) {
                                        return new AbstractMap.SimpleEntry<>(fieldName.substring(1), (JsonValue) new ValueWithVariable(rebuildVariable(executableElement, value)));
                                    } else {
                                        return new AbstractMap.SimpleEntry<>(fieldName, (JsonValue) new ValueWithVariable(rebuildValue(executableElement, value)));
                                    }
                                }
                        )
                        .collect(JsonCollectors.toJsonObject())
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
