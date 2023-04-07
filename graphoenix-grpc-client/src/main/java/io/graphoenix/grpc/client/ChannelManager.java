package io.graphoenix.grpc.client;

import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.spi.handler.PackageRegister;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class ChannelManager {
    private static final String PROTOCOL = "grpc";

    private final ConcurrentHashMap<String, ManagedChannel> channelMap = new ConcurrentHashMap<>();

    private final Provider<PackageRegister> packageRegisterProvider;

    @Inject
    public ChannelManager(GraphQLConfig graphQLConfig) {
        this.packageRegisterProvider = BeanContext.getProvider(PackageRegister.class, graphQLConfig.getPackageRegister());
    }

    public ManagedChannel getChannel(String packageName) {
        URL url = packageRegisterProvider.get().getURL(packageName, PROTOCOL);
        return channelMap.computeIfAbsent(packageName, key -> ManagedChannelBuilder.forAddress(url.getHost(), url.getPort()).usePlaintext().build());
    }
}
