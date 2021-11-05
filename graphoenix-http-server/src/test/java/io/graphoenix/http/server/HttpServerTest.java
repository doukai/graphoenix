package io.graphoenix.http.server;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import io.graphoenix.common.manager.*;
import io.graphoenix.common.pipeline.GraphQLDataFetcherFactory;
import io.graphoenix.graphql.builder.handler.bootstrap.DocumentBuildHandler;
import io.graphoenix.graphql.builder.handler.bootstrap.IntrospectionMutationBuildHandler;
import io.graphoenix.graphql.builder.introspection.IntrospectionMutationBuilder;
import io.graphoenix.graphql.builder.schema.DocumentBuilder;
import io.graphoenix.mysql.handler.bootstrap.IntrospectionRegisterHandler;
import io.graphoenix.mysql.handler.bootstrap.MutationToSQLConvertHandler;
import io.graphoenix.mysql.handler.bootstrap.TypeDefiniteToCreateTableSQLConvertHandler;
import io.graphoenix.mysql.handler.operation.OperationToSQLConvertHandler;
import io.graphoenix.mysql.translator.GraphqlArgumentsToWhere;
import io.graphoenix.mysql.translator.GraphqlMutationToStatements;
import io.graphoenix.mysql.translator.GraphqlQueryToSelect;
import io.graphoenix.r2dbc.connector.handler.bootstrap.CreateTableSQLExecuteHandler;
import io.graphoenix.r2dbc.connector.handler.bootstrap.MutationSQLExecuteHandler;
import io.graphoenix.r2dbc.connector.handler.operation.OperationSQLExecuteHandler;
import io.graphoenix.spi.antlr.*;
import io.graphoenix.spi.handler.IBootstrapHandler;
import io.graphoenix.spi.handler.IOperationHandler;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

public class HttpServerTest {

    @Test
    void serverTest() throws Exception {

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
//                        new IntrospectionMutationBuildHandler(),
//                        new MutationToSQLConvertHandler(),
//                        new MutationSQLExecuteHandler()
                };
    }

    public IOperationHandler[] addOperations() {
        return new IOperationHandler[]{
                new OperationToSQLConvertHandler(),
                new OperationSQLExecuteHandler()
        };
    }

    @Test
    void executeIntrospectionMutation() throws IOException {

        IGraphqlDocumentManager graphqlAntlrManager = new GraphqlDocumentManager(
                new GraphqlOperationManager(),
                new GraphqlSchemaManager(),
                new GraphqlDirectiveManager(),
                new GraphqlObjectManager(),
                new GraphqlInterfaceManager(),
                new GraphqlUnionManager(),
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
        graphqlAntlrManager.registerDocument(this.getClass().getClassLoader().getResourceAsStream("graphql/mysql/preset.gql"));
        graphqlAntlrManager.registerDocument(this.getClass().getClassLoader().getResourceAsStream("graphql/mysql/introspectionTypes.gql"));


        DocumentBuilder documentBuilder = new DocumentBuilder(graphqlAntlrManager);

//        System.out.println(documentBuilder.buildDocument().toString());

        graphqlAntlrManager.registerDocument(documentBuilder.buildDocument().toString());

        IntrospectionMutationBuilder introspectionMutationBuilder = new IntrospectionMutationBuilder(graphqlAntlrManager);

        System.out.println(introspectionMutationBuilder.buildIntrospectionSchemaMutation());

//
//        GraphqlArgumentsToWhere graphqlArgumentsToWhere = new GraphqlArgumentsToWhere(graphqlAntlrManager);
//        GraphqlQueryToSelect graphqlQueryToSelect = new GraphqlQueryToSelect(graphqlAntlrManager, graphqlArgumentsToWhere);
//        GraphqlMutationToStatements graphqlMutationToStatements = new GraphqlMutationToStatements(graphqlAntlrManager, graphqlQueryToSelect);
//        List<String> mutationsSql = graphqlMutationToStatements.createStatementsSql(introspectionMutationBuilder.buildIntrospectionSchemaMutation().toString());
//
//        StringBuffer stringBuffer = new StringBuffer();
//        mutationsSql.forEach(sql -> stringBuffer.append(sql).append(";\r\n"));
//
//        File file = new File("introspection.sql");
//        Files.write(stringBuffer, file, Charsets.UTF_8);
    }
}
