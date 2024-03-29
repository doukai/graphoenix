package io.graphoenix.core.manager;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.spi.antlr.IGraphQLInterfaceManager;
import jakarta.enterprise.context.ApplicationScoped;
import org.tinylog.Logger;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@ApplicationScoped
public class GraphQLInterfaceManager implements IGraphQLInterfaceManager {

    private final Map<String, GraphqlParser.InterfaceTypeDefinitionContext> interfaceTypeDefinitionMap = new LinkedHashMap<>();

    private final Map<String, Map<String, GraphqlParser.ObjectTypeDefinitionContext>> implementsMap = new LinkedHashMap<>();

    @Override
    public Map<String, GraphqlParser.InterfaceTypeDefinitionContext> register(GraphqlParser.InterfaceTypeDefinitionContext interfaceTypeDefinitionContext) {
        interfaceTypeDefinitionMap.put(interfaceTypeDefinitionContext.name().getText(), interfaceTypeDefinitionContext);
        Logger.info("registered interfaceType {}", interfaceTypeDefinitionContext.name().getText());
        return interfaceTypeDefinitionMap;
    }

    @Override
    public Map<String, Map<String, GraphqlParser.ObjectTypeDefinitionContext>> registerImplements(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        getInterfaceNames(objectTypeDefinitionContext.implementsInterfaces())
                .forEach(interfaceName -> {
                            implementsMap.computeIfAbsent(interfaceName, key -> new LinkedHashMap<>());
                            implementsMap.get(interfaceName).put(objectTypeDefinitionContext.name().getText(), objectTypeDefinitionContext);
                        }
                );
        return implementsMap;
    }

    @Override
    public boolean isInterface(String interfaceTypeName) {
        return interfaceTypeDefinitionMap.entrySet().stream().anyMatch(entry -> entry.getKey().equals(interfaceTypeName));
    }

    @Override
    public Optional<GraphqlParser.InterfaceTypeDefinitionContext> getInterfaceTypeDefinition(String interfaceTypeName) {
        return interfaceTypeDefinitionMap.entrySet().stream().filter(entry -> entry.getKey().equals(interfaceTypeName)).map(Map.Entry::getValue).findFirst();
    }

    @Override
    public Stream<GraphqlParser.ObjectTypeDefinitionContext> getImplementsObjectTypeDefinition(String interfaceTypeName) {
        return Stream.ofNullable(implementsMap.get(interfaceTypeName)).flatMap(map -> map.values().stream());
    }

    @Override
    public Stream<GraphqlParser.InterfaceTypeDefinitionContext> getInterfaceTypeDefinitions() {
        return interfaceTypeDefinitionMap.values().stream();
    }

    public Stream<String> getInterfaceNames(GraphqlParser.ImplementsInterfacesContext implementsInterfacesContext) {
        if (implementsInterfacesContext == null) {
            return Stream.empty();
        }
        return Stream.concat(
                Stream.ofNullable(implementsInterfacesContext.typeName())
                        .flatMap(Collection::stream)
                        .map(typeNameContext -> typeNameContext.name().getText()),
                Stream.ofNullable(implementsInterfacesContext.implementsInterfaces())
                        .flatMap(this::getInterfaceNames)
        );
    }

    @Override
    public void clear() {
        interfaceTypeDefinitionMap.clear();
        Logger.debug("clear all interfaceType");
    }
}
