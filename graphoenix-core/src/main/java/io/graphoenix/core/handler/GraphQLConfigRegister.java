package io.graphoenix.core.handler;

import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.vavr.control.Try;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.tinylog.Logger;

import javax.annotation.processing.Filer;
import javax.tools.StandardLocation;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
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
            Logger.info("registered graphql {}", graphQLConfig.getGraphQL());
        }
        if (graphQLConfig.getGraphQLFileName() != null) {
            manager.registerFileByName(graphQLConfig.getGraphQLFileName());
            Logger.info("registered file {}", graphQLConfig.getGraphQLFileName());
        }
        if (graphQLConfig.getGraphQLPath() != null) {
            manager.registerPathByName(graphQLConfig.getGraphQLPath());
            Logger.info("registered path {}", graphQLConfig.getGraphQLPath());
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
            Logger.info("registered graphql {}", graphQLConfig.getGraphQL());
        } else if (config.getGraphQLFileName() != null) {
            manager.registerFileByName(path.concat(config.getGraphQLFileName()));
            Logger.info("registered file {}", graphQLConfig.getGraphQLFileName());
        } else if (config.getGraphQLPath() != null) {
            manager.registerPathByName(path.concat(config.getGraphQLPath()));
            Logger.info("registered path {}", graphQLConfig.getGraphQLPath());
        }
    }

    public void registerConfig(GraphQLConfig config, Filer filer) throws IOException, URISyntaxException {
        if (config.getGraphQL() != null) {
            manager.registerGraphQL(config.getGraphQL());
            Logger.info("registered graphql {}", graphQLConfig.getGraphQL());
        } else if (config.getGraphQLFileName() != null) {
            manager.registerInputStream(filer.getResource(StandardLocation.SOURCE_PATH, "", config.getGraphQLFileName()).openInputStream());
            Logger.info("registered file {}", graphQLConfig.getGraphQLFileName());
        } else if (config.getGraphQLPath() != null) {
            manager.registerPath(Path.of(filer.getResource(StandardLocation.SOURCE_PATH, "", config.getGraphQLPath()).toUri()));
            Logger.info("registered path {}", graphQLConfig.getGraphQLPath());
        }
    }

    public void registerPreset(ClassLoader classLoader) throws IOException, URISyntaxException {
        Iterator<URL> urlIterator = Objects.requireNonNull(classLoader.getResources("META-INF/graphql")).asIterator();
        while (urlIterator.hasNext()) {
            URI uri = urlIterator.next().toURI();
            List<Path> pathList;
            try {
                pathList = Files.list(Path.of(uri)).collect(Collectors.toList());
            } catch (FileSystemNotFoundException fileSystemNotFoundException) {
                Map<String, String> env = new HashMap<>();
                FileSystem fileSystem = FileSystems.newFileSystem(uri, env);
                pathList = Files.list(fileSystem.getPath("META-INF/graphql")).collect(Collectors.toList());
            }
            try {
                Optional<Path> exportGraphQLFile = pathList.stream().filter(path -> path.getFileName().getFileName().toString().equals("export.gql")).findFirst();
                if (exportGraphQLFile.isPresent()) {
                    manager.mergePath(exportGraphQLFile.get());
                } else {
                    pathList.forEach(path -> {
                                Try.run(() -> manager.mergePath(path));
                                Logger.info("registered preset path {} from {}", path, classLoader.getName());
                            }
                    );
                }
            } catch (IllegalArgumentException e) {
                Logger.warn(e);
            }
        }
    }

    public void registerPreset() throws IOException, URISyntaxException {
        registerPreset(getClass().getClassLoader());
    }
}
