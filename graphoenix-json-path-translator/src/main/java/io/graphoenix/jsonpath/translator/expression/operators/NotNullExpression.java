package io.graphoenix.jsonpath.translator.expression.operators;

import io.graphoenix.jsonpath.translator.expression.NullValue;

public class NotNullExpression extends ComparisonOperator {
    public NotNullExpression(String element) {
        super(element, "!=", new NullValue());
    }
}
