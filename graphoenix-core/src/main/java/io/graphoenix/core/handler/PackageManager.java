package io.graphoenix.core.handler;

import io.graphoenix.core.config.GraphQLConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@ApplicationScoped
public class PackageManager {

    private final GraphQLConfig graphQLConfig;

    @Inject
    public PackageManager(GraphQLConfig graphQLConfig) {
        this.graphQLConfig = graphQLConfig;
    }

    public boolean isLocalPackage(String packageName) {
        return Stream.concat(
                Stream.ofNullable(graphQLConfig.getPackageName()),
                graphQLConfig.getLocalPackageNames().stream()
        ).anyMatch(localPackageName -> localPackageName.equals(packageName));
    }

    private final ConcurrentHashMap<String, URI> URIMap = new ConcurrentHashMap<>();

    public URI getURI(String packageName) {
        return URIMap.get(packageName);
    }
}
