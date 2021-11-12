package io.graphoenix.http.server.handler;

import com.google.gson.Gson;
import io.graphoenix.spi.dto.GraphQLRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;

import java.nio.charset.StandardCharsets;

public class PostRequestHandler implements RequestHandler {

    @Override
    public GraphQLRequest handle(FullHttpRequest fullHttpRequest) {
        String contentType = this.getContentType(fullHttpRequest.headers());
        if (contentType.equals("application/json")) {
            return new Gson().fromJson(fullHttpRequest.content().toString(StandardCharsets.UTF_8), GraphQLRequest.class);
        } else {
            throw new IllegalArgumentException("only receive application/json type data");
        }
    }

    private String getContentType(HttpHeaders headers) {
        String typeStr = headers.get("Content-Type");
        String[] list = typeStr.split(";");
        return list[0];
    }
}
