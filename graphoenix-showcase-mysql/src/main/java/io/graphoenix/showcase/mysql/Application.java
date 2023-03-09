package io.graphoenix.showcase.mysql;

import io.graphoenix.core.bootstrap.GraphoenixStarter;
import io.graphoenix.grpc.server.GraphQLGrpcGraphoenixServer;
import io.graphoenix.http.server.GraphQLHttpGraphoenixServer;

@io.graphoenix.spi.annotation.Application
public class Application {

    public static void main(String[] args) {
        GraphoenixStarter.with(GraphQLHttpGraphoenixServer.class, GraphQLGrpcGraphoenixServer.class).run();
    }
}
