package io.graphoenix.mysql.showcase;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import io.graphoenix.graphql.builder.introspection.IntrospectionMutationBuilder;
import io.graphoenix.graphql.builder.schema.GraphqlSchemaRegister;
import io.graphoenix.mysql.translator.*;
import io.graphoenix.r2dbc.connector.config.ConnectionConfiguration;
import io.graphoenix.r2dbc.connector.*;
import io.graphoenix.antlr.manager.impl.GraphqlAntlrManager;
import io.graphoenix.r2dbc.connector.connection.ConnectionCreator;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

public class GraphqlTest {

    @Test
    void createType() throws IOException {

        URL url = Resources.getResource("auth.gql");
        String graphql = Resources.toString(url, Charsets.UTF_8);

        GraphqlAntlrManager graphqlAntlrManager = new GraphqlAntlrManager(graphql);

        GraphqlSchemaRegister graphqlSchemaRegister = new GraphqlSchemaRegister(graphqlAntlrManager);
        graphqlSchemaRegister.register();

        GraphqlTypeToTable graphqlTypeToTable = new GraphqlTypeToTable(graphqlAntlrManager);
        List<String> tablesSql = graphqlTypeToTable.createTablesSql();


        Yaml yaml = new Yaml();
        InputStream inputStream = this.getClass()
                .getClassLoader()
                .getResourceAsStream("beans.yaml");
        ConnectionConfiguration connectionConfiguration = yaml.load(inputStream);

        TableCreator tableCreator = new TableCreator(new ConnectionCreator(connectionConfiguration));

        tableCreator.createTables(tablesSql).block();
    }

    @Test
    void executeQuery() throws IOException {

        URL url = Resources.getResource("test.gql");
        String graphql = Resources.toString(url, Charsets.UTF_8);

        GraphqlAntlrManager graphqlAntlrManager = new GraphqlAntlrManager(graphql);
        GraphqlArgumentsToWhere graphqlArgumentsToWhere = new GraphqlArgumentsToWhere(graphqlAntlrManager);
        GraphqlQueryToSelect graphqlQueryToSelect = new GraphqlQueryToSelect(graphqlAntlrManager, graphqlArgumentsToWhere);
        List<String> queriesSql = graphqlQueryToSelect.createSelectsSql(graphql);

        queriesSql.forEach(System.out::println);

//        Yaml yaml = new Yaml();
//        InputStream inputStream = this.getClass()
//                .getClassLoader()
//                .getResourceAsStream("application.yaml");
//        ConnectionConfiguration connectionConfiguration = yaml.load(inputStream);
//
//        QueryExecutor queryExecutor = new QueryExecutor(new PoolConnectionCreator(ConnectionPoolCreator.CONNECTION_POOL_CREATOR.createConnectionPool(connectionConfiguration)));
//
//        queryExecutor.executeQueries(queriesSql).toIterable().forEach(System.out::println);
    }

    @Test
    void executeMutation() throws IOException {

        URL url = Resources.getResource("test.gql");
        String graphql = Resources.toString(url, Charsets.UTF_8);

        GraphqlAntlrManager graphqlAntlrManager = new GraphqlAntlrManager(graphql);
        GraphqlArgumentsToWhere graphqlArgumentsToWhere = new GraphqlArgumentsToWhere(graphqlAntlrManager);
        GraphqlQueryToSelect graphqlQueryToSelect = new GraphqlQueryToSelect(graphqlAntlrManager, graphqlArgumentsToWhere);
        GraphqlMutationToStatements graphqlMutationToStatements = new GraphqlMutationToStatements(graphqlAntlrManager, graphqlQueryToSelect);
        List<String> mutationsSql = graphqlMutationToStatements.createStatementsSql(graphql);
        mutationsSql.forEach(sql -> System.out.println(sql + ";"));


//        Yaml yaml = new Yaml();
//        InputStream inputStream = this.getClass()
//                .getClassLoader()
//                .getResourceAsStream("application.yaml");
//        ConnectionConfiguration connectionConfiguration = yaml.load(inputStream);
//
//        MutationExecutor mutationExecutor = new MutationExecutor(new PoolConnectionCreator(ConnectionPoolCreator.CONNECTION_POOL_CREATOR.createConnectionPool(connectionConfiguration)));
//
//        String result = mutationExecutor.executeMutations(mutationsSql).block();
//        System.out.println(result);
    }

