package io.graphoenix.grpc.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ChannelManager {

    public ManagedChannel getChannel(String packageName) {
        return ManagedChannelBuilder.forAddress("localhost", 50051).usePlaintext().build();
    }
}
