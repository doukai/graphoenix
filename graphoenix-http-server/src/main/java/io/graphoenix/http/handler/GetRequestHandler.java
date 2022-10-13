package io.graphoenix.http.handler;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import io.graphoenix.core.dto.GraphQLRequest;
import io.graphoenix.core.handler.GraphQLRequestHandler;
import io.graphoenix.http.codec.MimeType;
import io.graphoenix.spi.handler.ScopeEventResolver;
import io.netty.handler.codec.http.HttpResponseStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.json.spi.JsonProvider;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import reactor.util.context.Context;

import java.io.StringReader;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static io.graphoenix.spi.constant.Hammurabi.GRAPHQL_REQUEST;
import static io.graphoenix.spi.constant.Hammurabi.REQUEST;
import static io.graphoenix.spi.constant.Hammurabi.REQUEST_ID;
import static io.graphoenix.spi.constant.Hammurabi.RESPONSE;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;

@ApplicationScoped
public class GetRequestHandler extends BaseRequestHandler {

    private final GraphQLRequestHandler graphQLRequestHandler;
    private final JsonProvider jsonProvider;

    @Inject
    public GetRequestHandler(GraphQLRequestHandler graphQLRequestHandler, JsonProvider jsonProvider) {
        this.graphQLRequestHandler = graphQLRequestHandler;
        this.jsonProvider = jsonProvider;
    }

    public Mono<Void> handle(HttpServerRequest request, HttpServerResponse response) {
        String requestId = NanoIdUtils.randomNanoId();
        Map<String, Object> context = new ConcurrentHashMap<>();
        context.put(REQUEST, request);
        context.put(RESPONSE, response);
        GraphQLRequest graphQLRequest = new GraphQLRequest(
                request.param("query"),
                request.param("operationName"),
                request.param("variables") == null ? null : jsonProvider.createReader(new StringReader(Objects.requireNonNull(request.param("variables")))).readObject()
        );
        context.put(GRAPHQL_REQUEST, graphQLRequest);

        return response.addHeader(CONTENT_TYPE, MimeType.Application.JSON)
                .sendString(
                        ScopeEventResolver.initialized(context, RequestScoped.class)
                                .transformDeferredContextual((mono, contextView) -> this.sessionHandler(context, mono, contextView))
                                .then(graphQLRequestHandler.handle(graphQLRequest))
                                .doOnSuccess(jsonString -> response.status(HttpResponseStatus.OK))
                                .onErrorResume(throwable -> this.errorHandler(throwable, response))
                )
                .then()
                .contextWrite(Context.of(REQUEST_ID, requestId));
    }
}
