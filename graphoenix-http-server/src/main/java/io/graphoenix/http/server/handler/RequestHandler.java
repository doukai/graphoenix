package io.graphoenix.http.server.handler;

import io.graphoenix.spi.dto.GraphQLRequestBody;
import io.netty.handler.codec.http.FullHttpRequest;

public interface RequestHandler {
    GraphQLRequestBody handle(FullHttpRequest fullHttpRequest);
}
