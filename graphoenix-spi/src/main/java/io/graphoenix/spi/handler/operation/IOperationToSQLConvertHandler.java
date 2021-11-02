package io.graphoenix.spi.handler.operation;

import io.graphoenix.spi.dto.GraphQLRequestBody;
import io.graphoenix.spi.dto.SQLStatements;

public interface IOperationToSQLConvertHandler extends IOperationHandler<GraphQLRequestBody, SQLStatements> {
}
