package io.graphoenix.http.handler;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import io.graphoenix.core.handler.GraphQLRequestHandler;
import io.graphoenix.http.codec.MimeType;
import io.graphoenix.spi.dto.GraphQLRequest;
import io.graphoenix.spi.handler.ScopeEventResolver;
import io.netty.handler.codec.http.HttpResponseStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.json.bind.Jsonb;
import org.tinylog.Logger;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import reactor.util.context.Context;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.graphoenix.core.context.RequestScopeInstanceFactory.REQUEST_ID;
import static io.graphoenix.core.utils.GraphQLResponseUtil.GRAPHQL_RESPONSE_UTIL;
import static io.graphoenix.spi.constant.Hammurabi.GRAPHQL_REQUEST_KEY;
import static io.graphoenix.spi.constant.Hammurabi.REQUEST_KEY;
import static io.graphoenix.spi.constant.Hammurabi.RESPONSE_KEY;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;

@ApplicationScoped
public class PostRequestHandler extends BaseRequestHandler {

    private final GraphQLRequestHandler graphQLRequestHandler;
    private final Jsonb jsonb;

    @Inject
    public PostRequestHandler(GraphQLRequestHandler graphQLRequestHandler, Jsonb jsonb) {
        this.graphQLRequestHandler = graphQLRequestHandler;
        this.jsonb = jsonb;
    }

    public Mono<Void> handle(HttpServerRequest request, HttpServerResponse response) {
        String requestId = NanoIdUtils.randomNanoId();
        Map<String, Object> context = new ConcurrentHashMap<>();
        context.put(REQUEST_KEY, request);
        context.put(RESPONSE_KEY, response);
        String contentType = request.requestHeaders().get(CONTENT_TYPE);

        if (contentType.contentEquals(MimeType.Application.JSON)) {
            return response.addHeader(CONTENT_TYPE, MimeType.Application.JSON)
                    .sendString(
                            request.receive().aggregate().asString()
                                    .map(content -> jsonb.fromJson(content, GraphQLRequest.class))
                                    .doOnNext(graphQLRequest -> context.put(GRAPHQL_REQUEST_KEY, graphQLRequest))
                                    .flatMap(graphQLRequest ->
                                            ScopeEventResolver.initialized(context, RequestScoped.class)
                                                    .transformDeferredContextual((mono, contextView) -> this.sessionHandler(context, mono, contextView))
                                                    .then(graphQLRequestHandler.handle(graphQLRequest))
                                    )
                                    .doOnSuccess(jsonString -> response.status(HttpResponseStatus.OK))
                                    .onErrorResume(throwable -> this.errorHandler(throwable, response))
                    )
                    .then()
                    .contextWrite(Context.of(REQUEST_ID, requestId));
        } else if (contentType.contentEquals(MimeType.Application.GRAPHQL)) {
            return response.addHeader(CONTENT_TYPE, MimeType.Application.JSON)
                    .sendString(
                            request.receive().aggregate().asString()
                                    .map(GraphQLRequest::new)
                                    .doOnNext(graphQLRequest -> context.put(GRAPHQL_REQUEST_KEY, graphQLRequest))
                                    .flatMap(graphQLRequest ->
                                            ScopeEventResolver.initialized(context, RequestScoped.class)
                                                    .transformDeferredContextual((mono, contextView) -> this.sessionHandler(context, mono, contextView))
                                                    .then(graphQLRequestHandler.handle(graphQLRequest))
                                    )
                                    .doOnSuccess(jsonString -> response.status(HttpResponseStatus.OK))
                                    .onErrorResume(throwable -> this.errorHandler(throwable, response))
                    )
                    .then()
                    .contextWrite(Context.of(REQUEST_ID, requestId));
        } else {
            IllegalArgumentException illegalArgumentException = new IllegalArgumentException("unsupported content-type: ".concat(contentType));
            Logger.error(illegalArgumentException);
            return response.addHeader(CONTENT_TYPE, MimeType.Application.JSON)
                    .status(HttpResponseStatus.BAD_REQUEST)
                    .sendString(Mono.just(GRAPHQL_RESPONSE_UTIL.error(illegalArgumentException)))
                    .then()
                    .contextWrite(Context.of(REQUEST_ID, requestId));
        }
    }
}
