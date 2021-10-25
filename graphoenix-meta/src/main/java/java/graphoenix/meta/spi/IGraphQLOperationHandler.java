package java.graphoenix.meta.spi;

import java.graphoenix.meta.dto.GraphQLRequestBody;
import java.graphoenix.meta.dto.GraphQLResult;
import java.util.concurrent.Future;

public interface IGraphQLOperationHandler {

    GraphQLResult query(GraphQLRequestBody requestBody);

    GraphQLResult mutation(GraphQLRequestBody requestBody);

    Future<GraphQLResult> subscription(GraphQLRequestBody requestBody);
}
