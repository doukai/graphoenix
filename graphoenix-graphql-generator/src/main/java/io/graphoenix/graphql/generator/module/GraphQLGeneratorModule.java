package io.graphoenix.graphql.generator.module;

import dagger.Module;
import dagger.Provides;
import io.graphoenix.core.module.DocumentManagerModule;
import io.graphoenix.graphql.generator.translator.ElementManager;
import io.graphoenix.graphql.generator.translator.JavaElementToOperation;
import io.graphoenix.graphql.generator.translator.MethodToMutationOperation;
import io.graphoenix.graphql.generator.translator.MethodToQueryOperation;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;

import javax.inject.Singleton;

@Module(includes = DocumentManagerModule.class)
public class GraphQLGeneratorModule {

    @Provides
    @Singleton
    ElementManager elementManager(IGraphQLDocumentManager manager) {
        return new ElementManager(manager);
    }

    @Provides
    @Singleton
    MethodToQueryOperation methodToQueryOperation(IGraphQLDocumentManager manager) {
        return new MethodToQueryOperation(manager, elementManager(manager));
    }

    @Provides
    @Singleton
    MethodToMutationOperation methodToMutationOperation(IGraphQLDocumentManager manager) {
        return new MethodToMutationOperation(manager, elementManager(manager));
    }

    @Provides
    @Singleton
    JavaElementToOperation javaElementToOperation(IGraphQLDocumentManager manager) {
        return new JavaElementToOperation(methodToQueryOperation(manager), methodToMutationOperation(manager));
    }
}
