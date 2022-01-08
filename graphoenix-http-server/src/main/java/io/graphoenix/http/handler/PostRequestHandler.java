package io.graphoenix.http.handler;

import com.google.gson.Gson;
import io.graphoenix.spi.dto.GraphQLRequest;
import io.netty.handler.codec.http.FullHttpRequest;

import java.nio.charset.StandardCharsets;

import static io.graphoenix.http.codec.HttpHeaderValues.APPLICATION_GRAPHQL;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;

public class PostRequestHandler implements RequestHandler {

    @Override
    public GraphQLRequest handle(FullHttpRequest fullHttpRequest) {
        String contentType = fullHttpRequest.headers().get(CONTENT_TYPE);
        if (contentType.contentEquals(APPLICATION_JSON)) {
            return new Gson().fromJson(fullHttpRequest.content().toString(StandardCharsets.UTF_8), GraphQLRequest.class);
        } else if (contentType.contentEquals(APPLICATION_GRAPHQL)) {
            return new GraphQLRequest(fullHttpRequest.content().toString(StandardCharsets.UTF_8));
        } else {
            throw new IllegalArgumentException("unsupported content-type:".concat(contentType));
        }
    }
}
