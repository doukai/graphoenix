package io.graphoenix.graphql.generator.translator;

import com.google.common.base.CaseFormat;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.error.ElementProblem;
import io.graphoenix.graphql.generator.operation.Field;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.graphql.DefaultValue;
import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.Id;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.NonNull;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.Types;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.graphoenix.core.error.ElementErrorType.EXPRESSION_VARIABLE_PARAMETER_NOT_EXIST;

@ApplicationScoped
public class ElementManager {
    private final IGraphQLDocumentManager manager;

    @Inject
    public ElementManager(IGraphQLDocumentManager manager) {
        this.manager = manager;
    }

    public Set<Field> buildFields(String typeName, int level, int layers) {
        return getFields(typeName, level, layers)
                .map(fieldDefinitionContext ->
                        {
                            Field field = new Field().setName(fieldDefinitionContext.name().getText());
                            if (level <= layers) {
                                field.setFields(buildFields(manager.getFieldTypeName(fieldDefinitionContext.type()), level + 1, layers));
                            }
                            return field;
                        }
                )
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Stream<GraphqlParser.FieldDefinitionContext> getFields(String typeName, int level, int layers) {
        return manager.getFields(typeName)
                .filter(fieldDefinitionContext -> level < layers ||
                        level == layers && (manager.isScalar(manager.getFieldTypeName(fieldDefinitionContext.type())) || manager.isEnum(manager.getFieldTypeName(fieldDefinitionContext.type())))
                );
    }

    public VariableElement getParameterFromExecutableElement(ExecutableElement executableElement, String name) {
        return executableElement.getParameters().stream()
                .filter(parameter -> parameter.getSimpleName().toString().equals(name))
                .findFirst()
                .orElseThrow(() -> new ElementProblem(EXPRESSION_VARIABLE_PARAMETER_NOT_EXIST.bind(executableElement.getSimpleName(), name)));
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
        TypeElement typeElement = (TypeElement) types.asElement(executableElement.getReturnType());
        return elementToTypeName(executableElement, typeElement, types);
    }

    public String variableElementToTypeName(VariableElement variableElement, Types types) {
        TypeElement typeElement = (TypeElement) types.asElement(variableElement.asType());
        return elementToTypeName(variableElement, typeElement, types);
    }

    public String elementToTypeName(Element element, TypeElement typeElement, Types types) {
        String typeName;
        if (element.getAnnotation(Id.class) != null) {
            typeName = "ID";
        } else if (typeElement.getQualifiedName().toString().equals(Integer.class.getName()) ||
                typeElement.getQualifiedName().toString().equals(Short.class.getName()) ||
                typeElement.getQualifiedName().toString().equals(Byte.class.getName())) {
            typeName = "Int";
        } else if (typeElement.getQualifiedName().toString().equals(Float.class.getName()) ||
                typeElement.getQualifiedName().toString().equals(Double.class.getName())) {
            typeName = "Float";
        } else if (typeElement.getQualifiedName().toString().equals(String.class.getName()) ||
                typeElement.getQualifiedName().toString().equals(Character.class.getName())) {
            typeName = "String";
        } else if (typeElement.getQualifiedName().toString().equals(Boolean.class.getName())) {
            typeName = "Boolean";
        } else if (typeElement.getQualifiedName().toString().equals(Collection.class.getName()) ||
                typeElement.getQualifiedName().toString().equals(List.class.getName()) ||
                typeElement.getQualifiedName().toString().equals(Set.class.getName())) {

            if (element.getKind().equals(ElementKind.METHOD)) {
                typeName = "[".concat(elementToTypeName(element, (TypeElement) types.asElement(((DeclaredType) ((ExecutableElement) element).getReturnType()).getTypeArguments().get(0)), types)).concat("]");
            } else if (element.getKind().equals(ElementKind.FIELD)) {
                typeName = "[".concat(elementToTypeName(element, (TypeElement) types.asElement(((DeclaredType) element.asType()).getTypeArguments().get(0)), types)).concat("]");
            } else {
                throw new RuntimeException();
            }
        } else {
            typeName = typeElement.getSimpleName().toString();
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
        } else if (typeElement.getQualifiedName().toString().equals(Integer.class.getName()) ||
                typeElement.getQualifiedName().toString().equals(Short.class.getName()) ||
                typeElement.getQualifiedName().toString().equals(Byte.class.getName())) {
            typeName = "Int";
        } else if (typeElement.getQualifiedName().toString().equals(Float.class.getName()) ||
                typeElement.getQualifiedName().toString().equals(Double.class.getName())) {
            typeName = "Float";
        } else if (typeElement.getQualifiedName().toString().equals(String.class.getName()) ||
                typeElement.getQualifiedName().toString().equals(Character.class.getName())) {
            typeName = "String";
        } else if (typeElement.getQualifiedName().toString().equals(Boolean.class.getName())) {
            typeName = "Boolean";
        } else if (typeElement.getQualifiedName().toString().equals(Collection.class.getName()) ||
                typeElement.getQualifiedName().toString().equals(List.class.getName()) ||
                typeElement.getQualifiedName().toString().equals(Set.class.getName())) {

            if (element.getKind().equals(ElementKind.METHOD)) {
                typeName = "[".concat(elementToInputTypeName(element, (TypeElement) types.asElement(((DeclaredType) ((ExecutableElement) element).getReturnType()).getTypeArguments().get(0)), types)).concat("]");
            } else if (element.getKind().equals(ElementKind.FIELD)) {
                typeName = "[".concat(elementToInputTypeName(element, (TypeElement) types.asElement(((DeclaredType) element.asType()).getTypeArguments().get(0)), types)).concat("]");
            } else {
                throw new RuntimeException();
            }
        } else {
            typeName = typeElement.getSimpleName().toString().concat("Input");
        }

        if (element.getAnnotation(NonNull.class) != null) {
            return typeName.concat("!");
        } else {
            return typeName;
        }
    }
}
