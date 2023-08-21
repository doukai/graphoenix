package io.graphoenix.http.handler;

import io.graphoenix.core.context.RequestScopeInstanceFactory;
import io.graphoenix.core.dto.GraphQLRequest;
import io.graphoenix.core.handler.GraphQLRequestHandler;
import io.graphoenix.core.handler.GraphQLSubscriptionHandler;
import io.graphoenix.core.handler.OperationPreprocessor;
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
import org.tinylog.Logger;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import reactor.util.context.Context;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static io.graphoenix.core.utils.GraphQLResponseUtil.GRAPHQL_RESPONSE_UTIL;
import static io.graphoenix.spi.constant.Hammurabi.OPERATION_DEFINITION;
import static io.graphoenix.spi.constant.Hammurabi.REQUEST;
import static io.graphoenix.spi.constant.Hammurabi.REQUEST_ID;
import static io.graphoenix.spi.constant.Hammurabi.RESPONSE;
import static io.netty.handler.codec.http.HttpHeaderNames.ACCEPT;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;

@ApplicationScoped
public class PostRequestHandler extends BaseHandler {

    private final OperationPreprocessor operationPreprocessor;
    private final GraphQLRequestHandler graphQLRequestHandler;
    private final GraphQLSubscriptionHandler graphQLSubscriptionHandler;
    private final JsonProvider jsonProvider;

    @Inject
    public PostRequestHandler(OperationPreprocessor operationPreprocessor, GraphQLRequestHandler graphQLRequestHandler, GraphQLSubscriptionHandler graphQLSubscriptionHandler, JsonProvider jsonProvider) {
        this.operationPreprocessor = operationPreprocessor;
        this.graphQLRequestHandler = graphQLRequestHandler;
        this.graphQLSubscriptionHandler = graphQLSubscriptionHandler;
        this.jsonProvider = jsonProvider;
    }

