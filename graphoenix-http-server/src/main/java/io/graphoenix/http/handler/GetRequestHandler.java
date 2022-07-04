package io.graphoenix.http.handler;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.graphoenix.core.context.RequestInstanceFactory;
import io.graphoenix.core.handler.GraphQLRequestHandler;
import io.graphoenix.http.codec.MimeType;
import io.graphoenix.spi.dto.GraphQLRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import reactor.util.context.Context;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.graphoenix.core.context.RequestInstanceFactory.REQUEST_ID;
import static io.graphoenix.core.context.SessionInstanceFactory.SESSION_ID;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;

@ApplicationScoped
public class GetRequestHandler extends BaseRequestHandler {

    private final GsonBuilder gsonBuilder = new GsonBuilder();
    private final GraphQLRequestHandler graphQLRequestHandler;

    @Inject
    public GetRequestHandler(GraphQLRequestHandler graphQLRequestHandler) {
        this.graphQLRequestHandler = graphQLRequestHandler;
    }

    public Mono<Void> handle(HttpServerRequest request, HttpServerResponse response) {
        String requestId = NanoIdUtils.randomNanoId();
        Map<String, Object> properties = new ConcurrentHashMap<>();
        Type type = new TypeToken<Map<String, String>>() {
        }.getType();
        GraphQLRequest graphQLRequest = new GraphQLRequest(
                request.param("query"),
                request.param("operationName"),
                gsonBuilder.create().fromJson(request.param("variables"), type)
        );

        return RequestInstanceFactory.putIfAbsent(requestId, HttpServerRequest.class, request)
                .thenEmpty(
                        response.addHeader(CONTENT_TYPE, MimeType.Application.JSON)
                                .sendString(
                                        Mono.just(graphQLRequest)
                                                .flatMap(graphQLRequestHandler::handle)
                                                .doOnNext(jsonString -> this.afterHandler(request, response, properties, jsonString))
                                                .doOnSuccess(jsonString -> response.status(HttpResponseStatus.OK))
                                                .onErrorResume(throwable -> this.errorHandler(throwable, response))
                                                .transformDeferredContextual((mono, context) ->
                                                        properties.containsKey(SESSION_ID) ?
                                                                mono.contextWrite(Context.of(SESSION_ID, properties.get(SESSION_ID))) :
                                                                mono
                                                )
                                                .doFirst(() -> this.beforeHandler(request, properties, graphQLRequest))
                                                .onErrorResume(throwable -> this.errorHandler(throwable, response))
                                )
                                .then()
                                .contextWrite(Context.of(REQUEST_ID, requestId))
                );
    }
}
