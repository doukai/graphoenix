package io.graphoenix.graphql.generator.translator;

import com.google.common.base.CaseFormat;
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
import org.eclipse.microprofile.graphql.Id;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.NonNull;
import org.eclipse.microprofile.graphql.Source;
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
        Name name = element.getAnnotation(Name.class);
        if (name != null) {
            return name.value();
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
        if (((TypeElement) types.asElement(executableElement.getReturnType())).getQualifiedName().contentEquals(Flux.class.getName())) {
            typeMirror = ((DeclaredType) (executableElement).getReturnType()).getTypeArguments().get(0);
            return "[".concat(elementToTypeName(executableElement, typeMirror, types)).concat("]");
        } else if (((TypeElement) types.asElement(executableElement.getReturnType())).getQualifiedName().contentEquals(Mono.class.getName())) {
            typeMirror = ((DeclaredType) (executableElement).getReturnType()).getTypeArguments().get(0);
            return elementToTypeName(executableElement, typeMirror, types);
        } else if (((TypeElement) types.asElement(executableElement.getReturnType())).getQualifiedName().contentEquals(PublisherBuilder.class.getName())) {
            typeMirror = ((DeclaredType) (executableElement).getReturnType()).getTypeArguments().get(0);
            return elementToTypeName(executableElement, typeMirror, types);
        } else {
            typeMirror = executableElement.getReturnType();
            return elementToTypeName(executableElement, typeMirror, types);
        }
    }

    public String variableElementToTypeName(VariableElement variableElement, Types types) {
        TypeMirror typeMirror;
        if (((TypeElement) types.asElement(variableElement.asType())).getQualifiedName().contentEquals(Flux.class.getName())) {
            typeMirror = ((DeclaredType) variableElement.asType()).getTypeArguments().get(0);
            return "[".concat(elementToTypeName(variableElement, typeMirror, types)).concat("]");
        } else if (((TypeElement) types.asElement(variableElement.asType())).getQualifiedName().contentEquals(Mono.class.getName())) {
            typeMirror = ((DeclaredType) variableElement.asType()).getTypeArguments().get(0);
            return elementToTypeName(variableElement, typeMirror, types);
        } else if (((TypeElement) types.asElement(variableElement.asType())).getQualifiedName().contentEquals(PublisherBuilder.class.getName())) {
            typeMirror = ((DeclaredType) variableElement.asType()).getTypeArguments().get(0);
            return elementToTypeName(variableElement, typeMirror, types);
        } else {
            typeMirror = variableElement.asType();
            return elementToTypeName(variableElement, typeMirror, types);
        }
    }

    public String elementToTypeName(Element element, TypeMirror typeMirror, Types types) {
        String typeName;
        String qualifiedName = ((TypeElement) types.asElement(typeMirror)).getQualifiedName().toString();
        if (element.getAnnotation(Id.class) != null) {
            typeName = "ID";
        } else if (element.asType().toString().equals(int.class.getName()) ||
                element.asType().toString().equals(long.class.getName()) ||
                element.asType().toString().equals(short.class.getName()) ||
                element.asType().toString().equals(byte.class.getName()) ||
                qualifiedName.equals(Integer.class.getName()) ||
                qualifiedName.equals(Long.class.getName()) ||
                qualifiedName.equals(Short.class.getName()) ||
                qualifiedName.equals(Byte.class.getName())) {
            typeName = "Int";
        } else if (element.asType().toString().equals(float.class.getName()) ||
                element.asType().toString().equals(double.class.getName()) ||
                qualifiedName.equals(Float.class.getName()) ||
                qualifiedName.equals(Double.class.getName())) {
            typeName = "Float";
        } else if (element.asType().toString().equals(char.class.getName()) ||
                qualifiedName.equals(String.class.getName()) ||
                qualifiedName.equals(Character.class.getName())) {
            typeName = "String";
        } else if (element.asType().toString().equals(boolean.class.getName()) ||
                qualifiedName.equals(Boolean.class.getName())) {
            typeName = "Boolean";
        } else if (qualifiedName.equals(BigInteger.class.getName())) {
            typeName = "BigInteger";
        } else if (qualifiedName.equals(BigDecimal.class.getName())) {
            typeName = "BigDecimal";
        } else if (qualifiedName.equals(LocalDate.class.getName())) {
            typeName = "Date";
        } else if (qualifiedName.equals(LocalTime.class.getName())) {
            typeName = "Time";
        } else if (qualifiedName.equals(LocalDateTime.class.getName())) {
            typeName = "DateTime";
        } else if (qualifiedName.equals(Collection.class.getName()) ||
                qualifiedName.equals(List.class.getName()) ||
                qualifiedName.equals(Set.class.getName())) {
            typeName = "[".concat(elementToTypeName(element, ((DeclaredType) typeMirror).getTypeArguments().get(0), types)).concat("]");
        } else {
            typeName = types.asElement(typeMirror).getSimpleName().toString();
        }

        if (element.getAnnotation(NonNull.class) != null) {
            return typeName.concat("!");
        } else {
            return typeName;
        }
    }

    public String executableElementToInputTypeName(ExecutableElement executableElement, Types types) {
        TypeElement typeElement = (TypeElement) types.asElement(executableElement.getReturnType());
        return elementToInputTypeName(executableElement, typeElement, types);
    }

    public String variableElementToInputTypeName(VariableElement variableElement, Types types) {
        TypeElement typeElement = (TypeElement) types.asElement(variableElement.asType());
        return elementToInputTypeName(variableElement, typeElement, types);
    }

    public String elementToInputTypeName(Element element, TypeElement typeElement, Types types) {
        String typeName;
        if (element.getAnnotation(Id.class) != null) {
            typeName = "ID";
        } else if (element.asType().toString().equals(int.class.getName()) ||
                element.asType().toString().equals(long.class.getName()) ||
                element.asType().toString().equals(short.class.getName()) ||
                element.asType().toString().equals(byte.class.getName()) ||
                typeElement.getQualifiedName().toString().equals(Integer.class.getName()) ||
                typeElement.getQualifiedName().toString().equals(Long.class.getName()) ||
                typeElement.getQualifiedName().toString().equals(Short.class.getName()) ||
                typeElement.getQualifiedName().toString().equals(Byte.class.getName())) {
            typeName = "Int";
        } else if (element.asType().toString().equals(float.class.getName()) ||
                element.asType().toString().equals(double.class.getName()) ||
                typeElement.getQualifiedName().toString().equals(Float.class.getName()) ||
                typeElement.getQualifiedName().toString().equals(Double.class.getName())) {
            typeName = "Float";
        } else if (element.asType().toString().equals(char.class.getName()) ||
                typeElement.getQualifiedName().toString().equals(String.class.getName()) ||
                typeElement.getQualifiedName().toString().equals(Character.class.getName())) {
            typeName = "String";
        } else if (element.asType().toString().equals(boolean.class.getName()) ||
                typeElement.getQualifiedName().toString().equals(Boolean.class.getName())) {
            typeName = "Boolean";
        } else if (typeElement.getQualifiedName().toString().equals(BigInteger.class.getName())) {
            typeName = "BigInteger";
        } else if (typeElement.getQualifiedName().toString().equals(BigDecimal.class.getName())) {
            typeName = "BigDecimal";
        } else if (typeElement.getQualifiedName().toString().equals(LocalDate.class.getName())) {
            typeName = "Date";
        } else if (typeElement.getQualifiedName().toString().equals(LocalTime.class.getName())) {
            typeName = "Time";
        } else if (typeElement.getQualifiedName().toString().equals(LocalDateTime.class.getName())) {
            typeName = "DateTime";
        } else if (typeElement.getKind().equals(ElementKind.ENUM)) {
            typeName = typeElement.getSimpleName().toString();
        } else if (typeElement.getQualifiedName().toString().equals(Collection.class.getName()) ||
                typeElement.getQualifiedName().toString().equals(List.class.getName()) ||
                typeElement.getQualifiedName().toString().equals(Set.class.getName())) {

            if (element.getKind().equals(ElementKind.METHOD)) {
                typeName = "[".concat(elementToInputTypeName(element, (TypeElement) types.asElement(((DeclaredType) ((ExecutableElement) element).getReturnType()).getTypeArguments().get(0)), types)).concat("]");
            } else if (element.getKind().equals(ElementKind.FIELD) || element.getKind().equals(ElementKind.PARAMETER)) {
                typeName = "[".concat(elementToInputTypeName(element, (TypeElement) types.asElement(((DeclaredType) element.asType()).getTypeArguments().get(0)), types)).concat("]");
            } else {
                throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE.bind(typeElement.getQualifiedName().toString()));
            }
        } else {
            if (typeElement.getQualifiedName().toString().endsWith(INPUT_SUFFIX)) {
                typeName = typeElement.getSimpleName().toString();
            } else {
                typeName = typeElement.getSimpleName().toString().concat(INPUT_SUFFIX);
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
                            new InputValue().setName(getNameFromElement(variableElement))
                                    .setDefaultValue(getDefaultValueFromElement(variableElement))
                                    .setTypeName(variableElementToTypeName(variableElement, typeUtils))
                                    .setDescription(getDescriptionFromElement(variableElement))
                    )
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        } else {
            return new LinkedHashSet<>();
        }
    }
}
