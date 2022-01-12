package io.graphoenix.graphql.generator.module;

import io.graphoenix.graphql.generator.translator.*;
import io.graphoenix.spi.module.Module;
import io.graphoenix.spi.module.Provides;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;

import javax.inject.Inject;
import javax.inject.Singleton;

@Module
public class GraphQLGeneratorModule {

    private final IGraphQLDocumentManager manager;

    @Inject
    public GraphQLGeneratorModule(IGraphQLDocumentManager manager) {
        this.manager = manager;
    }

    @Provides
    @Singleton
    ElementManager elementManager() {
        return new ElementManager(manager);
    }

    @Provides
    @Singleton
    MethodToQueryOperation methodToQueryOperation() {
        return new MethodToQueryOperation(manager, elementManager());
    }

    @Provides
    @Singleton
    MethodToMutationOperation methodToMutationOperation() {
        return new MethodToMutationOperation(manager, elementManager());
    }

    @Provides
    @Singleton
    JavaElementToOperation javaElementToOperation() {
        return new JavaElementToOperation(methodToQueryOperation(), methodToMutationOperation());
    }

    @Provides
    @Singleton
    JavaElementToEnum javaElementToEnum() {
        return new JavaElementToEnum(elementManager());
    }

    @Provides
    @Singleton
    JavaElementToObject javaElementToObject() {
        return new JavaElementToObject(elementManager());
    }

    @Provides
    @Singleton
    JavaElementToInterface javaElementToInterface() {
        return new JavaElementToInterface(elementManager());
    }

    @Provides
    @Singleton
    JavaElementToInputType javaElementToInputType() {
        return new JavaElementToInputType(elementManager());
    }
}
