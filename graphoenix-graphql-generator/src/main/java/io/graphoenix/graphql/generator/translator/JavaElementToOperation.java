package io.graphoenix.graphql.generator.translator;

import io.graphoenix.graphql.generator.document.InputObjectType;
import io.graphoenix.graphql.generator.operation.Field;
import io.graphoenix.graphql.generator.operation.Operation;
import io.graphoenix.spi.annotation.Mutation;
import io.graphoenix.spi.annotation.Query;
import io.graphoenix.spi.annotation.Subscription;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.config.JavaGeneratorConfig;

import javax.lang.model.element.*;
import javax.lang.model.type.ExecutableType;
import java.util.List;
import java.util.Optional;
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
            return executableElementToQuery(executableElement);
        }
        return null;
    }

    private String executableElementToQuery(ExecutableElement executableElement) {
        Operation operation = new Operation();

        Optional<? extends AnnotationMirror> expressions = executableElement.getAnnotationMirrors().stream()
                .filter(annotationMirror ->
                        annotationMirror.getAnnotationType().asElement().getEnclosedElements().stream()
                                .map(element -> (ExecutableElement) element)
                                .anyMatch(filedElement -> filedElement.getReturnType().toString().contains(".Conditional"))
                )
                .findFirst();
        if (expressions.isPresent()) {
            AnnotationMirror annotationMirror = expressions.get();

        }
        Optional<? extends AnnotationMirror> expression = executableElement.getAnnotationMirrors().stream()
                .filter(annotationMirror ->
                        annotationMirror.getAnnotationType().asElement().getEnclosedElements().stream()
                                .map(element -> (ExecutableElement) element)
                                .anyMatch(filedElement -> filedElement.getReturnType().toString().contains(".Operator"))
                )
                .findFirst();
        if (expression.isPresent()) {
            AnnotationMirror annotationMirror = expression.get();

        }
        return operation.toString();
    }

    private InputObjectType expressionsToInput(AnnotationMirror expressions) {

        return null;
    }
}
