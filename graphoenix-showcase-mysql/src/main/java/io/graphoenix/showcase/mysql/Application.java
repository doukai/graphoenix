package io.graphoenix.showcase.mysql;

import io.graphoenix.core.context.BeanContext;
import io.graphoenix.http.server.GraphqlHttpServer;

public class Application {

    public static void main(String[] args) throws Exception {
        new Application().run();
    }

    private void run() throws Exception {

        GraphqlHttpServer graphqlHttpServer = BeanContext.get(GraphqlHttpServer.class);
        graphqlHttpServer.run();
    }
}
