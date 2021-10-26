package io.graphoenix.meta.spi;

import io.graphoenix.meta.OperationType;
import io.graphoenix.meta.dto.GraphQLRequestBody;
import io.graphoenix.meta.dto.GraphQLResult;

import java.util.concurrent.Future;

public interface IGraphQLOperationHandler {

    OperationType getType(GraphQLRequestBody requestBody);

    GraphQLResult query(GraphQLRequestBody requestBody);

    GraphQLResult mutation(GraphQLRequestBody requestBody);

    Future<GraphQLResult> subscription(GraphQLRequestBody requestBody);
}
