package io.graphoenix.showcase.order;

import io.graphoenix.core.bootstrap.GraphoenixStarter;
import io.graphoenix.gossip.cluster.GossipPackageCluster;
import io.graphoenix.grpc.server.GraphQLGrpcGraphoenixServer;
import io.graphoenix.http.server.GraphQLHttpGraphoenixServer;
import io.graphoenix.spi.annotation.Application;

@Application
public class OrderApplication {

    public static void main(String[] args) {
        GraphoenixStarter.with(GraphQLHttpGraphoenixServer.class, GraphQLGrpcGraphoenixServer.class, GossipPackageCluster.class).run();
    }
}
