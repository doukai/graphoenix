package io.graphoenix.java.generator.utils;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import java.util.Arrays;

import static io.graphoenix.core.utils.TypeNameUtil.TYPE_NAME_UTIL;

public enum TypeUtil {
    TYPE_UTIL;

    public TypeName getTypeName(String typeName) {
        String className = TYPE_NAME_UTIL.getClassName(typeName);
        String[] argumentTypeNames = TYPE_NAME_UTIL.getArgumentTypeNames(typeName);
        if (argumentTypeNames.length == 1) {
            return ParameterizedTypeName.get(ClassName.bestGuess(className), getTypeName(argumentTypeNames[0]));
        } else if (argumentTypeNames.length > 1) {
            return ParameterizedTypeName.get(ClassName.bestGuess(className), Arrays.stream(argumentTypeNames).map(this::getTypeName).toArray(TypeName[]::new));
        } else {
            return ClassName.bestGuess(className);
        }
    }

    public ClassName getClassName(String typeName) {
        return ClassName.bestGuess(TYPE_NAME_UTIL.getClassName(typeName));
    }
}
