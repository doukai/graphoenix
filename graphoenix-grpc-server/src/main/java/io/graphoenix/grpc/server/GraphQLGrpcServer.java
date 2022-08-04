package io.graphoenix.grpc.server;

import io.graphoenix.spi.handler.ScopeEventResolver;
import io.grpc.Server;
import io.vavr.CheckedRunnable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.tinylog.Logger;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static io.graphoenix.core.utils.BannerUtil.BANNER_UTIL;

@ApplicationScoped
public class GraphQLGrpcServer {

    private final Server server;

    @Inject
    public GraphQLGrpcServer(Server server) {
        this.server = server;
    }

    public void run() {
        BANNER_UTIL.getBanner().ifPresent(Logger::info);
        ScopeEventResolver.initialized(ApplicationScoped.class).then(Mono.fromRunnable(CheckedRunnable.of(this::start).unchecked())).block();
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
