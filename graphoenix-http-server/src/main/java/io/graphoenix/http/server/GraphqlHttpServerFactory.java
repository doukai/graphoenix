package io.graphoenix.http.server;

import dagger.Component;
import io.graphoenix.http.module.HttpServerModule;

import javax.inject.Singleton;

@Singleton
@Component(modules = HttpServerModule.class)
public interface GraphqlHttpServerFactory {
    GraphqlHttpServer get();
}
