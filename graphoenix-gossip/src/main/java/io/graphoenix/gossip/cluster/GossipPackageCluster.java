package io.graphoenix.gossip.cluster;

import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.core.handler.PackageManager;
import io.graphoenix.gossip.config.GossipConfig;
import io.graphoenix.gossip.handler.GossipPackageRegister;
import io.graphoenix.spi.handler.RunningServer;
import io.scalecube.cluster.ClusterImpl;
import io.scalecube.cluster.ClusterMessageHandler;
import io.scalecube.cluster.membership.MembershipEvent;
import io.scalecube.net.Address;
import io.scalecube.transport.netty.tcp.TcpTransportFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.tinylog.Logger;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.graphoenix.spi.handler.PackageRegister.*;

@ApplicationScoped
public class GossipPackageCluster implements Runnable {

    private static final String PACKAGE_NAME = "package";
    private static final String SERVICES_NAME = "services";

    private final GraphQLConfig graphQLConfig;
    private final GossipConfig gossipConfig;
    private final PackageManager packageManager;
    private final GossipPackageRegister gossipPackageRegister;

    @Inject
    public GossipPackageCluster(GraphQLConfig graphQLConfig, GossipConfig gossipConfig, PackageManager packageManager, GossipPackageRegister gossipPackageRegister) {
        this.graphQLConfig = graphQLConfig;
        this.gossipConfig = gossipConfig;
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
                .transport(opts -> opts.port(Optional.ofNullable(gossipConfig.getPort()).orElse(0)))
                .config(opts ->
                        opts.metadata(
                                packageManager.getLocalPackages()
                                        .map(packageName ->
                                                Map.of(PACKAGE_NAME,
                                                        packageName,
                                                        SERVICES_NAME,
                                                        getServices()
                                                )
                                        )
                                        .collect(Collectors.toList())
                        )
                )
                .transportFactory(TcpTransportFactory::new)
                .handler(cluster -> new ClusterMessageHandler() {
                            @SuppressWarnings("unchecked")
                            @Override
                            public void onMembershipEvent(MembershipEvent event) {
                                switch (event.type()) {
                                    case ADDED:
                                    case UPDATED:
                                        Optional<Object> metadataOptional = cluster.metadata(event.member());
                                        if (metadataOptional.isPresent()) {
                                            List<Map<String, Object>> metadata = (List<Map<String, Object>>) metadataOptional.get();
                                            metadata.forEach(packageData -> {
                                                        String packageName = (String) packageData.get(PACKAGE_NAME);
                                                        List<Map<String, Object>> services = (List<Map<String, Object>>) packageData.get(SERVICES_NAME);
                                                        services.forEach(service -> {
                                                                    String protocol = (String) service.get(PROTOCOL_NAME);
                                                                    String host = (String) service.getOrDefault(HOST_NAME, event.member().address().host());
                                                                    int port = (int) service.getOrDefault(PORT_NAME, -1);
                                                                    String file = (String) service.getOrDefault(FILE_NAME, "");
                                                                    gossipPackageRegister.mergeMemberURLs(event.member().address().toString(), packageName, protocol, host, port, file);
                                                                }
                                                        );
                                                        if (services.size() > 0) {
                                                            gossipPackageRegister.mergeMemberProtocolURLList(packageName);
                                                            if (graphQLConfig.getPackageLoadBalance().equals(LOAD_BALANCE_ROUND_ROBIN)) {
                                                                gossipPackageRegister.mergeMemberProtocolURLIterator(packageName);
                                                            }
                                                        }
                                                    }
                                            );
                                        }
                                        Logger.debug(event.member().toString().concat(" merged"));
                                        break;
                                    case LEAVING:
                                        gossipPackageRegister.removeMemberURLs(event.member().address().toString());
                                        Logger.debug(event.member().toString().concat(" leaving"));
                                        break;
                                    case REMOVED:
                                        Logger.debug(event.member().toString().concat(" removed"));
                                        break;
                                }
                            }
                        }
                )
                .startAwait();
    }

    private List<Map<String, Object>> getServices() {
        return Optional.ofNullable(gossipConfig.getServices())
                .orElseGet(() ->
                        BeanContext.getMap(RunningServer.class).values().stream()
                                .map(runningServer -> Map.of(PROTOCOL_NAME, (Object) runningServer.protocol(), PORT_NAME, runningServer.port()))
                                .collect(Collectors.toList())
                );
    }
}
