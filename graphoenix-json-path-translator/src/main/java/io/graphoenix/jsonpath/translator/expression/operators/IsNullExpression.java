package io.graphoenix.jsonpath.translator.expression.operators;

import io.graphoenix.jsonpath.translator.expression.NullValue;

public class IsNullExpression extends ComparisonOperator {
    public IsNullExpression(String element) {
        super(element, "==", new NullValue());
    }
}
