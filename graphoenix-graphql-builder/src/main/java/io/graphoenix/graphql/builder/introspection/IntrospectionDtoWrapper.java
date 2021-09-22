package io.graphoenix.graphql.builder.introspection;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.antlr.manager.impl.GraphqlAntlrManager;
import io.graphoenix.graphql.builder.introspection.dto.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IntrospectionDtoWrapper {

    private final GraphqlAntlrManager manager;

    private final int levelThreshold;

    public IntrospectionDtoWrapper(GraphqlAntlrManager manager, int levelThreshold) {
        this.manager = manager;
        this.levelThreshold = levelThreshold;
    }

    public IntrospectionDtoWrapper(GraphqlAntlrManager manager) {
        this.manager = manager;
        this.levelThreshold = 1;
    }

    public __Schema buildIntrospectionSchema() {

        __Schema schema = new __Schema();
        schema.setTypes(
                Stream.concat(manager.getObjects().map(this::objectTypeDefinitionContextToType),
                        Stream.concat(
                                manager.getEnums().map(this::enumTypeDefinitionContextToType),
                                manager.getInputObjects().map(this::inputObjectTypeDefinitionContextToType)
                        )
                ).collect(Collectors.toList()));

        Optional<GraphqlParser.ObjectTypeDefinitionContext> queryTypeDefinitionContext = manager.getQueryOperationTypeName().flatMap(manager::getObject);
        queryTypeDefinitionContext.ifPresent(objectTypeDefinitionContext -> schema.setQueryType(this.objectTypeDefinitionContextToType(objectTypeDefinitionContext)));

        Optional<GraphqlParser.ObjectTypeDefinitionContext> mutationTypeDefinitionContext = manager.getMutationOperationTypeName().flatMap(manager::getObject);
        mutationTypeDefinitionContext.ifPresent(objectTypeDefinitionContext -> schema.setMutationType(this.objectTypeDefinitionContextToType(objectTypeDefinitionContext)));

        Optional<GraphqlParser.ObjectTypeDefinitionContext> subscriptionTypeDefinitionContext = manager.getSubscriptionOperationTypeName().flatMap(manager::getObject);
        subscriptionTypeDefinitionContext.ifPresent(objectTypeDefinitionContext -> schema.setSubscriptionType(this.objectTypeDefinitionContextToType(objectTypeDefinitionContext)));

        schema.setDirectives(manager.getDirectives().map(this::directiveDefinitionContextToDirective).collect(Collectors.toList()));
        return schema;
    }

    private __Type objectTypeDefinitionContextToType(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        return objectTypeDefinitionContextToType(objectTypeDefinitionContext, 0);
    }

    private __Type objectTypeDefinitionContextToType(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext, int level) {
        __Type type = new __Type();
        type.setKind(__TypeKind.OBJECT);
        type.setName(objectTypeDefinitionContext.name().getText());
        if (objectTypeDefinitionContext.description() != null) {
            type.setDescription(objectTypeDefinitionContext.description().getText());
        }
        type.setFields(objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                .filter(fieldDefinitionContext -> !manager.getFieldTypeName(fieldDefinitionContext.type()).equals(objectTypeDefinitionContext.name().getText()))
                .map(fieldDefinitionContext -> fieldDefinitionContextToField(fieldDefinitionContext, level + 1)).collect(Collectors.toList()));
        return type;
    }

    private __Field fieldDefinitionContextToField(GraphqlParser.FieldDefinitionContext fieldDefinitionContext, int level) {
        __Field field = new __Field();
        field.setName(fieldDefinitionContext.name().getText());
        if (fieldDefinitionContext.description() != null) {
            field.setDescription(fieldDefinitionContext.description().getText());
        }
        if (fieldDefinitionContext.argumentsDefinition() != null) {
            field.setArgs(fieldDefinitionContext.argumentsDefinition().inputValueDefinition().stream()
                    .map(this::inputValueDefinitionContextToInputValue).collect(Collectors.toList()));
        }

        field.setType(typeContextToType(fieldDefinitionContext.type(), level));
        return field;
    }

    private __Type typeContextToType(GraphqlParser.TypeContext typeContext, int level) {
        if (level > levelThreshold) {
            return null;
        }
        if (typeContext.typeName() != null) {
            if (manager.isInnerScalar(typeContext.typeName().getText())) {
                return innerScalarTypeDefinitionContextToType(typeContext.typeName().getText());
            } else if (manager.isScaLar(typeContext.typeName().getText())) {
                return manager.getScaLar(typeContext.typeName().getText()).map(this::scalarTypeDefinitionContextToType).orElse(null);
            } else if (manager.isObject(typeContext.typeName().getText())) {
                return manager.getObject(typeContext.typeName().getText()).map(objectTypeDefinitionContext -> objectTypeDefinitionContextToType(objectTypeDefinitionContext, level)).orElse(null);
            } else if (manager.isEnum(typeContext.typeName().getText())) {
                return manager.getEnum(typeContext.typeName().getText()).map(enumTypeDefinitionContext -> enumTypeDefinitionContextToType(enumTypeDefinitionContext, level)).orElse(null);
            } else if (manager.isInputObject(typeContext.typeName().getText())) {
                return manager.getInputObject(typeContext.typeName().getText()).map(inputObjectTypeDefinitionContext -> inputObjectTypeDefinitionContextToType(inputObjectTypeDefinitionContext, level)).orElse(null);
            }
        } else if (typeContext.listType() != null) {
            __Type type = new __Type();
            type.setKind(__TypeKind.LIST);
            type.setOfType(typeContextToType(typeContext.listType().type(), level));
            return type;
        } else if (typeContext.nonNullType() != null) {
            __Type type = new __Type();
            type.setKind(__TypeKind.NON_NULL);
            if (typeContext.nonNullType().typeName() != null) {
                if (manager.isInnerScalar(typeContext.nonNullType().typeName().getText())) {
                    type.setOfType(innerScalarTypeDefinitionContextToType(typeContext.nonNullType().typeName().getText()));
                } else if (manager.isScaLar(typeContext.nonNullType().typeName().getText())) {
                    type.setOfType(manager.getScaLar(typeContext.nonNullType().typeName().getText()).map(this::scalarTypeDefinitionContextToType).orElse(null));
                } else if (manager.isObject(typeContext.nonNullType().typeName().getText())) {
                    type.setOfType(manager.getObject(typeContext.nonNullType().typeName().getText()).map(objectTypeDefinitionContext -> objectTypeDefinitionContextToType(objectTypeDefinitionContext, level)).orElse(null));
                } else if (manager.isEnum(typeContext.nonNullType().typeName().getText())) {
                    type.setOfType(manager.getEnum(typeContext.nonNullType().typeName().getText()).map(enumTypeDefinitionContext -> enumTypeDefinitionContextToType(enumTypeDefinitionContext, level)).orElse(null));
                } else if (manager.isInputObject(typeContext.nonNullType().typeName().getText())) {
                    type.setOfType(manager.getInputObject(typeContext.nonNullType().typeName().getText()).map(inputObjectTypeDefinitionContext -> inputObjectTypeDefinitionContextToType(inputObjectTypeDefinitionContext, level)).orElse(null));
                }
            } else if (typeContext.nonNullType().listType() != null) {
                __Type listType = new __Type();
                listType.setKind(__TypeKind.LIST);
                listType.setOfType(typeContextToType(typeContext.nonNullType().listType().type(), level));
                type.setOfType(listType);
            }
            return type;
        }
        return null;
    }

    private __Type enumTypeDefinitionContextToType(GraphqlParser.EnumTypeDefinitionContext enumTypeDefinitionContext) {
        return enumTypeDefinitionContextToType(enumTypeDefinitionContext, 0);
    }

    private __Type enumTypeDefinitionContextToType(GraphqlParser.EnumTypeDefinitionContext enumTypeDefinitionContext, int level) {
        __Type type = new __Type();
        type.setKind(__TypeKind.ENUM);
        type.setName(enumTypeDefinitionContext.name().getText());
        if (enumTypeDefinitionContext.description() != null) {
            type.setDescription(enumTypeDefinitionContext.description().getText());
        }
        type.setEnumValues(enumTypeDefinitionContext.enumValueDefinitions().enumValueDefinition().stream()
                .map(this::enumValueDefinitionContextToEnumValue).collect(Collectors.toList()));
        return type;
    }

    private __EnumValue enumValueDefinitionContextToEnumValue(GraphqlParser.EnumValueDefinitionContext enumValueDefinitionContext) {
        __EnumValue enumValue = new __EnumValue();
        enumValue.setName(enumValueDefinitionContext.enumValue().enumValueName().getText());
        if (enumValueDefinitionContext.description() != null) {
            enumValue.setDescription(enumValueDefinitionContext.description().getText());
        }
        return enumValue;
    }

    private __InputValue inputValueDefinitionContextToInputValue(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return inputValueDefinitionContextToInputValue(inputValueDefinitionContext, 0);
    }

    private __InputValue inputValueDefinitionContextToInputValue(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, int level) {
        __InputValue inputValue = new __InputValue();
        inputValue.setName(inputValueDefinitionContext.name().getText());
        if (inputValueDefinitionContext.description() != null) {
            inputValue.setDescription(inputValueDefinitionContext.description().getText());
        }
        if (inputValueDefinitionContext.defaultValue() != null) {
            inputValue.setDefaultValue(inputValueDefinitionContext.defaultValue().value().getText());
        }
        inputValue.setType(typeContextToType(inputValueDefinitionContext.type(), level));
        return inputValue;
    }

    private __Type inputObjectTypeDefinitionContextToType(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {
        return inputObjectTypeDefinitionContextToType(inputObjectTypeDefinitionContext, 0);
    }

    private __Type inputObjectTypeDefinitionContextToType(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext, int level) {
        __Type type = new __Type();
        type.setKind(__TypeKind.INPUT_OBJECT);
        type.setName(inputObjectTypeDefinitionContext.name().getText());
        if (inputObjectTypeDefinitionContext.description() != null) {
            type.setDescription(inputObjectTypeDefinitionContext.description().getText());
        }
        type.setInputFields(inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition().stream()
                .map(inputValueDefinitionContext -> inputValueDefinitionContextToInputValue(inputValueDefinitionContext, level + 1)).collect(Collectors.toList()));
        return type;
    }

    private __Type scalarTypeDefinitionContextToType(GraphqlParser.ScalarTypeDefinitionContext scalarTypeDefinitionContext) {
        __Type type = new __Type();
        type.setKind(__TypeKind.SCALAR);
        type.setName(scalarTypeDefinitionContext.name().getText());
        if (scalarTypeDefinitionContext.description() != null) {
            type.setDescription(scalarTypeDefinitionContext.description().getText());
        }
        return type;
    }

    private __Type innerScalarTypeDefinitionContextToType(String scalarName) {
        __Type type = new __Type();
        type.setKind(__TypeKind.SCALAR);
        type.setName(scalarName);
        return type;
    }

    private __Directive directiveDefinitionContextToDirective(GraphqlParser.DirectiveDefinitionContext directiveDefinitionContext) {
        __Directive directive = new __Directive();
        directive.setName(directiveDefinitionContext.name().getText());

        if (directiveDefinitionContext.description() != null) {
            directive.setDescription(directiveDefinitionContext.description().getText());
        }
        if (directiveDefinitionContext.argumentsDefinition() != null) {
            directive.setArgs(directiveDefinitionContext.argumentsDefinition().inputValueDefinition().stream()
                    .map(this::inputValueDefinitionContextToInputValue).collect(Collectors.toList()));
        }
        List<__DirectiveLocation> directiveLocationList = new ArrayList<>();
        addDirectiveDefinitionsContextToDirectiveLocationList(directiveDefinitionContext.directiveLocations(), directiveLocationList);
        directive.setLocations(directiveLocationList);
        return directive;
    }

    private void addDirectiveDefinitionsContextToDirectiveLocationList(GraphqlParser.DirectiveLocationsContext directiveLocationsContext, List<__DirectiveLocation> directiveLocationList) {
        if (directiveLocationsContext.directiveLocation() != null) {
            directiveLocationList.add(__DirectiveLocation.valueOf(directiveLocationsContext.directiveLocation().name().getText()));
        }
        if (directiveLocationsContext.directiveLocations() != null) {
            addDirectiveDefinitionsContextToDirectiveLocationList(directiveLocationsContext.directiveLocations(), directiveLocationList);
        }
    }
}
