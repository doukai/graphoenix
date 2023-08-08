package io.graphoenix.core.handler;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.core.dto.GraphQLRequest;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.spi.dto.type.OperationType;
import io.graphoenix.spi.handler.MutationHandler;
import io.graphoenix.spi.handler.OperationHandler;
import io.graphoenix.spi.handler.QueryHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import reactor.core.publisher.Mono;

import java.util.Optional;

import static io.graphoenix.core.error.GraphQLErrorType.UNSUPPORTED_OPERATION_TYPE;
import static io.graphoenix.core.utils.GraphQLResponseUtil.GRAPHQL_RESPONSE_UTIL;

@ApplicationScoped
public class GraphQLRequestHandler {

    private final OperationPreprocessor operationPreprocessor;
    private final QueryHandler queryHandler;
    private final MutationHandler mutationHandler;
    private final GraphQLOperationRouter graphQLOperationRouter;
    private final Provider<OperationHandler> defaultOperationHandlerProvider;

    @Inject
    public GraphQLRequestHandler(GraphQLConfig graphQLConfig, OperationPreprocessor operationPreprocessor, QueryHandler queryHandler, MutationHandler mutationHandler, GraphQLOperationRouter graphQLOperationRouter) {
        this.operationPreprocessor = operationPreprocessor;
        this.queryHandler = queryHandler;
        this.mutationHandler = mutationHandler;
        this.graphQLOperationRouter = graphQLOperationRouter;
        this.defaultOperationHandlerProvider = Optional.ofNullable(graphQLConfig.getDefaultOperationHandlerName()).map(name -> BeanContext.getProvider(OperationHandler.class, name)).orElseGet(() -> BeanContext.getProvider(OperationHandler.class));
    }

    public Mono<String> handle(GraphQLRequest graphQLRequest) {
        return handle(defaultOperationHandlerProvider.get(), operationPreprocessor.preprocess(graphQLRequest.getQuery(), graphQLRequest.getVariables()));
    }

    public Mono<String> handle(GraphqlParser.OperationDefinitionContext operationDefinitionContext) {
        return handle(defaultOperationHandlerProvider.get(), operationDefinitionContext);
    }

    public Mono<String> handle(OperationHandler operationHandler, GraphqlParser.OperationDefinitionContext operationDefinitionContext) {
        OperationType type = graphQLOperationRouter.getType(operationDefinitionContext);
        switch (type) {
            case QUERY:
                return queryHandler.query(operationHandler, operationDefinitionContext)
                        .map(GRAPHQL_RESPONSE_UTIL::success);
            case MUTATION:
                return mutationHandler.mutation(operationHandler, operationDefinitionContext)
                        .map(GRAPHQL_RESPONSE_UTIL::success);
            default:
                throw new GraphQLErrors(UNSUPPORTED_OPERATION_TYPE);
        }
    }
}
