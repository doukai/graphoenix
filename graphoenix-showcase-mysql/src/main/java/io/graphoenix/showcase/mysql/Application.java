package io.graphoenix.showcase.mysql;

import io.graphoenix.common.pipeline.GraphQLDataFetcherFactory;
import io.graphoenix.graphql.builder.handler.bootstrap.DocumentBuildHandler;
import io.graphoenix.graphql.builder.handler.bootstrap.IntrospectionMutationBuildHandler;
import io.graphoenix.http.server.GraphqlHttpServer;
import io.graphoenix.mysql.handler.bootstrap.IntrospectionRegisterHandler;
import io.graphoenix.mysql.handler.bootstrap.MutationToSQLConvertHandler;
import io.graphoenix.mysql.handler.bootstrap.TypeDefiniteToCreateTableSQLConvertHandler;
import io.graphoenix.mysql.handler.operation.OperationToSQLConvertHandler;
import io.graphoenix.r2dbc.connector.handler.bootstrap.CreateTableSQLExecuteHandler;
import io.graphoenix.r2dbc.connector.handler.bootstrap.MutationSQLExecuteHandler;
import io.graphoenix.r2dbc.connector.handler.operation.OperationSQLExecuteHandler;
import io.graphoenix.spi.handler.IBootstrapHandler;
import io.graphoenix.spi.handler.IOperationHandler;

public class Application {

    public static void main(String[] args) throws Exception {
        new Application().run();
    }

    private void run() throws Exception {
        GraphQLDataFetcherFactory dataFetcherFactory = new GraphQLDataFetcherFactory(this::addBootstraps, this::addOperations);
        GraphqlHttpServer graphqlHttpServer = new GraphqlHttpServer(dataFetcherFactory.create());
        graphqlHttpServer.run();
    }

    public IBootstrapHandler[] addBootstraps() {
        return new IBootstrapHandler[]
                {
                        new IntrospectionRegisterHandler(),
                        new DocumentBuildHandler("auth.gql"),
                        new TypeDefiniteToCreateTableSQLConvertHandler(),
                        new CreateTableSQLExecuteHandler(),
                        new IntrospectionMutationBuildHandler(),
                        new MutationToSQLConvertHandler(),
                        new MutationSQLExecuteHandler()
                };
    }

    public IOperationHandler[] addOperations() {
        return new IOperationHandler[]{
                new OperationToSQLConvertHandler(),
                new OperationSQLExecuteHandler()
        };
    }
}
