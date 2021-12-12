package io.graphoenix.java.generator.builder;

import dagger.Component;
import io.graphoenix.java.generator.module.JavaGeneratorModule;

import javax.inject.Singleton;

@Singleton
@Component(modules = JavaGeneratorModule.class)
public interface JavaFileBuilderFactory {

    JavaFileBuilder get();
}
