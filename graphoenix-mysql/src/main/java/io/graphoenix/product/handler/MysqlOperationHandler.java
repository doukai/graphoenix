package io.graphoenix.product.handler;

import dagger.Module;
import dagger.Provides;
import io.graphoenix.mysql.handler.operation.OperationToSQLConvertHandler;
import io.graphoenix.mysql.module.MySQLTranslatorModule;
import io.graphoenix.r2dbc.connector.handler.operation.OperationSQLExecuteHandler;
import io.graphoenix.r2dbc.connector.module.R2dbcConnectorModule;
import io.graphoenix.spi.handler.IOperationHandler;
import io.graphoenix.spi.patterns.ChainsBean;
import io.graphoenix.spi.patterns.ChainsBeanBuilder;

import javax.inject.Singleton;

@Module(includes = {R2dbcConnectorModule.class, MySQLTranslatorModule.class})
public class MysqlOperationHandler {

    @Provides
    @Singleton
    @ChainsBean
    public IOperationHandler mySQLHandler(OperationToSQLConvertHandler operationToSQLConvertHandler, OperationSQLExecuteHandler operationSQLExecuteHandler) {

        ChainsBeanBuilder chainsBeanBuilder = ChainsBeanBuilder.create();
        chainsBeanBuilder.add("query", operationToSQLConvertHandler);
        chainsBeanBuilder.add("query", operationSQLExecuteHandler);
        chainsBeanBuilder.add("querySelections", operationToSQLConvertHandler);
        chainsBeanBuilder.add("querySelections", operationSQLExecuteHandler);
        chainsBeanBuilder.add("mutation", operationToSQLConvertHandler);
        chainsBeanBuilder.add("mutation", operationSQLExecuteHandler);
        return chainsBeanBuilder.build(MySQLHandler.class);
    }
}
