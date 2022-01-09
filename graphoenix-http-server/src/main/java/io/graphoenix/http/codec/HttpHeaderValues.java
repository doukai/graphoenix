package io.graphoenix.http.codec;

import io.netty.util.AsciiString;

public class HttpHeaderValues {

    /**
     * {@code "application/graphql"}
     */
    public static final AsciiString APPLICATION_GRAPHQL = AsciiString.cached("application/graphql");

    private HttpHeaderValues() { }
}
