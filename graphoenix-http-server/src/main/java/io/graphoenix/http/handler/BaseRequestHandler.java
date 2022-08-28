package io.graphoenix.http.handler;

import org.tinylog.Logger;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerResponse;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

import java.util.Map;

import static io.graphoenix.core.utils.GraphQLResponseUtil.GRAPHQL_RESPONSE_UTIL;
import static io.graphoenix.http.error.HttpErrorStatusUtil.HTTP_ERROR_STATUS_UTIL;
import static io.graphoenix.spi.constant.Hammurabi.SESSION_ID;

public abstract class BaseRequestHandler {

    protected Mono<Void> sessionHandler(Map<String, Object> context, Mono<Void> mono, ContextView contextView) {
        return context.containsKey(SESSION_ID) ?
                mono.contextWrite(Context.of(SESSION_ID, context.get(SESSION_ID))) :
                mono;
    }

    protected Mono<String> errorHandler(Throwable throwable, HttpServerResponse response) {
        Logger.error(throwable);
        response.status(HTTP_ERROR_STATUS_UTIL.getStatus(throwable.getClass()));
        return Mono.just(GRAPHQL_RESPONSE_UTIL.error(throwable));
    }
}
