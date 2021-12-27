package io.graphoenix.common.pipeline;

import io.graphoenix.spi.handler.IBootstrapHandler;
import org.apache.commons.chain.Context;

public interface ChainTest {

    GraphQLCodeGenerator registerGraphQL(String graphQL);

    GraphQLDataFetcher addBootstrapHandler(IBootstrapHandler bootstrapHandler);

    boolean execute(Context context);
}
