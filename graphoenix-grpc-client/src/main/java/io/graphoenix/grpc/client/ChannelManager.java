package io.graphoenix.grpc.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class ChannelManager {

    private final ConcurrentHashMap<String, ManagedChannel> channelMap = new ConcurrentHashMap<>();

    public ManagedChannel getChannel(String packageName) {
        return channelMap.computeIfAbsent(packageName, key -> ManagedChannelBuilder.forAddress("localhost", 50051).usePlaintext().build());
    }
}
