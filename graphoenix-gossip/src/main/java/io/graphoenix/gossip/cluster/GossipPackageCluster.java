package io.graphoenix.gossip.cluster;

import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.config.PackageConfig;
import io.graphoenix.core.handler.PackageManager;
import io.graphoenix.gossip.config.GossipConfig;
import io.graphoenix.gossip.handler.GossipPackageRegister;
import io.scalecube.cluster.ClusterImpl;
import io.scalecube.cluster.ClusterMessageHandler;
import io.scalecube.cluster.membership.MembershipEvent;
import io.scalecube.net.Address;
import io.scalecube.transport.netty.tcp.TcpTransportFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
public class GossipPackageCluster implements Runnable {

    private final GraphQLConfig graphQLConfig;
    private final GossipConfig gossipConfig;
    private final PackageConfig packageConfig;
    private final PackageManager packageManager;
    private final GossipPackageRegister gossipPackageRegister;

    @Inject
    public GossipPackageCluster(GraphQLConfig graphQLConfig, GossipConfig gossipConfig, PackageConfig packageConfig, PackageManager packageManager, GossipPackageRegister gossipPackageRegister) {
        this.graphQLConfig = graphQLConfig;
        this.gossipConfig = gossipConfig;
        this.packageConfig = packageConfig;
        this.packageManager = packageManager;
        this.gossipPackageRegister = gossipPackageRegister;
    }

    @Override
    public void run() {
        new ClusterImpl()
                .membership(opts ->
                        opts.seedMembers(
                                Stream.ofNullable(gossipConfig.getSeedMembers())
                                        .flatMap(Collection::stream)
                                        .map(Address::from)
                                        .collect(Collectors.toList())
                        )
                )
                .config(opts ->
                        opts.memberAlias(
                                Optional.ofNullable(graphQLConfig.getPackageName())
                                        .orElseGet(packageManager::getDefaultPackageName)
                        )
                )
                .transportFactory(TcpTransportFactory::new)
                .handler(cluster -> new ClusterMessageHandler() {
                            @Override
                            public void onMembershipEvent(MembershipEvent event) {
                                switch (event.type()) {
                                    case ADDED:
                                    case LEAVING:
                                    case REMOVED:
                                    case UPDATED:
                                }
                            }
                        }
                )
                .startAwait();
    }
}
