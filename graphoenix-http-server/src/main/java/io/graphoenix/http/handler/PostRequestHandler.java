package io.graphoenix.http.handler;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import com.google.gson.GsonBuilder;
import io.graphoenix.core.context.RequestScopeInstanceFactory;
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.graphoenix.core.context.RequestScopeInstanceFactory.REQUEST_ID;
import static io.graphoenix.core.context.SessionScopeInstanceFactory.SESSION_ID;
import static io.graphoenix.core.utils.GraphQLResponseUtil.GRAPHQL_RESPONSE_UTIL;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;

@ApplicationScoped
public class PostRequestHandler extends BaseRequestHandler {

    private final GsonBuilder gsonBuilder = new GsonBuilder();
    private final GraphQLRequestHandler graphQLRequestHandler;

    @Inject
    public PostRequestHandler(GraphQLRequestHandler graphQLRequestHandler) {
        this.graphQLRequestHandler = graphQLRequestHandler;
    }

    public Mono<Void> handle(HttpServerRequest request, HttpServerResponse response) {
        String requestId = NanoIdUtils.randomNanoId();
        Map<String, Object> properties = new ConcurrentHashMap<>();
        String contentType = request.requestHeaders().get(CONTENT_TYPE);

        if (contentType.contentEquals(MimeType.Application.JSON)) {
            return RequestScopeInstanceFactory.putIfAbsent(requestId, HttpServerRequest.class, request)
                    .thenEmpty(
                            response.addHeader(CONTENT_TYPE, MimeType.Application.JSON)
                                    .sendString(
                                            request.receive().aggregate().asString()
                                                    .map(content -> gsonBuilder.create().fromJson(content, GraphQLRequest.class))
                                                    .flatMap(graphQLRequest ->
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
                                    )
                                    .then()
                                    .contextWrite(Context.of(REQUEST_ID, requestId))
                    );
        } else if (contentType.contentEquals(MimeType.Application.GRAPHQL)) {
            return RequestScopeInstanceFactory.putIfAbsent(requestId, HttpServerRequest.class, request)
                    .thenEmpty(
                            response.addHeader(CONTENT_TYPE, MimeType.Application.JSON)
                                    .sendString(
                                            request.receive().aggregate().asString()
                                                    .map(GraphQLRequest::new)
                                                    .flatMap(graphQLRequest ->
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
                                    )
                                    .then()
                                    .contextWrite(Context.of(REQUEST_ID, requestId))
                    );
        } else {
            IllegalArgumentException illegalArgumentException = new IllegalArgumentException("unsupported content-type: ".concat(contentType));
            Logger.error(illegalArgumentException);
            return RequestScopeInstanceFactory.putIfAbsent(requestId, HttpServerRequest.class, request)
                    .thenEmpty(
                            response.addHeader(CONTENT_TYPE, MimeType.Application.JSON)
                                    .status(HttpResponseStatus.BAD_REQUEST)
                                    .sendString(Mono.just(GRAPHQL_RESPONSE_UTIL.error(illegalArgumentException)))
                                    .then()
                                    .contextWrite(Context.of(REQUEST_ID, requestId))
                    );
        }
    }
}
