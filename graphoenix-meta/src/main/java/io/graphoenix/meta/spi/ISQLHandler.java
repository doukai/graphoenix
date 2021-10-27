package io.graphoenix.meta.spi;

import io.graphoenix.meta.dto.GraphQLResult;
import io.graphoenix.meta.dto.SQLStatements;

public interface ISQLHandler extends IGraphQLOperationHandler<SQLStatements, GraphQLResult> {
}
