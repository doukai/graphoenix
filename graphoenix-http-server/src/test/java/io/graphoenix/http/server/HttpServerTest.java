package io.graphoenix.http.server;

import io.graphoenix.common.handler.GraphQLOperationPipeline;
import io.graphoenix.spi.handler.IGraphQLToSQLHandler;
import io.graphoenix.spi.handler.ISQLHandler;
import io.graphoenix.spi.task.*;
import org.junit.jupiter.api.Test;

import static io.graphoenix.common.handler.GraphQLOperationPipelineBootstrap.GRAPHQL_OPERATION_PIPELINE_BOOTSTRAP;

public class HttpServerTest {

    @Test
    void serverTest() throws Exception {
        GraphQLOperationPipeline graphQLOperationPipeline = GRAPHQL_OPERATION_PIPELINE_BOOTSTRAP
                .startup()
                .task(IGraphQLTypeFileRegisterTask.class, "auth.gql")
                .task(IGraphQLIntrospectionTypeRegisterTask.class)
                .task(IGraphQLTypeDefineToCreateSQLTask.class)
                .task(ICreateSQLTask.class)
                .task(IGraphQLCompletionTask.class)
                .runTask()
                .push(IGraphQLToSQLHandler.class)
                .push(ISQLHandler.class);
        GraphqlHttpServer graphqlHttpServer = new GraphqlHttpServer(graphQLOperationPipeline);
        graphqlHttpServer.run();
    }
}
