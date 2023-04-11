package io.graphoenix.graphql.builder.introspection;

import com.google.common.collect.Streams;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.core.introspection.__Directive;
import io.graphoenix.core.introspection.__DirectiveLocation;
import io.graphoenix.core.introspection.__EnumValue;
import io.graphoenix.core.introspection.__Field;
import io.graphoenix.core.introspection.__InputValue;
import io.graphoenix.core.introspection.__Schema;
import io.graphoenix.core.introspection.__Type;
import io.graphoenix.core.introspection.__TypeKind;
import io.graphoenix.core.operation.Field;
import io.graphoenix.core.operation.ObjectValueWithVariable;
import io.graphoenix.core.operation.Operation;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.tinylog.Logger;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.graphoenix.core.error.GraphQLErrorType.UNSUPPORTED_FIELD_TYPE;
import static io.graphoenix.spi.constant.Hammurabi.INTROSPECTION_PREFIX;
import static io.graphoenix.spi.constant.Hammurabi.LIST_INPUT_NAME;

@ApplicationScoped
public class IntrospectionMutationBuilder {

    private final IGraphQLDocumentManager manager;

    private final int levelThreshold;

    @Inject
    public IntrospectionMutationBuilder(IGraphQLDocumentManager manager) {
        this.manager = manager;
        this.levelThreshold = 1;
    }

    public Operation buildIntrospectionSchemaMutation() {
        __Schema schema = buildIntrospectionSchema();

        Operation operation = new Operation()
                .setOperationType("mutation")
                .setName("introspection")
                .addField(
                        new Field("__schema")
                                .setArguments(schema.toArguments())
                                .addField(new Field("id"))
                );

        List<ObjectValueWithVariable> ofTypes =
                Stream.ofNullable(schema.getTypes())
                        .flatMap(Collection::stream)
                        .flatMap(type -> {
                                    switch (type.getKind()) {
                                        case SCALAR:
                                        case OBJECT:
                                        case INTERFACE:
                                        case UNION:
                                        case ENUM:
                                        case INPUT_OBJECT:
                                            return Stream.of(
                                                    buildNonNullType(type),
                                                    buildListType(type),
                                                    buildNonNullListType(type),
                                                    buildListNonNullType(type),
                                                    buildNonNullListNonNullType(type)
                                            );
                                        default:
                                            return Stream.empty();
                                    }
                                }
                        )
                        .map(__Type::toObjectValue)
                        .collect(Collectors.toList());

        if (ofTypes.size() > 0) {
            operation.addField(
                    new Field("__typeList")
                            .addArgument(LIST_INPUT_NAME, ofTypes)
                            .addField(new Field("name"))
            );
        }

        List<ObjectValueWithVariable> interfacesObjectValues = manager.getObjects()
                .map(this::buildType)
                .flatMap(__Type::getInterfacesObjectValues)
                .collect(Collectors.toList());

        if (interfacesObjectValues.size() > 0) {
            operation.addField(
                    new Field("__typeInterfacesList")
                            .addArgument(LIST_INPUT_NAME, interfacesObjectValues)
                            .addField(new Field("id"))
            );
        }

        List<ObjectValueWithVariable> possibleTypesObjectValues = manager.getInterfaces()
                .map(this::buildType)
                .flatMap(__Type::getPossibleTypesObjectValues)
                .collect(Collectors.toList());

        if (possibleTypesObjectValues.size() > 0) {
            operation.addField(
                    new Field("__typePossibleTypesList")
                            .addArgument(LIST_INPUT_NAME, possibleTypesObjectValues)
                            .addField(new Field("id"))
            );
        }

        Logger.info("introspection schema mutation build success");
        Logger.debug("\r\n{}", operation.toString());
        return operation;
    }

