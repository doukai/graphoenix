package io.graphoenix.http.handler;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import io.graphoenix.core.dto.GraphQLRequest;
import io.graphoenix.core.handler.GraphQLRequestHandler;
import io.graphoenix.core.handler.GraphQLSubscriptionHandler;
import io.graphoenix.http.codec.MimeType;
import io.graphoenix.spi.handler.ScopeEventResolver;
import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringDecoder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.json.spi.JsonProvider;
import org.reactivestreams.Publisher;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import reactor.util.context.Context;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static io.graphoenix.spi.constant.Hammurabi.GRAPHQL_REQUEST;
import static io.graphoenix.spi.constant.Hammurabi.REQUEST;
import static io.graphoenix.spi.constant.Hammurabi.REQUEST_ID;
import static io.graphoenix.spi.constant.Hammurabi.RESPONSE;
import static io.netty.handler.codec.http.HttpHeaderNames.ACCEPT;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;

@ApplicationScoped
public class DeleteRequestHandler extends BaseHandler {

    private final GraphQLRequestHandler graphQLRequestHandler;
    private final GraphQLSubscriptionHandler graphQLSubscriptionHandler;
    private final JsonProvider jsonProvider;

    @Inject
    public DeleteRequestHandler(GraphQLRequestHandler graphQLRequestHandler, GraphQLSubscriptionHandler graphQLSubscriptionHandler, JsonProvider jsonProvider) {
        this.graphQLRequestHandler = graphQLRequestHandler;
        this.graphQLSubscriptionHandler = graphQLSubscriptionHandler;
        this.jsonProvider = jsonProvider;
    }

    public Publisher<Void> handle(HttpServerRequest request, HttpServerResponse response) {
        String requestId = NanoIdUtils.randomNanoId();
        Map<String, Object> context = new ConcurrentHashMap<>();
        context.put(REQUEST, request);
        context.put(RESPONSE, response);

        QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
        String accept = decoder.parameters().getOrDefault(ACCEPT.toString(), Collections.singletonList(request.requestHeaders().get(ACCEPT))).get(0);
        GraphQLRequest graphQLRequest = new GraphQLRequest(
                decoder.parameters().get("query").get(0),
                decoder.parameters().getOrDefault("operationName", null) == null ? null : decoder.parameters().get("operationName").get(0),
                decoder.parameters().getOrDefault("variables", null) == null ? null : jsonProvider.createReader(new StringReader(Objects.requireNonNull(decoder.parameters().get("variables").get(0)))).readObject()
        );
        context.put(GRAPHQL_REQUEST, graphQLRequest);

        if (accept.startsWith(MimeType.Text.EVENT_STREAM)) {
            return response.sse()
                    .addHeader(HttpHeaderNames.CACHE_CONTROL, "no-cache")
                    .addHeader(HttpHeaderNames.CONNECTION, "keep-alive")
                    .status(HttpResponseStatus.ACCEPTED)
                    .send(
                            ScopeEventResolver.initialized(context, RequestScoped.class)
                                    .transformDeferredContextual((mono, contextView) -> this.sessionHandler(context, mono, contextView))
                                    .thenMany(graphQLSubscriptionHandler.handle(graphQLRequest, requestId))
                                    .onErrorResume(throwable -> this.errorSSEHandler(throwable, response, requestId))
                                    .map(eventString -> ByteBufAllocator.DEFAULT.buffer().writeBytes(eventString.getBytes(StandardCharsets.UTF_8)))
                                    .contextWrite(Context.of(REQUEST_ID, requestId)),
                            byteBuf -> true
                    );
        } else {
            return response
                    .addHeader(CONTENT_TYPE, MimeType.Application.JSON)
                    .sendString(
                            ScopeEventResolver.initialized(context, RequestScoped.class)
                                    .transformDeferredContextual((mono, contextView) -> this.sessionHandler(context, mono, contextView))
                                    .then(graphQLRequestHandler.handle(graphQLRequest))
                                    .doOnSuccess(jsonString -> response.status(HttpResponseStatus.OK))
                                    .onErrorResume(throwable -> this.errorHandler(throwable, response))
                                    .contextWrite(Context.of(REQUEST_ID, requestId))
                    );
        }
    }
}
