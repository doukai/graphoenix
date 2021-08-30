package io.graphoenix.grantlr.manager;

import graphql.parser.antlr.GraphqlParser;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public interface IGraphqlObjectManager {

    Map<String, GraphqlParser.ObjectTypeDefinitionContext> register(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext);

    boolean isObject(String objectTypeName);

    Optional<GraphqlParser.ObjectTypeDefinitionContext> getObjectTypeDefinition(String objectTypeName);

    Stream<GraphqlParser.ObjectTypeDefinitionContext> getObjectTypeDefinitions();
}
