package io.graphoenix.showcase;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.graphoenix.gr2dbc.config.ConnectionConfiguration;
import io.graphoenix.gr2dbc.connector.ConnectionPoolCreator;
import io.graphoenix.gr2dbc.connector.PoolConnectionCreator;
import io.graphoenix.gr2dbc.connector.TableCreator;
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

        Yaml yaml = new Yaml();
        InputStream inputStream = this.getClass()
                .getClassLoader()
                .getResourceAsStream("beans.yaml");
        ConnectionConfiguration connectionConfiguration = yaml.load(inputStream);

        TableCreator tableCreator = new TableCreator(new PoolConnectionCreator(ConnectionPoolCreator.CONNECTION_POOL_CREATOR.createConnectionPool(connectionConfiguration)));

        tableCreator.createTables(tablesSql).block();
    }

}
