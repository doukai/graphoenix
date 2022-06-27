package io.graphoenix.http.handler;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.graphoenix.core.context.RequestCache;
import io.graphoenix.core.handler.GraphQLRequestHandler;
import io.graphoenix.http.codec.MimeType;
import io.graphoenix.spi.dto.GraphQLRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.tinylog.Logger;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import reactor.util.context.Context;

import java.lang.reflect.Type;
import java.util.Map;

import static io.graphoenix.core.context.RequestCache.REQUEST_ID;
import static io.graphoenix.core.utils.GraphQLResponseUtil.GRAPHQL_RESPONSE_UTIL;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;

@ApplicationScoped
public class GetRequestHandler {

    private final GsonBuilder gsonBuilder = new GsonBuilder();
    private final GraphQLRequestHandler graphQLRequestHandler;

    @Inject
    public GetRequestHandler(GraphQLRequestHandler graphQLRequestHandler) {
        this.graphQLRequestHandler = graphQLRequestHandler;
    }

    public Mono<Void> handle(HttpServerRequest request, HttpServerResponse response) {
        String requestId = NanoIdUtils.randomNanoId();
        Type type = new TypeToken<Map<String, String>>() {
        }.getType();
        GraphQLRequest graphQLRequest = new GraphQLRequest(
                request.param("query"),
                request.param("operationName"),
                gsonBuilder.create().fromJson(request.param("variables"), type)
        );
        return RequestCache.putIfAbsent(requestId, HttpServerRequest.class, request)
                .thenEmpty(
                        response.addHeader(CONTENT_TYPE, MimeType.Application.JSON)
                                .sendString(
                                        graphQLRequestHandler.handle(graphQLRequest)
                                                .doOnSuccess(jsonString -> response.status(HttpResponseStatus.OK))
                                                .onErrorResume(throwable -> {
                                                            Logger.error(throwable);
                                                            response.status(HttpResponseStatus.BAD_REQUEST);
                                                            return Mono.just(GRAPHQL_RESPONSE_UTIL.error(throwable));
                                                        }
                                                )
                                )
                                .then()
                                .contextWrite(Context.of(REQUEST_ID, requestId))
                );
    }
}
