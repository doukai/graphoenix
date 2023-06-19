package io.graphoenix.http.server;

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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

import static io.graphoenix.http.handler.SchemaRequestHandler.SCHEMA_PARAM_NAME;

@ApplicationScoped
@Named("http")
public class GraphQLHttpGraphoenixServer implements Runnable, RunningServer {

    private final HttpServerConfig httpServerConfig;
    private final SchemaRequestHandler schemaRequestHandler;
    private final GetRequestHandler getRequestHandler;
    private final PostRequestHandler postRequestHandler;
    private final PostRequestSubscriptionHandler postRequestSubscriptionHandler;

    @Inject
    public GraphQLHttpGraphoenixServer(HttpServerConfig httpServerConfig, SchemaRequestHandler schemaRequestHandler, GetRequestHandler getRequestHandler, PostRequestHandler postRequestHandler, PostRequestSubscriptionHandler postRequestSubscriptionHandler) {
        this.httpServerConfig = httpServerConfig;
        this.schemaRequestHandler = schemaRequestHandler;
        this.getRequestHandler = getRequestHandler;
        this.postRequestHandler = postRequestHandler;
        this.postRequestSubscriptionHandler = postRequestSubscriptionHandler;
    }

    @Override
    public void run() {
        CorsConfig corsConfig = CorsConfigBuilder.forAnyOrigin()
                .allowedRequestHeaders(HttpHeaderNames.CONTENT_TYPE)
                .allowedRequestMethods(HttpMethod.GET)
                .allowedRequestMethods(HttpMethod.POST)
                .build();

        DisposableServer server = HttpServer.create()
//                .option(ChannelOption.SO_BACKLOG, httpServerChangonfig.getSoBackLog())
//                .childOption(ChannelOption.TCP_NODELAY, httpServerConfig.getTcpNoDelay())
//                .childOption(ChannelOption.SO_KEEPALIVE, httpServerConfig.getSoKeepAlive())
//                .doOnConnection(connection -> connection.addHandlerLast("cors", new CorsHandler(corsConfig)))
                .route(httpServerRoutes ->
                        httpServerRoutes
                                .get(httpServerConfig.getSchemaContextPath().concat("/{").concat(SCHEMA_PARAM_NAME).concat("}"), schemaRequestHandler::handle)
                                .get(httpServerConfig.getGraphqlContextPath(), getRequestHandler::handle)
                                .post(httpServerConfig.getGraphqlContextPath(), postRequestHandler::handle)
                                .get(httpServerConfig.getSubscriptionsContextPath(), (request, response) -> response.sse().sendString(Flux.just("hello", "world")))
                                .post(httpServerConfig.getSubscriptionsContextPath(), postRequestSubscriptionHandler::handle)
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
