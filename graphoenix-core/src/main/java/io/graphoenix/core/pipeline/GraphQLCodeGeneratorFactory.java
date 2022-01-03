package io.graphoenix.core.pipeline;

import dagger.Component;

import javax.inject.Singleton;

@Singleton
//@Component(modules = PipelineModule.class)
public interface GraphQLCodeGeneratorFactory {

    GraphQLCodeGenerator buildGenerator();
}
