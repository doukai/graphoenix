package io.graphoenix.core.utils;

import com.google.common.base.CaseFormat;
import com.google.common.base.Strings;
import io.graphoenix.core.document.InputValue;
import io.graphoenix.core.error.ElementProcessException;
import io.graphoenix.core.error.GraphQLErrorType;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.core.operation.Directive;
import io.graphoenix.spi.annotation.MutationOperation;
import io.graphoenix.spi.annotation.QueryOperation;
import io.graphoenix.spi.dto.type.OperationType;
import org.eclipse.microprofile.graphql.Enum;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.*;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.lang.model.element.*;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.graphoenix.core.error.ElementProcessErrorType.EXPRESSION_VARIABLE_PARAMETER_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.UNSUPPORTED_FIELD_TYPE;
import static io.graphoenix.spi.constant.Hammurabi.INPUT_SUFFIX;
import static io.graphoenix.spi.dto.type.OperationType.MUTATION;
import static io.graphoenix.spi.dto.type.OperationType.QUERY;
import static javax.lang.model.type.TypeKind.ARRAY;

public enum ElementUtil {
    ELEMENT_UTIL;

    public VariableElement getParameterFromExecutableElement(ExecutableElement executableElement, String name) {
        return executableElement.getParameters().stream()
                .filter(parameter -> parameter.getSimpleName().toString().equals(name))
                .findFirst()
                .orElseThrow(() -> new ElementProcessException(EXPRESSION_VARIABLE_PARAMETER_NOT_EXIST.bind(executableElement.getSimpleName(), name)));
    }

