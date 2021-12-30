package io.graphoenix.core.pipeline;

import dagger.Component;
import io.graphoenix.core.module.PipelineModule;

import javax.inject.Singleton;

@Singleton
@Component(modules = PipelineModule.class)
public interface GraphQLDataFetcherFactory {

    GraphQLDataFetcher buildFetcher();
}
