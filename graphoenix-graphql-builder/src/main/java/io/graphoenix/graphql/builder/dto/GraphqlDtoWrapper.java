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

    public List<GraphqlObject> objectTypeDefinitionsToDto() {
        return manager.getObjects()
                .filter(objectTypeDefinitionContext -> !manager.isQueryOperationType(objectTypeDefinitionContext.name().getText()))
                .filter(objectTypeDefinitionContext -> !manager.isMutationOperationType(objectTypeDefinitionContext.name().getText()))
                .filter(objectTypeDefinitionContext -> !manager.isSubscriptionOperationType(objectTypeDefinitionContext.name().getText()))
                .map(this::objectTypeDefinitionToDto).collect(Collectors.toList());

    }

    public GraphqlObject objectTypeDefinitionToDto(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        return new GraphqlObject(objectTypeDefinitionContext.name().getText(),
                objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                        .filter(fieldDefinitionContext -> !manager.fieldTypeIsList(fieldDefinitionContext.type()))
                        .map(this::fieldDefinitionToDto).collect(Collectors.toList()));
    }

    private GraphqlField fieldDefinitionToDto(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return new GraphqlField(fieldDefinitionContext.name().getText(),
                manager.getFieldTypeName(fieldDefinitionContext.type()),
                manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type())));
    }
}
