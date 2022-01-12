package io.graphoenix.graphql.generator.translator;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.graphql.generator.operation.Field;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import org.eclipse.microprofile.graphql.Id;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.NonNull;

import javax.inject.Inject;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.Types;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ElementManager {
    private final IGraphQLDocumentManager manager;

    @Inject
    public ElementManager(IGraphQLDocumentManager manager) {
        this.manager = manager;
    }

    public List<Field> buildFields(String typeName, int level, int layers) {
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
                .collect(Collectors.toList());
    }

    public Stream<GraphqlParser.FieldDefinitionContext> getFields(String typeName, int level, int layers) {
        return manager.getFields(typeName)
                .filter(fieldDefinitionContext -> level < layers ||
                        level == layers && (manager.isScaLar(manager.getFieldTypeName(fieldDefinitionContext.type())) || manager.isEnum(manager.getFieldTypeName(fieldDefinitionContext.type())))
                );
    }

    public VariableElement getParameterFromExecutableElement(ExecutableElement executableElement, String name) {
        return executableElement.getParameters().stream()
                .filter(parameter -> parameter.getSimpleName().toString().equals(name))
                .findFirst()
                .orElseThrow();
    }

    public String getNameFormElement(Element element) {
        Name name = element.getAnnotation(Name.class);
        if (name != null) {
            return name.value();
        } else {
            return element.getSimpleName().toString();
        }
    }

    public String typeElementToTypeName(VariableElement variableElement, Types types) {
        TypeElement typeElement = (TypeElement) types.asElement(variableElement.asType());
        return typeElementToTypeName(variableElement, typeElement, types);
    }

    public String typeElementToTypeName(VariableElement variableElement, TypeElement typeElement, Types types) {
        String typeName;
        if (variableElement.getAnnotation(Id.class) != null) {
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
            typeName = "[".concat(typeElementToTypeName(variableElement, (TypeElement) types.asElement(((DeclaredType) variableElement.asType()).getTypeArguments().get(0)), types)).concat("]");
        } else {
            typeName = typeElement.getSimpleName().toString();
        }

        if (variableElement.getAnnotation(NonNull.class) != null) {
            return typeName.concat("!");
        } else {
            return typeName;
        }
    }
}
