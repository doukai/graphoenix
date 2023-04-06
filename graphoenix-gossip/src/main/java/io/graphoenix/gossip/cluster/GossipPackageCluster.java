package io.graphoenix.gossip.cluster;

import io.graphoenix.core.config.PackageConfig;
import io.graphoenix.gossip.config.GossipConfig;
import io.scalecube.cluster.Cluster;
import io.scalecube.cluster.ClusterImpl;
import io.scalecube.cluster.ClusterMessageHandler;
import io.scalecube.cluster.transport.api.Message;
import io.scalecube.net.Address;
import io.scalecube.transport.netty.tcp.TcpTransportFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
public class GossipPackageCluster implements Runnable {

    private final GossipConfig gossipConfig;
    private final PackageConfig packageConfig;

    @Inject
    public GossipPackageCluster(GossipConfig gossipConfig, PackageConfig packageConfig) {
        this.gossipConfig = gossipConfig;
        this.packageConfig = packageConfig;
    }

    @Override
    public void run() {

        Cluster alice =
                new ClusterImpl()
                        .membership(opts -> opts.seedMembers(Stream.ofNullable(gossipConfig.getSeedMembers()).flatMap(Collection::stream).map(Address::from).collect(Collectors.toList())))
                        .transportFactory(TcpTransportFactory::new).handler(
                                cluster -> new ClusterMessageHandler() {
                                    @Override
                                    public void onGossip(Message gossip) {

                                        System.out.println("Alice heard: " + gossip.data());
                                    }
                                }
                        )
                        .startAwait();

    }
}
