package io.graphoenix.jsonpath.translator.expression.operators;

import io.graphoenix.jsonpath.translator.expression.RegularValue;

public class Like extends ComparisonOperator {
    public Like(String element, String value) {
        super(element, "=~", new RegularValue(value));
    }
}
