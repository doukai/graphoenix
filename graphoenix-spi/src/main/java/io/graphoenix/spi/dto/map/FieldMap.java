package io.graphoenix.spi.dto.map;

import graphql.parser.antlr.GraphqlParser;

public class FieldMap {
    GraphqlParser.FieldDefinitionContext from;
    FieldMapWith with;
    GraphqlParser.FieldDefinitionContext to;

    public boolean withType() {
        return with != null;
    }

    public FieldMap(GraphqlParser.FieldDefinitionContext from,
                    FieldMapWith with,
                    GraphqlParser.FieldDefinitionContext to) {
        this.from = from;
        this.with = with;
        this.to = to;
    }

    public FieldMap(GraphqlParser.FieldDefinitionContext from,
                    GraphqlParser.FieldDefinitionContext to) {
        this.from = from;
        this.to = to;
    }

    public GraphqlParser.FieldDefinitionContext getFrom() {
        return from;
    }

    public void setFrom(GraphqlParser.FieldDefinitionContext from) {
        this.from = from;
    }

    public FieldMapWith getWith() {
        return with;
    }

    public void setWith(FieldMapWith with) {
        this.with = with;
    }

    public GraphqlParser.FieldDefinitionContext getTo() {
        return to;
    }

    public void setTo(GraphqlParser.FieldDefinitionContext to) {
        this.to = to;
    }
}
