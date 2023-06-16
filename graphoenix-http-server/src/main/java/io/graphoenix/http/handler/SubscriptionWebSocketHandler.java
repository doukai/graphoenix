package io.graphoenix.http.handler;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import io.graphoenix.core.dto.GraphQLRequest;
import io.graphoenix.core.handler.GraphQLSubscriptionHandler;
import io.graphoenix.http.codec.MimeType;
import io.graphoenix.spi.handler.ScopeEventResolver;
import io.netty.handler.codec.http.HttpResponseStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.json.spi.JsonProvider;
import org.tinylog.Logger;
import reactor.core.publisher.Mono;
import reactor.netty.Connection;
import reactor.netty.http.websocket.WebsocketInbound;
import reactor.netty.http.websocket.WebsocketOutbound;
import reactor.util.context.Context;

import java.io.StringReader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.graphoenix.core.utils.GraphQLResponseUtil.GRAPHQL_RESPONSE_UTIL;
import static io.graphoenix.spi.constant.Hammurabi.GRAPHQL_REQUEST;
import static io.graphoenix.spi.constant.Hammurabi.INBOUND;
import static io.graphoenix.spi.constant.Hammurabi.OUTBOUND;
import static io.graphoenix.spi.constant.Hammurabi.REQUEST_ID;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;

@ApplicationScoped
public class SubscriptionWebSocketHandler extends BaseHandler {

    private final GraphQLSubscriptionHandler graphQLSubscriptionHandler;
    private final JsonProvider jsonProvider;

    @Inject
    public SubscriptionWebSocketHandler(GraphQLSubscriptionHandler graphQLSubscriptionHandler, JsonProvider jsonProvider) {
        this.graphQLSubscriptionHandler = graphQLSubscriptionHandler;
        this.jsonProvider = jsonProvider;
    }

    public Mono<Void> handle(WebsocketInbound inbound, WebsocketOutbound outbound) {
        String requestId = NanoIdUtils.randomNanoId();
        Map<String, Object> context = new ConcurrentHashMap<>();
        context.put(INBOUND, inbound);
        context.put(OUTBOUND, outbound);
        String contentType = inbound.headers().get(CONTENT_TYPE);

        if (contentType.contentEquals(MimeType.Application.JSON)) {
            return inbound.receive().aggregate().asString()
                    .map(content -> GraphQLRequest.fromJson(jsonProvider.createReader(new StringReader(content)).readObject()))
                    .doOnNext(graphQLRequest -> context.put(GRAPHQL_REQUEST, graphQLRequest))
                    .flatMap(graphQLRequest ->
                            ScopeEventResolver.initialized(context, RequestScoped.class)
                                    .transformDeferredContextual((mono, contextView) -> this.sessionHandler(context, mono, contextView))
                                    .doOnSuccess(v -> outbound.sendString(graphQLSubscriptionHandler.handle(graphQLRequest)))
                    )
                    .onErrorResume(throwable -> this.errorHandler(throwable, outbound));
        } else if (contentType.contentEquals(MimeType.Application.GRAPHQL)) {
            return inbound.receive().aggregate().asString()
                    .map(GraphQLRequest::new)
                    .doOnNext(graphQLRequest -> context.put(GRAPHQL_REQUEST, graphQLRequest))
                    .flatMap(graphQLRequest ->
                            ScopeEventResolver.initialized(context, RequestScoped.class)
                                    .transformDeferredContextual((mono, contextView) -> this.sessionHandler(context, mono, contextView))
                                    .doOnSuccess(v -> outbound.sendString(graphQLSubscriptionHandler.handle(graphQLRequest)))
                    )
                    .onErrorResume(throwable -> this.errorHandler(throwable, outbound));
        } else {
            IllegalArgumentException illegalArgumentException = new IllegalArgumentException("unsupported content-type: ".concat(contentType));
            Logger.error(illegalArgumentException);
            return outbound.sendClose(HttpResponseStatus.BAD_REQUEST.code(), GRAPHQL_RESPONSE_UTIL.error(illegalArgumentException))
                    .contextWrite(Context.of(REQUEST_ID, requestId));
        }
    }
}
