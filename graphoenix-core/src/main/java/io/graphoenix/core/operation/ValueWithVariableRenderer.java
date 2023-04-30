package io.graphoenix.core.operation;

import jakarta.json.JsonValue;
import org.stringtemplate.v4.AttributeRenderer;

import java.util.Locale;

public class ValueWithVariableRenderer implements AttributeRenderer<JsonValue> {
    @Override
    public String toString(JsonValue value, String formatString, Locale locale) {
        if (value instanceof ValueWithVariable) {
            return ((ValueWithVariable) value).render();
        }
        return value.toString();
    }
}
