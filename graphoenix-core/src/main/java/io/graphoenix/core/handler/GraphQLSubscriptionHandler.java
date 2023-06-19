package io.graphoenix.core.handler;

import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.core.dto.GraphQLRequest;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.spi.dto.type.OperationType;
import io.graphoenix.spi.handler.OperationHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.tinylog.Logger;
import reactor.core.publisher.Flux;

import java.util.Optional;

import static io.graphoenix.core.error.GraphQLErrorType.UNSUPPORTED_OPERATION_TYPE;
import static io.graphoenix.core.utils.GraphQLResponseUtil.GRAPHQL_RESPONSE_UTIL;

@ApplicationScoped
public class GraphQLSubscriptionHandler {

    private final OperationSubscriber operationSubscriber;
    private final GraphQLOperationRouter graphQLOperationRouter;
    private final Provider<OperationHandler> defaultOperationHandlerProvider;

    @Inject
    public GraphQLSubscriptionHandler(GraphQLConfig graphQLConfig, OperationSubscriber operationSubscriber, GraphQLOperationRouter graphQLOperationRouter) {
        this.operationSubscriber = operationSubscriber;
        this.graphQLOperationRouter = graphQLOperationRouter;
        this.defaultOperationHandlerProvider = Optional.ofNullable(graphQLConfig.getDefaultOperationHandlerName()).map(name -> BeanContext.getProvider(OperationHandler.class, name)).orElseGet(() -> BeanContext.getProvider(OperationHandler.class));
    }

    public Flux<String> handle(GraphQLRequest requestBody) {
        return handle(defaultOperationHandlerProvider.get(), requestBody);
    }

    public Flux<String> handle(OperationHandler operationHandler, GraphQLRequest requestBody) {
        Logger.info("Handle websocket subscription:{}", requestBody.getQuery());
        OperationType type = graphQLOperationRouter.getType(requestBody.getQuery());
        if (type == OperationType.SUBSCRIPTION) {
            return operationSubscriber.subscriptionOperation(operationHandler, requestBody.getQuery(), requestBody.getVariables())
                    .map(GRAPHQL_RESPONSE_UTIL::next);
        }
        throw new GraphQLErrors(UNSUPPORTED_OPERATION_TYPE);
    }
}
