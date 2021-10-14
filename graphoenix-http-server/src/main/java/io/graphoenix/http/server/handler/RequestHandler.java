package io.graphoenix.http.server.handler;

import io.netty.handler.codec.http.FullHttpRequest;

public interface RequestHandler {
    Object handle(FullHttpRequest fullHttpRequest);
}
