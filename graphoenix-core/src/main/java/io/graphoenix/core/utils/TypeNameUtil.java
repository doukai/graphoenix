package io.graphoenix.core.utils;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;

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

    public boolean isReactiveType(String typeName) {
        String className = getClassName(typeName);
        return className.equals(PublisherBuilder.class.getCanonicalName()) ||
                className.equals(Mono.class.getCanonicalName()) ||
                className.equals(Flux.class.getCanonicalName());
    }

    public boolean isListType(String typeName) {
        String className = getClassName(typeName);
        return className.equals(Collection.class.getCanonicalName()) ||
                className.equals(List.class.getCanonicalName()) ||
                className.equals(Set.class.getCanonicalName()) ||
                className.equals(Flux.class.getCanonicalName());
    }

    public ClassName toClassName(String className) {
        int i = className.lastIndexOf(".");
        if (i == -1) {
            ClassName.bestGuess(className);
        }
        return ClassName.get(className.substring(0, i), className.substring(i + 1));
    }

    public TypeName toTypeName(String typeName) {
        int i = typeName.lastIndexOf(".");
        if (i == -1) {
            switch (typeName) {
                case "void":
                    return TypeName.VOID;
                case "boolean":
                    return TypeName.BOOLEAN;
                case "byte":
                    return TypeName.BYTE;
                case "short":
                    return TypeName.SHORT;
                case "int":
                    return TypeName.INT;
                case "long":
                    return TypeName.LONG;
                case "char":
                    return TypeName.CHAR;
                case "float":
                    return TypeName.FLOAT;
                case "double":
                    return TypeName.DOUBLE;
                default:
                    return ClassName.bestGuess(typeName);
            }
        }
        return ClassName.get(typeName.substring(0, i), typeName.substring(i + 1));
    }

    public String packageNameToUnderline(String packageName) {
        return String.join("_", packageName.split("\\."));
    }
}
