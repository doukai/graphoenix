package io.graphoenix.core.utils;

import com.squareup.javapoet.ClassName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum TypeNameUtil {
    TYPE_NAME_UTIL;

    public String getClassName(String typeName) {
        if (typeName.contains("<")) {
            int index = typeName.indexOf('<');
            return typeName.substring(0, index);
        } else {
            return typeName;
        }
    }

    public String[] getArgumentTypeNames(String typeName) {
        if (typeName.contains("<")) {
            int index = typeName.indexOf('<');
            String argumentTypeNames = typeName.substring(index + 1, typeName.length() - 1);
            if (argumentTypeNames.contains(",")) {
                List<String> argumentTypeNameList = new ArrayList<>();
                Arrays.stream(argumentTypeNames.split(","))
                        .forEach(argumentClassName -> {
                                    if (argumentTypeNameList.size() > 0) {
                                        int lastIndex = argumentTypeNameList.size() - 1;
                                        String lastArgumentClassName = argumentTypeNameList.get(lastIndex);
                                        if (lastArgumentClassName.contains("<") && !lastArgumentClassName.contains(">")) {
                                            argumentTypeNameList.set(lastIndex, lastArgumentClassName + "," + argumentClassName);
                                        } else {
                                            argumentTypeNameList.add(argumentClassName);
                                        }
                                    } else {
                                        argumentTypeNameList.add(argumentClassName);
                                    }
                                }
                        );
                return argumentTypeNameList.toArray(new String[]{});
            } else {
                return new String[]{argumentTypeNames};
            }
        }
        return new String[]{};
    }

    public String getArgumentTypeName0(String typeName) {
        String[] argumentTypeNames = getArgumentTypeNames(typeName);
        if (argumentTypeNames.length == 0) {
            throw new RuntimeException("argument type not exist in " + typeName);
        }
        return argumentTypeNames[0];
    }

    public ClassName bestGuess(String classNameString) {
        int i = classNameString.lastIndexOf(".");
        return ClassName.get(classNameString.substring(0, i), classNameString.substring(i + 1));
    }

    public String packageNameToUnderline(String packageName) {
        return String.join("_", packageName.split("\\."));
    }
}
