package io.graphoenix.grpc.server;

import io.graphoenix.core.bootstrap.GraphoenixServer;
import io.grpc.Server;
import io.vavr.CheckedRunnable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.tinylog.Logger;
import reactor.core.publisher.Mono;

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
    public Mono<Void> run() {
        return Mono.fromRunnable(CheckedRunnable.of(this::start).unchecked());
    }

    private void start() throws IOException, InterruptedException {
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
