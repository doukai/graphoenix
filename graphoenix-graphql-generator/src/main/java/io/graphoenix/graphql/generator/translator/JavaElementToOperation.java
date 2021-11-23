package io.graphoenix.graphql.generator.translator;

import io.graphoenix.graphql.generator.document.InputObjectType;
import io.graphoenix.graphql.generator.operation.*;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.config.JavaGeneratorConfig;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Query;

import javax.lang.model.element.*;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JavaElementToOperation {

    private final IGraphQLDocumentManager manager;
    private final JavaGeneratorConfig configuration;

    public JavaElementToOperation(IGraphQLDocumentManager manager, JavaGeneratorConfig configuration) {
        this.manager = manager;
        this.configuration = configuration;
    }

    public Stream<String> buildOperationResources(PackageElement packageElement, TypeElement typeElement) {
        return typeElement.getEnclosedElements().stream()
                .filter(element -> element.getKind().equals(ElementKind.METHOD))
                .map(element -> executableElementToOperation((ExecutableElement) element));
    }

    private String executableElementToOperation(ExecutableElement executableElement) {
        Query query = executableElement.getAnnotation(Query.class);
        if (query != null) {
            return executableElementToQuery(query.value(), executableElement);
        }
        return null;
    }

    private String executableElementToQuery(String queryName, ExecutableElement executableElement) {
        Operation operation = new Operation()
                .setName(executableElement.getSimpleName().toString())
                .setOperationType("query");
        Field field = new Field().setName(queryName);

//        Optional<? extends AnnotationMirror> expressions = executableElement.getAnnotationMirrors().stream()
//                .filter(annotationMirror ->
//                        annotationMirror.getAnnotationType().asElement().getEnclosedElements().stream()
//                                .map(element -> (ExecutableElement) element)
//                                .anyMatch(filedElement -> filedElement.getReturnType().toString().equals(configuration.getEnumTypePackageName().concat(".Conditional")))
//                )
//                .findFirst();
//        if (expressions.isPresent()) {
//
//        }
        Optional<? extends AnnotationMirror> expression = executableElement.getAnnotationMirrors().stream()
                .filter(annotationMirror ->
                        annotationMirror.getAnnotationType().asElement().getEnclosedElements().stream()
                                .map(element -> (ExecutableElement) element)
                                .anyMatch(filedElement -> filedElement.getReturnType().toString().equals(configuration.getEnumTypePackageName().concat(".Operator")))
                )
                .findFirst();
        if (expression.isPresent()) {

            ValueWithVariable valueWithVariable = new ValueWithVariable(
                    expression.get().getElementValues().entrySet().stream()
                            .collect(
                                    Collectors.toMap(
                                            entry -> entry.getKey().getSimpleName().toString(),
                                            entry -> annotationValueToVariableElement(executableElement, entry.getKey(), entry.getValue())
                                    )
                            )
            );
            String argumentName = getArgumentName(expression.get());
            field.addArgument(new Argument(argumentName, valueWithVariable.toString()));
            operation.addVariableDefinition(variableElementToVariableDefinition(queryName, argumentName, getParameter(executableElement, argumentName)));
        }
        operation.addField(field);
        return operation.toString();
    }

    private Object annotationValueToVariableElement(ExecutableElement parentExecutableElement, ExecutableElement executableElement, AnnotationValue annotationValue) {
        if (executableElement.getReturnType().toString().equals(configuration.getEnumTypePackageName().concat(".Operator"))) {
            return annotationValue;
        } else {
            return parentExecutableElement.getParameters().stream()
                    .filter(variableElement -> variableElement.getSimpleName().toString().equals(annotationValue.getValue()))
                    .findFirst()
                    .orElseThrow();
        }
    }

    private String getArgumentName(AnnotationMirror expression) {
        return expression.getElementValues().entrySet().stream()
                .filter(entry -> !entry.getKey().getReturnType().toString().equals(configuration.getEnumTypePackageName().concat(".Operator")))
                .findFirst()
                .map(entry -> entry.getKey().getSimpleName().toString())
                .orElseThrow();
    }

    private VariableElement getParameter(ExecutableElement executableElement, String parameterName) {
        return executableElement.getParameters().stream()
                .filter(variableElement -> variableElement.getSimpleName().toString().equals(parameterName))
                .findFirst()
                .orElseThrow();
    }

    private VariableDefinition variableElementToVariableDefinition(String queryName, String argumentName, VariableElement variableElement) {
        VariableDefinition variableDefinition = new VariableDefinition()
                .setVariable(argumentName);
        String typeName = manager.getQueryOperationTypeName()
                .flatMap(queryTypeName -> manager.getField(queryTypeName, queryName))
                .map(fieldDefinitionContext -> manager.getFieldTypeName(fieldDefinitionContext.type()))
                .flatMap(parentTypeName -> manager.getField(parentTypeName, argumentName))
                .map(fieldDefinitionContext -> manager.getFieldTypeName(fieldDefinitionContext.type()))
                .orElseThrow();

        variableDefinition.setTypeName(typeName);

        DefaultValue defaultValue = variableElement.getAnnotation(DefaultValue.class);
        if (defaultValue != null) {
            variableDefinition.setDefaultValue(defaultValue.value());
        }
        return variableDefinition;
    }

    private InputObjectType expressionsToInput(AnnotationMirror expressions) {


        return null;
    }

    private InputObjectType expressionToInput(AnnotationMirror expression) {


        return null;
    }
}
