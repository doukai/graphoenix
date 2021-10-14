package io.graphoenix.http.server.handler;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GetRequestHandler implements RequestHandler {
    @Override
    public Object handle(FullHttpRequest fullHttpRequest) {
        String requestUri = fullHttpRequest.uri();
        Map<String, String> queryParameterMappings = this.getQueryParams(requestUri);
        return queryParameterMappings.toString();
    }

    private Map<String, String> getQueryParams(String uri) {
        QueryStringDecoder queryDecoder = new QueryStringDecoder(uri, StandardCharsets.UTF_8);
        Map<String, List<String>> parameters = queryDecoder.parameters();
        Map<String, String> queryParams = new HashMap<>();
        for (Map.Entry<String, List<String>> attr : parameters.entrySet()) {
            for (String attrVal : attr.getValue()) {
                queryParams.put(attr.getKey(), attrVal);
            }
        }
        return queryParams;
    }

}
