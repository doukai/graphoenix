package io.graphoenix.showcase.mysql;

import io.graphoenix.core.bootstrap.GraphoenixStarter;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.http.server.GraphQLHttpGraphoenixServer;
import io.graphoenix.spi.annotation.GraphoenixApplication;

@GraphoenixApplication
public class Application {

    public static void main(String[] args) throws Exception {
        GraphoenixStarter.with(GraphQLHttpGraphoenixServer.class).run();
    }

    private void run() throws Exception {
        GraphQLHttpGraphoenixServer graphqlHttpServer = BeanContext.get(GraphQLHttpGraphoenixServer.class);
        graphqlHttpServer.run();
//        GraphQLGrpcServer graphQLGrpcServer = BeanContext.get(GraphQLGrpcServer.class);
//        graphQLGrpcServer.run();
    }
}
