package io.graphoenix.jsonpath.utils;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.jsonpath.expression.ArrayValue;
import io.graphoenix.jsonpath.expression.BooleanValue;
import io.graphoenix.jsonpath.expression.Expression;
import io.graphoenix.jsonpath.expression.NullValue;
import io.graphoenix.jsonpath.expression.NumberValue;
import io.graphoenix.jsonpath.expression.StringValue;
import jakarta.enterprise.context.ApplicationScoped;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.stream.Collectors;

import static io.graphoenix.core.utils.DocumentUtil.DOCUMENT_UTIL;

@ApplicationScoped
public class JsonValueUtil {
    public Expression valueToJsonValue(GraphqlParser.ValueContext valueContext) {
        if (valueContext.NullValue() != null) {
            return new NullValue();
        } else if (valueContext.enumValue() != null) {
            return enumValueToJsonValue(valueContext.enumValue());
        } else if (valueContext.arrayValue() != null) {
            return new ArrayValue(valueContext.arrayValue().value().stream().map(this::valueToJsonValue).collect(Collectors.toList()));
        } else {
            return scalarValueToJsonValue(
                    valueContext.StringValue(),
                    valueContext.IntValue(),
                    valueContext.FloatValue(),
                    valueContext.BooleanValue(),
                    valueContext.NullValue()
            );
        }
    }

    public Expression valueWithVariableToJsonValue(GraphqlParser.ValueWithVariableContext valueWithVariableContext) {
        if (valueWithVariableContext.NullValue() != null) {
            return new NullValue();
        } else if (valueWithVariableContext.enumValue() != null) {
            return enumValueToJsonValue(valueWithVariableContext.enumValue());
        } else if (valueWithVariableContext.arrayValueWithVariable() != null) {
            return new ArrayValue(valueWithVariableContext.arrayValueWithVariable().valueWithVariable().stream().map(this::valueWithVariableToJsonValue).collect(Collectors.toList()));
        } else {
            return scalarValueToJsonValue(
                    valueWithVariableContext.StringValue(),
                    valueWithVariableContext.IntValue(),
                    valueWithVariableContext.FloatValue(),
                    valueWithVariableContext.BooleanValue(),
                    valueWithVariableContext.NullValue()
            );
        }
    }

    public Expression enumValueToJsonValue(GraphqlParser.EnumValueContext enumValueContext) {
        return new StringValue(enumValueContext.enumValueName().getText());
    }

    public Expression scalarValueToJsonValue(TerminalNode stringValue, TerminalNode intValue, TerminalNode floatValue, TerminalNode booleanValue, TerminalNode nullValue) {
        if (stringValue != null) {
            return new StringValue(DOCUMENT_UTIL.getStringValue(stringValue));
        } else if (intValue != null) {
            return new NumberValue(intValue.getText());
        } else if (floatValue != null) {
            return new NumberValue(floatValue.getText());
        } else if (booleanValue != null) {
            return new BooleanValue(booleanValue.getText());
        } else if (nullValue != null) {
            return new NullValue();
        }
        return null;
    }
}
