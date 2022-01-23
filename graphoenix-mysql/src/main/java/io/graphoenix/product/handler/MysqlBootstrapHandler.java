package io.graphoenix.product.handler;

import io.graphoenix.graphql.builder.introspection.IntrospectionMutationBuilder;
import io.graphoenix.graphql.builder.schema.DocumentBuilder;
import io.graphoenix.graphql.generator.operation.Operation;
import io.graphoenix.mysql.translator.GraphQLMutationToStatements;
import io.graphoenix.mysql.translator.GraphQLTypeToTable;
import io.graphoenix.product.config.MysqlConfig;
import io.graphoenix.r2dbc.connector.executor.MutationExecutor;
import io.graphoenix.r2dbc.connector.executor.TableCreator;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.handler.BootstrapHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.stream.Stream;

public class MysqlBootstrapHandler implements BootstrapHandler {

    private static final Logger log = LoggerFactory.getLogger(MysqlBootstrapHandler.class);
    private static final int sqlCount = 500;

    private final MysqlConfig mysqlConfig;
    private final IGraphQLDocumentManager manager;

    private final DocumentBuilder documentBuilder;
    private final IntrospectionMutationBuilder introspectionMutationBuilder;

    private final GraphQLTypeToTable graphqlTypeToTable;
    private final GraphQLMutationToStatements mutationToStatements;

    private final TableCreator tableCreator;
    private final MutationExecutor mutationExecutor;

    public MysqlBootstrapHandler(MysqlConfig mysqlConfig,
                                 IGraphQLDocumentManager manager,
                                 DocumentBuilder documentBuilder,
                                 IntrospectionMutationBuilder introspectionMutationBuilder,
                                 GraphQLTypeToTable graphqlTypeToTable,
                                 GraphQLMutationToStatements mutationToStatements,
                                 TableCreator tableCreator,
                                 MutationExecutor mutationExecutor) {
        this.mysqlConfig = mysqlConfig;
        this.manager = manager;
        this.documentBuilder = documentBuilder;
        this.introspectionMutationBuilder = introspectionMutationBuilder;
        this.graphqlTypeToTable = graphqlTypeToTable;
        this.mutationToStatements = mutationToStatements;
        this.tableCreator = tableCreator;
        this.mutationExecutor = mutationExecutor;
    }

    @Override
    public void bootstrap() {

        try {
            if (mysqlConfig.getCrateTable()) {
                manager.registerFileByName("graphql/mysql/preset.gql");
                manager.registerFileByName("graphql/mysql/introspectionTypes.gql");
                documentBuilder.buildManager();
                Stream<String> createTablesSQLStream = graphqlTypeToTable.createTablesSQL();
                tableCreator.createTables(createTablesSQLStream).block();
                manager.clearAll();
            }

            manager.registerFileByName("graphql/mysql/preset.gql");
            manager.registerFileByName("graphql/mysql/introspectionTypes.gql");


            URL fileURL = getClass().getResource("schema.gql");
            assert fileURL != null;
            manager.registerInputStream(fileURL.openStream());

            if (mysqlConfig.getCrateIntrospection()) {
                Operation operation = introspectionMutationBuilder.buildIntrospectionSchemaMutation();
                Stream<String> introspectionMutationSQLStream = mutationToStatements.createStatementsSQL(operation.toString());

                log.info("introspection data SQL insert started");
                mutationExecutor.executeMutationsInBatchByGroup(introspectionMutationSQLStream, sqlCount).forEach(count -> log.info(count + " introspection data SQL insert success"));
                log.info("All introspection data SQL insert success");
            }
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }
}
