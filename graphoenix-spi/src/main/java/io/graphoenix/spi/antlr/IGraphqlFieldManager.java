package io.graphoenix.spi.antlr;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.spi.dto.map.FieldMap;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public interface IGraphqlFieldManager {

    Map<String, Map<String, GraphqlParser.FieldDefinitionContext>> register(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext);

    Map<String, Map<String, GraphqlParser.FieldDefinitionContext>> register(GraphqlParser.InterfaceTypeDefinitionContext interfaceTypeDefinitionContext);

    Stream<GraphqlParser.FieldDefinitionContext> getFieldDefinitions(String objectTypeName);

    Optional<GraphqlParser.FieldDefinitionContext> getFieldDefinition(String objectTypeName, String fieldName);

    Map<String, Map<String, FieldMap>> registerMap(String objectTypeName,
                                                   String fieldName,
                                                   GraphqlParser.FieldDefinitionContext from,
                                                   GraphqlParser.ObjectTypeDefinitionContext toType,
                                                   GraphqlParser.FieldDefinitionContext to);

    Map<String, Map<String, FieldMap>> registerMap(String objectTypeName,
                                                   String fieldName,
                                                   GraphqlParser.FieldDefinitionContext from,
                                                   GraphqlParser.ObjectTypeDefinitionContext withType,
                                                   GraphqlParser.FieldDefinitionContext withFrom,
                                                   GraphqlParser.FieldDefinitionContext withTo,
                                                   GraphqlParser.ObjectTypeDefinitionContext toType,
                                                   GraphqlParser.FieldDefinitionContext to);

    GraphqlParser.FieldDefinitionContext getFromFieldDefinition(String objectTypeName, String fieldName);

    GraphqlParser.ObjectTypeDefinitionContext getToObjectTypeDefinition(String objectTypeName, String fieldName);

    GraphqlParser.FieldDefinitionContext getToFieldDefinition(String objectTypeName, String fieldName);

    boolean mapWithType(String objectTypeName, String fieldName);

    GraphqlParser.ObjectTypeDefinitionContext getWithObjectTypeDefinition(String objectTypeName, String fieldName);

    GraphqlParser.FieldDefinitionContext getWithFromFieldDefinition(String objectTypeName, String fieldName);

    GraphqlParser.FieldDefinitionContext getWithToFieldDefinition(String objectTypeName, String fieldName);
}
