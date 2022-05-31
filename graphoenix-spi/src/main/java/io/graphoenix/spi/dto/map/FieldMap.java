package io.graphoenix.spi.dto.map;

import graphql.parser.antlr.GraphqlParser;

public class FieldMap {
    private GraphqlParser.FieldDefinitionContext from;
    private Boolean anchor;
    private FieldMapWith with;
    private GraphqlParser.FieldDefinitionContext to;

    public boolean withType() {
        return with != null;
    }

    public FieldMap(GraphqlParser.FieldDefinitionContext from,
                    Boolean anchor,
                    FieldMapWith with,
                    GraphqlParser.FieldDefinitionContext to) {
        this.from = from;
        this.anchor = anchor;
        this.with = with;
        this.to = to;
    }

    public FieldMap(GraphqlParser.FieldDefinitionContext from,
                    Boolean anchor,
                    GraphqlParser.FieldDefinitionContext to) {
        this.from = from;
        this.anchor = anchor;
        this.to = to;
    }

    public GraphqlParser.FieldDefinitionContext getFrom() {
        return from;
    }

    public void setFrom(GraphqlParser.FieldDefinitionContext from) {
        this.from = from;
    }

    public Boolean getAnchor() {
        return anchor;
    }

    public void setAnchor(Boolean anchor) {
        this.anchor = anchor;
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
