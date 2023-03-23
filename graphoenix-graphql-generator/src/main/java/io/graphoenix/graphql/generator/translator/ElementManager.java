package io.graphoenix.graphql.generator.translator;

import com.google.common.base.CaseFormat;
import com.google.common.base.Strings;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.error.ElementProcessException;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.core.document.InputValue;
import io.graphoenix.core.operation.Field;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.Enum;
import org.eclipse.microprofile.graphql.Id;
import org.eclipse.microprofile.graphql.Input;
import org.eclipse.microprofile.graphql.Interface;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.NonNull;
import org.eclipse.microprofile.graphql.Source;
import org.eclipse.microprofile.graphql.Type;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.graphoenix.core.error.ElementProcessErrorType.EXPRESSION_VARIABLE_PARAMETER_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.UNSUPPORTED_FIELD_TYPE;
import static io.graphoenix.core.utils.DocumentUtil.DOCUMENT_UTIL;
import static io.graphoenix.spi.constant.Hammurabi.AGGREGATE_SUFFIX;
import static io.graphoenix.spi.constant.Hammurabi.INPUT_SUFFIX;

@ApplicationScoped
public class ElementManager {
    private final IGraphQLDocumentManager manager;

    @Inject
    public ElementManager(IGraphQLDocumentManager manager) {
        this.manager = manager;
    }

