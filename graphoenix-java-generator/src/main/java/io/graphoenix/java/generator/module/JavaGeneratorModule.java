package io.graphoenix.java.generator.module;

import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.java.generator.builder.JavaFileBuilder;
import io.graphoenix.java.generator.builder.ModuleBuilder;
import io.graphoenix.java.generator.builder.TypeSpecBuilder;
import io.graphoenix.java.generator.implementer.*;
import io.graphoenix.java.generator.implementer.OperationHandlerImplementer;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.module.Module;
import io.graphoenix.spi.module.Provides;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.inject.Inject;
import javax.inject.Singleton;

@Module
public class JavaGeneratorModule {

    @ConfigProperty
    private GraphQLConfig graphQLConfig;

    private final IGraphQLDocumentManager manager;

    @Inject
    public JavaGeneratorModule(IGraphQLDocumentManager manager) {
        this.manager = manager;
    }

    @Provides
    @Singleton
    public TypeSpecBuilder typeSpecBuilder() {
        return new TypeSpecBuilder(manager, graphQLConfig);
    }

    @Provides
    @Singleton
    public JavaFileBuilder javaFileBuilder() {
        return new JavaFileBuilder(manager, graphQLConfig, typeSpecBuilder());
    }

    @Provides
    @Singleton
    public ModuleBuilder moduleBuilder() {
        return new ModuleBuilder();
    }

    @Provides
    @Singleton
    public TypeManager typeManager() {
        return new TypeManager(manager);
    }

    @Provides
    @Singleton
    public OperationInterfaceImplementer operationInterfaceImplementer() {
        return new OperationInterfaceImplementer();
    }

    @Provides
    @Singleton
    public InvokeHandlerImplementer invokeHandlerImplementer() {
        return new InvokeHandlerImplementer(manager, typeManager());
    }

    @Provides
    @Singleton
    public SelectionFilterHandlerImplementer invokeHandlerImplementer2() {
        return new SelectionFilterHandlerImplementer(manager, typeManager());
    }

    @Provides
    @Singleton
    public OperationHandlerImplementer queryHandlerImplementer() {
        return new OperationHandlerImplementer(manager, typeManager());
    }
}
