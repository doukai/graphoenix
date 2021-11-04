package io.graphoenix.http.server.handler;

import io.graphoenix.spi.dto.GraphQLRequest;
import io.netty.handler.codec.http.FullHttpRequest;

public interface RequestHandler {
    GraphQLRequest handle(FullHttpRequest fullHttpRequest);
}
