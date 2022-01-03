package io.graphoenix.core.manager;

import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;

import java.io.IOException;
import java.net.URISyntaxException;

public class GraphQLConfigRegister {

    private final GraphQLConfig graphQLConfig;
    private final IGraphQLDocumentManager manager;

    public GraphQLConfigRegister(GraphQLConfig graphQLConfig, IGraphQLDocumentManager manager) {
        this.graphQLConfig = graphQLConfig;
        this.manager = manager;
    }

    public void registerConfig() throws IOException, URISyntaxException {
        if (graphQLConfig.getGraphQL() != null) {
            manager.registerGraphQL(graphQLConfig.getGraphQL());
        }
        if (graphQLConfig.getGraphQLFileName() != null) {
            manager.registerFileByName(graphQLConfig.getGraphQLFileName());
        }
        if (graphQLConfig.getGraphQLPath() != null) {
            manager.registerPathByName(graphQLConfig.getGraphQLPath());
        }
    }
}
