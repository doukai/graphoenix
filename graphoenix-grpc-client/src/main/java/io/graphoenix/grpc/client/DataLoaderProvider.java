package io.graphoenix.grpc.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;
import org.dataloader.DataLoaderOptions;

@ApplicationScoped
public class DataLoaderProvider {

    @Produces
    @RequestScoped
    public DataLoaderOptions dataLoaderOptions() {
        return DataLoaderOptions.newOptions();
    }

    @Produces
    @ApplicationScoped
    public ManagedChannel managedChannel() {
        return ManagedChannelBuilder.forAddress("localhost", 50051).usePlaintext().build();
    }
}
