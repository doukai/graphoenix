package io.graphoenix.common.pipeline;

import dagger.Component;
import io.graphoenix.common.module.PipelineModule;

import javax.inject.Singleton;

@Singleton
@Component(modules = PipelineModule.class)
public interface GraphQLDataFetcherFactory {

    GraphQLDataFetcher buildFetcher();
}
