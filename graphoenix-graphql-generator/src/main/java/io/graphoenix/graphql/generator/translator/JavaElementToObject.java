package io.graphoenix.graphql.generator.translator;

import io.graphoenix.graphql.generator.document.Field;
import io.graphoenix.graphql.generator.document.ObjectType;
import org.eclipse.microprofile.graphql.Ignore;

import javax.inject.Inject;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Types;
import java.util.stream.Collectors;

public class JavaElementToObject {

    private final ElementManager elementManager;

    @Inject
    public JavaElementToObject(ElementManager elementManager) {
        this.elementManager = elementManager;
    }

    public String buildObject(TypeElement typeElement, Types typeUtils) {
        return new ObjectType()
                .setName(elementManager.getNameFromElement(typeElement))
                .setDescription(elementManager.getDescriptionFromElement(typeElement))
                .setFields(
                        typeElement.getEnclosedElements().stream()
                                .filter(element -> element.getKind().equals(ElementKind.FIELD))
                                .filter(element -> element.getAnnotation(Ignore.class) == null)
                                .map(element ->
                                        new Field()
                                                .setName(elementManager.getNameFromElement(element))
                                                .setTypeName(elementManager.variableElementToTypeName((VariableElement) element, typeUtils))
                                                .setDescription(elementManager.getDescriptionFromElement(element))
                                )
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
