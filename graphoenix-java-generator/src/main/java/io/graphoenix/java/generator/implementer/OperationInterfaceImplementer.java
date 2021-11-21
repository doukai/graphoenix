package io.graphoenix.java.generator.implementer;

import com.squareup.javapoet.TypeSpec;
import io.graphoenix.graphql.generator.operation.Field;
import io.graphoenix.graphql.generator.operation.Operation;
import io.graphoenix.spi.config.JavaGeneratorConfig;
import io.graphoenix.spi.annotation.Mutation;
import io.graphoenix.spi.annotation.Query;
import io.graphoenix.spi.annotation.Subscription;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ExecutableType;
import java.util.Optional;

public class OperationInterfaceImplementer {

    private final IGraphQLDocumentManager manager;
    private final JavaGeneratorConfig configuration;

    public OperationInterfaceImplementer(IGraphQLDocumentManager manager, JavaGeneratorConfig configuration) {
        this.manager = manager;
        this.configuration = configuration;
    }

    public TypeSpec buildImplementClass(TypeElement typeElement) {

        typeElement.getEnclosedElements()
                .stream().filter(element -> element.getKind().equals(ElementKind.METHOD))
                .map(element -> {
                    ExecutableType methodElement = (ExecutableType) element.asType();

                    Query query = methodElement.getAnnotation(Query.class);
                    if (query != null) {
                        return javaTypeToGraphQLQuery(methodElement);
                    }
                    Mutation mutation = methodElement.getAnnotation(Mutation.class);
                    Subscription subscription = methodElement.getAnnotation(Subscription.class);
                    return null;
                });
        return null;
    }

    private String javaTypeToGraphQLQuery(ExecutableType executableType) {
        Operation operation = new Operation();

        Query query = executableType.getAnnotation(Query.class);
        Field field = new Field().setName(query.value());
        Optional<? extends AnnotationMirror> expressions = executableType.getAnnotationMirrors().stream()
                .filter(annotationMirror ->
                        annotationMirror.getElementValues().entrySet().stream()
                                .anyMatch(entry -> entry.getKey().getReturnType().getClass().getName().equals("Conditional")))
                .findFirst();

        return operation.toString();
    }
}
