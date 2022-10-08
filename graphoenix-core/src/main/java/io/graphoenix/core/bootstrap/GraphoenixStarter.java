package io.graphoenix.core.bootstrap;

import io.graphoenix.core.context.BeanContext;
import io.graphoenix.spi.handler.ScopeEventResolver;
import jakarta.enterprise.context.ApplicationScoped;
import org.tinylog.Logger;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static io.graphoenix.core.utils.BannerUtil.BANNER_UTIL;

public class GraphoenixStarter {

    private CountDownLatch latch;

    private List<Runnable> serverList;

    private GraphoenixStarter() {
    }

    public GraphoenixStarter addServers(Runnable... servers) {
        if (this.serverList == null) {
            this.serverList = new ArrayList<>();
        }
        this.serverList.addAll(List.of(servers));
        return this;
    }

    public static GraphoenixStarter with(Runnable... servers) {
        return getInstance().addServers(servers);
    }

    @SafeVarargs
    public static GraphoenixStarter with(Class<? extends Runnable>... classes) {
        return getInstance().addServers(Arrays.stream(classes).map(BeanContext::get).toArray(Runnable[]::new));
    }

    public void run() {
        ExecutorService executorService = Executors.newCachedThreadPool();
        this.latch = new CountDownLatch(1);
        if (serverList != null && serverList.size() > 0) {
            for (Runnable server : serverList) {
                executorService.execute(
                        new Thread(() -> {
                            try {
                                latch.await();
                            } catch (InterruptedException e) {
                                Logger.error(e);
                            }
                            server.run();
                        })
                );
            }
        }

        ScopeEventResolver.initialized(ApplicationScoped.class)
                .then(Mono.fromRunnable(() -> latch.countDown()))
                .doFirst(() -> BANNER_UTIL.getBanner().ifPresent(Logger::info))
                .block();

        executorService.shutdown();
    }

    private static class Holder {
        private static final GraphoenixStarter INSTANCE = new GraphoenixStarter();
    }

    public static GraphoenixStarter getInstance() {
        return Holder.INSTANCE;
    }
}
