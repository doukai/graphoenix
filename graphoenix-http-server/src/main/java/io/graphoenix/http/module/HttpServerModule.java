package io.graphoenix.http.module;

import dagger.Module;
import dagger.Provides;
import io.graphoenix.core.module.PipelineModule;
import io.graphoenix.core.pipeline.operation.OperationRouter;
import io.graphoenix.http.config.HttpServerConfig;
import io.graphoenix.http.server.GraphqlHttpServer;
import io.graphoenix.http.server.GraphqlHttpServerHandler;
import io.graphoenix.http.server.GraphqlHttpServerInitializer;
import io.graphoenix.http.config.NettyConfig;
import io.graphoenix.spi.handler.BootstrapHandler;
import io.graphoenix.spi.handler.OperationHandler;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.inject.Singleton;

@Module(includes = PipelineModule.class)
public class HttpServerModule {

    @ConfigProperty
    private HttpServerConfig httpServerConfig;

    @ConfigProperty
    private NettyConfig nettyConfig;

    @Provides
    public GraphqlHttpServerHandler graphqlHttpServerHandler(OperationRouter operationRouter, OperationHandler operationHandler) {
        return new GraphqlHttpServerHandler(operationRouter, operationHandler);
    }

    @Provides
    @Singleton
    public GraphqlHttpServerInitializer graphqlHttpServerInitializer(OperationHandler operationHandler, OperationRouter operationRouter, BootstrapHandler bootstrapHandler) {
        return new GraphqlHttpServerInitializer(httpServerConfig, graphqlHttpServerHandler(operationRouter, operationHandler), bootstrapHandler);
    }

    @Provides
    @Singleton
    public GraphqlHttpServer graphqlHttpServer(OperationHandler operationHandler, OperationRouter operationRouter, BootstrapHandler bootstrapHandler) {
        return new GraphqlHttpServer(nettyConfig, httpServerConfig, graphqlHttpServerInitializer(operationHandler, operationRouter, bootstrapHandler));
    }
}
