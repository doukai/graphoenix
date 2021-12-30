package io.graphoenix.showcase.mysql.module;

import dagger.Module;
import dagger.Provides;
import io.graphoenix.http.module.HttpServerModule;
import io.graphoenix.http.server.GraphqlHttpServer;
import io.graphoenix.product.module.MysqlModule;

import javax.inject.Singleton;

@Module(includes = {MysqlModule.class, HttpServerModule.class})
public class ShowcaseModule {

    @Provides
    @Singleton
    public GraphqlHttpServer httpServer(GraphqlHttpServer httpServer) {
        return httpServer;
    }
}
