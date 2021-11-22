package io.graphoenix.graphql.generator.operation;

import com.pivovarit.function.ThrowingBiFunction;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class ObjectValueWithVariable {

    private final STGroup stGroupFile = new STGroupFile("stg/operation/ObjectValueWithVariable.stg");

    private final Map<String, ValueWithVariable> objectValueWithVariable;

    public ObjectValueWithVariable(Object objectValueWithVariable) {
        Class<?> clazz = objectValueWithVariable.getClass();
        ThrowingBiFunction<Field, Object, Object, IllegalAccessException> getField = (field, object) -> {
            field.setAccessible(true);
            return field.get(object);
        };
        this.objectValueWithVariable = Arrays.stream(clazz.getDeclaredFields()).collect(Collectors.toMap(Field::getName, field -> new ValueWithVariable(getField.unchecked().apply(field, objectValueWithVariable))));
    }

    @Override
    public String toString() {
        ST st = stGroupFile.getInstanceOf("objectValueWithVariableDefinition");
        st.add("objectValueWithVariable", objectValueWithVariable.keySet().stream().collect(Collectors.toMap(key -> key, key -> objectValueWithVariable.get(key).toString())));
        return st.render();
    }
}