    public Set<Field> buildFields(String typeName, int level, int layers) {
        return getFields(typeName, level, layers)
                .map(fieldDefinitionContext -> {
                            Field field = new Field().setName(fieldDefinitionContext.name().getText());
                            if (level <= layers) {
                                field.setFields(buildFields(manager.getFieldTypeName(fieldDefinitionContext.type()), level + 1, layers));
                            }
                            return field;
                        }
                )
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Set<Field> buildFields(String selectionSet) {
        return DOCUMENT_UTIL.graphqlToSelectionSet(selectionSet).selection().stream().map(Field::new).collect(Collectors.toSet());
    }

    public Stream<GraphqlParser.FieldDefinitionContext> getFields(String typeName, int level, int layers) {
        return manager.getFields(typeName)
                .filter(fieldDefinitionContext -> manager.isNotFunctionField(typeName, fieldDefinitionContext.name().getText()))
                .filter(fieldDefinitionContext -> manager.isNotConnectionField(typeName, fieldDefinitionContext.name().getText()))
                .filter(fieldDefinitionContext -> !fieldDefinitionContext.name().getText().endsWith(AGGREGATE_SUFFIX))
                .filter(fieldDefinitionContext -> level < layers ||
                        level == layers && (manager.isScalar(manager.getFieldTypeName(fieldDefinitionContext.type())) || manager.isEnum(manager.getFieldTypeName(fieldDefinitionContext.type())))
                );
    }

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

    public String getDescriptionFromElement(Element element) {
        Description description = element.getAnnotation(Description.class);
        if (description != null) {
            return description.value();
        } else {
            return null;
        }
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
        String typeMirrorName = getTypeMirrorName(executableElement.getReturnType(), types);
        if (typeMirrorName.equals(Flux.class.getCanonicalName())) {
            typeMirror = ((DeclaredType) (executableElement).getReturnType()).getTypeArguments().get(0);
            return "[".concat(elementToTypeName(executableElement, typeMirror, types)).concat("]");
        } else if (typeMirrorName.equals(Mono.class.getCanonicalName())) {
            typeMirror = ((DeclaredType) (executableElement).getReturnType()).getTypeArguments().get(0);
            return elementToTypeName(executableElement, typeMirror, types);
        } else if (typeMirrorName.equals(PublisherBuilder.class.getCanonicalName())) {
            typeMirror = ((DeclaredType) (executableElement).getReturnType()).getTypeArguments().get(0);
            return elementToTypeName(executableElement, typeMirror, types);
        } else {
            typeMirror = executableElement.getReturnType();
            return elementToTypeName(executableElement, typeMirror, types);
        }
    }

    public String variableElementToTypeName(VariableElement variableElement, Types types) {
        TypeMirror typeMirror;
        String typeMirrorName = getTypeMirrorName(variableElement.asType(), types);
        if (typeMirrorName.equals(Flux.class.getCanonicalName())) {
            typeMirror = ((DeclaredType) variableElement.asType()).getTypeArguments().get(0);
            return "[".concat(elementToTypeName(variableElement, typeMirror, types)).concat("]");
        } else if (typeMirrorName.equals(Mono.class.getCanonicalName())) {
            typeMirror = ((DeclaredType) variableElement.asType()).getTypeArguments().get(0);
            return elementToTypeName(variableElement, typeMirror, types);
        } else if (typeMirrorName.equals(PublisherBuilder.class.getCanonicalName())) {
            typeMirror = ((DeclaredType) variableElement.asType()).getTypeArguments().get(0);
            return elementToTypeName(variableElement, typeMirror, types);
        } else {
            typeMirror = variableElement.asType();
            return elementToTypeName(variableElement, typeMirror, types);
        }
    }

    public String elementToTypeName(Element element, TypeMirror typeMirror, Types types) {
        String typeName;
        String typeMirrorName = getTypeMirrorName(typeMirror, types);
        if (element.getAnnotation(Id.class) != null) {
            typeName = "ID";
        } else if (typeMirrorName.equals(int.class.getCanonicalName()) ||
                typeMirrorName.equals(long.class.getCanonicalName()) ||
                typeMirrorName.equals(short.class.getCanonicalName()) ||
                typeMirrorName.equals(byte.class.getCanonicalName()) ||
                typeMirrorName.equals(Integer.class.getCanonicalName()) ||
                typeMirrorName.equals(Long.class.getCanonicalName()) ||
                typeMirrorName.equals(Short.class.getCanonicalName()) ||
                typeMirrorName.equals(Byte.class.getCanonicalName())) {
            typeName = "Int";
        } else if (typeMirrorName.equals(float.class.getCanonicalName()) ||
                typeMirrorName.equals(double.class.getCanonicalName()) ||
                typeMirrorName.equals(Float.class.getCanonicalName()) ||
                typeMirrorName.equals(Double.class.getCanonicalName())) {
            typeName = "Float";
        } else if (typeMirrorName.equals(char.class.getCanonicalName()) ||
                typeMirrorName.equals(String.class.getCanonicalName()) ||
                typeMirrorName.equals(Character.class.getCanonicalName())) {
            typeName = "String";
        } else if (typeMirrorName.equals(boolean.class.getCanonicalName()) ||
                typeMirrorName.equals(Boolean.class.getCanonicalName())) {
            typeName = "Boolean";
        } else if (typeMirrorName.equals(BigInteger.class.getCanonicalName())) {
            typeName = "BigInteger";
        } else if (typeMirrorName.equals(BigDecimal.class.getCanonicalName())) {
            typeName = "BigDecimal";
        } else if (typeMirrorName.equals(LocalDate.class.getCanonicalName())) {
            typeName = "Date";
        } else if (typeMirrorName.equals(LocalTime.class.getCanonicalName())) {
            typeName = "Time";
        } else if (typeMirrorName.equals(LocalDateTime.class.getCanonicalName())) {
            typeName = "DateTime";
        } else if (typeMirrorName.equals(Collection.class.getCanonicalName()) ||
                typeMirrorName.equals(List.class.getCanonicalName()) ||
                typeMirrorName.equals(Set.class.getCanonicalName())) {
            typeName = "[".concat(elementToTypeName(element, ((DeclaredType) typeMirror).getTypeArguments().get(0), types)).concat("]");
        } else {
            typeName = getNameFromElement(types.asElement(typeMirror));
        }

        if (element.getAnnotation(NonNull.class) != null) {
            return typeName.concat("!");
        } else {
            return typeName;
        }
    }

    public String executableElementToInputTypeName(ExecutableElement executableElement, Types types) {
        return elementToInputTypeName(executableElement, executableElement.getReturnType(), types);
    }

    public String variableElementToInputTypeName(VariableElement variableElement, Types types) {
        return elementToInputTypeName(variableElement, variableElement.asType(), types);
    }

    public String elementToInputTypeName(Element element, TypeMirror typeMirror, Types types) {
        String typeName;
        String typeMirrorName = getTypeMirrorName(typeMirror, types);
        if (element.getAnnotation(Id.class) != null) {
            typeName = "ID";
        } else if (typeMirrorName.equals(int.class.getCanonicalName()) ||
                typeMirrorName.equals(long.class.getCanonicalName()) ||
                typeMirrorName.equals(short.class.getCanonicalName()) ||
                typeMirrorName.equals(byte.class.getCanonicalName()) ||
                typeMirrorName.equals(Integer.class.getCanonicalName()) ||
                typeMirrorName.equals(Long.class.getCanonicalName()) ||
                typeMirrorName.equals(Short.class.getCanonicalName()) ||
                typeMirrorName.equals(Byte.class.getCanonicalName())) {
            typeName = "Int";
        } else if (typeMirrorName.equals(float.class.getCanonicalName()) ||
                typeMirrorName.equals(double.class.getCanonicalName()) ||
                typeMirrorName.equals(Float.class.getCanonicalName()) ||
                typeMirrorName.equals(Double.class.getCanonicalName())) {
            typeName = "Float";
        } else if (typeMirrorName.equals(char.class.getCanonicalName()) ||
                typeMirrorName.equals(String.class.getCanonicalName()) ||
                typeMirrorName.equals(Character.class.getCanonicalName())) {
            typeName = "String";
        } else if (typeMirrorName.equals(boolean.class.getCanonicalName()) ||
                typeMirrorName.equals(Boolean.class.getCanonicalName())) {
            typeName = "Boolean";
        } else if (typeMirrorName.equals(BigInteger.class.getCanonicalName())) {
            typeName = "BigInteger";
        } else if (typeMirrorName.equals(BigDecimal.class.getCanonicalName())) {
            typeName = "BigDecimal";
        } else if (typeMirrorName.equals(LocalDate.class.getCanonicalName())) {
            typeName = "Date";
        } else if (typeMirrorName.equals(LocalTime.class.getCanonicalName())) {
            typeName = "Time";
        } else if (typeMirrorName.equals(LocalDateTime.class.getCanonicalName())) {
            typeName = "DateTime";
        } else if (types.asElement(typeMirror).getKind().equals(ElementKind.ENUM)) {
            typeName = getNameFromElement(types.asElement(typeMirror));
        } else if (typeMirrorName.equals(Collection.class.getCanonicalName()) ||
                typeMirrorName.equals(List.class.getCanonicalName()) ||
                typeMirrorName.equals(Set.class.getCanonicalName())) {

            if (element.getKind().equals(ElementKind.METHOD)) {
                typeName = "[".concat(elementToInputTypeName(element, ((DeclaredType) ((ExecutableElement) element).getReturnType()).getTypeArguments().get(0), types)).concat("]");
            } else if (element.getKind().equals(ElementKind.FIELD) || element.getKind().equals(ElementKind.PARAMETER)) {
                typeName = "[".concat(elementToInputTypeName(element, ((DeclaredType) element.asType()).getTypeArguments().get(0), types)).concat("]");
            } else {
                throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE.bind(typeMirrorName));
            }
        } else {
            if (typeMirrorName.endsWith(INPUT_SUFFIX)) {
                typeName = getNameFromElement(types.asElement(typeMirror));
            } else {
                typeName = getNameFromElement(types.asElement(typeMirror)).concat(INPUT_SUFFIX);
            }
        }

        if (element.getAnnotation(NonNull.class) != null) {
            return typeName.concat("!");
        } else {
            return typeName;
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
                                    .setTypeName(variableElementToTypeName(variableElement, typeUtils))
                                    .setDescription(getDescriptionFromElement(variableElement))
                    )
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        } else {
            return new LinkedHashSet<>();
        }
    }

    public String getTypeMirrorName(TypeMirror typeMirror, Types types) {
        if (typeMirror.getKind().isPrimitive()) {
            return typeMirror.toString();
        }
        return ((TypeElement) types.asElement(typeMirror)).getQualifiedName().toString();
    }
}
