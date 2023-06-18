package io.graphoenix.jsonpath.expression.operators;

import io.graphoenix.jsonpath.expression.RegularValue;

public class NotLike extends ComparisonOperator {
    public NotLike(String element, String value) {
        super(element, "=~", new RegularValue(value), true);
    }
}
