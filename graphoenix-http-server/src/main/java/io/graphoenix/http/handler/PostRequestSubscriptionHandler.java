package io.graphoenix.http.handler;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import io.graphoenix.core.dto.GraphQLRequest;
import io.graphoenix.core.handler.GraphQLSubscriptionHandler;
import io.graphoenix.http.codec.MimeType;
import io.graphoenix.spi.handler.ScopeEventResolver;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.json.spi.JsonProvider;
import org.reactivestreams.Publisher;
import org.tinylog.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import reactor.util.context.Context;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.graphoenix.core.utils.GraphQLResponseUtil.GRAPHQL_RESPONSE_UTIL;
import static io.graphoenix.spi.constant.Hammurabi.*;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;

@ApplicationScoped
public class PostRequestSubscriptionHandler extends BaseHandler {

    private final GraphQLSubscriptionHandler graphQLSubscriptionHandler;
    private final JsonProvider jsonProvider;

    @Inject
    public PostRequestSubscriptionHandler(GraphQLSubscriptionHandler graphQLSubscriptionHandler, JsonProvider jsonProvider) {
        this.graphQLSubscriptionHandler = graphQLSubscriptionHandler;
        this.jsonProvider = jsonProvider;
    }

    public Publisher<Void> handle(HttpServerRequest request, HttpServerResponse response) {
        String requestId = NanoIdUtils.randomNanoId();
        Map<String, Object> context = new ConcurrentHashMap<>();
        context.put(REQUEST, request);
        context.put(RESPONSE, response);

        return response.status(HttpResponseStatus.OK)
                .header(HttpHeaderNames.CONTENT_TYPE, "text/event-stream;charset=UTF-8")
                .header(HttpHeaderNames.CACHE_CONTROL, "no-cache")
                .header(HttpHeaderNames.CONNECTION, "keep-alive")
                .sse()
                .send(request.receive().aggregate().asString()
                                .map(content -> GraphQLRequest.fromJson(jsonProvider.createReader(new StringReader(content)).readObject()))
                                .doOnNext(graphQLRequest -> context.put(GRAPHQL_REQUEST, graphQLRequest))
                                .flatMapMany(graphQLRequest ->
                                        ScopeEventResolver.initialized(context, RequestScoped.class)
                                                .transformDeferredContextual((mono, contextView) -> this.sessionHandler(context, mono, contextView))
                                                .thenMany(graphQLSubscriptionHandler.handle(graphQLRequest))
                                )
                                .onErrorResume(throwable -> this.errorHandler(throwable, response))
                                .map(jsonString -> ByteBufAllocator.DEFAULT.buffer().writeBytes(("data: " + jsonString + "\n\n").getBytes(StandardCharsets.UTF_8)))
                                .contextWrite(Context.of(REQUEST_ID, requestId))
                );
    }
}
