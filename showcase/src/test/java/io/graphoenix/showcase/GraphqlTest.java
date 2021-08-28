package io.graphoenix.showcase;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.graphoenix.gr2dbc.config.ConnectionConfiguration;
import io.graphoenix.gr2dbc.connector.*;
import io.graphoenix.mygql.parser.GraphqlArgumentsToWhere;
import io.graphoenix.mygql.parser.GraphqlMutationToStatements;
import io.graphoenix.mygql.parser.GraphqlQueryToSelect;
import io.graphoenix.mygql.parser.GraphqlTypeToTable;
import io.graphoenix.grantlr.manager.impl.GraphqlAntlrManager;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

public class GraphqlTest {

    @Test
    void createType() throws IOException {

        URL url = Resources.getResource("test.graphqls");
        String graphql = Resources.toString(url, Charsets.UTF_8);

        GraphqlAntlrManager graphqlAntlrManager = new GraphqlAntlrManager(graphql);
        GraphqlTypeToTable graphqlTypeToTable = new GraphqlTypeToTable(graphqlAntlrManager);
        List<String> tablesSql = graphqlTypeToTable.createTablesSql(graphql);

        tablesSql.stream().forEach(System.out::println);

        Yaml yaml = new Yaml();
        InputStream inputStream = this.getClass()
                .getClassLoader()
                .getResourceAsStream("beans.yaml");
        ConnectionConfiguration connectionConfiguration = yaml.load(inputStream);

        TableCreator tableCreator = new TableCreator(new PoolConnectionCreator(ConnectionPoolCreator.CONNECTION_POOL_CREATOR.createConnectionPool(connectionConfiguration)));

        tableCreator.createTables(tablesSql).block();
    }

    @Test
    void executeQuery() throws IOException {

        URL url = Resources.getResource("test.graphqls");
        String graphql = Resources.toString(url, Charsets.UTF_8);

        GraphqlAntlrManager graphqlAntlrManager = new GraphqlAntlrManager(graphql);
        GraphqlArgumentsToWhere graphqlArgumentsToWhere = new GraphqlArgumentsToWhere(graphqlAntlrManager);
        GraphqlQueryToSelect graphqlQueryToSelect = new GraphqlQueryToSelect(graphqlAntlrManager, graphqlArgumentsToWhere);
        List<String> queriesSql = graphqlQueryToSelect.createSelectsSql(graphql);

        Yaml yaml = new Yaml();
        InputStream inputStream = this.getClass()
                .getClassLoader()
                .getResourceAsStream("beans.yaml");
        ConnectionConfiguration connectionConfiguration = yaml.load(inputStream);

        QueryExecutor queryExecutor = new QueryExecutor(new PoolConnectionCreator(ConnectionPoolCreator.CONNECTION_POOL_CREATOR.createConnectionPool(connectionConfiguration)));

        queryExecutor.executeQueries(queriesSql).toIterable().forEach(System.out::println);
    }

    @Test
    void executeMutation() throws IOException {

        URL url = Resources.getResource("test.graphqls");
        String graphql = Resources.toString(url, Charsets.UTF_8);

        GraphqlAntlrManager graphqlAntlrManager = new GraphqlAntlrManager(graphql);
        GraphqlArgumentsToWhere graphqlArgumentsToWhere = new GraphqlArgumentsToWhere(graphqlAntlrManager);
        GraphqlQueryToSelect graphqlQueryToSelect = new GraphqlQueryToSelect(graphqlAntlrManager, graphqlArgumentsToWhere);
        GraphqlMutationToStatements graphqlMutationToStatements = new GraphqlMutationToStatements(graphqlAntlrManager, graphqlQueryToSelect);
        List<String> mutationsSql = graphqlMutationToStatements.createStatementsSql(graphql);

        Yaml yaml = new Yaml();
        InputStream inputStream = this.getClass()
                .getClassLoader()
                .getResourceAsStream("beans.yaml");
        ConnectionConfiguration connectionConfiguration = yaml.load(inputStream);

        MutationExecutor mutationExecutor = new MutationExecutor(new PoolConnectionCreator(ConnectionPoolCreator.CONNECTION_POOL_CREATOR.createConnectionPool(connectionConfiguration)));

        String result = mutationExecutor.executeMutations(mutationsSql).block();
        System.out.println(result);
    }

}
