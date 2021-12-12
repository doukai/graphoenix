package io.graphoenix.java.generator.module;

import dagger.Module;
import dagger.Provides;
import io.graphoenix.common.module.DocumentManagerModule;
import io.graphoenix.java.generator.builder.JavaFileBuilder;
import io.graphoenix.java.generator.builder.TypeSpecBuilder;
import io.graphoenix.java.generator.implementer.OperationInterfaceImplementer;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.config.JavaGeneratorConfig;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.inject.Singleton;

@Module(includes = DocumentManagerModule.class)
public class JavaGeneratorModule {

    @ConfigProperty
    private JavaGeneratorConfig configuration;

    @Provides
    @Singleton
    public TypeSpecBuilder typeSpecBuilder(IGraphQLDocumentManager manager) {
        return new TypeSpecBuilder(manager, configuration);
    }

    @Provides
    @Singleton
    public JavaFileBuilder javaFileBuilder(IGraphQLDocumentManager manager) {
        return new JavaFileBuilder(manager, configuration, typeSpecBuilder(manager));
    }

    @Provides
    @Singleton
    public OperationInterfaceImplementer operationInterfaceImplementer() {
        return new OperationInterfaceImplementer();
    }
}
