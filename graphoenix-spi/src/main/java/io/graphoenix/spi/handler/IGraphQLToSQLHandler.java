package io.graphoenix.spi.handler;

import io.graphoenix.spi.dto.GraphQLRequestBody;
import io.graphoenix.spi.dto.SQLStatements;

public interface IGraphQLToSQLHandler extends IGraphQLOperationHandler<GraphQLRequestBody, SQLStatements> {
}
