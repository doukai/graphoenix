package io.graphoenix.java.generator.implementer;

import com.squareup.javapoet.TypeSpec;
import io.graphoenix.common.manager.SimpleGraphQLDocumentManager;
import io.graphoenix.java.generator.config.CodegenConfiguration;
import io.graphoenix.spi.annotation.Mutation;
import io.graphoenix.spi.annotation.Query;
import io.graphoenix.spi.annotation.Subscription;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ExecutableType;

public class OperationInterfaceImplementer {

    private final IGraphQLDocumentManager manager;
    private final CodegenConfiguration configuration;

    public OperationInterfaceImplementer(IGraphQLDocumentManager manager, CodegenConfiguration configuration) {
        this.manager = manager;
        this.configuration = configuration;
    }

    TypeSpec buildImplementClass(TypeElement typeElement) {

        typeElement.getEnclosedElements()
                .stream().filter(element -> element.getKind().equals(ElementKind.METHOD))
                .forEach(element -> {
                    ExecutableType methodElement = (ExecutableType) element.asType();
                    Query query = methodElement.getAnnotation(Query.class);
                    Mutation mutation = methodElement.getAnnotation(Mutation.class);
                    Subscription subscription = methodElement.getAnnotation(Subscription.class);
                });
        return null;
    }
}
