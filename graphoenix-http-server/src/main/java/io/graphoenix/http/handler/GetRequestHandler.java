package io.graphoenix.http.handler;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.graphoenix.core.handler.GraphQLRequestHandler;
import io.graphoenix.http.codec.MimeType;
import io.graphoenix.spi.dto.GraphQLRequest;
import io.graphoenix.spi.handler.ScopeEventResolver;
import io.netty.handler.codec.http.HttpResponseStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import reactor.util.context.Context;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.graphoenix.core.context.RequestScopeInstanceFactory.REQUEST_ID;
import static io.graphoenix.spi.constant.Hammurabi.GRAPHQL_REQUEST_KEY;
import static io.graphoenix.spi.constant.Hammurabi.REQUEST_KEY;
import static io.graphoenix.spi.constant.Hammurabi.RESPONSE_KEY;
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
        Map<String, Object> context = new ConcurrentHashMap<>();
        context.put(REQUEST_KEY, request);
        context.put(RESPONSE_KEY, response);
        Type type = new TypeToken<Map<String, String>>() {
        }.getType();
        GraphQLRequest graphQLRequest = new GraphQLRequest(
                request.param("query"),
                request.param("operationName"),
                gsonBuilder.create().fromJson(request.param("variables"), type)
        );
        context.put(GRAPHQL_REQUEST_KEY, graphQLRequest);

        return response.addHeader(CONTENT_TYPE, MimeType.Application.JSON)
                .sendString(
                        ScopeEventResolver.initialized(context, RequestScoped.class)
                                .transformDeferredContextual((mono, contextView) -> this.sessionHandler(context, mono, contextView))
                                .then(Mono.just(graphQLRequest).flatMap(graphQLRequestHandler::handle))
                                .doOnSuccess(jsonString -> response.status(HttpResponseStatus.OK))
                                .onErrorResume(throwable -> this.errorHandler(throwable, response))
                )
                .then()
                .contextWrite(Context.of(REQUEST_ID, requestId));
    }
}
