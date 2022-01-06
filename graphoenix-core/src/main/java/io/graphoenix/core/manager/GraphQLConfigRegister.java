package io.graphoenix.core.manager;

import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

public class GraphQLConfigRegister {

    private final GraphQLConfig graphQLConfig;
    private final IGraphQLDocumentManager manager;

    @Inject
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

    public void registerConfig(String path) throws IOException, URISyntaxException {
        registerConfig(graphQLConfig, path);
    }

    public void registerConfig(GraphQLConfig config, String path) throws IOException, URISyntaxException {
        if (!path.endsWith(File.separator)) {
            path = path.concat(File.separator);
        }
        if (config.getGraphQL() != null) {
            manager.registerGraphQL(config.getGraphQL());
        } else if (config.getGraphQLFileName() != null) {
            manager.registerFileByName(path.concat(config.getGraphQLFileName()));
        } else if (config.getGraphQLPath() != null) {
            manager.registerPathByName(path.concat(config.getGraphQLPath()));
        }
    }
}
