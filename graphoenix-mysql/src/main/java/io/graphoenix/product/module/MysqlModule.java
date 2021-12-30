package io.graphoenix.product.module;

import dagger.Module;
import dagger.Provides;
import io.graphoenix.core.module.DocumentManagerModule;
import io.graphoenix.graphql.builder.introspection.IntrospectionMutationBuilder;
import io.graphoenix.graphql.builder.module.GraphQLBuilderModule;
import io.graphoenix.graphql.builder.schema.DocumentBuilder;
import io.graphoenix.mysql.handler.operation.OperationToSQLConvertHandler;
import io.graphoenix.mysql.module.MySQLTranslatorModule;
import io.graphoenix.mysql.translator.GraphQLMutationToStatements;
import io.graphoenix.mysql.translator.GraphQLTypeToTable;
import io.graphoenix.product.config.MysqlConfig;
import io.graphoenix.product.handler.MysqlBootstrapHandler;
import io.graphoenix.product.handler.MysqlR2dbcHandler;
import io.graphoenix.r2dbc.connector.executor.MutationExecutor;
import io.graphoenix.r2dbc.connector.executor.TableCreator;
import io.graphoenix.r2dbc.connector.handler.operation.OperationSQLExecuteHandler;
import io.graphoenix.r2dbc.connector.module.R2dbcConnectorModule;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.handler.BootstrapHandler;
import io.graphoenix.spi.handler.OperationHandler;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.inject.Singleton;

@Module(includes = {DocumentManagerModule.class, GraphQLBuilderModule.class, R2dbcConnectorModule.class, MySQLTranslatorModule.class})
public class MysqlModule {

    @ConfigProperty
    private MysqlConfig mysqlConfig;

    @Provides
    @Singleton
    public BootstrapHandler mysqlBootstrapHandler(
            IGraphQLDocumentManager manager,
            DocumentBuilder documentBuilder,
            IntrospectionMutationBuilder introspectionMutationBuilder,
            GraphQLTypeToTable graphqlTypeToTable,
            GraphQLMutationToStatements mutationToStatements,
            TableCreator tableCreator,
            MutationExecutor mutationExecutor) {

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
    public OperationHandler mysqlR2dbcHandler(OperationToSQLConvertHandler operationToSQLConvertHandler,
                                              OperationSQLExecuteHandler operationSQLExecuteHandler) {
        return new MysqlR2dbcHandler(operationToSQLConvertHandler, operationSQLExecuteHandler);
    }
}
