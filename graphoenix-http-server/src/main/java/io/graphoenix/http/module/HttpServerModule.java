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
import io.graphoenix.core.module.HandlerModule;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.inject.Singleton;
import java.util.Optional;

@Module(includes = {PipelineModule.class, HandlerModule.class})
public class HttpServerModule {

    @ConfigProperty
    private HttpServerConfig httpServerConfig;

    @ConfigProperty
    private NettyConfig nettyConfig;

    @Provides
    public GraphqlHttpServerHandler graphqlHttpServerHandler(Optional<OperationHandler> operationHandler, OperationRouter operationRouter) {
        return new GraphqlHttpServerHandler(operationHandler, operationRouter);
    }

    @Provides
    @Singleton
    public GraphqlHttpServerInitializer graphqlHttpServerInitializer(Optional<OperationHandler> operationHandler, OperationRouter operationRouter, Optional<BootstrapHandler> bootstrapHandler) {
        return new GraphqlHttpServerInitializer(httpServerConfig, bootstrapHandler, graphqlHttpServerHandler(operationHandler, operationRouter));
    }

    @Provides
    @Singleton
    public GraphqlHttpServer graphqlHttpServer(Optional<OperationHandler> operationHandler, OperationRouter operationRouter, Optional<BootstrapHandler> bootstrapHandler) {
        return new GraphqlHttpServer(nettyConfig, httpServerConfig, graphqlHttpServerInitializer(operationHandler, operationRouter, bootstrapHandler));
    }
}
