package io.graphoenix.graphql.generator.module;

import io.graphoenix.spi.module.Module;
import io.graphoenix.spi.module.Provides;
import io.graphoenix.graphql.generator.translator.ElementManager;
import io.graphoenix.graphql.generator.translator.JavaElementToOperation;
import io.graphoenix.graphql.generator.translator.MethodToMutationOperation;
import io.graphoenix.graphql.generator.translator.MethodToQueryOperation;
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
}
