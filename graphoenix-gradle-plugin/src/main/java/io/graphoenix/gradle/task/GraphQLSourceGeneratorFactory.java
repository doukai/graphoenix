package io.graphoenix.gradle.task;

import dagger.BindsInstance;
import dagger.Component;
import io.graphoenix.gradle.module.GradlePluginModule;
import io.graphoenix.java.generator.config.JavaGeneratorConfig;

import javax.inject.Singleton;

@Singleton
@Component(modules = GradlePluginModule.class)
public interface GraphQLSourceGeneratorFactory {
    GraphQLSourceGenerator get();

    @Component.Builder
    interface Builder {
        @BindsInstance
        Builder config(JavaGeneratorConfig config);

        GraphQLSourceGeneratorFactory build();
    }
}