    public __Schema buildIntrospectionSchema() {
        __Schema schema = new __Schema();
        schema.setTypes(
                Streams.concat(
                        manager.getObjects()
                                .filter(objectTypeDefinitionContext -> !objectTypeDefinitionContext.name().getText().startsWith(INTROSPECTION_PREFIX))
                                .map(this::buildType),
                        manager.getInterfaces()
                                .filter(interfaceTypeDefinitionContext -> !interfaceTypeDefinitionContext.name().getText().startsWith(INTROSPECTION_PREFIX))
                                .map(this::buildType),
                        manager.getInputObjects()
                                .filter(inputObjectTypeDefinitionContext -> !inputObjectTypeDefinitionContext.name().getText().startsWith(INTROSPECTION_PREFIX))
                                .map(this::buildType),
                        manager.getEnums()
                                .filter(enumTypeDefinitionContext -> !enumTypeDefinitionContext.name().getText().startsWith(INTROSPECTION_PREFIX))
                                .map(this::buildType),
                        manager.getScalars()
                                .map(this::buildType)
                ).collect(Collectors.toCollection(LinkedHashSet::new))
        );

        manager.getQueryOperationTypeName()
                .flatMap(manager::getObject)
                .map(this::buildType)
                .ifPresent(schema::setQueryType);

        manager.getMutationOperationTypeName()
                .flatMap(manager::getObject)
                .map(this::buildType)
                .ifPresent(schema::setMutationType);

        manager.getSubscriptionOperationTypeName()
                .flatMap(manager::getObject)
                .map(this::buildType)
                .ifPresent(schema::setSubscriptionType);

        schema.setDirectives(
                manager.getDirectives()
                        .map(this::buildDirective)
                        .collect(Collectors.toCollection(LinkedHashSet::new))
        );

        Logger.info("introspection schema build success");
        Logger.debug("\r\n{}", schema.toString());
        return schema;
    }

    private __Type buildType(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        return buildType(objectTypeDefinitionContext, 0);
    }

    private __Type buildType(GraphqlParser.InterfaceTypeDefinitionContext interfaceTypeDefinitionContext) {
        return buildType(interfaceTypeDefinitionContext, 0);
    }

    private __Type buildType(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext, int level) {
        __Type type = new __Type();
        type.setKind(__TypeKind.OBJECT);
        type.setName(objectTypeDefinitionContext.name().getText());

        if (level == 0) {
            if (objectTypeDefinitionContext.implementsInterfaces() != null) {
                type.setInterfaces(getInterfaces(objectTypeDefinitionContext.implementsInterfaces(), level + 1).collect(Collectors.toCollection(LinkedHashSet::new)));
            } else {
                type.setInterfaces(new LinkedHashSet<>());
            }

            if (objectTypeDefinitionContext.description() != null) {
                type.setDescription(objectTypeDefinitionContext.description().getText());
            }
            type.setFields(
                    objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                            .filter(fieldDefinitionContext -> !manager.getFieldTypeName(fieldDefinitionContext.type()).equals(objectTypeDefinitionContext.name().getText()))
                            .filter(fieldDefinitionContext -> !fieldDefinitionContext.name().getText().startsWith(INTROSPECTION_PREFIX))
                            .map(fieldDefinitionContext -> buildField(fieldDefinitionContext, level + 1))
                            .collect(Collectors.toCollection(LinkedHashSet::new))
            );
        }
        return type;
    }

    private __Type buildType(GraphqlParser.InterfaceTypeDefinitionContext interfaceTypeDefinitionContext, int level) {
        __Type type = new __Type();
        type.setKind(__TypeKind.INTERFACE);
        type.setName(interfaceTypeDefinitionContext.name().getText());

        if (level == 0) {
            if (interfaceTypeDefinitionContext.implementsInterfaces() != null) {
                type.setInterfaces(getInterfaces(interfaceTypeDefinitionContext.implementsInterfaces(), level + 1).collect(Collectors.toCollection(LinkedHashSet::new)));
            } else {
                type.setInterfaces(new LinkedHashSet<>());
            }

            if (interfaceTypeDefinitionContext.description() != null) {
                type.setDescription(interfaceTypeDefinitionContext.description().getText());
            }
            type.setFields(
                    interfaceTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                            .filter(fieldDefinitionContext -> !manager.getFieldTypeName(fieldDefinitionContext.type()).equals(interfaceTypeDefinitionContext.name().getText()))
                            .filter(fieldDefinitionContext -> !fieldDefinitionContext.name().getText().startsWith(INTROSPECTION_PREFIX))
                            .map(fieldDefinitionContext -> buildField(fieldDefinitionContext, level + 1))
                            .collect(Collectors.toCollection(LinkedHashSet::new))
            );
        }
        return type;
    }

