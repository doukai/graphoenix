package io.graphoenix.java.generator.module;

import dagger.Module;
import dagger.Provides;
import io.graphoenix.core.module.DocumentManagerModule;
import io.graphoenix.java.generator.builder.JavaFileBuilder;
import io.graphoenix.java.generator.builder.TypeSpecBuilder;
import io.graphoenix.java.generator.implementer.OperationInterfaceImplementer;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.java.generator.config.JavaGeneratorConfig;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.inject.Inject;
import javax.inject.Singleton;

@Module(includes = DocumentManagerModule.class)
public class JavaGeneratorModule {

    @ConfigProperty
    private JavaGeneratorConfig configuration;

    private final IGraphQLDocumentManager manager;

    @Inject
    public JavaGeneratorModule(IGraphQLDocumentManager manager) {
        this.manager = manager;
    }

    @Provides
    @Singleton
    public TypeSpecBuilder typeSpecBuilder() {
        return new TypeSpecBuilder(manager, configuration);
    }

    @Provides
    @Singleton
    public JavaFileBuilder javaFileBuilder() {
        return new JavaFileBuilder(manager, configuration, typeSpecBuilder());
    }

    @Provides
    @Singleton
    public OperationInterfaceImplementer operationInterfaceImplementer() {
        return new OperationInterfaceImplementer();
    }
}
