package io.graphoenix.graphql.builder.dto;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.antlr.manager.impl.GraphqlAntlrManager;

import java.util.List;
import java.util.stream.Collectors;

public class GraphqlDtoWrapper {

    private final GraphqlAntlrManager manager;

    public GraphqlDtoWrapper(GraphqlAntlrManager manager) {
        this.manager = manager;
    }

    public List<GraphqlObjectDto> objectTypeDefinitionsToDto() {
        return manager.getObjects()
                .filter(objectTypeDefinitionContext -> !manager.isQueryOperationType(objectTypeDefinitionContext.name().getText()))
                .filter(objectTypeDefinitionContext -> !manager.isMutationOperationType(objectTypeDefinitionContext.name().getText()))
                .filter(objectTypeDefinitionContext -> !manager.isSubscriptionOperationType(objectTypeDefinitionContext.name().getText()))
                .map(this::objectTypeDefinitionToDto).collect(Collectors.toList());
    }

    public List<GraphqlObjectDto> enumTypeDefinitionsToDto() {
        return manager.getEnums()
                .map(this::enumTypeDefinitionToDto).collect(Collectors.toList());
    }

    public GraphqlObjectDto enumTypeDefinitionToDto(GraphqlParser.EnumTypeDefinitionContext enumTypeDefinitionContext) {

        return new GraphqlObjectDto(enumTypeDefinitionContext.name().getText(), null);
    }

    public GraphqlObjectDto objectTypeDefinitionToDto(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        List<GraphqlFieldDto> fields =
                objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                        .filter(fieldDefinitionContext -> !manager.fieldTypeIsList(fieldDefinitionContext.type()))
                        .map(this::fieldDefinitionToDto).collect(Collectors.toList());

        fields.get(fields.size() - 1).setLast(true);

        return new GraphqlObjectDto(objectTypeDefinitionContext.name().getText(), fields);
    }

    private GraphqlFieldDto fieldDefinitionToDto(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return new GraphqlFieldDto(fieldDefinitionContext.name().getText(),
                manager.getFieldTypeName(fieldDefinitionContext.type()),
                manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type())));
    }
}
