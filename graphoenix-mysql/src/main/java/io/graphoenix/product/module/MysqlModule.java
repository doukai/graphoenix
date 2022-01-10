package io.graphoenix.product.module;

import io.graphoenix.graphql.builder.introspection.IntrospectionMutationBuilder;
import io.graphoenix.graphql.builder.schema.DocumentBuilder;
import io.graphoenix.mysql.handler.OperationToSQLConvertHandler;
import io.graphoenix.mysql.handler.SQLFormatHandler;
import io.graphoenix.mysql.translator.GraphQLMutationToStatements;
import io.graphoenix.mysql.translator.GraphQLTypeToTable;
import io.graphoenix.product.config.MysqlConfig;
import io.graphoenix.product.handler.MysqlBootstrapHandler;
import io.graphoenix.product.handler.MysqlGeneratorHandler;
import io.graphoenix.product.handler.MysqlR2dbcHandler;
import io.graphoenix.r2dbc.connector.executor.MutationExecutor;
import io.graphoenix.r2dbc.connector.executor.TableCreator;
import io.graphoenix.r2dbc.connector.handler.OperationSQLExecuteHandler;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.handler.BootstrapHandler;
import io.graphoenix.spi.handler.GeneratorHandler;
import io.graphoenix.spi.handler.OperationHandler;
import io.graphoenix.spi.module.Module;
import io.graphoenix.spi.module.Provides;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.inject.Inject;
import javax.inject.Singleton;

@Module
public class MysqlModule {

    @ConfigProperty
    private MysqlConfig mysqlConfig;

    private final IGraphQLDocumentManager manager;
    private final DocumentBuilder documentBuilder;
    private final IntrospectionMutationBuilder introspectionMutationBuilder;
    private final GraphQLTypeToTable graphqlTypeToTable;
    private final GraphQLMutationToStatements mutationToStatements;
    private final TableCreator tableCreator;
    private final MutationExecutor mutationExecutor;
    private final OperationToSQLConvertHandler operationToSQLConvertHandler;
    private final OperationSQLExecuteHandler operationSQLExecuteHandler;
    private final SQLFormatHandler sqlFormatHandler;

    @Inject
    public MysqlModule(IGraphQLDocumentManager manager,
                       DocumentBuilder documentBuilder,
                       IntrospectionMutationBuilder introspectionMutationBuilder,
                       GraphQLTypeToTable graphqlTypeToTable,
                       GraphQLMutationToStatements mutationToStatements,
                       TableCreator tableCreator,
                       MutationExecutor mutationExecutor,
                       OperationToSQLConvertHandler operationToSQLConvertHandler,
                       OperationSQLExecuteHandler operationSQLExecuteHandler,
                       SQLFormatHandler sqlFormatHandler) {

        this.manager = manager;
        this.documentBuilder = documentBuilder;
        this.introspectionMutationBuilder = introspectionMutationBuilder;
        this.graphqlTypeToTable = graphqlTypeToTable;
        this.mutationToStatements = mutationToStatements;
        this.tableCreator = tableCreator;
        this.mutationExecutor = mutationExecutor;
        this.operationToSQLConvertHandler = operationToSQLConvertHandler;
        this.operationSQLExecuteHandler = operationSQLExecuteHandler;
        this.sqlFormatHandler = sqlFormatHandler;
    }

    @Provides
    @Singleton
    public BootstrapHandler mysqlBootstrapHandler() {

        return new MysqlBootstrapHandler(
                mysqlConfig,
                manager,
                documentBuilder,
                introspectionMutationBuilder,
                graphqlTypeToTable,
                mutationToStatements,
                tableCreator,
                mutationExecutor
        );
    }

    @Provides
    @Singleton
    public GeneratorHandler generatorHandler() {
        return new MysqlGeneratorHandler(operationToSQLConvertHandler, sqlFormatHandler);
    }

    @Provides
    @Singleton
    public OperationHandler mysqlR2dbcHandler() {
        return new MysqlR2dbcHandler(operationToSQLConvertHandler, operationSQLExecuteHandler);
    }
}
