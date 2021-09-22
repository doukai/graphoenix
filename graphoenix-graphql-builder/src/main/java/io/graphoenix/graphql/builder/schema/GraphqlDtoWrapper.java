package io.graphoenix.graphql.builder.schema;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.antlr.manager.impl.GraphqlAntlrManager;
import io.graphoenix.graphql.builder.schema.dto.GraphqlField;
import io.graphoenix.graphql.builder.schema.dto.GraphqlObject;

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

    public List<GraphqlObject> enumTypeDefinitionsToDto() {
        return manager.getEnums()
                .map(this::enumTypeDefinitionToDto).collect(Collectors.toList());
    }

    public GraphqlObject enumTypeDefinitionToDto(GraphqlParser.EnumTypeDefinitionContext enumTypeDefinitionContext) {

        return new GraphqlObject(enumTypeDefinitionContext.name().getText(), null);
    }

    public GraphqlObject objectTypeDefinitionToDto(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        List<GraphqlField> fields =
                objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                        .map(this::fieldDefinitionToDto).collect(Collectors.toList());

        fields.get(fields.size() - 1).setLast(true);

        return new GraphqlObject(objectTypeDefinitionContext.name().getText(), fields);
    }

    private GraphqlField fieldDefinitionToDto(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        if (fieldDefinitionContext.name().getText().equals("types")) {
            fieldDefinitionContext.name();
        }
        return new GraphqlField(fieldDefinitionContext.name().getText(),
                manager.getFieldTypeName(fieldDefinitionContext.type()),
                manager.fieldTypeIsList(fieldDefinitionContext.type()) && fieldDefinitionContext.type().nonNullType() != null && fieldDefinitionContext.type().nonNullType().listType().type().nonNullType() != null ||
                        manager.fieldTypeIsList(fieldDefinitionContext.type()) && fieldDefinitionContext.type().listType().type().nonNullType() != null ||
                        !manager.fieldTypeIsList(fieldDefinitionContext.type()) && fieldDefinitionContext.type().nonNullType() != null,
                manager.fieldTypeIsList(fieldDefinitionContext.type()),
                manager.fieldTypeIsList(fieldDefinitionContext.type()) && fieldDefinitionContext.type().nonNullType() != null,
                manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type())));
    }
}
