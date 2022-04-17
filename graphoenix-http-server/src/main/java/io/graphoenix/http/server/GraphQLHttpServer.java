package io.graphoenix.http.server;

import io.graphoenix.http.config.HttpServerConfig;
import io.graphoenix.http.handler.GetRequestHandler;
import io.graphoenix.http.handler.PostRequestHandler;
import io.graphoenix.spi.handler.BootstrapHandler;
import io.netty.channel.ChannelOption;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

@ApplicationScoped
public class GraphQLHttpServer {

    private final HttpServerConfig httpServerConfig;
    private final GetRequestHandler getRequestHandler;
    private final PostRequestHandler postRequestHandler;
    private final BootstrapHandler bootstrapHandler;

    @Inject
    public GraphQLHttpServer(HttpServerConfig httpServerConfig, GetRequestHandler getRequestHandler, PostRequestHandler postRequestHandler, BootstrapHandler bootstrapHandler) {
        this.httpServerConfig = httpServerConfig;
        this.getRequestHandler = getRequestHandler;
        this.postRequestHandler = postRequestHandler;
        this.bootstrapHandler = bootstrapHandler;
    }

    public void run() {
        bootstrapHandler.bootstrap();

        DisposableServer server = HttpServer.create()
                .option(ChannelOption.TCP_NODELAY, httpServerConfig.getTcpNoDelay())
                .option(ChannelOption.SO_KEEPALIVE, httpServerConfig.getSoKeepAlive())
                .option(ChannelOption.SO_BACKLOG, httpServerConfig.getSoBackLog())
                .port(httpServerConfig.getPort())
                .route(httpServerRoutes ->
                        httpServerRoutes
                                .options(httpServerConfig.getGraphqlContextPath(), getRequestHandler::handle)
                                .get(httpServerConfig.getGraphqlContextPath(), getRequestHandler::handle)
                                .post(httpServerConfig.getGraphqlContextPath(), postRequestHandler::handle)
                )
                .bindNow();

        server.onDispose().block();
    }
}
