package io.graphoenix.showcase.mysql.module;

import dagger.Module;
import dagger.Provides;
import io.graphoenix.core.pipeline.operation.OperationRouter;
import io.graphoenix.http.module.HttpServerModule;
import io.graphoenix.http.server.GraphqlHttpServer;
import io.graphoenix.product.handler.MysqlBootstrapHandler;
import io.graphoenix.product.handler.MysqlR2dbcHandler;
import io.graphoenix.product.module.MysqlModule;
import io.graphoenix.spi.handler.BootstrapHandler;
import io.graphoenix.spi.handler.OperationHandler;

import javax.inject.Singleton;

@Module(includes = {MysqlModule.class, HttpServerModule.class})
public abstract class ShowcaseModule {

    @Provides
    @Singleton
    public OperationHandler operationHandler(MysqlR2dbcHandler mysqlR2dbcHandler) {
        return mysqlR2dbcHandler;
    }

    @Provides
    @Singleton
    public BootstrapHandler bootstrapHandler(MysqlBootstrapHandler mysqlBootstrapHandler) {
        return mysqlBootstrapHandler;
    }

    @Provides
    @Singleton
    public GraphqlHttpServer getGraphqlHttpServer(GraphqlHttpServer graphqlHttpServer) {

        return graphqlHttpServer;
    }
}