    @Test
    void executeIntrospectionMutation() throws IOException {

        URL url = Resources.getResource("auth.gql");
        String graphql = Resources.toString(url, Charsets.UTF_8);

        GraphqlAntlrManager graphqlAntlrManager = new GraphqlAntlrManager(graphql);

        GraphqlSchemaRegister graphqlSchemaRegister = new GraphqlSchemaRegister(graphqlAntlrManager);
        graphqlSchemaRegister.register();

        IntrospectionMutationBuilder introspectionMutationBuilder = new IntrospectionMutationBuilder(graphqlAntlrManager);
        String mutationGraphql = introspectionMutationBuilder.build();

//        System.out.println(mutationGraphql);

        GraphqlArgumentsToWhere graphqlArgumentsToWhere = new GraphqlArgumentsToWhere(graphqlAntlrManager);
        GraphqlQueryToSelect graphqlQueryToSelect = new GraphqlQueryToSelect(graphqlAntlrManager, graphqlArgumentsToWhere);
        GraphqlMutationToStatements graphqlMutationToStatements = new GraphqlMutationToStatements(graphqlAntlrManager, graphqlQueryToSelect);
        List<String> mutationsSql = graphqlMutationToStatements.createStatementsSql(mutationGraphql);

        StringBuffer stringBuffer = new StringBuffer();
        mutationsSql.forEach(sql -> stringBuffer.append(sql).append(";\r\n"));

        File file = new File("introspection.sql");
        Files.write(stringBuffer, file, Charsets.UTF_8);
//
//        Yaml yaml = new Yaml();
//        InputStream inputStream = this.getClass()
//                .getClassLoader()
//                .getResourceAsStream("application.yaml");
//        ConnectionConfiguration connectionConfiguration = yaml.load(inputStream);
//
//        MutationExecutor mutationExecutor = new MutationExecutor(new PoolConnectionCreator(ConnectionPoolCreator.CONNECTION_POOL_CREATOR.createConnectionPool(connectionConfiguration)));
//
//        String result = mutationExecutor.executeMutations(mutationsSql).block();
//        System.out.println(result);
    }

    @Test
    void executeIntrospectionQuery() throws IOException {

        URL url = Resources.getResource("introspectionQuery.gql");
        String graphql = Resources.toString(url, Charsets.UTF_8);

        GraphqlAntlrManager graphqlAntlrManager = new GraphqlAntlrManager(graphql);

        GraphqlSchemaRegister graphqlSchemaRegister = new GraphqlSchemaRegister(graphqlAntlrManager);
        graphqlSchemaRegister.register();


        GraphqlArgumentsToWhere graphqlArgumentsToWhere = new GraphqlArgumentsToWhere(graphqlAntlrManager);
        GraphqlQueryToSelect graphqlQueryToSelect = new GraphqlQueryToSelect(graphqlAntlrManager, graphqlArgumentsToWhere);
        List<String> querySql = graphqlQueryToSelect.createSelectsSql(graphql);

        querySql.forEach(System.out::println);

//
//        Yaml yaml = new Yaml();
//        InputStream inputStream = this.getClass()
//                .getClassLoader()
//                .getResourceAsStream("application.yaml");
//        ConnectionConfiguration connectionConfiguration = yaml.load(inputStream);
//
//        MutationExecutor mutationExecutor = new MutationExecutor(new PoolConnectionCreator(ConnectionPoolCreator.CONNECTION_POOL_CREATOR.createConnectionPool(connectionConfiguration)));
//
//        String result = mutationExecutor.executeMutations(mutationsSql).block();
//        System.out.println(result);
    }

}