    private Stream<__Type> getInterfaces(GraphqlParser.ImplementsInterfacesContext implementsInterfacesContext, int level) {
        return Stream.concat(
                Stream.ofNullable(implementsInterfacesContext.typeName())
                        .flatMap(Collection::stream)
                        .flatMap(typeNameContext -> manager.getInterface(typeNameContext.name().getText()).stream())
                        .map(interfaceTypeDefinitionContext -> buildType(interfaceTypeDefinitionContext, level)),
                Stream.ofNullable(implementsInterfacesContext.implementsInterfaces())
                        .flatMap(subImplementsInterfacesContext -> getInterfaces(subImplementsInterfacesContext, level))
        );
    }

    private __Field buildField(GraphqlParser.FieldDefinitionContext fieldDefinitionContext, int level) {
        __Field field = new __Field();
        field.setName(fieldDefinitionContext.name().getText());

        if (fieldDefinitionContext.description() != null) {
            field.setDescription(fieldDefinitionContext.description().getText());
        }

        if (fieldDefinitionContext.argumentsDefinition() != null) {
            field.setArgs(fieldDefinitionContext.argumentsDefinition().inputValueDefinition().stream()
                    .filter(inputValueDefinitionContext -> !inputValueDefinitionContext.name().getText().startsWith(INTROSPECTION_PREFIX))
                    .filter(inputValueDefinitionContext -> !manager.getFieldTypeName(inputValueDefinitionContext.type()).startsWith(INTROSPECTION_PREFIX))
                    .map(this::buildInputValue).collect(Collectors.toCollection(LinkedHashSet::new)));
        } else {
            field.setArgs(new LinkedHashSet<>());
        }
        field.setType(buildType(fieldDefinitionContext.type(), level));
        return field;
    }

    private __Type buildType(GraphqlParser.TypeContext typeContext) {
        return buildType(typeContext, 0);
    }

