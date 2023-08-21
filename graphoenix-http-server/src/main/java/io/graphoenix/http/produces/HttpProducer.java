package io.graphoenix.http.produces;

import io.graphoenix.core.context.RequestScopeInstanceFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

@ApplicationScoped
public class HttpProducer {

    @Produces
    @RequestScoped
    public Mono<HttpServerRequest> httpServerRequestMono() {
        return RequestScopeInstanceFactory.get(HttpServerRequest.class);
    }

    @Produces
    @RequestScoped
    public Mono<HttpServerResponse> httpServerResponseMono() {
        return RequestScopeInstanceFactory.get(HttpServerResponse.class);
    }
}
