package io.graphoenix.graphql.builder.introspection;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.antlr.manager.impl.GraphqlAntlrManager;
import io.graphoenix.graphql.builder.introspection.dto.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class IntrospectionDtoWrapper {

    private final GraphqlAntlrManager manager;

    public IntrospectionDtoWrapper(GraphqlAntlrManager manager) {
        this.manager = manager;
    }

    public __Schema buildIntrospectionSchema() {

        __Schema schema = new __Schema();
        schema.setTypes(manager.getObjects()
                .filter(objectTypeDefinitionContext -> !manager.isQueryOperationType(objectTypeDefinitionContext.name().getText()))
                .filter(objectTypeDefinitionContext -> !manager.isMutationOperationType(objectTypeDefinitionContext.name().getText()))
                .filter(objectTypeDefinitionContext -> !manager.isSubscriptionOperationType(objectTypeDefinitionContext.name().getText()))
                .map(this::objectTypeDefinitionContextToType).collect(Collectors.toList()));

        Optional<GraphqlParser.ObjectTypeDefinitionContext> queryTypeDefinitionContext = manager.getQueryOperationTypeName().flatMap(manager::getObject);
        queryTypeDefinitionContext.ifPresent(objectTypeDefinitionContext -> schema.setQueryType(this.objectTypeDefinitionContextToType(objectTypeDefinitionContext)));

        Optional<GraphqlParser.ObjectTypeDefinitionContext> mutationTypeDefinitionContext = manager.getMutationOperationTypeName().flatMap(manager::getObject);
        mutationTypeDefinitionContext.ifPresent(objectTypeDefinitionContext -> schema.setMutationType(this.objectTypeDefinitionContextToType(objectTypeDefinitionContext)));

        Optional<GraphqlParser.ObjectTypeDefinitionContext> subscriptionTypeDefinitionContext = manager.getSubscriptionOperationTypeName().flatMap(manager::getObject);
        subscriptionTypeDefinitionContext.ifPresent(objectTypeDefinitionContext -> schema.setSubscriptionType(this.objectTypeDefinitionContextToType(objectTypeDefinitionContext)));

        schema.setDirectives(manager.getDirectives()
                .filter(directiveDefinitionContext -> directiveDefinitionContext.name().getText().equals(__DirectiveLocation.SCHEMA.name()))
                .map(this::directiveDefinitionContextToDirective).collect(Collectors.toList()));
        return schema;
    }

    private __Type objectTypeDefinitionContextToType(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        __Type type = new __Type();
        type.setKind(__TypeKind.OBJECT);
        type.setName(objectTypeDefinitionContext.name().getText());
        if (objectTypeDefinitionContext.description() != null) {
            type.setDescription(objectTypeDefinitionContext.description().getText());
        }
        type.setFields(objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream().map(this::fieldDefinitionContextToField).collect(Collectors.toList()));
        type.setDirectives(manager.getDirectives()
                .filter(directiveDefinitionContext -> directiveDefinitionContext.name().getText().equals(__DirectiveLocation.OBJECT.name()))
                .map(this::directiveDefinitionContextToDirective).collect(Collectors.toList()));
        return type;
    }

    private __Field fieldDefinitionContextToField(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        __Field field = new __Field();
        field.setName(fieldDefinitionContext.name().getText());
        if (fieldDefinitionContext.description() != null) {
            field.setDescription(fieldDefinitionContext.description().getText());
        }
        field.setArgs(fieldDefinitionContext.argumentsDefinition().inputValueDefinition().stream()
                .map(this::inputValueDefinitionContextToInputValue).collect(Collectors.toList()));

        field.setType(typeContextToType(fieldDefinitionContext.type()));
        field.setDirectives(manager.getDirectives()
                .filter(directiveDefinitionContext -> directiveDefinitionContext.name().getText().equals(__DirectiveLocation.FIELD_DEFINITION.name()))
                .map(this::directiveDefinitionContextToDirective).collect(Collectors.toList()));
        return field;
    }

    private __Type typeContextToType(GraphqlParser.TypeContext typeContext) {
        if (typeContext.typeName() != null) {
            if (manager.isObject(typeContext.typeName().getText())) {
                return manager.getObject(typeContext.typeName().getText()).map(this::objectTypeDefinitionContextToType).orElse(null);
            } else if (manager.isEnum(typeContext.typeName().getText())) {
                return manager.getEnum(typeContext.typeName().getText()).map(this::enumTypeDefinitionContextToType).orElse(null);
            } else if (manager.isInputObject(typeContext.typeName().getText())) {
                return manager.getInputObject(typeContext.typeName().getText()).map(this::inputObjectTypeDefinitionContextToType).orElse(null);
            }
        } else if (typeContext.listType() != null) {
            __Type type = new __Type();
            type.setKind(__TypeKind.LIST);
            type.setOfType(typeContextToType(typeContext.listType().type()));
            return type;
        } else if (typeContext.nonNullType() != null) {
            __Type type = new __Type();
            type.setKind(__TypeKind.NON_NULL);
            if (typeContext.nonNullType().typeName() != null) {
                if (manager.isObject(typeContext.typeName().getText())) {
                    type.setOfType(manager.getObject(typeContext.nonNullType().typeName().getText()).map(this::objectTypeDefinitionContextToType).orElse(null));
                } else if (manager.isEnum(typeContext.typeName().getText())) {
                    type.setOfType(manager.getEnum(typeContext.nonNullType().typeName().getText()).map(this::enumTypeDefinitionContextToType).orElse(null));
                } else if (manager.isInputObject(typeContext.typeName().getText())) {
                    type.setOfType(manager.getInputObject(typeContext.nonNullType().typeName().getText()).map(this::inputObjectTypeDefinitionContextToType).orElse(null));
                }
            } else if (typeContext.nonNullType().listType() != null) {
                type.setOfType(typeContextToType(typeContext.listType().type()));
            }
            return type;
        }
        return null;
    }

    private __Type enumTypeDefinitionContextToType(GraphqlParser.EnumTypeDefinitionContext enumTypeDefinitionContext) {
        __Type type = new __Type();
        type.setKind(__TypeKind.ENUM);
        type.setName(enumTypeDefinitionContext.name().getText());
        if (enumTypeDefinitionContext.description() != null) {
            type.setDescription(enumTypeDefinitionContext.description().getText());
        }
        type.setEnumValues(enumTypeDefinitionContext.enumValueDefinitions().enumValueDefinition().stream()
                .map(this::enumValueDefinitionContextToEnumValue).collect(Collectors.toList()));
        type.setDirectives(manager.getDirectives()
                .filter(directiveDefinitionContext -> directiveDefinitionContext.name().getText().equals(__DirectiveLocation.ENUM.name()))
                .map(this::directiveDefinitionContextToDirective).collect(Collectors.toList()));
        return type;
    }

    private __EnumValue enumValueDefinitionContextToEnumValue(GraphqlParser.EnumValueDefinitionContext enumValueDefinitionContext) {
        __EnumValue enumValue = new __EnumValue();
        enumValue.setName(enumValueDefinitionContext.enumValue().enumValueName().getText());
        if (enumValueDefinitionContext.description() != null) {
            enumValue.setDescription(enumValueDefinitionContext.description().getText());
        }
        enumValue.setDirectives(manager.getDirectives()
                .filter(directiveDefinitionContext -> directiveDefinitionContext.name().getText().equals(__DirectiveLocation.ENUM_VALUE.name()))
                .map(this::directiveDefinitionContextToDirective).collect(Collectors.toList()));
        return enumValue;
    }

    private __InputValue inputValueDefinitionContextToInputValue(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        __InputValue inputValue = new __InputValue();
        inputValue.setName(inputValueDefinitionContext.name().getText());
        if (inputValueDefinitionContext.description() != null) {
            inputValue.setDescription(inputValueDefinitionContext.description().getText());
        }
        inputValue.setType(typeContextToType(inputValueDefinitionContext.type()));
        inputValue.setDefaultValue(inputValueDefinitionContext.defaultValue().value().getText());
        inputValue.setDirectives(manager.getDirectives()
                .filter(directiveDefinitionContext -> directiveDefinitionContext.name().getText().equals(__DirectiveLocation.INPUT_FIELD_DEFINITION.name()))
                .map(this::directiveDefinitionContextToDirective).collect(Collectors.toList()));
        return inputValue;
    }

    private __Type inputObjectTypeDefinitionContextToType(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {
        __Type type = new __Type();
        type.setKind(__TypeKind.INPUT_OBJECT);
        type.setName(inputObjectTypeDefinitionContext.name().getText());
        if (inputObjectTypeDefinitionContext.description() != null) {
            type.setDescription(inputObjectTypeDefinitionContext.description().getText());
        }
        type.setInputFields(inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition().stream()
                .map(this::inputValueDefinitionContextToInputValue).collect(Collectors.toList()));
        type.setDirectives(manager.getDirectives()
                .filter(directiveDefinitionContext -> directiveDefinitionContext.name().getText().equals(__DirectiveLocation.INPUT_OBJECT.name()))
                .map(this::directiveDefinitionContextToDirective).collect(Collectors.toList()));
        return type;
    }

    private __Directive directiveDefinitionContextToDirective(GraphqlParser.DirectiveDefinitionContext directiveDefinitionContext) {
        __Directive directive = new __Directive();
        directive.setName(directiveDefinitionContext.name().getText());

        if (directiveDefinitionContext.directiveLocations() != null) {
            directive.setDescription(directiveDefinitionContext.description().getText());
        }
        directive.setArgs(directiveDefinitionContext.argumentsDefinition().inputValueDefinition().stream()
                .map(this::inputValueDefinitionContextToInputValue).collect(Collectors.toList()));
        List<__DirectiveLocation> directiveLocationList = new ArrayList<>();
        addDirectiveDefinitionsContextToDirectiveList(directiveDefinitionContext.directiveLocations(), directiveLocationList);
        directive.setLocations(directiveLocationList);
        return directive;
    }

    private void addDirectiveDefinitionsContextToDirectiveList(GraphqlParser.DirectiveLocationsContext directiveLocationsContext, List<__DirectiveLocation> directiveLocationList) {
        if (directiveLocationsContext.directiveLocation() != null) {
            directiveLocationList.add(__DirectiveLocation.valueOf(directiveLocationsContext.directiveLocation().name().getText()));
        } else if (directiveLocationsContext.directiveLocations() != null) {
            addDirectiveDefinitionsContextToDirectiveList(directiveLocationsContext.directiveLocations(), directiveLocationList);
        }
    }
}
