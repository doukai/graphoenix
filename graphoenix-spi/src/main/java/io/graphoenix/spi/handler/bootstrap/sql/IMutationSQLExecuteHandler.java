package io.graphoenix.spi.handler.bootstrap.sql;

import io.graphoenix.spi.dto.SQLStatements;
import io.graphoenix.spi.handler.bootstrap.IBootstrapHandler;

public interface IMutationSQLExecuteHandler extends IBootstrapHandler<SQLStatements, Void> {
}
