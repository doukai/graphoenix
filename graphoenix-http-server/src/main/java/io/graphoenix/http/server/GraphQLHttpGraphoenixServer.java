package io.graphoenix.http.server;

import io.graphoenix.core.bootstrap.GraphoenixServer;
import io.graphoenix.http.config.HttpServerConfig;
import io.graphoenix.http.handler.GetRequestHandler;
import io.graphoenix.http.handler.PostRequestHandler;
import io.graphoenix.http.handler.SchemaRequestHandler;
import io.netty.channel.ChannelOption;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.codec.http.cors.CorsConfigBuilder;
import io.netty.handler.codec.http.cors.CorsHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

import static io.graphoenix.http.handler.SchemaRequestHandler.SCHEMA_PARAM_NAME;

@ApplicationScoped
public class GraphQLHttpGraphoenixServer implements GraphoenixServer {

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
    public Mono<Void> run() {
        CorsConfig corsConfig = CorsConfigBuilder.forAnyOrigin()
                .allowedRequestHeaders(HttpHeaderNames.CONTENT_TYPE)
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
                                .get(httpServerConfig.getSchemaContextPath().concat("/{").concat(SCHEMA_PARAM_NAME).concat("}"), schemaRequestHandler::handle)
                                .get(httpServerConfig.getGraphqlContextPath(), getRequestHandler::handle)
                                .post(httpServerConfig.getGraphqlContextPath(), postRequestHandler::handle)
                )
                .port(httpServerConfig.getPort())
                .bindNow();

        return server.onDispose();
    }
}
