package io.graphoenix.http.server.handler;

import io.graphoenix.http.server.dto.graphql.GraphQLRequestBody;
import io.netty.handler.codec.http.FullHttpRequest;

public interface RequestHandler {
    GraphQLRequestBody handle(FullHttpRequest fullHttpRequest);
}
