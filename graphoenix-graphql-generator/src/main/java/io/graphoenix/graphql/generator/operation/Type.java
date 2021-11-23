package io.graphoenix.graphql.generator.operation;

import java.util.Collection;

public class Type {

    private final String name;

    public Type(Class<?> clazz) {
        this.name = getTypeName(clazz);
    }

    private String getTypeName(Class<?> clazz) {
        if (clazz.isAssignableFrom(Boolean.class) || clazz.isAssignableFrom(boolean.class)) {
            return "Boolean";
        } else if (clazz.isAssignableFrom(Integer.class) ||
                clazz.isAssignableFrom(Short.class) ||
                clazz.isAssignableFrom(Byte.class) ||
                clazz.isAssignableFrom(int.class) ||
                clazz.isAssignableFrom(short.class) ||
                clazz.isAssignableFrom(byte.class)) {
            return "Int";
        } else if (clazz.isAssignableFrom(Float.class) ||
                clazz.isAssignableFrom(Double.class) ||
                clazz.isAssignableFrom(float.class) ||
                clazz.isAssignableFrom(double.class)) {
            return "Float";
        } else if (clazz.isAssignableFrom(String.class) || clazz.isAssignableFrom(Character.class) || clazz.isAssignableFrom(char.class)) {
            return "String";
        } else if (clazz.isEnum()) {
            return clazz.getSimpleName();
        } else if (clazz.isArray()) {
            return "[" + getTypeName(clazz.getComponentType()) + "]";
        } else if (clazz.isAssignableFrom(Collection.class)) {
            return "[" + getTypeName(clazz.getGenericSuperclass().getClass()) + "]";
        } else {
            return clazz.getSimpleName();
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
