package io.graphoenix.http.server;

import io.graphoenix.core.context.RequestScopeInstanceFactory;
import io.graphoenix.http.config.HttpServerConfig;
import io.graphoenix.http.handler.*;
import io.graphoenix.spi.handler.RunningServer;
import io.netty.channel.ChannelOption;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.codec.http.cors.CorsConfigBuilder;
import io.netty.handler.codec.http.cors.CorsHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import reactor.netty.ConnectionObserver;
import reactor.netty.DisposableServer;
import reactor.netty.http.HttpOperations;
import reactor.netty.http.server.HttpServer;

import static io.graphoenix.http.handler.SchemaRequestHandler.SCHEMA_PARAM_NAME;

@ApplicationScoped
@Named("http")
public class GraphQLHttpGraphoenixServer implements Runnable, RunningServer {

    private final HttpServerConfig httpServerConfig;
    private final SchemaRequestHandler schemaRequestHandler;
    private final GetRequestHandler getRequestHandler;
    private final PostRequestHandler postRequestHandler;

    @Inject
    public GraphQLHttpGraphoenixServer(HttpServerConfig httpServerConfig, SchemaRequestHandler schemaRequestHandler, GetRequestHandler getRequestHandler, PostRequestHandler postRequestHandler) {
        this.httpServerConfig = httpServerConfig;
        this.schemaRequestHandler = schemaRequestHandler;
        this.getRequestHandler = getRequestHandler;
        this.postRequestHandler = postRequestHandler;
    }

    @Override
    public void run() {
        CorsConfig corsConfig = CorsConfigBuilder.forAnyOrigin()
                .allowedRequestHeaders(HttpHeaderNames.CONTENT_TYPE)
                .allowedRequestHeaders(HttpHeaderNames.AUTHORIZATION)
                .allowedRequestMethods(HttpMethod.GET)
                .allowedRequestMethods(HttpMethod.POST)
                .build();

        DisposableServer server = HttpServer.create()
                .option(ChannelOption.SO_BACKLOG, httpServerConfig.getSoBackLog())
                .childOption(ChannelOption.TCP_NODELAY, httpServerConfig.getTcpNoDelay())
                .childOption(ChannelOption.SO_KEEPALIVE, httpServerConfig.getSoKeepAlive())
                .doOnConnection(connection -> connection.addHandlerLast("cors", new CorsHandler(corsConfig)))
                .route(httpServerRoutes ->
                        httpServerRoutes
                                .get(httpServerConfig.getSchemaContextPath() + "/{" + SCHEMA_PARAM_NAME + "}", schemaRequestHandler::handle)
                                .get(httpServerConfig.getGraphqlContextPath(), getRequestHandler::handle)
                                .post(httpServerConfig.getGraphqlContextPath(), postRequestHandler::handle)
                )
                .childObserve((connection, newState) -> {
                            if (connection instanceof HttpOperations<?, ?> && newState.equals(ConnectionObserver.State.DISCONNECTING)) {
                                String requestId = ((HttpOperations<?, ?>) connection).requestId();
                                RequestScopeInstanceFactory.invalidate(requestId);
                            }
                        }
                )
                .port(httpServerConfig.getPort())
                .bindNow();

        server.onDispose().block();
    }

    @Override
    public String protocol() {
        return "http";
    }

    @Override
    public int port() {
        return httpServerConfig.getPort();
    }
}
