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
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.tinylog.Logger;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.stream.Stream;

@ApplicationScoped
public class MysqlBootstrapHandler implements BootstrapHandler {

    private static final int sqlCount = 300;

    private final MysqlConfig mysqlConfig;
    private final IGraphQLDocumentManager manager;

    private final DocumentBuilder documentBuilder;
    private final IntrospectionMutationBuilder introspectionMutationBuilder;

    private final GraphQLTypeToTable graphqlTypeToTable;
    private final GraphQLMutationToStatements mutationToStatements;

    private final TableCreator tableCreator;
    private final MutationExecutor mutationExecutor;

    @Inject
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
            Logger.info("action!");
            documentBuilder.startupManager();
            if (mysqlConfig.getCrateTable()) {
                Logger.info("start create type table");
                Stream<String> createTablesSQLStream = graphqlTypeToTable.createTablesSQL();
                tableCreator.createTables(createTablesSQLStream).block();
                Logger.info("create type table success");
            }
            if (mysqlConfig.getCrateIntrospection()) {
                Logger.info("introspection data SQL insert started");
                Operation operation = introspectionMutationBuilder.buildIntrospectionSchemaMutation();
                Stream<String> introspectionMutationSQLStream = mutationToStatements.createStatementsSQL(operation.toString());
                Integer totalCount = mutationExecutor.executeMutationsInBatchByGroup(introspectionMutationSQLStream, sqlCount)
                        .doOnNext(count -> Logger.info(count + " introspection data SQL insert success"))
                        .doOnComplete(() -> Logger.info("all introspection data SQL insert success"))
                        .reduce(0, Integer::sum)
                        .block();
                Logger.info("introspection insert total:\r\n{}", totalCount);
            }
            Logger.info("startup success");
        } catch (IOException | URISyntaxException e) {
            Logger.error(e);
            Logger.info("startup failed");
        }
    }
}
