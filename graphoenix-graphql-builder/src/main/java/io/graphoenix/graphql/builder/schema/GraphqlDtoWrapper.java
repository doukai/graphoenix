package io.graphoenix.graphql.builder.schema;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.graphql.builder.schema.dto.GraphqlDirective;
import io.graphoenix.graphql.builder.schema.dto.GraphqlField;
import io.graphoenix.graphql.builder.schema.dto.GraphqlObject;
import io.graphoenix.spi.antlr.IGraphqlDocumentManager;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GraphqlDtoWrapper {

    private final IGraphqlDocumentManager manager;

    public GraphqlDtoWrapper(IGraphqlDocumentManager manager) {
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
        List<GraphqlField> fields =
                enumTypeDefinitionContext.enumValueDefinitions().enumValueDefinition().stream()
                        .map(this::fieldDefinitionToDto).collect(Collectors.toList());

        fields.get(fields.size() - 1).setLast(true);
        List<GraphqlDirective> directives = new ArrayList<>();

        if (enumTypeDefinitionContext.directives() != null) {
            directives = enumTypeDefinitionContext.directives().directive().stream().map(this::directiveDefinitionToDto).collect(Collectors.toList());
            directives.get(directives.size() - 1).setLast(true);
        }
        return new GraphqlObject(enumTypeDefinitionContext.name().getText(), fields, directives);
    }

    public GraphqlObject objectTypeDefinitionToDto(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        List<GraphqlField> fields =
                objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                        .map(this::fieldDefinitionToDto).collect(Collectors.toList());

        fields.get(fields.size() - 1).setLast(true);
        List<GraphqlDirective> directives = new ArrayList<>();

        if (objectTypeDefinitionContext.directives() != null) {
            directives = objectTypeDefinitionContext.directives().directive().stream().map(this::directiveDefinitionToDto).collect(Collectors.toList());
            directives.get(directives.size() - 1).setLast(true);
        }
        return new GraphqlObject(objectTypeDefinitionContext.name().getText(), fields, directives);
    }

    private GraphqlField fieldDefinitionToDto(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        List<GraphqlDirective> directives = new ArrayList<>();

        if (fieldDefinitionContext.directives() != null) {
            directives = fieldDefinitionContext.directives().directive().stream().map(this::directiveDefinitionToDto).collect(Collectors.toList());
            directives.get(directives.size() - 1).setLast(true);
        }
        GraphqlField graphqlField = new GraphqlField(fieldDefinitionContext.name().getText(),
                manager.getFieldTypeName(fieldDefinitionContext.type()),
                directives,
                manager.fieldTypeIsNonNull(fieldDefinitionContext.type()),
                manager.fieldTypeIsList(fieldDefinitionContext.type()),
                manager.fieldTypeIsNonNullList(fieldDefinitionContext.type()),
                manager.isObject(manager.getFieldTypeName(fieldDefinitionContext.type())));

        manager.getObject(graphqlField.getTypeName()).ifPresent(objectTypeDefinitionContext -> graphqlField.setType(objectTypeDefinitionToFieldType(objectTypeDefinitionContext)));
        return graphqlField;
    }

    public GraphqlObject objectTypeDefinitionToFieldType(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        List<GraphqlField> fields =
                objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                        .map(fieldDefinitionContext -> new GraphqlField(fieldDefinitionContext.name().getText(), manager.getFieldTypeName(fieldDefinitionContext.type()))).collect(Collectors.toList());
        fields.get(fields.size() - 1).setLast(true);
        return new GraphqlObject(objectTypeDefinitionContext.name().getText(), fields);
    }

    private GraphqlField fieldDefinitionToDto(GraphqlParser.EnumValueDefinitionContext enumValueDefinitionContext) {
        List<GraphqlDirective> directives = new ArrayList<>();

        if (enumValueDefinitionContext.directives() != null) {
            directives = enumValueDefinitionContext.directives().directive().stream().map(this::directiveDefinitionToDto).collect(Collectors.toList());
            directives.get(directives.size() - 1).setLast(true);
        }
        return new GraphqlField(enumValueDefinitionContext.getText(), directives);
    }

    private GraphqlDirective directiveDefinitionToDto(GraphqlParser.DirectiveContext directiveContext) {
        return new GraphqlDirective(directiveContext.getText());
    }
}
