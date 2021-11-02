package io.graphoenix.spi.handler.operation.sql;

import io.graphoenix.spi.dto.GraphQLResult;
import io.graphoenix.spi.dto.SQLStatements;
import io.graphoenix.spi.handler.operation.IOperationHandler;

public interface IOperationSQLExecuteHandler extends IOperationHandler<SQLStatements, GraphQLResult> {
}
