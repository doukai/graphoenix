package io.graphoenix.spi.handler.operation;

import io.graphoenix.spi.dto.GraphQLResult;
import io.graphoenix.spi.dto.SQLStatements;

public interface IOperationSQLExecuteHandler extends IOperationHandler<SQLStatements, GraphQLResult> {
}
