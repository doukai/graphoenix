package io.graphoenix.common.module;

import dagger.Module;
import dagger.Provides;
import io.graphoenix.common.pipeline.ChainTest;
import io.graphoenix.common.pipeline.GraphQLDataFetcher;
import io.graphoenix.common.pipeline.operation.OperationRouter;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.chain.BeanChain;

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

    //    @Provides
//    @Singleton
//    GraphQLCodeGenerator graphQLCodeGenerator(IGraphQLDocumentManager manager) {
//        return new GraphQLCodeGenerator(manager, operationRouter(manager));
//    }

    @BeanChain(ChainTest.class)
    void chainTest(IGraphQLDocumentManager manager) {
        operationRouter(manager);
        operationRouter(manager);
        operationRouter(manager);
    }
}
