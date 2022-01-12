package io.graphoenix.graphql.generator.translator;

import io.graphoenix.graphql.generator.document.Field;
import io.graphoenix.graphql.generator.document.ObjectType;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Types;
import java.util.stream.Collectors;

public class JavaElementToObject {

    public String buildObject(TypeElement typeElement, Types typeUtils) {
        return new ObjectType()
                .setFields(
                        typeElement.getEnclosedElements().stream()
                                .filter(element -> element.getKind().equals(ElementKind.FIELD))
                                .map(element -> new Field().setName(element.getSimpleName().toString()).setType((VariableElement) element, (TypeElement) typeUtils.asElement(element.asType())))
                                .collect(Collectors.toList())
                )
                .setInterfaces(
                        typeElement.getInterfaces().stream()
                                .map(typeMirror -> typeUtils.asElement(typeMirror).getSimpleName().toString())
                                .collect(Collectors.toList())
                )
                .toString();
    }
}
