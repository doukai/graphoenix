package io.graphoenix.graphql.builder.handler.bootstrap;

import dagger.Component;
import io.graphoenix.graphql.builder.module.GraphQLBuilderModule;
import io.graphoenix.spi.handler.IBootstrapHandlerFactory;

import javax.inject.Singleton;

@Singleton
@Component(modules = GraphQLBuilderModule.class)
public interface DocumentBuildHandlerFactory extends IBootstrapHandlerFactory {

    @Override
    DocumentBuildHandler createHandler();
}
