package io.graphoenix.graphql.generator.translator;

import dagger.Component;
import io.graphoenix.graphql.generator.module.GraphQLGeneratorModule;

import javax.inject.Singleton;

@Singleton
@Component(modules = GraphQLGeneratorModule.class)
public interface JavaElementToOperationFactory {

    JavaElementToOperation build();
}
