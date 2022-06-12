package io.graphoenix.graphql.generator.translator;

import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.error.GraphQLErrorType;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.spi.annotation.MutationOperation;
import io.graphoenix.spi.annotation.QueryOperation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Types;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class JavaElementToOperation {

    private final MethodToQueryOperation methodToQueryOperation;
    private final MethodToMutationOperation methodToMutationOperation;
    private GraphQLConfig graphQLConfig;

    @Inject
    public JavaElementToOperation(MethodToQueryOperation methodToQueryOperation,
                                  MethodToMutationOperation methodToMutationOperation,
                                  GraphQLConfig graphQLConfig) {
        this.methodToQueryOperation = methodToQueryOperation;
        this.methodToMutationOperation = methodToMutationOperation;
        this.graphQLConfig = graphQLConfig;
    }

    public void setGraphQLConfig(GraphQLConfig graphQLConfig) {
        this.graphQLConfig = graphQLConfig;
        this.methodToQueryOperation.setGraphQLConfig(graphQLConfig);
    }

    public Map<String, String> buildOperationResources(PackageElement packageElement, TypeElement typeElement, Types typeUtils) {
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
            return methodToQueryOperation.executableElementToQuery(queryOperation.value(), executableElement, queryOperation.selectionSet(), queryOperation.layers(), typeUtils);
        }
        MutationOperation mutationOperation = executableElement.getAnnotation(MutationOperation.class);
        if (mutationOperation != null) {
            return methodToMutationOperation.executableElementToMutation(mutationOperation.value(), executableElement, mutationOperation.selectionSet(), mutationOperation.layers(), typeUtils);
        }
        throw new GraphQLErrors(GraphQLErrorType.UNSUPPORTED_OPERATION_TYPE);
    }
}