    public Publisher<Void> handle(HttpServerRequest request, HttpServerResponse response) {
        String requestId = request.requestId();
        Map<String, Object> context = new ConcurrentHashMap<>();
        context.put(REQUEST, request);
        context.put(RESPONSE, response);

        String accept = request.requestHeaders().get(ACCEPT);
        String contentType = request.requestHeaders().get(CONTENT_TYPE);
        QueryStringDecoder decoder = new QueryStringDecoder(request.uri());

        if (contentType.startsWith(MimeType.Application.JSON)) {
            return response
                    .addHeader(CONTENT_TYPE, MimeType.Application.JSON)
                    .sendString(
                            RequestScopeInstanceFactory.computeIfAbsent(requestId, HttpServerRequest.class, request)
                                    .then(RequestScopeInstanceFactory.computeIfAbsent(requestId, HttpServerResponse.class, response))
                                    .then(
                                            request.receive().aggregate().asString()
                                                    .map(content -> GraphQLRequest.fromJson(jsonProvider.createReader(new StringReader(content)).readObject()))
                                                    .map(graphQLRequest -> operationPreprocessor.preprocess(graphQLRequest.getQuery(), graphQLRequest.getVariables()))
                                                    .doOnNext(operationDefinitionContext -> context.put(OPERATION_DEFINITION, operationDefinitionContext))
                                                    .flatMap(operationDefinitionContext ->
                                                            ScopeEventResolver.initialized(context, RequestScoped.class)
                                                                    .transformDeferredContextual((mono, contextView) -> this.sessionHandler(context, mono, contextView))
                                                                    .then(graphQLRequestHandler.handle(operationDefinitionContext))
                                                    )
                                                    .doOnSuccess(jsonString -> response.status(HttpResponseStatus.OK))
                                                    .onErrorResume(throwable -> this.errorHandler(throwable, response))
                                                    .contextWrite(Context.of(REQUEST_ID, requestId))
                                    )
                    );
        } else if (contentType.startsWith(MimeType.Application.GRAPHQL)) {
            return response
                    .addHeader(CONTENT_TYPE, MimeType.Application.JSON)
                    .sendString(
                            RequestScopeInstanceFactory.computeIfAbsent(requestId, HttpServerRequest.class, request)
                                    .then(RequestScopeInstanceFactory.computeIfAbsent(requestId, HttpServerResponse.class, response))
                                    .then(
                                            request.receive().aggregate().asString()
                                                    .map(GraphQLRequest::new)
                                                    .map(graphQLRequest -> operationPreprocessor.preprocess(graphQLRequest.getQuery(), graphQLRequest.getVariables()))
                                                    .doOnNext(operationDefinitionContext -> context.put(OPERATION_DEFINITION, operationDefinitionContext))
                                                    .flatMap(operationDefinitionContext ->
                                                            ScopeEventResolver.initialized(context, RequestScoped.class)
                                                                    .transformDeferredContextual((mono, contextView) -> this.sessionHandler(context, mono, contextView))
                                                                    .then(graphQLRequestHandler.handle(operationDefinitionContext))
                                                    )
                                                    .doOnSuccess(jsonString -> response.status(HttpResponseStatus.OK))
                                                    .onErrorResume(throwable -> this.errorHandler(throwable, response))
                                                    .contextWrite(Context.of(REQUEST_ID, requestId))
                                    )
                    );
        } else if (contentType.startsWith(MimeType.Text.PLAIN) && accept.startsWith(MimeType.Text.EVENT_STREAM)) {
            String token = Optional.ofNullable(request.requestHeaders().get("X-GraphQL-Event-Stream-Token")).orElseGet(() -> decoder.parameters().containsKey("token") ? decoder.parameters().get("token").get(0) : null);
            String operationId = decoder.parameters().containsKey("operationId") ? decoder.parameters().get("operationId").get(0) : null;
            return response.sse()
                    .addHeader(HttpHeaderNames.CACHE_CONTROL, "no-cache")
                    .addHeader(HttpHeaderNames.CONNECTION, "keep-alive")
                    .status(HttpResponseStatus.ACCEPTED)
                    .send(
                            RequestScopeInstanceFactory.computeIfAbsent(requestId, HttpServerRequest.class, request)
                                    .then(RequestScopeInstanceFactory.computeIfAbsent(requestId, HttpServerResponse.class, response))
                                    .thenMany(
                                            request.receive().aggregate().asString()
                                                    .map(content -> GraphQLRequest.fromJson(jsonProvider.createReader(new StringReader(content)).readObject()))
                                                    .map(graphQLRequest -> operationPreprocessor.preprocess(graphQLRequest.getQuery(), graphQLRequest.getVariables()))
                                                    .doOnNext(operationDefinitionContext -> context.put(OPERATION_DEFINITION, operationDefinitionContext))
                                                    .flatMapMany(operationDefinitionContext ->
                                                            ScopeEventResolver.initialized(context, RequestScoped.class)
                                                                    .transformDeferredContextual((mono, contextView) -> this.sessionHandler(context, mono, contextView))
                                                                    .thenMany(graphQLSubscriptionHandler.handle(operationDefinitionContext, token, operationId))
                                                    )
                                                    .onErrorResume(throwable -> this.errorSSEHandler(throwable, response, operationId))
                                                    .map(eventString -> ByteBufAllocator.DEFAULT.buffer().writeBytes(eventString.getBytes(StandardCharsets.UTF_8)))
                                                    .contextWrite(Context.of(REQUEST_ID, requestId))
                                    ),
                            byteBuf -> true
                    );
        } else {
            IllegalArgumentException illegalArgumentException = new IllegalArgumentException("unsupported content-type: ".concat(contentType));
            Logger.error(illegalArgumentException);
            return response
                    .addHeader(CONTENT_TYPE, MimeType.Application.JSON)
                    .status(HttpResponseStatus.BAD_REQUEST)
                    .sendString(Mono.just(GRAPHQL_RESPONSE_UTIL.error(illegalArgumentException)).contextWrite(Context.of(REQUEST_ID, requestId)));
        }
    }
}
