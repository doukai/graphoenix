package io.graphoenix.product.module;

import dagger.Module;
import dagger.Provides;
import io.graphoenix.mysql.handler.operation.OperationToSQLConvertHandler;
import io.graphoenix.mysql.module.MySQLTranslatorModule;
import io.graphoenix.product.handler.MysqlR2dbcHandler;
import io.graphoenix.r2dbc.connector.handler.operation.OperationSQLExecuteHandler;
import io.graphoenix.r2dbc.connector.module.R2dbcConnectorModule;

import javax.inject.Singleton;

@Module(includes = {R2dbcConnectorModule.class, MySQLTranslatorModule.class})
public class MysqlModule {

    @Provides
    @Singleton
    public MysqlR2dbcHandler mysqlR2dbcHandler(OperationToSQLConvertHandler operationToSQLConvertHandler,
                                               OperationSQLExecuteHandler operationSQLExecuteHandler) {
        return new MysqlR2dbcHandler(operationToSQLConvertHandler, operationSQLExecuteHandler);
    }
}
