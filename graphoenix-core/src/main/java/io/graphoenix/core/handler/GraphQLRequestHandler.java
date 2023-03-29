package io.graphoenix.core.handler;

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
import org.tinylog.Logger;
import reactor.core.publisher.Mono;

import java.util.Optional;

import static io.graphoenix.core.error.GraphQLErrorType.UNSUPPORTED_OPERATION_TYPE;
import static io.graphoenix.core.utils.GraphQLResponseUtil.GRAPHQL_RESPONSE_UTIL;

@ApplicationScoped
public class GraphQLRequestHandler {

    private final QueryHandler queryHandler;
    private final MutationHandler mutationHandler;
    private final GraphQLOperationRouter graphQLOperationRouter;
    private final Provider<OperationHandler> defaultOperationHandlerProvider;

    @Inject
    public GraphQLRequestHandler(GraphQLConfig graphQLConfig, QueryHandler queryHandler, MutationHandler mutationHandler, GraphQLOperationRouter graphQLOperationRouter) {
        this.queryHandler = queryHandler;
        this.mutationHandler = mutationHandler;
        this.graphQLOperationRouter = graphQLOperationRouter;
        this.defaultOperationHandlerProvider = Optional.ofNullable(graphQLConfig.getDefaultOperationHandlerName()).map(name -> BeanContext.getProvider(OperationHandler.class, name)).orElseGet(() -> BeanContext.getProvider(OperationHandler.class));
    }

    public Mono<String> handle(GraphQLRequest requestBody) {
        return handle(defaultOperationHandlerProvider.get(), requestBody);
    }

    public Mono<String> handle(OperationHandler operationHandler, GraphQLRequest requestBody) {
        Logger.info("Handle http query:{}", requestBody.getQuery());
        OperationType type = graphQLOperationRouter.getType(requestBody.getQuery());
        switch (type) {
            case QUERY:
                return queryHandler.query(operationHandler, requestBody.getQuery(), requestBody.getVariables())
                        .map(GRAPHQL_RESPONSE_UTIL::success);
            case MUTATION:
                return mutationHandler.mutation(operationHandler, requestBody.getQuery(), requestBody.getVariables())
                        .map(GRAPHQL_RESPONSE_UTIL::success);
            default:
                throw new GraphQLErrors(UNSUPPORTED_OPERATION_TYPE);
        }
    }
}
