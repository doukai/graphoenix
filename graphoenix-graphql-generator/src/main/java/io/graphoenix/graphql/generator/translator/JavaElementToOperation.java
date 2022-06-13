package io.graphoenix.graphql.generator.translator;

import io.graphoenix.core.error.GraphQLErrorType;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.spi.annotation.MutationOperation;
import io.graphoenix.spi.annotation.QueryOperation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Types;
import java.util.Map;
import java.util.stream.Collectors;

import static io.graphoenix.spi.dto.type.OperationType.MUTATION;
import static io.graphoenix.spi.dto.type.OperationType.QUERY;

@ApplicationScoped
public class JavaElementToOperation {

    private final MethodToOperation methodToOperation;

    @Inject
    public JavaElementToOperation(MethodToOperation methodToOperation) {
        this.methodToOperation = methodToOperation;
    }

    public Map<String, String> buildOperationResources(TypeElement typeElement, Types typeUtils) {
        return typeElement.getEnclosedElements().stream()
                .filter(element -> element.getKind().equals(ElementKind.METHOD))
                .collect(Collectors.toMap(
                                element -> element.getSimpleName().toString()
                                        .concat("_" + typeElement.getEnclosedElements().indexOf(element)),
                                element -> executableElementToOperation((ExecutableElement) element, typeUtils)
                        )
                );
    }

    private String executableElementToOperation(ExecutableElement executableElement, Types typeUtils) {
        QueryOperation queryOperation = executableElement.getAnnotation(QueryOperation.class);
        if (queryOperation != null) {
            return methodToOperation.executableElementToOperation(QUERY, queryOperation.value(), executableElement, queryOperation.selectionSet(), queryOperation.layers(), typeUtils);
        }
        MutationOperation mutationOperation = executableElement.getAnnotation(MutationOperation.class);
        if (mutationOperation != null) {
            return methodToOperation.executableElementToOperation(MUTATION, mutationOperation.value(), executableElement, mutationOperation.selectionSet(), mutationOperation.layers(), typeUtils);
        }
        throw new GraphQLErrors(GraphQLErrorType.UNSUPPORTED_OPERATION_TYPE);
    }
}
