package io.graphoenix.http.handler;

import io.graphoenix.http.context.HttpRequestContext;
import io.graphoenix.http.context.HttpResponseContext;
import io.graphoenix.spi.dto.GraphQLRequest;
import io.vavr.CheckedRunnable;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseFilter;
import org.tinylog.Logger;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

import java.util.Map;
import java.util.ServiceLoader;

import static io.graphoenix.core.utils.GraphQLResponseUtil.GRAPHQL_RESPONSE_UTIL;
import static io.graphoenix.http.error.HttpErrorStatusUtil.HTTP_ERROR_STATUS_UTIL;
import static io.graphoenix.spi.constant.Hammurabi.GRAPHQL_REQUEST_KEY;
import static io.graphoenix.spi.constant.Hammurabi.RESULT_CONTENT_KEY;

public abstract class BaseRequestHandler {

    protected void beforeHandler(HttpServerRequest request, Map<String, Object> properties, GraphQLRequest graphQLRequest) {
        properties.put(GRAPHQL_REQUEST_KEY, graphQLRequest);
        ServiceLoader.load(ContainerRequestFilter.class, this.getClass().getClassLoader()).stream()
                .map(ServiceLoader.Provider::get)
                .forEach(containerRequestFilter -> CheckedRunnable.of(() -> containerRequestFilter.filter(new HttpRequestContext(request, properties))).unchecked().run());
    }

    protected void afterHandler(HttpServerRequest request, HttpServerResponse response, Map<String, Object> properties, String resultContent) {
        properties.put(RESULT_CONTENT_KEY, resultContent);
        ServiceLoader.load(ContainerResponseFilter.class, this.getClass().getClassLoader()).stream()
                .map(ServiceLoader.Provider::get)
                .forEach(containerRequestFilter -> CheckedRunnable.of(() -> containerRequestFilter.filter(new HttpRequestContext(request, properties), new HttpResponseContext(response))).unchecked().run());
    }

    protected Mono<String> errorHandler(Throwable throwable, HttpServerResponse response) {
        Logger.error(throwable);
        response.status(HTTP_ERROR_STATUS_UTIL.getStatus(throwable.getClass()));
        return Mono.just(GRAPHQL_RESPONSE_UTIL.error(throwable));
    }
}
