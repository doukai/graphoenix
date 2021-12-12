package io.graphoenix.java.generator.implementer;

import dagger.Component;
import io.graphoenix.java.generator.module.JavaGeneratorModule;

import javax.inject.Singleton;

@Singleton
@Component(modules = JavaGeneratorModule.class)
public interface OperationInterfaceImplementerFactory {

    OperationInterfaceImplementer get();

}
