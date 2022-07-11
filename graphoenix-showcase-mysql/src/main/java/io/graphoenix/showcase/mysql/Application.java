package io.graphoenix.showcase.mysql;

import io.graphoenix.core.context.BeanContext;
import io.graphoenix.http.server.GraphQLHttpServer;
import io.graphoenix.spi.annotation.GraphoenixApplication;

@GraphoenixApplication
public class Application {

    public static void main(String[] args) throws Exception {
        new Application().run();
    }

    private void run() throws Exception {
        GraphQLHttpServer graphqlHttpServer = BeanContext.get(GraphQLHttpServer.class);
        graphqlHttpServer.run();
    }
}
