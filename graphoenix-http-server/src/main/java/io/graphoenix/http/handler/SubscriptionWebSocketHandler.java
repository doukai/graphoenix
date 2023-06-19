package io.graphoenix.http.handler;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import io.graphoenix.core.dto.GraphQLRequest;
import io.graphoenix.core.handler.GraphQLSubscriptionHandler;
import io.graphoenix.spi.handler.ScopeEventResolver;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.json.spi.JsonProvider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.websocket.WebsocketInbound;
import reactor.netty.http.websocket.WebsocketOutbound;
import reactor.util.context.Context;

import java.io.StringReader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.graphoenix.spi.constant.Hammurabi.GRAPHQL_REQUEST;
import static io.graphoenix.spi.constant.Hammurabi.INBOUND;
import static io.graphoenix.spi.constant.Hammurabi.OUTBOUND;
import static io.graphoenix.spi.constant.Hammurabi.REQUEST_ID;

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

        return outbound.sendString(inbound.receive().map(frame -> frame.toString()).doOnNext(System.out::println)).then();
//        return inbound.receiveFrames()
//                .map(frame -> GraphQLRequest.fromJson(jsonProvider.createReader(new StringReader(frame.content().toString())).readObject()))
//                .doOnNext(graphQLRequest -> context.put(GRAPHQL_REQUEST, graphQLRequest))
//                .flatMap(graphQLRequest ->
//                        ScopeEventResolver.initialized(context, RequestScoped.class)
//                                .transformDeferredContextual((mono, contextView) -> this.sessionHandler(context, mono, contextView))
//                                .doOnSuccess(v -> outbound.sendString(graphQLSubscriptionHandler.handle(graphQLRequest)).neverComplete())
//                )
//                .onErrorResume(throwable -> this.errorHandler(throwable, outbound))
//                .contextWrite(Context.of(REQUEST_ID, requestId));
    }
}
