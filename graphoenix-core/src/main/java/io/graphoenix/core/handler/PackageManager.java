package io.graphoenix.core.handler;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@ApplicationScoped
public class PackageManager {

    private final GraphQLConfig graphQLConfig;
    private final IGraphQLDocumentManager manager;

    @Inject
    public PackageManager(GraphQLConfig graphQLConfig, IGraphQLDocumentManager manager) {
        this.graphQLConfig = graphQLConfig;
        this.manager = manager;
    }

    public boolean isOwnPackage(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        return manager.getPackageName(objectTypeDefinitionContext).map(this::isOwnPackage).orElse(true);
    }

    public boolean isOwnPackage(GraphqlParser.EnumTypeDefinitionContext enumTypeDefinitionContext) {
        return manager.getPackageName(enumTypeDefinitionContext).map(this::isOwnPackage).orElse(true);
    }

    public boolean isOwnPackage(GraphqlParser.InterfaceTypeDefinitionContext interfaceTypeDefinitionContext) {
        return manager.getPackageName(interfaceTypeDefinitionContext).map(this::isOwnPackage).orElse(true);
    }

    public boolean isOwnPackage(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {
        return manager.getPackageName(inputObjectTypeDefinitionContext).map(this::isOwnPackage).orElse(true);
    }

    public boolean isNotOwnPackage(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        return !isOwnPackage(objectTypeDefinitionContext);
    }

    public boolean isNotOwnPackage(GraphqlParser.EnumTypeDefinitionContext enumTypeDefinitionContext) {
        return !isOwnPackage(enumTypeDefinitionContext);
    }

    public boolean isNotOwnPackage(GraphqlParser.InterfaceTypeDefinitionContext interfaceTypeDefinitionContext) {
        return !isOwnPackage(interfaceTypeDefinitionContext);
    }

    public boolean isNotOwnPackage(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {
        return !isOwnPackage(inputObjectTypeDefinitionContext);
    }

    public boolean isOwnPackage(String packageName) {
        return graphQLConfig.getPackageName().equals(packageName);
    }

    public boolean isLocalPackage(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        return manager.getPackageName(objectTypeDefinitionContext).map(this::isLocalPackage).orElse(true);
    }

    public boolean isLocalPackage(GraphqlParser.EnumTypeDefinitionContext enumTypeDefinitionContext) {
        return manager.getPackageName(enumTypeDefinitionContext).map(this::isLocalPackage).orElse(true);
    }

    public boolean isLocalPackage(GraphqlParser.InterfaceTypeDefinitionContext interfaceTypeDefinitionContext) {
        return manager.getPackageName(interfaceTypeDefinitionContext).map(this::isLocalPackage).orElse(true);
    }

    public boolean isLocalPackage(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {
        return manager.getPackageName(inputObjectTypeDefinitionContext).map(this::isLocalPackage).orElse(true);
    }

    public boolean isNotLocalPackage(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        return !isLocalPackage(objectTypeDefinitionContext);
    }

    public boolean isNotLocalPackage(GraphqlParser.EnumTypeDefinitionContext enumTypeDefinitionContext) {
        return !isLocalPackage(enumTypeDefinitionContext);
    }

    public boolean isNotLocalPackage(GraphqlParser.InterfaceTypeDefinitionContext interfaceTypeDefinitionContext) {
        return !isLocalPackage(interfaceTypeDefinitionContext);
    }

    public boolean isNotLocalPackage(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {
        return !isLocalPackage(inputObjectTypeDefinitionContext);
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
