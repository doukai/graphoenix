package io.graphoenix.java.generator.module;

import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.java.generator.builder.JavaFileBuilder;
import io.graphoenix.java.generator.builder.ModuleContextBuilder;
import io.graphoenix.java.generator.builder.TypeSpecBuilder;
import io.graphoenix.java.generator.implementer.InvokeHandlerImplementer;
import io.graphoenix.java.generator.implementer.OperationInterfaceImplementer;
import io.graphoenix.java.generator.implementer.QueryHandlerImplementer;
import io.graphoenix.java.generator.implementer.TypeManager;
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
    public ModuleContextBuilder moduleContextBuilder() {
        return new ModuleContextBuilder();
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
    public QueryHandlerImplementer queryHandlerImplementer() {
        return new QueryHandlerImplementer(manager, typeManager());
    }
}
