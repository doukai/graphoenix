package io.graphoenix.jsonpath.expression.operators;

import io.graphoenix.jsonpath.expression.NullValue;

public class IsNullExpression extends ComparisonOperator {
    public IsNullExpression(String element) {
        super(element, "==", new NullValue());
    }
}
