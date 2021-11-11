package io.graphoenix.http.server;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import io.graphoenix.common.manager.*;
import io.graphoenix.common.pipeline.GraphQLDataFetcherFactory;
import io.graphoenix.graphql.builder.handler.bootstrap.DocumentBuildHandler;
import io.graphoenix.graphql.builder.introspection.IntrospectionMutationBuilder;
import io.graphoenix.graphql.builder.schema.DocumentBuilder;
import io.graphoenix.mysql.handler.bootstrap.IntrospectionRegisterHandler;
import io.graphoenix.mysql.handler.bootstrap.TypeDefiniteToCreateTableSQLConvertHandler;
import io.graphoenix.mysql.handler.operation.OperationToSQLConvertHandler;
import io.graphoenix.mysql.translator.GraphQLArgumentsToWhere;
import io.graphoenix.mysql.translator.GraphQLMutationToStatements;
import io.graphoenix.mysql.translator.GraphQLQueryToSelect;
import io.graphoenix.r2dbc.connector.handler.bootstrap.CreateTableSQLExecuteHandler;
import io.graphoenix.r2dbc.connector.handler.operation.OperationSQLExecuteHandler;
import io.graphoenix.spi.antlr.*;
import io.graphoenix.spi.handler.IBootstrapHandler;
import io.graphoenix.spi.handler.IOperationHandler;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

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

        IGraphQLDocumentManager graphqlAntlrManager = new GraphQLDocumentManager(
                new GraphQLOperationManager(),
                new GraphQLSchemaManager(),
                new GraphQLDirectiveManager(),
                new GraphQLObjectManager(),
                new GraphQLInterfaceManager(),
                new GraphQLUnionManager(),
                new GraphQLFieldManager(),
                new GraphQLInputObjectManager(),
                new GraphQLInputValueManager(),
                new GraphQLEnumManager(),
                new GraphQLScalarManager(),
                new GraphQLFragmentManager()
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

//        System.out.println(introspectionMutationBuilder.buildIntrospectionSchemaMutation());


        GraphQLFieldMapManager mapper = new GraphQLFieldMapManager(graphqlAntlrManager);
        GraphQLArgumentsToWhere graphqlArgumentsToWhere = new GraphQLArgumentsToWhere(graphqlAntlrManager, mapper);
        GraphQLQueryToSelect graphqlQueryToSelect = new GraphQLQueryToSelect(graphqlAntlrManager, mapper, graphqlArgumentsToWhere);
        GraphQLMutationToStatements graphqlMutationToStatements = new GraphQLMutationToStatements(graphqlAntlrManager, mapper, graphqlQueryToSelect);
        List<String> mutationsSql = graphqlMutationToStatements.createStatementsSQL(introspectionMutationBuilder.buildIntrospectionSchemaMutation().toString()).collect(Collectors.toList());

        StringBuffer stringBuffer = new StringBuffer();
        mutationsSql.forEach(sql -> stringBuffer.append(sql).append(";\r\n"));

        File file = new File("introspection.sql");
        Files.write(stringBuffer, file, Charsets.UTF_8);
    }
}
