package io.graphoenix.http.handler;

import graphql.parser.antlr.GraphqlParser;
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
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import reactor.util.context.Context;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static io.graphoenix.spi.constant.Hammurabi.OPERATION_DEFINITION;
import static io.graphoenix.spi.constant.Hammurabi.REQUEST;
import static io.graphoenix.spi.constant.Hammurabi.REQUEST_ID;
import static io.graphoenix.spi.constant.Hammurabi.RESPONSE;
import static io.netty.handler.codec.http.HttpHeaderNames.ACCEPT;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;

@ApplicationScoped
public class GetRequestHandler extends BaseHandler {

    private final OperationPreprocessor operationPreprocessor;
    private final GraphQLRequestHandler graphQLRequestHandler;
    private final GraphQLSubscriptionHandler graphQLSubscriptionHandler;
    private final JsonProvider jsonProvider;

    @Inject
    public GetRequestHandler(OperationPreprocessor operationPreprocessor, GraphQLRequestHandler graphQLRequestHandler, GraphQLSubscriptionHandler graphQLSubscriptionHandler, JsonProvider jsonProvider) {
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

        request = request.withConnection(connection -> {
            if (connection.isDisposed()) {
                System.out.println(requestId);
            }
        });

        QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
        String accept = decoder.parameters().getOrDefault(ACCEPT.toString(), Collections.singletonList(request.requestHeaders().get(ACCEPT))).get(0);
        GraphQLRequest graphQLRequest = new GraphQLRequest(
                decoder.parameters().get("query").get(0),
                decoder.parameters().containsKey("operationName") ? decoder.parameters().get("operationName").get(0) : null,
                decoder.parameters().containsKey("variables") ? jsonProvider.createReader(new StringReader(Objects.requireNonNull(decoder.parameters().get("variables").get(0)))).readObject() : null
        );

        GraphqlParser.OperationDefinitionContext operationDefinitionContext = operationPreprocessor.preprocess(graphQLRequest.getQuery(), graphQLRequest.getVariables());
        context.put(OPERATION_DEFINITION, operationDefinitionContext);

        if (accept.startsWith(MimeType.Text.EVENT_STREAM)) {
            String token = Optional.ofNullable(request.requestHeaders().get("X-GraphQL-Event-Stream-Token")).orElseGet(() -> decoder.parameters().containsKey("token") ? decoder.parameters().get("token").get(0) : null);
            String operationId = decoder.parameters().containsKey("operationId") ? decoder.parameters().get("operationId").get(0) : null;
            return response.sse()
                    .addHeader(HttpHeaderNames.CACHE_CONTROL, "no-cache")
                    .addHeader(HttpHeaderNames.CONNECTION, "keep-alive")
                    .status(HttpResponseStatus.ACCEPTED)
                    .send(
                            ScopeEventResolver.initialized(context, RequestScoped.class)
                                    .transformDeferredContextual((mono, contextView) -> this.sessionHandler(context, mono, contextView))
                                    .thenMany(graphQLSubscriptionHandler.handle(operationDefinitionContext, token, operationId))
                                    .onErrorResume(throwable -> this.errorSSEHandler(throwable, response, operationId))
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
                                    .then(graphQLRequestHandler.handle(operationDefinitionContext))
                                    .doOnSuccess(jsonString -> response.status(HttpResponseStatus.OK))
                                    .onErrorResume(throwable -> this.errorHandler(throwable, response))
                                    .contextWrite(Context.of(REQUEST_ID, requestId))
                    );
        }
    }
}
