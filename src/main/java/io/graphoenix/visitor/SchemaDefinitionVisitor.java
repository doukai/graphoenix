package io.graphoenix.visitor;

import graphql.parser.antlr.GraphqlBaseVisitor;
import graphql.parser.antlr.GraphqlParser;


public class SchemaDefinitionVisitor<T> extends GraphqlBaseVisitor<T> {

    @Override
    public T visitTypeDefinition(GraphqlParser.TypeDefinitionContext ctx) {

        return super.visitTypeDefinition(ctx);
    }

    @Override
    public T visitFieldsDefinition(GraphqlParser.FieldsDefinitionContext ctx) {
        return super.visitFieldsDefinition(ctx);
    }


}
