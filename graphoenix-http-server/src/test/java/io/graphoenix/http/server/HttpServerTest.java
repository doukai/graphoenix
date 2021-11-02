package io.graphoenix.http.server;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import io.graphoenix.common.manager.*;
import io.graphoenix.graphql.builder.handler.GraphQLDocumentBuildHandler;
import io.graphoenix.graphql.builder.introspection.IntrospectionMutationBuilder;
import io.graphoenix.graphql.builder.schema.GraphQLDocumentBuilder;
import io.graphoenix.mysql.translator.GraphqlArgumentsToWhere;
import io.graphoenix.mysql.translator.GraphqlMutationToStatements;
import io.graphoenix.mysql.translator.GraphqlQueryToSelect;
import io.graphoenix.spi.antlr.*;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

public class HttpServerTest {

    @Test
    void serverTest() throws Exception {

        GraphqlHttpServer graphqlHttpServer = new GraphqlHttpServer();
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


        graphqlAntlrManager.registerDocument(this.getClass().getClassLoader().getResourceAsStream("graphql/preset.gql"));

        GraphQLDocumentBuilder graphQLDocumentBuilder = new GraphQLDocumentBuilder(graphqlAntlrManager);

//        System.out.println(graphQLDocumentBuilder.buildDocument().toString());
//
        graphqlAntlrManager.registerDocument(graphQLDocumentBuilder.buildDocument().toString());


        IntrospectionMutationBuilder introspectionMutationBuilder = new IntrospectionMutationBuilder(graphqlAntlrManager);

        System.out.println(introspectionMutationBuilder.buildIntrospectionSchema());


//        GraphqlArgumentsToWhere graphqlArgumentsToWhere = new GraphqlArgumentsToWhere(graphqlAntlrManager);
//        GraphqlQueryToSelect graphqlQueryToSelect = new GraphqlQueryToSelect(graphqlAntlrManager, graphqlArgumentsToWhere);
//        GraphqlMutationToStatements graphqlMutationToStatements = new GraphqlMutationToStatements(graphqlAntlrManager, graphqlQueryToSelect);
//        List<String> mutationsSql = graphqlMutationToStatements.createStatementsSql(introspectionMutationBuilder.buildIntrospectionSchema().toString());
//
//        StringBuffer stringBuffer = new StringBuffer();
//        mutationsSql.forEach(sql -> stringBuffer.append(sql).append(";\r\n"));
//
//        File file = new File("introspection.sql");
//        Files.write(stringBuffer, file, Charsets.UTF_8);
    }
}
