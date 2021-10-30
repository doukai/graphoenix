package io.graphoenix.http.server;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import io.graphoenix.common.handler.GraphQLOperationPipeline;
import io.graphoenix.common.manager.*;
import io.graphoenix.graphql.builder.introspection.IntrospectionMutationBuilder;
import io.graphoenix.graphql.builder.schema.GraphqlSchemaRegister;
import io.graphoenix.mysql.translator.GraphqlArgumentsToWhere;
import io.graphoenix.mysql.translator.GraphqlMutationToStatements;
import io.graphoenix.mysql.translator.GraphqlQueryToSelect;
import io.graphoenix.spi.antlr.*;
import io.graphoenix.spi.handler.IGraphQLToSQLHandler;
import io.graphoenix.spi.handler.ISQLHandler;
import io.graphoenix.spi.task.*;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

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

    @Test
    void executeIntrospectionMutation() throws IOException {

        IGraphqlDocumentManager graphqlAntlrManager = new GraphqlDocumentManager(
                new GraphqlOperationManager(),
                new GraphqlSchemaManager(),
                new GraphqlDirectiveManager(),
                new GraphqlObjectManager(),
                new GraphqlFieldManager(),
                new GraphqlInputObjectManager(),
                new GraphqlInputValueManager(),
                new GraphqlEnumManager(),
                new GraphqlScalarManager(),
                new GraphqlFragmentManager()
        );


        URL url = Resources.getResource("auth.gql");
        String graphql = Resources.toString(url, Charsets.UTF_8);
        graphqlAntlrManager.registerDocument(graphql);

        url = Resources.getResource("graphql/introspectionTypes.gql");
        graphql = Resources.toString(url, Charsets.UTF_8);
        graphqlAntlrManager.registerDocument(graphql);

        GraphqlSchemaRegister graphqlSchemaRegister = new GraphqlSchemaRegister(graphqlAntlrManager);
        graphqlSchemaRegister.register();

        IntrospectionMutationBuilder introspectionMutationBuilder = new IntrospectionMutationBuilder(graphqlAntlrManager);
        String mutationGraphql = introspectionMutationBuilder.build();

        GraphqlArgumentsToWhere graphqlArgumentsToWhere = new GraphqlArgumentsToWhere(graphqlAntlrManager);
        GraphqlQueryToSelect graphqlQueryToSelect = new GraphqlQueryToSelect(graphqlAntlrManager, graphqlArgumentsToWhere);
        GraphqlMutationToStatements graphqlMutationToStatements = new GraphqlMutationToStatements(graphqlAntlrManager, graphqlQueryToSelect);
        List<String> mutationsSql = graphqlMutationToStatements.createStatementsSql(mutationGraphql);

        StringBuffer stringBuffer = new StringBuffer();
        mutationsSql.forEach(sql -> stringBuffer.append(sql).append(";\r\n"));

        File file = new File("introspection.sql");
        Files.write(stringBuffer, file, Charsets.UTF_8);
    }
}
