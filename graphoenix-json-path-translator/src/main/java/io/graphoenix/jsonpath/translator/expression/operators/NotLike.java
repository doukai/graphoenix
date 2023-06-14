package io.graphoenix.jsonpath.translator.expression.operators;

import io.graphoenix.jsonpath.translator.expression.RegularValue;

public class NotLike extends ComparisonOperator {
    public NotLike(String element, String value) {
        super(element, "=~", new RegularValue(value), true);
    }
}
