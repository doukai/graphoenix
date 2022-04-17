package io.graphoenix.http.handler;

import io.graphoenix.core.error.GraphQLProblem;
import io.graphoenix.core.manager.GraphQLOperationRouter;
import io.graphoenix.spi.dto.GraphQLRequest;
import io.graphoenix.spi.dto.type.OperationType;
import io.graphoenix.spi.handler.MutationHandler;
import io.graphoenix.spi.handler.QueryHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.tinylog.Logger;
import reactor.core.publisher.Mono;

import static io.graphoenix.core.error.GraphQLErrorType.UNSUPPORTED_OPERATION_TYPE;
import static io.graphoenix.core.utils.GraphQLResponseUtil.GRAPHQL_RESPONSE_UTIL;

@ApplicationScoped
public class GraphQLRequestHandler {

    private final QueryHandler queryHandler;
    private final MutationHandler mutationHandler;
    private final GraphQLOperationRouter graphQLOperationRouter;

    @Inject
    public GraphQLRequestHandler(QueryHandler queryHandler, MutationHandler mutationHandler, GraphQLOperationRouter graphQLOperationRouter) {
        this.queryHandler = queryHandler;
        this.mutationHandler = mutationHandler;
        this.graphQLOperationRouter = graphQLOperationRouter;
    }

    public Mono<String> handle(GraphQLRequest requestBody) {
        Logger.info("Handle http query:{}", requestBody.getQuery());
        OperationType type = graphQLOperationRouter.getType(requestBody.getQuery());
        switch (type) {
            case QUERY:
                return queryHandler.query(requestBody.getQuery(), requestBody.getVariables())
                        .map(GRAPHQL_RESPONSE_UTIL::success);
            case MUTATION:
                return mutationHandler.mutation(requestBody.getQuery(), requestBody.getVariables())
                        .map(GRAPHQL_RESPONSE_UTIL::success);
            default:
                throw new GraphQLProblem(UNSUPPORTED_OPERATION_TYPE);
        }
    }
}
