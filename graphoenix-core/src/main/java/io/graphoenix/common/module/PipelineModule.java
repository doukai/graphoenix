package io.graphoenix.common.module;

import dagger.Module;
import dagger.Provides;
import io.graphoenix.common.pipeline.ChainTest;
import io.graphoenix.common.pipeline.GraphQLCodeGenerator;
import io.graphoenix.common.pipeline.GraphQLDataFetcher;
import io.graphoenix.common.pipeline.operation.OperationRouter;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.patterns.ChainsBean;
import io.graphoenix.spi.patterns.ChainsBeanBuilder;
import io.graphoenix.spi.patterns.CompositeBean;
import io.graphoenix.spi.patterns.CompositeBeanBuilder;

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

    @Provides
    @Singleton
    @ChainsBean
    ChainTest chainTest(IGraphQLDocumentManager manager) {
        ChainsBeanBuilder chainsBeanBuilder = ChainsBeanBuilder.create();
        chainsBeanBuilder.add("gen", operationRouter(manager));
        chainsBeanBuilder.add("gen", graphQLDataFetcher(manager));
        chainsBeanBuilder.add("gen", graphQLCodeGenerator(manager));
        return chainsBeanBuilder.build(ChainTest.class);
    }

    @Provides
    @Singleton
    @CompositeBean
    ChainTest chainTest2(IGraphQLDocumentManager manager) {
        CompositeBeanBuilder compositeBeanBuilder = CompositeBeanBuilder.create();
        compositeBeanBuilder.put("gen", operationRouter(manager));
        compositeBeanBuilder.put("gen", graphQLDataFetcher(manager));
        compositeBeanBuilder.put("gen", graphQLCodeGenerator(manager));
        return compositeBeanBuilder.build(ChainTest.class);
    }
}
