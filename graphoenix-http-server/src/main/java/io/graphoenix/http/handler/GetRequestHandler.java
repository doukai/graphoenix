package io.graphoenix.http.handler;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.graphoenix.core.context.RequestCache;
import io.graphoenix.core.handler.GraphQLRequestHandler;
import io.graphoenix.http.codec.MimeType;
import io.graphoenix.http.context.HttpRequestContext;
import io.graphoenix.http.context.HttpResponseContext;
import io.graphoenix.spi.dto.GraphQLRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vavr.CheckedRunnable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseFilter;
import org.tinylog.Logger;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import reactor.util.context.Context;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;

import static io.graphoenix.core.context.RequestCache.REQUEST_ID;
import static io.graphoenix.core.context.SessionCache.SESSION_ID;
import static io.graphoenix.core.utils.GraphQLResponseUtil.GRAPHQL_RESPONSE_UTIL;
import static io.graphoenix.http.error.HttpErrorStatusUtil.HTTP_ERROR_STATUS_UTIL;
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
                                        Mono.just(graphQLRequest)
                                                .flatMap(graphQLRequestHandler::handle)
                                                .doOnNext(jsonString ->
                                                        ServiceLoader.load(ContainerResponseFilter.class, this.getClass().getClassLoader()).stream()
                                                                .map(ServiceLoader.Provider::get)
                                                                .forEach(containerRequestFilter -> CheckedRunnable.of(() -> containerRequestFilter.filter(new HttpRequestContext(request), new HttpResponseContext(response))).unchecked().run())
                                                )
                                                .doOnSuccess(jsonString -> response.status(HttpResponseStatus.OK))
                                                .onErrorResume(throwable -> {
                                                            Logger.error(throwable);
                                                            response.status(HTTP_ERROR_STATUS_UTIL.getStatus(throwable.getClass()));
                                                            return Mono.just(GRAPHQL_RESPONSE_UTIL.error(throwable));
                                                        }
                                                )
                                                .transformDeferredContextual((mono, context) ->
                                                        mono.contextWrite(
                                                                request.param(SESSION_ID) == null ?
                                                                        Context.empty() :
                                                                        Context.of(SESSION_ID, Objects.requireNonNull(request.param(SESSION_ID)))
                                                        )
                                                )
                                                .doFirst(() ->
                                                        ServiceLoader.load(ContainerRequestFilter.class, this.getClass().getClassLoader()).stream()
                                                                .map(ServiceLoader.Provider::get)
                                                                .forEach(containerRequestFilter -> CheckedRunnable.of(() -> containerRequestFilter.filter(new HttpRequestContext(request))).unchecked().run())
                                                )
                                                .onErrorResume(throwable -> {
                                                            Logger.error(throwable);
                                                            response.status(HTTP_ERROR_STATUS_UTIL.getStatus(throwable.getClass()));
                                                            return Mono.just(GRAPHQL_RESPONSE_UTIL.error(throwable));
                                                        }
                                                )
                                )
                                .then()
                                .contextWrite(Context.of(REQUEST_ID, requestId))
                );
    }
}
