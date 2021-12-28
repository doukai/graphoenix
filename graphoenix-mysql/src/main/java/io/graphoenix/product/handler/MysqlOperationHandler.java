package io.graphoenix.product.handler;

import dagger.Module;
import io.graphoenix.mysql.module.MySQLTranslatorModule;
import io.graphoenix.r2dbc.connector.module.R2dbcConnectorModule;

@Module(includes = {R2dbcConnectorModule.class, MySQLTranslatorModule.class})
public class MysqlOperationHandler {



}
