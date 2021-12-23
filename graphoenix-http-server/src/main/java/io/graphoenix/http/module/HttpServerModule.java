package io.graphoenix.http.module;

import dagger.Module;
import dagger.Provides;
import io.graphoenix.common.module.PipelineModule;
import io.graphoenix.common.pipeline.GraphQLDataFetcher;
import io.graphoenix.http.server.GraphqlHttpServer;
import io.graphoenix.http.server.GraphqlHttpServerHandler;
import io.graphoenix.http.server.GraphqlHttpServerInitializer;
import io.graphoenix.http.server.TestAop;
import io.graphoenix.spi.aop.InterceptorBean;
import io.graphoenix.spi.config.HttpServerConfig;
import io.graphoenix.spi.config.NettyConfig;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.annotation.Nonnull;
import javax.inject.Singleton;

@Module(includes = PipelineModule.class)
public class HttpServerModule {

    @ConfigProperty(name = "aaa.bbb",defaultValue = "cccc.aaaa")
    private HttpServerConfig httpServerConfig;

    @ConfigProperty(name = "ccc.ddd")
    private NettyConfig nettyConfig;

    @Provides
    @Singleton
    @InterceptorBean(Nonnull.class)
    public TestAop testAop() {
        return new TestAop();
    }

    @Provides
    public GraphqlHttpServerHandler graphqlHttpServerHandler(GraphQLDataFetcher dataFetcher) {
        return new GraphqlHttpServerHandler(dataFetcher);
    }

    @Provides
    @Singleton
    public GraphqlHttpServerInitializer graphqlHttpServerInitializer(GraphQLDataFetcher dataFetcher) {
        return new GraphqlHttpServerInitializer(httpServerConfig, graphqlHttpServerHandler(dataFetcher));
    }

    @Provides
    @Singleton
    public GraphqlHttpServer graphqlHttpServer(GraphQLDataFetcher dataFetcher) {
        return new GraphqlHttpServer(nettyConfig, httpServerConfig, graphqlHttpServerInitializer(dataFetcher));
    }
}
