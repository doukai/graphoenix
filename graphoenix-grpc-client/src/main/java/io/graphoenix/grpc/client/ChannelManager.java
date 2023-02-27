package io.graphoenix.grpc.client;

import io.graphoenix.core.handler.PackageManager;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Provider;

import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class ChannelManager {

    private final ConcurrentHashMap<String, ManagedChannel> channelMap = new ConcurrentHashMap<>();

    private final Provider<PackageManager> packageManagerProvider;

    public ChannelManager(Provider<PackageManager> packageManagerProvider) {
        this.packageManagerProvider = packageManagerProvider;
    }

    public ManagedChannel getChannel(String packageName) {
        URI uri = packageManagerProvider.get().getURI(packageName);
        return channelMap.computeIfAbsent(packageName, key -> ManagedChannelBuilder.forAddress(uri.getHost(), uri.getPort()).usePlaintext().build());
    }
}
