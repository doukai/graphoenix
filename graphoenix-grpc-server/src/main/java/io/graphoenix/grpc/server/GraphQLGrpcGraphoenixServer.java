package io.graphoenix.grpc.server;

import io.graphoenix.grpc.server.config.GrpcServerConfig;
import io.graphoenix.spi.handler.RunningServer;
import io.grpc.Server;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.tinylog.Logger;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
@Named("grpc")
public class GraphQLGrpcGraphoenixServer implements Runnable, RunningServer {

    private final Server server;

    private final GrpcServerConfig grpcServerConfig;


    @Inject
    public GraphQLGrpcGraphoenixServer(Server server, GrpcServerConfig grpcServerConfig) {
        this.server = server;
        this.grpcServerConfig = grpcServerConfig;
    }

    @Override
    public void run() {
        try {
            server.start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Logger.error(e);
                }
            }));
            server.awaitTermination();
        } catch (IOException | InterruptedException e) {
            Logger.error(e);
        }
    }

    @Override
    public String protocol() {
        return "grpc";
    }

    @Override
    public int port() {
        return grpcServerConfig.getPort();
    }
}
