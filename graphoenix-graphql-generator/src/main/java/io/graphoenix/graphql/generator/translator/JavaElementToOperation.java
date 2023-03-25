package io.graphoenix.graphql.generator.translator;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Types;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class JavaElementToOperation {

    private final MethodToOperation methodToOperation;

    @Inject
    public JavaElementToOperation(MethodToOperation methodToOperation) {
        this.methodToOperation = methodToOperation;
    }

    public List<String> buildOperations(TypeElement typeElement, Types typeUtils) {
        return typeElement.getEnclosedElements().stream()
                .filter(element -> element.getKind().equals(ElementKind.METHOD))
                .map(element -> (ExecutableElement) element)
                .map(executableElement ->
                        methodToOperation.executableElementToOperation(executableElement, typeElement.getEnclosedElements().indexOf(executableElement), typeUtils)
                )
                .collect(Collectors.toList());
    }
}