    public String getNameFromElement(Element element) {
        Name nameAnnotation = element.getAnnotation(Name.class);
        Type typeAnnotation = element.getAnnotation(Type.class);
        Enum enumAnnotation = element.getAnnotation(Enum.class);
        Interface interfaceAnnotation = element.getAnnotation(Interface.class);
        Input inputAnnotation = element.getAnnotation(Input.class);
        Query queryAnnotation = element.getAnnotation(Query.class);
        Mutation mutationAnnotation = element.getAnnotation(Mutation.class);
        if (nameAnnotation != null && !Strings.isNullOrEmpty(nameAnnotation.value())) {
            return nameAnnotation.value();
        } else if (typeAnnotation != null && !Strings.isNullOrEmpty(typeAnnotation.value())) {
            return typeAnnotation.value();
        } else if (enumAnnotation != null && !Strings.isNullOrEmpty(enumAnnotation.value())) {
            return enumAnnotation.value();
        } else if (interfaceAnnotation != null && !Strings.isNullOrEmpty(interfaceAnnotation.value())) {
            return interfaceAnnotation.value();
        } else if (inputAnnotation != null && !Strings.isNullOrEmpty(inputAnnotation.value())) {
            return inputAnnotation.value();
        } else if (queryAnnotation != null && !Strings.isNullOrEmpty(queryAnnotation.value())) {
            return queryAnnotation.value();
        } else if (mutationAnnotation != null && !Strings.isNullOrEmpty(mutationAnnotation.value())) {
            return mutationAnnotation.value();
        } else {
            if (element.getSimpleName().toString().startsWith("get")) {
                return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, element.getSimpleName().toString().replaceFirst("get", ""));
            } else if (element.getSimpleName().toString().startsWith("set")) {
                return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, element.getSimpleName().toString().replaceFirst("set", ""));
            } else {
                return element.getSimpleName().toString();
            }
        }
    }

    public OperationType getOperationTypeFromExecutableElement(ExecutableElement executableElement) {
        QueryOperation queryOperation = executableElement.getAnnotation(QueryOperation.class);
        if (queryOperation != null) {
            return QUERY;
        }
        MutationOperation mutationOperation = executableElement.getAnnotation(MutationOperation.class);
        if (mutationOperation != null) {
            return MUTATION;
        }
        throw new GraphQLErrors(GraphQLErrorType.UNSUPPORTED_OPERATION_TYPE);
    }

    public String getSelectionSetFromExecutableElement(ExecutableElement executableElement) {
        QueryOperation queryOperation = executableElement.getAnnotation(QueryOperation.class);
        if (queryOperation != null) {
            return queryOperation.selectionSet();
        }
        MutationOperation mutationOperation = executableElement.getAnnotation(MutationOperation.class);
        if (mutationOperation != null) {
            return mutationOperation.selectionSet();
        }
        throw new GraphQLErrors(GraphQLErrorType.UNSUPPORTED_OPERATION_TYPE);
    }

    public int getLayersFromExecutableElement(ExecutableElement executableElement) {
        QueryOperation queryOperation = executableElement.getAnnotation(QueryOperation.class);
        if (queryOperation != null) {
            return queryOperation.layers();
        }
        MutationOperation mutationOperation = executableElement.getAnnotation(MutationOperation.class);
        if (mutationOperation != null) {
            return mutationOperation.layers();
        }
        throw new GraphQLErrors(GraphQLErrorType.UNSUPPORTED_OPERATION_TYPE);
    }

    public String getOperationFieldNameFromExecutableElement(ExecutableElement executableElement) {
        QueryOperation queryOperationAnnotation = executableElement.getAnnotation(QueryOperation.class);
        if (queryOperationAnnotation != null && !Strings.isNullOrEmpty(queryOperationAnnotation.value())) {
            return queryOperationAnnotation.value();
        }
        MutationOperation mutationOperationAnnotation = executableElement.getAnnotation(MutationOperation.class);
        if (mutationOperationAnnotation != null && !Strings.isNullOrEmpty(mutationOperationAnnotation.value())) {
            return mutationOperationAnnotation.value();
        }
        return executableElement.getSimpleName().toString();
    }

    public String getOperationNameFromExecutableElement(ExecutableElement executableElement, int index) {
        TypeElement typeElement = (TypeElement) executableElement.getEnclosingElement();
        return typeElement.getQualifiedName().toString().replaceAll("\\.", "_") +
                "_" +
                executableElement.getSimpleName().toString() +
                "_" +
                index;
    }

    public Optional<String> getSourceNameFromExecutableElement(ExecutableElement executableElement) {
        return executableElement.getParameters().stream()
                .flatMap(variableElement -> Stream.ofNullable(variableElement.getAnnotation(Source.class)))
                .map(Source::name)
                .filter(name -> !Strings.isNullOrEmpty(name))
                .findFirst();
    }

    public String getDescriptionFromElement(Element element) {
        Description description = element.getAnnotation(Description.class);
        if (description != null) {
            return description.value();
        } else {
            return null;
        }
    }

    public List<Directive> getDirectivesFromElement(Element element) {
        return element.getAnnotationMirrors().stream()
                .filter(annotationMirror -> annotationMirror.getAnnotationType().getAnnotation(io.graphoenix.spi.annotation.Directive.class) != null)
                .map(Directive::new)
                .collect(Collectors.toList());
    }

    public String getDefaultValueFromElement(Element element) {
        DefaultValue defaultValue = element.getAnnotation(DefaultValue.class);
        if (defaultValue != null) {
            return defaultValue.value();
        } else {
            return null;
        }
    }

    public String executableElementToTypeName(ExecutableElement executableElement, Types types) {
        TypeMirror typeMirror;
        String typeName = getTypeName(executableElement.getReturnType(), types);
        if (typeName.equals(Flux.class.getCanonicalName())) {
            typeMirror = ((DeclaredType) (executableElement).getReturnType()).getTypeArguments().get(0);
            return "[" + elementToTypeName(executableElement, typeMirror, types) + "]";
        } else if (typeName.equals(Mono.class.getCanonicalName())) {
            typeMirror = ((DeclaredType) (executableElement).getReturnType()).getTypeArguments().get(0);
            return elementToTypeName(executableElement, typeMirror, types);
        } else if (typeName.equals(PublisherBuilder.class.getCanonicalName())) {
            typeMirror = ((DeclaredType) (executableElement).getReturnType()).getTypeArguments().get(0);
            return elementToTypeName(executableElement, typeMirror, types);
        } else {
            typeMirror = executableElement.getReturnType();
            return elementToTypeName(executableElement, typeMirror, types);
        }
    }

    public String variableElementToTypeName(VariableElement variableElement, Types types) {
        TypeMirror typeMirror;
        String typeName = getTypeName(variableElement.asType(), types);
        if (typeName.equals(Flux.class.getCanonicalName())) {
            typeMirror = ((DeclaredType) variableElement.asType()).getTypeArguments().get(0);
            return "[" + elementToTypeName(variableElement, typeMirror, types) + "]";
        } else if (typeName.equals(Mono.class.getCanonicalName())) {
            typeMirror = ((DeclaredType) variableElement.asType()).getTypeArguments().get(0);
            return elementToTypeName(variableElement, typeMirror, types);
        } else if (typeName.equals(PublisherBuilder.class.getCanonicalName())) {
            typeMirror = ((DeclaredType) variableElement.asType()).getTypeArguments().get(0);
            return elementToTypeName(variableElement, typeMirror, types);
        } else {
            typeMirror = variableElement.asType();
            return elementToTypeName(variableElement, typeMirror, types);
        }
    }

    public String elementToTypeName(Element element, TypeMirror typeMirror, Types types) {
        String objectTypeName;
        String typeName = getTypeName(typeMirror, types);
        if (element.getAnnotation(Id.class) != null) {
            objectTypeName = "ID";
        } else if (typeMirror.getKind().equals(ARRAY)) {
            objectTypeName = "[" + elementToTypeName(element, ((ArrayType) typeMirror).getComponentType(), types) + "]";
        }  else if (typeName.equals(int.class.getCanonicalName()) ||
                typeName.equals(long.class.getCanonicalName()) ||
                typeName.equals(short.class.getCanonicalName()) ||
                typeName.equals(byte.class.getCanonicalName()) ||
                typeName.equals(Integer.class.getCanonicalName()) ||
                typeName.equals(Long.class.getCanonicalName()) ||
                typeName.equals(Short.class.getCanonicalName()) ||
                typeName.equals(Byte.class.getCanonicalName())) {
            objectTypeName = "Int";
        } else if (typeName.equals(float.class.getCanonicalName()) ||
                typeName.equals(double.class.getCanonicalName()) ||
                typeName.equals(Float.class.getCanonicalName()) ||
                typeName.equals(Double.class.getCanonicalName())) {
            objectTypeName = "Float";
        } else if (typeName.equals(char.class.getCanonicalName()) ||
                typeName.equals(String.class.getCanonicalName()) ||
                typeName.equals(Character.class.getCanonicalName())) {
            objectTypeName = "String";
        } else if (typeName.equals(boolean.class.getCanonicalName()) ||
                typeName.equals(Boolean.class.getCanonicalName())) {
            objectTypeName = "Boolean";
        } else if (typeName.equals(BigInteger.class.getCanonicalName())) {
            objectTypeName = "BigInteger";
        } else if (typeName.equals(BigDecimal.class.getCanonicalName())) {
            objectTypeName = "BigDecimal";
        } else if (typeName.equals(LocalDate.class.getCanonicalName())) {
            objectTypeName = "Date";
        } else if (typeName.equals(LocalTime.class.getCanonicalName())) {
            objectTypeName = "Time";
        } else if (typeName.equals(LocalDateTime.class.getCanonicalName())) {
            objectTypeName = "DateTime";
        } else if (typeName.equals(Collection.class.getCanonicalName()) ||
                typeName.equals(List.class.getCanonicalName()) ||
                typeName.equals(Set.class.getCanonicalName())) {
            objectTypeName = "[" + elementToTypeName(element, ((DeclaredType) typeMirror).getTypeArguments().get(0), types) + "]";
        } else {
            objectTypeName = getNameFromElement(types.asElement(typeMirror));
        }

        if (element.getAnnotation(NonNull.class) != null || typeMirror.getKind().isPrimitive()) {
            return objectTypeName + "!";
        } else {
            return objectTypeName;
        }
    }

    public String executableElementToInputTypeName(ExecutableElement executableElement, Types types) {
        return elementToInputTypeName(executableElement, executableElement.getReturnType(), types);
    }

    public String variableElementToInputTypeName(VariableElement variableElement, Types types) {
        return elementToInputTypeName(variableElement, variableElement.asType(), types);
    }

    public String elementToInputTypeName(Element element, TypeMirror typeMirror, Types types) {
        String inputTypeName;
        String typeName = getTypeName(typeMirror, types);
        if (element.getAnnotation(Id.class) != null) {
            inputTypeName = "ID";
        } else if (typeMirror.getKind().equals(ARRAY)) {
            inputTypeName = "[" + elementToInputTypeName(element, ((ArrayType) typeMirror).getComponentType(), types) + "]";
        } else if (typeName.equals(int.class.getCanonicalName()) ||
                typeName.equals(long.class.getCanonicalName()) ||
                typeName.equals(short.class.getCanonicalName()) ||
                typeName.equals(byte.class.getCanonicalName()) ||
                typeName.equals(Integer.class.getCanonicalName()) ||
                typeName.equals(Long.class.getCanonicalName()) ||
                typeName.equals(Short.class.getCanonicalName()) ||
                typeName.equals(Byte.class.getCanonicalName())) {
            inputTypeName = "Int";
        } else if (typeName.equals(float.class.getCanonicalName()) ||
                typeName.equals(double.class.getCanonicalName()) ||
                typeName.equals(Float.class.getCanonicalName()) ||
                typeName.equals(Double.class.getCanonicalName())) {
            inputTypeName = "Float";
        } else if (typeName.equals(char.class.getCanonicalName()) ||
                typeName.equals(String.class.getCanonicalName()) ||
                typeName.equals(Character.class.getCanonicalName())) {
            inputTypeName = "String";
        } else if (typeName.equals(boolean.class.getCanonicalName()) ||
                typeName.equals(Boolean.class.getCanonicalName())) {
            inputTypeName = "Boolean";
        } else if (typeName.equals(BigInteger.class.getCanonicalName())) {
            inputTypeName = "BigInteger";
        } else if (typeName.equals(BigDecimal.class.getCanonicalName())) {
            inputTypeName = "BigDecimal";
        } else if (typeName.equals(LocalDate.class.getCanonicalName())) {
            inputTypeName = "Date";
        } else if (typeName.equals(LocalTime.class.getCanonicalName())) {
            inputTypeName = "Time";
        } else if (typeName.equals(LocalDateTime.class.getCanonicalName())) {
            inputTypeName = "DateTime";
        } else if (types.asElement(typeMirror).getKind().equals(ElementKind.ENUM)) {
            inputTypeName = getNameFromElement(types.asElement(typeMirror));
        } else if (typeName.equals(Collection.class.getCanonicalName()) ||
                typeName.equals(List.class.getCanonicalName()) ||
                typeName.equals(Set.class.getCanonicalName())) {
            if (element.getKind().equals(ElementKind.METHOD)) {
                inputTypeName = "[" + elementToInputTypeName(element, ((DeclaredType) ((ExecutableElement) element).getReturnType()).getTypeArguments().get(0), types) + "]";
            } else if (element.getKind().equals(ElementKind.FIELD) || element.getKind().equals(ElementKind.PARAMETER)) {
                inputTypeName = "[" + elementToInputTypeName(element, ((DeclaredType) element.asType()).getTypeArguments().get(0), types) + "]";
            } else {
                throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE.bind(typeName));
            }
        } else {
            if (typeName.endsWith(INPUT_SUFFIX)) {
                inputTypeName = getNameFromElement(types.asElement(typeMirror));
            } else {
                inputTypeName = getNameFromElement(types.asElement(typeMirror)) + INPUT_SUFFIX;
            }
        }

        if (element.getAnnotation(NonNull.class) != null || typeMirror.getKind().isPrimitive()) {
            return inputTypeName + "!";
        } else {
            return inputTypeName;
        }
    }

    public Set<InputValue> executableElementParametersToInputValues(ExecutableElement executableElement, Types typeUtils) {
        if (executableElement.getParameters() != null) {
            return executableElement.getParameters().stream()
                    .filter(variableElement -> variableElement.getAnnotation(Source.class) == null)
                    .map(variableElement ->
                            new InputValue()
                                    .setName(getNameFromElement(variableElement))
                                    .setDefaultValue(getDefaultValueFromElement(variableElement))
                                    .setType(variableElementToTypeName(variableElement, typeUtils))
                                    .setDescription(getDescriptionFromElement(variableElement))
                                    .addDirectives(ELEMENT_UTIL.getDirectivesFromElement(variableElement))
                    )
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        } else {
            return new LinkedHashSet<>();
        }
    }

    public String getTypeMirrorName(TypeMirror typeMirror, Types types) {
        if (typeMirror.getKind().isPrimitive()) {
            return typeMirror.getKind().toString().toLowerCase();
        } else if (typeMirror.getKind().equals(ARRAY)) {
            return getTypeMirrorName(((ArrayType) typeMirror).getComponentType(), types) + "[]";
        } else if (typeMirror.getKind().equals(TypeKind.DECLARED)) {
            DeclaredType declaredType = (DeclaredType) typeMirror;
            if (declaredType.getTypeArguments() != null && !declaredType.getTypeArguments().isEmpty()) {
                return ((TypeElement) types.asElement(declaredType)).getQualifiedName().toString() +
                        "<" +
                        declaredType.getTypeArguments().stream().map(argumentTypeMirror -> getTypeMirrorName(argumentTypeMirror, types))
                                .collect(Collectors.joining(", "))
                        + ">";
            }
            return ((TypeElement) types.asElement(declaredType)).getQualifiedName().toString();
        }
        throw new RuntimeException("illegal typeMirror: " + typeMirror);
    }

    public String getTypeName(TypeMirror typeMirror, Types types) {
        if (typeMirror.getKind().isPrimitive()) {
            return typeMirror.getKind().toString().toLowerCase();
        } else if (typeMirror.getKind().equals(ARRAY)) {
            return getTypeName(((ArrayType) typeMirror).getComponentType(), types) + "[]";
        } else if (typeMirror.getKind().equals(TypeKind.DECLARED)) {
            return ((TypeElement) types.asElement(typeMirror)).getQualifiedName().toString();
        }
        throw new RuntimeException("illegal typeMirror: " + typeMirror);
    }
}
