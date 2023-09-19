package io.graphoenix.core.handler;

import io.graphoenix.core.dto.GraphQLRequest;
import io.graphoenix.spi.handler.FetchHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import reactor.core.publisher.Mono;

@ApplicationScoped
@Named("local")
public class LocalFetchHandler implements FetchHandler {

    private final GraphQLRequestHandler graphQLRequestHandler;

    @Inject
    public LocalFetchHandler(GraphQLRequestHandler graphQLRequestHandler) {
        this.graphQLRequestHandler = graphQLRequestHandler;
    }

    @Override
    public Mono<String> request(String packageName, String graphql) {
        return graphQLRequestHandler.handle(new GraphQLRequest(graphql));
    }
}
