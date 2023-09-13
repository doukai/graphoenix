package io.graphoenix.core.utils;

import com.google.common.base.CaseFormat;
import graphql.parser.antlr.GraphqlParser;

import static io.graphoenix.spi.constant.Hammurabi.INTROSPECTION_PREFIX;

public enum NameUtil {
    NAME_UTIL;

    public String getSchemaFieldName(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        return getSchemaFieldName(objectTypeDefinitionContext.name().getText());
    }

    public String getSchemaFieldName(String name) {
        if (name.startsWith(INTROSPECTION_PREFIX)) {
            return INTROSPECTION_PREFIX + CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, name.replace(INTROSPECTION_PREFIX, ""));
        } else {
            return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, name);
        }
    }
}
