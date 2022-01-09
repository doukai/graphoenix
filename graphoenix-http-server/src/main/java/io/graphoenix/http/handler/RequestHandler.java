package io.graphoenix.http.handler;

import io.graphoenix.spi.dto.GraphQLRequest;
import io.netty.handler.codec.http.FullHttpRequest;

public interface RequestHandler {

    GraphQLRequest handle(FullHttpRequest fullHttpRequest);
}
