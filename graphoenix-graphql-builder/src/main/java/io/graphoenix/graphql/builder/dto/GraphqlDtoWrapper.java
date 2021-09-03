package io.graphoenix.graphql.builder.dto;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.antlr.manager.impl.GraphqlAntlrManager;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GraphqlDtoWrapper {

    private final GraphqlAntlrManager manager;

    public GraphqlDtoWrapper(GraphqlAntlrManager manager) {
        this.manager = manager;
    }

    public List<GraphqlObject> objectTypeDefinitionsToDto() {
        return manager.getObjects()
                .filter(objectTypeDefinitionContext -> !manager.isQueryOperationType(objectTypeDefinitionContext.name().getText()))
                .filter(objectTypeDefinitionContext -> !manager.isMutationOperationType(objectTypeDefinitionContext.name().getText()))
                .filter(objectTypeDefinitionContext -> !manager.isSubscriptionOperationType(objectTypeDefinitionContext.name().getText()))
                .map(this::objectTypeDefinitionToDto).collect(Collectors.toList());
    }

    public List<GraphqlObject> enumTypeDefinitionsToDto() {
        return manager.getEnums()
                .map(this::enumTypeDefinitionToDto).collect(Collectors.toList());
    }

    public GraphqlObject enumTypeDefinitionToDto(GraphqlParser.EnumTypeDefinitionContext enumTypeDefinitionContext) {

        return new GraphqlObject(enumTypeDefinitionContext.name().getText(), null);
    }

    public GraphqlObject objectTypeDefinitionToDto(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        Stream<GraphqlField> fieldsStream =
                objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                        .filter(fieldDefinitionContext -> !manager.fieldTypeIsList(fieldDefinitionContext.type()))
                        .map(this::fieldDefinitionToDto);

        List<GraphqlField> fields = fieldsStream.skip(fieldsStream.count() - 1).map(graphqlField -> graphqlField.setLast(true)).collect(Collectors.toList());

        return new GraphqlObject(objectTypeDefinitionContext.name().getText(), fields);
    }

    private GraphqlField fieldDefinitionToDto(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return new GraphqlField(fieldDefinitionContext.name().getText(),
                manager.getFieldTypeName(fieldDefinitionContext.type()),
                manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type())));
    }
}
