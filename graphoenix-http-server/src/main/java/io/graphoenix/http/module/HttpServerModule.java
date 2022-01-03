package io.graphoenix.http.module;

import dagger.Module;
import dagger.Provides;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.core.manager.GraphQLOperationRouter;
import io.graphoenix.core.module.DocumentManagerModule;
import io.graphoenix.http.config.HttpServerConfig;
import io.graphoenix.http.server.GraphqlHttpServer;
import io.graphoenix.http.server.GraphqlHttpServerHandler;
import io.graphoenix.http.server.GraphqlHttpServerInitializer;
import io.graphoenix.http.config.NettyConfig;
import io.graphoenix.spi.handler.BootstrapHandler;
import io.graphoenix.spi.handler.OperationHandler;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.inject.Singleton;

@Module(includes = DocumentManagerModule.class)
public class HttpServerModule {

    @ConfigProperty
    private HttpServerConfig httpServerConfig;

    @ConfigProperty
    private NettyConfig nettyConfig;

    @Provides
    public GraphqlHttpServerHandler graphqlHttpServerHandler(GraphQLOperationRouter graphQLOperationRouter) {
        return new GraphqlHttpServerHandler(graphQLOperationRouter, BeanContext.get(OperationHandler.class));
    }

    @Provides
    @Singleton
    public GraphqlHttpServerInitializer graphqlHttpServerInitializer(GraphQLOperationRouter graphQLOperationRouter) {
        return new GraphqlHttpServerInitializer(httpServerConfig, graphqlHttpServerHandler(graphQLOperationRouter));
    }

    @Provides
    @Singleton
    public GraphqlHttpServer graphqlHttpServer(GraphQLOperationRouter graphQLOperationRouter) {
        return new GraphqlHttpServer(nettyConfig, httpServerConfig, graphqlHttpServerInitializer(graphQLOperationRouter), BeanContext.get(BootstrapHandler.class));
    }
}
