package io.graphoenix.graphql.generator.translator;

import io.graphoenix.spi.annotation.MutationOperation;
import io.graphoenix.spi.annotation.QueryOperation;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.config.JavaGeneratorConfig;

import javax.lang.model.element.*;
import java.util.Map;
import java.util.stream.Collectors;

public class JavaElementToOperation {

    private final IGraphQLDocumentManager manager;
    private final JavaGeneratorConfig configuration;
    private final MethodToQueryOperation methodToQueryOperation;
    private final MethodToMutationOperation methodToMutationOperation;

    public JavaElementToOperation(IGraphQLDocumentManager manager, JavaGeneratorConfig configuration) {
        this.manager = manager;
        this.configuration = configuration;
        methodToQueryOperation = new MethodToQueryOperation(manager, configuration);
        methodToMutationOperation = new MethodToMutationOperation(manager, configuration);
    }

    public Map<String, String> buildOperationResources(PackageElement packageElement, TypeElement typeElement) {
        return typeElement.getEnclosedElements().stream()
                .filter(element -> element.getKind().equals(ElementKind.METHOD))
                .collect(Collectors.toMap(
                        element -> element.getSimpleName().toString(),
                        element -> executableElementToOperation((ExecutableElement) element)
                        )
                );
    }

    private String executableElementToOperation(ExecutableElement executableElement) {
        QueryOperation queryOperation = executableElement.getAnnotation(QueryOperation.class);
        if (queryOperation != null) {
            return methodToQueryOperation.executableElementToQuery(queryOperation.value(), executableElement, queryOperation.layers());
        }
        MutationOperation mutationOperation = executableElement.getAnnotation(MutationOperation.class);
        if (mutationOperation != null) {
            return methodToMutationOperation.executableElementToMutation(mutationOperation.value(), executableElement, mutationOperation.layers());
        }
        return null;
    }
}