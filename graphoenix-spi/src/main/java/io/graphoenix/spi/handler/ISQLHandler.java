package io.graphoenix.spi.handler;

import io.graphoenix.spi.dto.GraphQLResult;
import io.graphoenix.spi.dto.SQLStatements;

public interface ISQLHandler extends IGraphQLOperationHandler<SQLStatements, GraphQLResult> {
}
