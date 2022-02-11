package io.graphoenix.http.produces;

import io.graphoenix.core.manager.GraphQLOperationRouter;
import io.graphoenix.http.handler.GetRequestHandler;
import io.graphoenix.http.handler.PostRequestHandler;
import io.graphoenix.http.server.GraphqlHttpServerHandler;
import io.graphoenix.spi.handler.MutationHandler;
import io.graphoenix.spi.handler.QueryHandler;
import io.netty.handler.codec.http.HttpMethod;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import java.util.Map;

@ApplicationScoped
public class HttpServerProduces {

    private final GetRequestHandler getRequestHandler;

    private final PostRequestHandler postRequestHandler;

    private final GraphQLOperationRouter graphQLOperationRouter;

    private final QueryHandler queryHandler;

    private final MutationHandler mutationHandler;

    @Inject
    public HttpServerProduces(GetRequestHandler getRequestHandler,
                              PostRequestHandler postRequestHandler,
                              GraphQLOperationRouter graphQLOperationRouter,
                              QueryHandler queryHandler,
                              MutationHandler mutationHandler) {
        this.getRequestHandler = getRequestHandler;
        this.postRequestHandler = postRequestHandler;
        this.graphQLOperationRouter = graphQLOperationRouter;
        this.queryHandler = queryHandler;
        this.mutationHandler = mutationHandler;
    }

    @Produces
    public GraphqlHttpServerHandler graphqlHttpServerHandler() {
        return new GraphqlHttpServerHandler(
                Map.of(
                        HttpMethod.GET, getRequestHandler,
                        HttpMethod.POST, postRequestHandler
                ),
                graphQLOperationRouter,
                queryHandler,
                mutationHandler
        );
    }
}
