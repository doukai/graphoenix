package io.graphoenix.http.server;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.graphoenix.common.manager.*;
import io.graphoenix.graphql.builder.introspection.IntrospectionMutationBuilder;
import io.graphoenix.graphql.builder.schema.DocumentBuilder;
import io.graphoenix.spi.antlr.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;

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


        DocumentBuilder documentBuilder = new DocumentBuilder(graphqlAntlrManager);

//        System.out.println(documentBuilder.buildDocument().toString());

        graphqlAntlrManager.registerDocument(documentBuilder.buildDocument().toString());
//
//
        IntrospectionMutationBuilder introspectionMutationBuilder = new IntrospectionMutationBuilder(graphqlAntlrManager);
//
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
