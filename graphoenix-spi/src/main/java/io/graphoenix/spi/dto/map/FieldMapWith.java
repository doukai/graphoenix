package io.graphoenix.spi.dto.map;

import graphql.parser.antlr.GraphqlParser;

public class FieldMapWith {
    GraphqlParser.ObjectTypeDefinitionContext type;
    GraphqlParser.FieldDefinitionContext from;
    GraphqlParser.FieldDefinitionContext to;

    public FieldMapWith(GraphqlParser.ObjectTypeDefinitionContext type,
                        GraphqlParser.FieldDefinitionContext from,
                        GraphqlParser.FieldDefinitionContext to) {
        this.type = type;
        this.from = from;
        this.to = to;
    }

    public GraphqlParser.ObjectTypeDefinitionContext getType() {
        return type;
    }

    public void setType(GraphqlParser.ObjectTypeDefinitionContext type) {
        this.type = type;
    }

    public GraphqlParser.FieldDefinitionContext getFrom() {
        return from;
    }

    public void setFrom(GraphqlParser.FieldDefinitionContext from) {
        this.from = from;
    }

    public GraphqlParser.FieldDefinitionContext getTo() {
        return to;
    }

    public void setTo(GraphqlParser.FieldDefinitionContext to) {
        this.to = to;
    }
}
