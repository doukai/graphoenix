package io.graphoenix.common.module;

import dagger.Module;
import dagger.Provides;
import io.graphoenix.common.pipeline.GraphQLCodeGenerator;
import io.graphoenix.common.pipeline.GraphQLDataFetcher;
import io.graphoenix.common.pipeline.operation.OperationRouter;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;

import javax.inject.Singleton;

@Module(includes = DocumentManagerModule.class)
public class PipelineModule {

    @Provides
    @Singleton
    OperationRouter operationRouter(IGraphQLDocumentManager manager) {
        return new OperationRouter(manager);
    }

    @Provides
    @Singleton
    GraphQLDataFetcher graphQLDataFetcher(IGraphQLDocumentManager manager) {
        return new GraphQLDataFetcher(operationRouter(manager));
    }

    @Provides
    @Singleton
    GraphQLCodeGenerator graphQLCodeGenerator(IGraphQLDocumentManager manager) {
        return new GraphQLCodeGenerator(manager, operationRouter(manager));
    }
}