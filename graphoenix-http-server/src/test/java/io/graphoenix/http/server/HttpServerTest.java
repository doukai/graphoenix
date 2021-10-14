package io.graphoenix.http.server;

import org.junit.jupiter.api.Test;

public class HttpServerTest {

    @Test
    void serverTest() throws Exception {
        GraphqlHttpServer graphqlHttpServer = new GraphqlHttpServer();
        graphqlHttpServer.run();
    }
}
