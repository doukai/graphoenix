package io.graphoenix.http.handler;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import com.google.gson.GsonBuilder;
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

import java.util.Objects;
import java.util.ServiceLoader;

import static io.graphoenix.core.context.RequestCache.REQUEST_ID;
import static io.graphoenix.core.context.SessionCache.SESSION_ID;
import static io.graphoenix.core.utils.GraphQLResponseUtil.GRAPHQL_RESPONSE_UTIL;
import static io.graphoenix.http.error.HttpErrorStatusUtil.HTTP_ERROR_STATUS_UTIL;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;

@ApplicationScoped
public class PostRequestHandler {

    private final GsonBuilder gsonBuilder = new GsonBuilder();
    private final GraphQLRequestHandler graphQLRequestHandler;

    @Inject
    public PostRequestHandler(GraphQLRequestHandler graphQLRequestHandler) {
        this.graphQLRequestHandler = graphQLRequestHandler;
    }

    public Mono<Void> handle(HttpServerRequest request, HttpServerResponse response) {
        String requestId = NanoIdUtils.randomNanoId();
        String contentType = request.requestHeaders().get(CONTENT_TYPE);

        if (contentType.contentEquals(MimeType.Application.JSON)) {
            return RequestCache.putIfAbsent(requestId, HttpServerRequest.class, request)
                    .thenEmpty(
                            response.addHeader(CONTENT_TYPE, MimeType.Application.JSON)
                                    .sendString(
                                            request.receive().aggregate().asString()
                                                    .flatMap(content ->
                                                            Mono.just(gsonBuilder.create().fromJson(content, GraphQLRequest.class))
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
                                    )
                                    .then()
                                    .contextWrite(Context.of(REQUEST_ID, requestId))
                    );
        } else if (contentType.contentEquals(MimeType.Application.GRAPHQL)) {
            return RequestCache.putIfAbsent(requestId, HttpServerRequest.class, request)
                    .thenEmpty(
                            response.addHeader(CONTENT_TYPE, MimeType.Application.JSON)
                                    .sendString(
                                            request.receive().aggregate().asString()
                                                    .flatMap(content ->
                                                            Mono.just(new GraphQLRequest(content))
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
                                    )
                                    .then()
                                    .contextWrite(Context.of(REQUEST_ID, requestId))
                    );
        } else {
            IllegalArgumentException illegalArgumentException = new IllegalArgumentException("unsupported content-type: ".concat(contentType));
            Logger.error(illegalArgumentException);
            return RequestCache.putIfAbsent(requestId, HttpServerRequest.class, request)
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
