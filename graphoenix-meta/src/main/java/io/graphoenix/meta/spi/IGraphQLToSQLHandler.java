package io.graphoenix.meta.spi;

import io.graphoenix.meta.dto.GraphQLRequestBody;
import io.graphoenix.meta.dto.SQLStatements;

public interface IGraphQLToSQLHandler extends IGraphQLOperationHandler<GraphQLRequestBody, SQLStatements> {
}
