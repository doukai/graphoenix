package io.graphoenix.grpc.server;

import io.graphoenix.core.bootstrap.GraphoenixServer;
import io.grpc.Server;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.tinylog.Logger;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class GraphQLGrpcGraphoenixServer implements GraphoenixServer {

    private final Server server;

    @Inject
    public GraphQLGrpcGraphoenixServer(Server server) {
        this.server = server;
    }

    @Override
    public void run() throws IOException, InterruptedException {
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Logger.error(e);
            }
        }));
        server.awaitTermination();
    }
}
