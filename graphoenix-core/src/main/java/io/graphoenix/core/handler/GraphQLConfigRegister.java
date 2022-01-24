package io.graphoenix.core.handler;

import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.vavr.control.Try;

import javax.annotation.processing.Filer;
import javax.inject.Inject;
import javax.tools.StandardLocation;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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

    public void registerConfig(GraphQLConfig config, Filer filer) throws IOException, URISyntaxException {
        if (config.getGraphQL() != null) {
            manager.registerGraphQL(config.getGraphQL());
        } else if (config.getGraphQLFileName() != null) {
            manager.registerInputStream(filer.getResource(StandardLocation.SOURCE_PATH, "", config.getGraphQLFileName()).openInputStream());
        } else if (config.getGraphQLPath() != null) {
            manager.registerPath(Path.of(filer.getResource(StandardLocation.SOURCE_PATH, "", config.getGraphQLPath()).toUri()));
        }
    }

    public void registerPreset(ClassLoader classLoader) throws IOException, URISyntaxException {
        URI uri = Objects.requireNonNull(classLoader.getResource("META-INF/graphql")).toURI();
        Map<String, String> env = new HashMap<>();
        try (FileSystem fileSystem = FileSystems.newFileSystem(uri, env)) {
            for (Path path : fileSystem.getRootDirectories()) {
                Files.list(path.resolve("META-INF/graphql")).forEach(filePath -> Try.run(() -> manager.registerPath(filePath)));
            }
        }
    }
}
