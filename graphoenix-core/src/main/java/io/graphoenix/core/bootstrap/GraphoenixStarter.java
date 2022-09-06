package io.graphoenix.core.bootstrap;

import io.graphoenix.core.context.BeanContext;
import io.graphoenix.spi.handler.ScopeEventResolver;
import jakarta.enterprise.context.ApplicationScoped;
import org.tinylog.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static io.graphoenix.core.utils.BannerUtil.BANNER_UTIL;

public class GraphoenixStarter {

    private CountDownLatch latch;

    private List<GraphoenixServer> serverList;

    private GraphoenixStarter() {
    }

    public GraphoenixStarter addServers(GraphoenixServer... servers) {
        if (this.serverList == null) {
            this.serverList = new ArrayList<>();
        }
        this.serverList.addAll(List.of(servers));
        return this;
    }

    public static GraphoenixStarter with(GraphoenixServer... servers) {
        return getInstance().addServers(servers);
    }

    @SafeVarargs
    public static <T extends GraphoenixServer> GraphoenixStarter with(Class<T>... classes) {
        return getInstance().addServers(Arrays.stream(classes).map(BeanContext::get).toArray(GraphoenixServer[]::new));
    }

    public void run() throws Exception {
        latch = new CountDownLatch(1);
        ScopeEventResolver.initialized(ApplicationScoped.class)
                .then(Flux.fromIterable(serverList)
                        .map(graphoenixServer ->
                                new Thread(() -> {
                                    try {
                                        latch.await();
                                        graphoenixServer.run();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                })
                        )
                        .flatMap(thread -> Mono.fromRunnable(()->{
                            thread.start();
                            try {
                                thread.join();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }))
                        .then(Mono.fromRunnable(() -> latch.countDown()))
                )
                .doFirst(() -> BANNER_UTIL.getBanner().ifPresent(Logger::info))
                .block();
    }

    private static class Holder {
        private static final GraphoenixStarter INSTANCE = new GraphoenixStarter();
    }

    public static GraphoenixStarter getInstance() {
        return Holder.INSTANCE;
    }
}
