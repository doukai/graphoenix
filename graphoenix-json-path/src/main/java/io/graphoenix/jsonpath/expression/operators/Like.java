package io.graphoenix.jsonpath.expression.operators;

import io.graphoenix.jsonpath.expression.RegularValue;

public class Like extends ComparisonOperator {
    public Like(String element, String value) {
        super(element, "=~", new RegularValue(value));
    }
}
