package io.graphoenix.graphql.builder.introspection;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.antlr.manager.impl.GraphqlAntlrManager;
import io.graphoenix.graphql.builder.introspection.dto.__Field;
import io.graphoenix.graphql.builder.introspection.dto.__Schema;
import io.graphoenix.graphql.builder.introspection.dto.__Type;
import io.graphoenix.graphql.builder.introspection.dto.__TypeKind;

public class IntrospectionDtoWrapper {

    private final GraphqlAntlrManager manager;

    public IntrospectionDtoWrapper(GraphqlAntlrManager manager) {
        this.manager = manager;
    }

    public __Schema buildIntrospectionSchema() {

        __Schema schema = new __Schema();


        return schema;
    }

    private __Type ObjectTypeDefinitionContextToType(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        __Type type = new __Type();
        type.setKind(__TypeKind.OBJECT);
        type.setName(objectTypeDefinitionContext.name().getText());
        if (objectTypeDefinitionContext.description() != null) {
            type.setDescription(objectTypeDefinitionContext.description().getText());
        }
        return type;
    }

    private __Field fieldDefinitionContextToField(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        __Field field = new __Field();
        field.setName(fieldDefinitionContext.name().getText());
        if (fieldDefinitionContext.description() != null) {
            field.setDescription(fieldDefinitionContext.description().getText());
        }
        return field;
    }

    private __Type typeContextToType(GraphqlParser.TypeContext typeContext) {
        if (typeContext.typeName() != null) {
            if (manager.isObject(typeContext.typeName().getText())) {
                return manager.getObject(typeContext.typeName().getText()).map(this::ObjectTypeDefinitionContextToType).orElse(null);
            } else if (manager.isEnum(typeContext.typeName().getText())) {
                return manager.getEnum(typeContext.typeName().getText()).map(this::EnumTypeDefinitionContextToType).orElse(null);
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
                    type.setOfType(manager.getObject(typeContext.nonNullType().typeName().getText()).map(this::ObjectTypeDefinitionContextToType).orElse(null));
                } else if (manager.isEnum(typeContext.typeName().getText())) {
                    type.setOfType(manager.getEnum(typeContext.nonNullType().typeName().getText()).map(this::EnumTypeDefinitionContextToType).orElse(null));
                }
            } else if (typeContext.nonNullType().listType() != null) {
                type.setOfType(typeContextToType(typeContext.listType().type()));
            }
            return type;
        }

        return null;
    }

    private __Type EnumTypeDefinitionContextToType(GraphqlParser.EnumTypeDefinitionContext enumTypeDefinitionContext) {
        __Type type = new __Type();
        type.setKind(__TypeKind.ENUM);
        type.setName(enumTypeDefinitionContext.name().getText());
        if (enumTypeDefinitionContext.description() != null) {
            type.setDescription(enumTypeDefinitionContext.description().getText());
        }
        return type;
    }

}
