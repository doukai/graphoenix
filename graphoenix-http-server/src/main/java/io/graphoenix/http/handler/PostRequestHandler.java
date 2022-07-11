package io.graphoenix.http.handler;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import com.google.gson.GsonBuilder;
import io.graphoenix.core.handler.GraphQLRequestHandler;
import io.graphoenix.http.codec.MimeType;
import io.graphoenix.spi.dto.GraphQLRequest;
import io.graphoenix.spi.handler.ScopeEventResolver;
import io.netty.handler.codec.http.HttpResponseStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import org.tinylog.Logger;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import reactor.util.context.Context;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.graphoenix.core.context.RequestScopeInstanceFactory.REQUEST_ID;
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
            return response.addHeader(CONTENT_TYPE, MimeType.Application.JSON)
                    .sendString(
                            ScopeEventResolver.initialized(properties, RequestScoped.class)
                                    .transformDeferredContextual((mono, context) -> this.sessionHandler(properties, mono, context))
                                    .then(request.receive().aggregate().asString()
                                            .map(content -> gsonBuilder.create().fromJson(content, GraphQLRequest.class))
                                            .flatMap(graphQLRequestHandler::handle)
                                    )
                                    .doOnSuccess(jsonString -> response.status(HttpResponseStatus.OK))
                                    .onErrorResume(throwable -> this.errorHandler(throwable, response))
                    )
                    .then()
                    .contextWrite(Context.of(REQUEST_ID, requestId));
        } else if (contentType.contentEquals(MimeType.Application.GRAPHQL)) {
            return response.addHeader(CONTENT_TYPE, MimeType.Application.JSON)
                    .sendString(
                            ScopeEventResolver.initialized(properties, RequestScoped.class)
                                    .transformDeferredContextual((mono, context) -> this.sessionHandler(properties, mono, context))
                                    .then(request.receive().aggregate().asString()
                                            .map(GraphQLRequest::new)
                                            .flatMap(graphQLRequestHandler::handle)
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
