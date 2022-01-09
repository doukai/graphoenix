package io.graphoenix.http.handler;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.graphoenix.spi.dto.GraphQLRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class GetRequestHandler implements RequestHandler {

    @Override
    public GraphQLRequest handle(FullHttpRequest fullHttpRequest) {
        String requestUri = fullHttpRequest.uri();
        QueryStringDecoder queryDecoder = new QueryStringDecoder(requestUri, StandardCharsets.UTF_8);
        Map<String, List<String>> parameters = queryDecoder.parameters();
        Type type = new TypeToken<Map<String, Object>>() {}.getType();

        return new GraphQLRequest(
                parameters.get("query").get(0),
                parameters.get("operationName").get(0),
                new Gson().fromJson(parameters.get("variables").get(0), type)
        );
    }
}
