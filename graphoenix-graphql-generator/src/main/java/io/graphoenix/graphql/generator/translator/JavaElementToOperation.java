package io.graphoenix.graphql.generator.translator;

import io.graphoenix.spi.annotation.MutationOperation;
import io.graphoenix.spi.annotation.QueryOperation;

import javax.inject.Inject;
import javax.lang.model.element.*;
import java.util.Map;
import java.util.stream.Collectors;

public class JavaElementToOperation {

    private final MethodToQueryOperation methodToQueryOperation;
    private final MethodToMutationOperation methodToMutationOperation;

    @Inject
    public JavaElementToOperation(MethodToQueryOperation methodToQueryOperation,
                                  MethodToMutationOperation methodToMutationOperation) {
        this.methodToQueryOperation = methodToQueryOperation;
        this.methodToMutationOperation = methodToMutationOperation;
    }

    public Map<String, String> buildOperationResources(PackageElement packageElement, TypeElement typeElement) {
        return typeElement.getEnclosedElements().stream()
                .filter(element -> element.getKind().equals(ElementKind.METHOD))
                .collect(Collectors.toMap(
                                element -> element.getSimpleName().toString()
                                        .concat("_" + typeElement.getEnclosedElements().indexOf(element)),
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
