package io.graphoenix.showcase.mysql;

import io.graphoenix.common.pipeline.DaggerGraphQLDataFetcherFactory;
import io.graphoenix.common.pipeline.GraphQLDataFetcher;
import io.graphoenix.graphql.builder.handler.bootstrap.DaggerDocumentBuildHandlerFactory;
import io.graphoenix.graphql.builder.handler.bootstrap.DaggerIntrospectionMutationBuildHandlerFactory;
import io.graphoenix.http.server.DaggerGraphqlHttpServerFactory;
import io.graphoenix.http.server.GraphqlHttpServer;
import io.graphoenix.mysql.handler.bootstrap.*;
import io.graphoenix.mysql.handler.operation.DaggerOperationToSQLConvertHandlerFactory;
import io.graphoenix.r2dbc.connector.handler.bootstrap.DaggerCreateTableSQLExecuteHandlerFactory;
import io.graphoenix.r2dbc.connector.handler.bootstrap.DaggerIntrospectionMutationExecuteHandlerFactory;
import io.graphoenix.r2dbc.connector.handler.operation.DaggerOperationSQLExecuteHandlerFactory;
import io.graphoenix.spi.config.JavaGeneratorConfig;
import org.eclipse.microprofile.config.inject.ConfigProperty;

public class Application {

    @ConfigProperty
    JavaGeneratorConfig javaGeneratorConfig = new JavaGeneratorConfig("io.graphoenix.showcase.mysql.generated", null, null, null, null, null, null, null, null, "auth.gql", null);

    public static void main(String[] args) throws Exception {
        new Application().run();
    }

    private void run() throws Exception {
        GraphQLDataFetcher dataFetcher = DaggerGraphQLDataFetcherFactory.create()
                .buildFetcher()
                .addBootstrapHandler(DaggerIntrospectionRegisterHandlerFactory.create().createHandler())
                .addBootstrapHandler(DaggerDocumentBuildHandlerFactory.create().createHandler())
                .addBootstrapHandler(DaggerTypeDefiniteToCreateTableSQLConvertHandlerFactory.create().createHandler())
                .addBootstrapHandler(DaggerCreateTableSQLExecuteHandlerFactory.create().createHandler())
                .addBootstrapHandler(DaggerIntrospectionMutationBuildHandlerFactory.create().createHandler())
                .addBootstrapHandler(DaggerMutationToSQLConvertHandlerFactory.create().createHandler())
                .addBootstrapHandler(DaggerIntrospectionMutationExecuteHandlerFactory.create().createHandler())
                .bootstrap()
                .addOperationHandler(DaggerOperationToSQLConvertHandlerFactory.create().createHandler())
                .addOperationHandler(DaggerOperationSQLExecuteHandlerFactory.create().createHandler());
        GraphqlHttpServer graphqlHttpServer = DaggerGraphqlHttpServerFactory.create().get();
        graphqlHttpServer.run();
    }
}
