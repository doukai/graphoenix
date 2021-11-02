package io.graphoenix.spi.handler.operation.convert;

import io.graphoenix.spi.dto.GraphQLRequestBody;
import io.graphoenix.spi.dto.SQLStatements;
import io.graphoenix.spi.handler.operation.IOperationHandler;

public interface IOperationToSQLConvertHandler extends IOperationHandler<GraphQLRequestBody, SQLStatements> {
}
