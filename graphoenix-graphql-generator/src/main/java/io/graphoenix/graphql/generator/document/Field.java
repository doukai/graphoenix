package io.graphoenix.graphql.generator.document;

import org.eclipse.microprofile.graphql.Id;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class Field {

    private final STGroup stGroupFile = new STGroupFile("stg/document/Field.stg");

    private String name;
    private List<InputValue> arguments;
    private String typeName;
    private List<String> directives;
    private String description;

    public String getName() {
        return name;
    }

    public Field setName(String name) {
        this.name = name;
        return this;
    }

    public List<InputValue> getArguments() {
        return arguments;
    }

    public Field setArguments(List<InputValue> arguments) {
        this.arguments = arguments;
        return this;
    }

    public Field addArguments(List<InputValue> arguments) {
        if (this.arguments == null) {
            this.arguments = arguments;
        } else {
            this.arguments.addAll(arguments);
        }
        return this;
    }

    public Field addArgument(InputValue argument) {
        if (this.arguments == null) {
            this.arguments = new ArrayList<>();
        }
        this.arguments.add(argument);
        return this;
    }

    public String getTypeName() {
        return typeName;
    }

    public Field setTypeName(String typeName) {
        this.typeName = typeName;
        return this;
    }

    public Field setType(VariableElement variableElement, TypeElement typeElement) {
        this.typeName = typeElementToTypeName(variableElement, typeElement);
        return this;
    }

    public List<String> getDirectives() {
        return directives;
    }

    public Field setDirectives(List<String> directives) {
        this.directives = directives;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public Field setDescription(String description) {
        this.description = description;
        return this;
    }

    private String typeElementToTypeName(VariableElement variableElement, TypeElement typeElement) {
        if (variableElement.getAnnotation(Id.class) != null) {
            return "ID";
        } else if (typeElement.getQualifiedName().toString().equals(Integer.class.getName()) ||
                typeElement.getQualifiedName().toString().equals(Short.class.getName()) ||
                typeElement.getQualifiedName().toString().equals(Byte.class.getName())) {
            return "Int";
        } else if (typeElement.getQualifiedName().toString().equals(Float.class.getName()) ||
                typeElement.getQualifiedName().toString().equals(Double.class.getName())) {
            return "Float";
        } else if (typeElement.getQualifiedName().toString().equals(String.class.getName()) ||
                typeElement.getQualifiedName().toString().equals(Character.class.getName())) {
            return "String";
        } else if (typeElement.getQualifiedName().toString().equals(Boolean.class.getName())) {
            return "Boolean";
        } else if (typeElement.getQualifiedName().toString().equals(Collection.class.getName()) ||
                typeElement.getQualifiedName().toString().equals(List.class.getName()) ||
                typeElement.getQualifiedName().toString().equals(Set.class.getName())) {

            typeElement.getTypeParameters().get(0);
            ((DeclaredType) variableElement.asType()).getTypeArguments();
            return "Int";
        } else {
            return typeElement.getQualifiedName().toString();
        }
    }

    @Override
    public String toString() {
        ST st = stGroupFile.getInstanceOf("fieldDefinition");
        st.add("filed", this);
        return st.render();
    }
}