    private __Type buildType(GraphqlParser.TypeContext typeContext, int level) {
        if (level > levelThreshold) {
            return null;
        }
        if (typeContext.typeName() != null) {
            if (manager.isScalar(typeContext.typeName().getText())) {
                return manager.getScalar(typeContext.typeName().getText()).map(scalarTypeDefinitionContext -> buildType(scalarTypeDefinitionContext, level)).orElse(null);
            } else if (manager.isObject(typeContext.typeName().getText())) {
                return manager.getObject(typeContext.typeName().getText()).map(objectTypeDefinitionContext -> buildType(objectTypeDefinitionContext, level)).orElse(null);
            } else if (manager.isEnum(typeContext.typeName().getText())) {
                return manager.getEnum(typeContext.typeName().getText()).map(enumTypeDefinitionContext -> buildType(enumTypeDefinitionContext, level)).orElse(null);
            } else if (manager.isInputObject(typeContext.typeName().getText())) {
                return manager.getInputObject(typeContext.typeName().getText()).map(inputObjectTypeDefinitionContext -> buildType(inputObjectTypeDefinitionContext, level)).orElse(null);
            }
        } else if (typeContext.listType() != null) {
            __Type listType = new __Type();
            listType.setKind(__TypeKind.LIST);
            listType.setOfType(buildType(typeContext.listType().type(), level));
            listType.setName("[".concat(listType.getOfType().getName().concat("]")));
            return listType;
        } else if (typeContext.nonNullType() != null) {
            __Type nonNullType = new __Type();
            nonNullType.setKind(__TypeKind.NON_NULL);
            if (typeContext.nonNullType().typeName() != null) {
                if (manager.isScalar(typeContext.nonNullType().typeName().getText())) {
                    nonNullType.setOfType(manager.getScalar(typeContext.nonNullType().typeName().getText()).map(scalarTypeDefinitionContext -> buildType(scalarTypeDefinitionContext, level)).orElse(null));
                } else if (manager.isObject(typeContext.nonNullType().typeName().getText())) {
                    nonNullType.setOfType(manager.getObject(typeContext.nonNullType().typeName().getText()).map(objectTypeDefinitionContext -> buildType(objectTypeDefinitionContext, level)).orElse(null));
                } else if (manager.isEnum(typeContext.nonNullType().typeName().getText())) {
                    nonNullType.setOfType(manager.getEnum(typeContext.nonNullType().typeName().getText()).map(enumTypeDefinitionContext -> buildType(enumTypeDefinitionContext, level)).orElse(null));
                } else if (manager.isInputObject(typeContext.nonNullType().typeName().getText())) {
                    nonNullType.setOfType(manager.getInputObject(typeContext.nonNullType().typeName().getText()).map(inputObjectTypeDefinitionContext -> buildType(inputObjectTypeDefinitionContext, level)).orElse(null));
                }
            } else if (typeContext.nonNullType().listType() != null) {
                __Type listType = new __Type();
                listType.setKind(__TypeKind.LIST);
                listType.setOfType(buildType(typeContext.nonNullType().listType().type(), level));
                listType.setName("[".concat(listType.getOfType().getName()).concat("]"));
                nonNullType.setOfType(listType);
            }
            nonNullType.setName(nonNullType.getOfType().getName().concat("!"));
            return nonNullType;
        }
        throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE.bind(typeContext.getText()));
    }

    private __Type buildType(GraphqlParser.EnumTypeDefinitionContext enumTypeDefinitionContext) {
        return buildType(enumTypeDefinitionContext, 0);
    }

    private __Type buildType(GraphqlParser.EnumTypeDefinitionContext enumTypeDefinitionContext, int level) {
        __Type type = new __Type();
        type.setKind(__TypeKind.ENUM);
        type.setName(enumTypeDefinitionContext.name().getText());

        if (level == 0) {
            if (enumTypeDefinitionContext.description() != null) {
                type.setDescription(enumTypeDefinitionContext.description().getText());
            }
            type.setEnumValues(
                    enumTypeDefinitionContext.enumValueDefinitions().enumValueDefinition().stream()
                            .map(this::buildEnumValue)
                            .collect(Collectors.toCollection(LinkedHashSet::new))
            );
        }
        return type;
    }

    private __EnumValue buildEnumValue(GraphqlParser.EnumValueDefinitionContext enumValueDefinitionContext) {
        __EnumValue enumValue = new __EnumValue();
        enumValue.setName(enumValueDefinitionContext.enumValue().enumValueName().getText());

        if (enumValueDefinitionContext.description() != null) {
            enumValue.setDescription(enumValueDefinitionContext.description().getText());
        }
        return enumValue;
    }

    private __InputValue buildInputValue(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return buildInputValue(inputValueDefinitionContext, 0);
    }

    private __InputValue buildInputValue(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, int level) {
        __InputValue inputValue = new __InputValue();
        inputValue.setName(inputValueDefinitionContext.name().getText());

        if (inputValueDefinitionContext.description() != null) {
            inputValue.setDescription(inputValueDefinitionContext.description().getText());
        }

        if (inputValueDefinitionContext.defaultValue() != null) {
            if (inputValueDefinitionContext.defaultValue().value().StringValue() != null) {
                String stringValue = inputValueDefinitionContext.defaultValue().value().StringValue().getText();
                inputValue.setDefaultValue(stringValue.substring(1, stringValue.length() - 1));
            } else {
                inputValue.setDefaultValue(inputValueDefinitionContext.defaultValue().value().getText());
            }
        }
        inputValue.setType(buildType(inputValueDefinitionContext.type(), level));
        return inputValue;
    }

    private __Type buildType(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {
        return buildType(inputObjectTypeDefinitionContext, 0);
    }

    private __Type buildType(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext, int level) {
        __Type type = new __Type();
        type.setKind(__TypeKind.INPUT_OBJECT);
        type.setName(inputObjectTypeDefinitionContext.name().getText());

        if (level == 0) {
            if (inputObjectTypeDefinitionContext.description() != null) {
                type.setDescription(inputObjectTypeDefinitionContext.description().getText());
            }
            type.setInputFields(
                    inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition().stream()
                            .filter(inputValueDefinitionContext -> !inputValueDefinitionContext.name().getText().startsWith(INTROSPECTION_PREFIX))
                            .filter(inputValueDefinitionContext -> !manager.getFieldTypeName(inputValueDefinitionContext.type()).startsWith(INTROSPECTION_PREFIX))
                            .map(inputValueDefinitionContext -> buildInputValue(inputValueDefinitionContext, level + 1))
                            .collect(Collectors.toCollection(LinkedHashSet::new))
            );
        }
        return type;
    }

    private __Type buildType(GraphqlParser.ScalarTypeDefinitionContext scalarTypeDefinitionContext) {
        return buildType(scalarTypeDefinitionContext, 0);
    }

    private __Type buildType(GraphqlParser.ScalarTypeDefinitionContext scalarTypeDefinitionContext, int level) {
        __Type type = new __Type();
        type.setKind(__TypeKind.SCALAR);
        type.setName(scalarTypeDefinitionContext.name().getText());

        if (level == 0) {
            if (scalarTypeDefinitionContext.description() != null) {
                type.setDescription(scalarTypeDefinitionContext.description().getText());
            }
        }
        return type;
    }

    private __Type buildNonNullType(__Type __type) {
        __Type nonNullType = new __Type();
        nonNullType.setName(__type.getName().concat("!"));
        nonNullType.setOfType(__type);
        nonNullType.setKind(__TypeKind.NON_NULL);
        return nonNullType;
    }

    private __Type buildListType(__Type __type) {
        __Type listType = new __Type();
        listType.setName("[".concat(__type.getName()).concat("]"));
        listType.setOfType(__type);
        listType.setKind(__TypeKind.LIST);
        return listType;
    }

    private __Type buildNonNullListType(__Type __type) {
        return buildNonNullType(buildListType(__type));
    }

    private __Type buildListNonNullType(__Type __type) {
        return buildListType(buildNonNullType(__type));
    }

    private __Type buildNonNullListNonNullType(__Type __type) {
        return buildNonNullType(buildListNonNullType(__type));
    }

    private __Directive buildDirective(GraphqlParser.DirectiveDefinitionContext directiveDefinitionContext) {
        __Directive directive = new __Directive();
        directive.setName(directiveDefinitionContext.name().getText());

        if (directiveDefinitionContext.description() != null) {
            directive.setDescription(directiveDefinitionContext.description().getText());
        }

        if (directiveDefinitionContext.argumentsDefinition() != null) {
            directive.setArgs(
                    directiveDefinitionContext.argumentsDefinition().inputValueDefinition().stream()
                            .map(this::buildInputValue)
                            .collect(Collectors.toCollection(LinkedHashSet::new))
            );
        } else {
            directive.setArgs(new LinkedHashSet<>());
        }

        directive.setLocations(buildDirectiveLocationList(directiveDefinitionContext.directiveLocations()));
        return directive;
    }

    public Set<__DirectiveLocation> buildDirectiveLocationList(GraphqlParser.DirectiveLocationsContext directiveLocationsContext) {
        Set<__DirectiveLocation> directiveLocationList = new LinkedHashSet<>();
        if (directiveLocationsContext.directiveLocation() != null) {
            directiveLocationList.add(__DirectiveLocation.valueOf(directiveLocationsContext.directiveLocation().name().getText()));
        }
        if (directiveLocationsContext.directiveLocations() != null) {
            directiveLocationList.addAll(buildDirectiveLocationList(directiveLocationsContext.directiveLocations()));
        }
        return directiveLocationList;
    }
}
