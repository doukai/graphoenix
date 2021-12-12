package io.graphoenix.graphql.builder.schema;

import dagger.Component;;
import io.graphoenix.graphql.builder.module.GraphQLBuilderModule;

import javax.inject.Singleton;

@Singleton
@Component(modules = GraphQLBuilderModule.class)
public interface DocumentBuilderFactory {
    DocumentBuilder get();
}
