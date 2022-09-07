package io.graphoenix.showcase.mysql;

import io.graphoenix.core.bootstrap.GraphoenixStarter;
import io.graphoenix.grpc.server.GraphQLGrpcGraphoenixServer;
import io.graphoenix.http.server.GraphQLHttpGraphoenixServer;
import io.graphoenix.spi.annotation.GraphoenixApplication;

@GraphoenixApplication
public class Application {

    public static void main(String[] args) {
        GraphoenixStarter.with(GraphQLHttpGraphoenixServer.class, GraphQLGrpcGraphoenixServer.class).run();
    }
}
