package io.graphoenix.http.module;

import dagger.Module;
import dagger.Provides;
import io.graphoenix.core.manager.GraphQLOperationRouter;
import io.graphoenix.http.config.HttpServerConfig;
import io.graphoenix.http.config.NettyConfig;
import io.graphoenix.http.server.GraphqlHttpServer;
import io.graphoenix.http.server.GraphqlHttpServerHandler;
import io.graphoenix.http.server.GraphqlHttpServerInitializer;
import io.graphoenix.spi.handler.BootstrapHandler;
import io.graphoenix.spi.handler.OperationHandler;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.inject.Inject;
import javax.inject.Singleton;

@Module
public class HttpServerModule {

    @ConfigProperty
    private HttpServerConfig httpServerConfig;

    @ConfigProperty
    private NettyConfig nettyConfig;

    private final GraphQLOperationRouter graphQLOperationRouter;
    private final BootstrapHandler bootstrapHandler;
    private final OperationHandler operationHandler;

    @Inject
    public HttpServerModule(GraphQLOperationRouter graphQLOperationRouter, BootstrapHandler bootstrapHandler, OperationHandler operationHandler) {
        this.graphQLOperationRouter = graphQLOperationRouter;
        this.bootstrapHandler = bootstrapHandler;
        this.operationHandler = operationHandler;
    }

    @Provides
    @Singleton
    public GraphqlHttpServerHandler graphqlHttpServerHandler() {
        return new GraphqlHttpServerHandler(graphQLOperationRouter, operationHandler);
    }

    @Provides
    @Singleton
    public GraphqlHttpServerInitializer graphqlHttpServerInitializer() {
        return new GraphqlHttpServerInitializer(httpServerConfig, graphqlHttpServerHandler());
    }

    @Provides
    @Singleton
    public GraphqlHttpServer graphqlHttpServer() {
        return new GraphqlHttpServer(nettyConfig, httpServerConfig, graphqlHttpServerInitializer(), bootstrapHandler);
    }
}
