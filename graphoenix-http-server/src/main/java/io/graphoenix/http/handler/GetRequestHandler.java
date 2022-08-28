package io.graphoenix.http.handler;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import com.google.common.reflect.TypeToken;
import io.graphoenix.core.handler.GraphQLRequestHandler;
import io.graphoenix.http.codec.MimeType;
import io.graphoenix.core.dto.GraphQLRequest;
import io.graphoenix.spi.handler.ScopeEventResolver;
import io.netty.handler.codec.http.HttpResponseStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.json.JsonValue;
import jakarta.json.bind.Jsonb;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import reactor.util.context.Context;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.graphoenix.spi.constant.Hammurabi.*;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;

@ApplicationScoped
public class GetRequestHandler extends BaseRequestHandler {

    private final GraphQLRequestHandler graphQLRequestHandler;
    private final Jsonb jsonb;

    @Inject
    public GetRequestHandler(GraphQLRequestHandler graphQLRequestHandler, Jsonb jsonb) {
        this.graphQLRequestHandler = graphQLRequestHandler;
        this.jsonb = jsonb;
    }

    public Mono<Void> handle(HttpServerRequest request, HttpServerResponse response) {
        String requestId = NanoIdUtils.randomNanoId();
        Map<String, Object> context = new ConcurrentHashMap<>();
        context.put(REQUEST, request);
        context.put(RESPONSE, response);
        Type type = new TypeToken<Map<String, JsonValue>>() {
        }.getType();
        GraphQLRequest graphQLRequest = new GraphQLRequest(
                request.param("query"),
                request.param("operationName"),
                jsonb.fromJson(request.param("variables"), type)
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
